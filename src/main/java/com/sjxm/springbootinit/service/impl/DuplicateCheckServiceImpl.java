package com.sjxm.springbootinit.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sjxm.springbootinit.common.ErrorCode;
import com.sjxm.springbootinit.exception.BusinessException;
import com.sjxm.springbootinit.mapper.DuplicateCheckMapper;
import com.sjxm.springbootinit.mapper.MqMessageMapper;
import com.sjxm.springbootinit.model.entity.DuplicateCheck;
import com.sjxm.springbootinit.model.entity.Homework;
import com.sjxm.springbootinit.model.entity.MqMessage;
import com.sjxm.springbootinit.model.enums.CheckStatusEnum;
import com.sjxm.springbootinit.service.DuplicateCheckService;
import com.sjxm.springbootinit.service.HomeworkService;
import com.sjxm.springbootinit.service.MqMessageService;
import com.sjxm.springbootinit.utils.AliOssUtil;
import com.sjxm.springbootinit.utils.VideoPostGenerateUtil;
import com.sjxm.springbootinit.utils.similarity.CodeSimilarityUtil;
import com.sjxm.springbootinit.utils.similarity.ConsineSimilarityUtil;
import com.sjxm.springbootinit.utils.similarity.SimHashUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
* @author sijixiamu
* @description 针对表【duplicate_check】的数据库操作Service实现
* @createDate 2025-01-02 11:33:17
*/
@Service
@Slf4j
@RocketMQMessageListener(
        consumerGroup = "homework_duplicate_check_consumer",
        topic = "homework_duplicate_check_topic",
        consumeMode = ConsumeMode.CONCURRENTLY,
        messageModel = MessageModel.CLUSTERING
)
public class DuplicateCheckServiceImpl extends ServiceImpl<DuplicateCheckMapper, DuplicateCheck>
    implements DuplicateCheckService, RocketMQListener<MessageExt> {

    @Resource
    private HomeworkService homeworkService;

    @Resource
    private ThreadPoolExecutor duplicateCheckThreadPool;

    @Resource
    private MqMessageService mqMessageService;

    @Resource
    private AliOssUtil aliOssUtil;

    void duplicateCheck(Homework source,Homework target){

        DuplicateCheck duplicateCheck = new DuplicateCheck();
        duplicateCheck.setSourceId(source.getId());
        duplicateCheck.setTargetId(target.getId());
        //引入线程池异步
        CompletableFuture<BigDecimal> briefFuture = CompletableFuture.supplyAsync(()->BigDecimal.valueOf(ConsineSimilarityUtil.calculate(source.getBrief(), target.getBrief())),duplicateCheckThreadPool);
        CompletableFuture<BigDecimal> backgroundFuture = CompletableFuture.supplyAsync(()->BigDecimal.valueOf(ConsineSimilarityUtil.calculate(source.getBackground(), target.getBackground())),duplicateCheckThreadPool);
        CompletableFuture<BigDecimal> systemDesignFuture = CompletableFuture.supplyAsync(()->BigDecimal.valueOf(ConsineSimilarityUtil.calculate(source.getSystemDesign(), target.getSystemDesign())),duplicateCheckThreadPool);
        CompletableFuture<BigDecimal> wordFuture = CompletableFuture.supplyAsync(()->{
            try{
                return BigDecimal.valueOf(SimHashUtil.calculateGroupSimilarity(source.getAttachmentWord(), target.getAttachmentWord(), "docx"));
            }catch (Exception e){
                throw new BusinessException(ErrorCode.DUPLICATE_CHECK_ERROR,e.getMessage());
            }
        },duplicateCheckThreadPool);
        CompletableFuture<BigDecimal> pdfFuture = CompletableFuture.supplyAsync(()->{
            try{
                return BigDecimal.valueOf(SimHashUtil.calculateGroupSimilarity(source.getAttachmentPdf(), target.getAttachmentPdf(), "pdf"));
            }catch (Exception e){
                throw new BusinessException(ErrorCode.DUPLICATE_CHECK_ERROR,e.getMessage());
            }
        },duplicateCheckThreadPool);
        CompletableFuture<BigDecimal> sourceFuture = CompletableFuture.supplyAsync(()->BigDecimal.valueOf(CodeSimilarityUtil.calculateCodeSimilarity(source.getAttachmentSource(), target.getAttachmentSource())),duplicateCheckThreadPool);

        try{
            duplicateCheck.setBriefValue(briefFuture.get());
            duplicateCheck.setBackgroundValue(backgroundFuture.get());
            duplicateCheck.setSystemDesignValue(systemDesignFuture.get());
            duplicateCheck.setWordValue(wordFuture.get());
            duplicateCheck.setPdfValue(pdfFuture.get());
            duplicateCheck.setSourceValue(sourceFuture.get());


            BigDecimal pct1 = new BigDecimal("0.1");
            BigDecimal pct2 = new BigDecimal("0.2");

            BigDecimal similarity = duplicateCheck.getBriefValue().multiply(pct1)
                    .add(duplicateCheck.getBackgroundValue().multiply(pct1))
                    .add(duplicateCheck.getSystemDesignValue().multiply(pct2))
                    .add(duplicateCheck.getWordValue().multiply(pct2))
                    .add(duplicateCheck.getPdfValue().multiply(pct2))
                    .add(duplicateCheck.getSourceValue().multiply(pct2));

            duplicateCheck.setSimilarity(similarity);
            this.save(duplicateCheck);
        }catch (Exception e){
            throw new BusinessException(ErrorCode.DUPLICATE_CHECK_ERROR,e.getMessage());
        }


    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void getSimilarity(Long homeworkId) {
        Homework source = homeworkService.getById(homeworkId);
        //生成封面
        try{
            CompletableFuture<String> videoPostFuture = CompletableFuture.supplyAsync(() -> {
                String attachmentMp4 = source.getAttachmentMp4();
                if (!StrUtil.isBlankIfStr(attachmentMp4)) {
                    String[] videoPaths = attachmentMp4.split(",");
                    if (videoPaths.length != 0) {
                        return VideoPostGenerateUtil.extractAndUploadThumbnail(videoPaths[0], aliOssUtil);
                    }
                }
                return "";
            }, duplicateCheckThreadPool);

            source.setPost(videoPostFuture.get());
            homeworkService.updateById(source);
        }catch (ExecutionException | InterruptedException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"生成视频封面失败"+e.getMessage());
        }


        if(ObjectUtil.isNull(source)){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        List<Homework> homeworkList = homeworkService.list();
            for (Homework homework : homeworkList) {
                if(!Objects.equals(homework.getId(), homeworkId)){
                    duplicateCheck(source,homework);
                }
            }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(MessageExt message) {
        log.info("\n\n\nmessageExt:{}\n\n\n",message);
        String msgId = message.getMsgId();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        Long homeworkId = Long.parseLong(body);
        try {
            // 消息幂等性检查
            LambdaQueryWrapper<MqMessage> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(MqMessage::getMsgId, msgId);
            MqMessage mqMessage = mqMessageService.getOne(queryWrapper);

            if (mqMessage != null) {
                if (mqMessage.getStatus().equals(CheckStatusEnum.COMPLETED.getCode())) {
                    log.info("消息已处理，忽略，msgId={}", msgId);
                    return;
                }
                if (mqMessage.getStatus().equals(CheckStatusEnum.PROCESSING.getCode())) {
                    log.info("消息正在处理中，忽略，msgId={}", msgId);
                    return;
                }
            }else{
                mqMessage = new MqMessage();
                mqMessage.setMsgId(msgId);
                mqMessage.setMessageBody(String.valueOf(homeworkId));
                mqMessage.setBusinessKey("homework_"+homeworkId);
                mqMessage.setStatus(CheckStatusEnum.WAITING.getCode());
                mqMessageService.save(mqMessage);
            }

            mqMessage.setStatus(CheckStatusEnum.PROCESSING.getCode());
            mqMessageService.updateById(mqMessage);

            // 更新作业查重状态
            updateHomeworkCheckStatus(homeworkId, CheckStatusEnum.PROCESSING);

            // 执行查重
            getSimilarity(homeworkId);

            mqMessage.setStatus(CheckStatusEnum.COMPLETED.getCode());
            mqMessageService.updateById(mqMessage);

            // 更新状态为完成
            updateHomeworkCheckStatus(homeworkId, CheckStatusEnum.COMPLETED);

            log.info("作业查重完成，homeworkId={}", homeworkId);
        } catch (Exception e) {
            log.error("处理查重消息失败，msgId=" + msgId, e);
            // 获取重试次数
            int reconsumeTimes = message.getReconsumeTimes();

            // 更新消息状态为失败
            if (reconsumeTimes >= 3) {
                MqMessage mqMessage = new MqMessage();
                mqMessage.setMsgId(msgId);
                mqMessage.setStatus(CheckStatusEnum.FAILED.getCode());

                LambdaUpdateWrapper<MqMessage> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(MqMessage::getMsgId, msgId);
                mqMessageService.update(mqMessage, updateWrapper);

                log.error("查重消息处理失败超过最大重试次数，msgId={}", msgId);
                return;
            }

            // 抛出异常进行重试
            throw new BusinessException(ErrorCode.DUPLICATE_CHECK_ERROR,
                    "查重失败：" + e.getMessage());
        }
    }

    private void updateHomeworkCheckStatus(Long homeworkId, CheckStatusEnum status) {
        try{
            Homework homework = homeworkService.getById(homeworkId);
            if(homework!=null){
                homework.setCheckStatus(status.getCode());
                homeworkService.updateById(homework);
            }
        }catch (Exception e){
            log.error("更新作业查重状态失败，homeworkId=" + homeworkId, e);
        }
    }
}




