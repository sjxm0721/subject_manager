package com.sjxm.springbootinit.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sjxm.springbootinit.common.ErrorCode;
import com.sjxm.springbootinit.exception.BusinessException;
import com.sjxm.springbootinit.mapper.DuplicateCheckMapper;
import com.sjxm.springbootinit.model.entity.DuplicateCheck;
import com.sjxm.springbootinit.model.entity.Homework;
import com.sjxm.springbootinit.model.entity.MqMessage;
import com.sjxm.springbootinit.model.enums.CheckStatusEnum;
import com.sjxm.springbootinit.service.DuplicateCheckService;
import com.sjxm.springbootinit.service.HomeworkService;
import com.sjxm.springbootinit.service.MqMessageService;
import com.sjxm.springbootinit.utils.AliOssUtil;
import com.sjxm.springbootinit.utils.RedisUtil;
import com.sjxm.springbootinit.utils.VideoPostGenerateUtil;
import com.sjxm.springbootinit.utils.similarity.CodeSimilarityUtil;
import com.sjxm.springbootinit.utils.similarity.ConsineSimilarityUtil;
import com.sjxm.springbootinit.utils.similarity.SimHashUtil;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
* @author sijixiamu
* @description 针对表【duplicate_check】的数据库操作Service实现
* @createDate 2025-01-02 11:33:17
*/
@Service
@RocketMQMessageListener(
        consumerGroup = "homework_duplicate_check_consumer",
        topic = "homework_duplicate_check_topic",
        consumeMode = ConsumeMode.CONCURRENTLY,
        messageModel = MessageModel.CLUSTERING
)
public class DuplicateCheckServiceImpl extends ServiceImpl<DuplicateCheckMapper, DuplicateCheck>
    implements DuplicateCheckService, RocketMQListener<MessageExt> {

    private static final Logger logger = LoggerFactory.getLogger(DuplicateCheckServiceImpl.class);


    @Resource
    private HomeworkService homeworkService;

    @Resource
    private ThreadPoolExecutor duplicateCheckThreadPool;

    @Resource
    private MqMessageService mqMessageService;

    @Resource
    private AliOssUtil aliOssUtil;


    private static final int BATCH_SIZE = 100;

    // 锁的超时时间设置为30秒
    private static final long LOCK_TIMEOUT = 30;
    private static final TimeUnit LOCK_TIMEOUT_UNIT = TimeUnit.SECONDS;


    private final ConcurrentLinkedDeque<DuplicateCheck> synchronizedList = new ConcurrentLinkedDeque<>();




    /**
     * 执行作业查重操作
     * 计算两份作业之间的相似度，包括：
     * 1. 项目简介相似度 (10%)
     * 2. 项目背景相似度 (10%)
     * 3. 系统设计相似度 (20%)
     * 4. Word文档相似度 (20%)
     * 5. PDF文档相似度 (20%)
     * 6. 源代码相似度 (20%)
     *
     * @param source 源作业
     * @param target 目标作业(用于对比的作业)
     */
    void duplicateCheck(Homework source, Homework target) {
        String sourceId = source.getId().toString();
        String targetId = target.getId().toString();
        logger.info("开始查重作业 sourceId=[{}] targetId=[{}]", sourceId, targetId);

        // 创建查重结果对象
        DuplicateCheck duplicateCheck = new DuplicateCheck();
        duplicateCheck.setSourceId(source.getId());
        duplicateCheck.setTargetId(target.getId());

        try {
            // 1. 异步计算文本相似度
            logger.info("开始计算文本相似度: sourceId=[{}]", sourceId);
            CompletableFuture<BigDecimal> briefFuture = calculateTextSimilarity(
                    "简介", source.getBrief(), target.getBrief());
            CompletableFuture<BigDecimal> backgroundFuture = calculateTextSimilarity(
                    "背景", source.getBackground(), target.getBackground());
            CompletableFuture<BigDecimal> systemDesignFuture = calculateTextSimilarity(
                    "系统设计", source.getSystemDesign(), target.getSystemDesign());

            // 2. 异步计算文档相似度
            logger.info("开始计算文档相似度: sourceId=[{}]", sourceId);
            CompletableFuture<BigDecimal> wordFuture = calculateDocumentSimilarity(
                    "Word", source.getAttachmentWord(), target.getAttachmentWord(), "docx");
            CompletableFuture<BigDecimal> pdfFuture = calculateDocumentSimilarity(
                    "PDF", source.getAttachmentPdf(), target.getAttachmentPdf(), "pdf");

            // 3. 异步计算源代码相似度
            logger.info("开始计算源代码相似度: sourceId=[{}]", sourceId);
            CompletableFuture<BigDecimal> sourceFuture = calculateSourceCodeSimilarity(
                    source.getAttachmentSource(), target.getAttachmentSource());

            // 4. 获取所有异步计算结果
            try {
                logger.debug("等待所有相似度计算完成: sourceId=[{}]", sourceId);
                duplicateCheck.setBriefValue(briefFuture.get(30, TimeUnit.SECONDS));
                duplicateCheck.setBackgroundValue(backgroundFuture.get(30, TimeUnit.SECONDS));
                duplicateCheck.setSystemDesignValue(systemDesignFuture.get(30, TimeUnit.SECONDS));
                duplicateCheck.setWordValue(wordFuture.get(30, TimeUnit.SECONDS));
                duplicateCheck.setPdfValue(pdfFuture.get(30, TimeUnit.SECONDS));
                duplicateCheck.setSourceValue(sourceFuture.get(30, TimeUnit.SECONDS));
            } catch (TimeoutException e) {
                logger.error("相似度计算超时: sourceId=[{}]", sourceId, e);
                throw new BusinessException(ErrorCode.DUPLICATE_CHECK_ERROR, "相似度计算超时");
            }

            // 5. 计算总相似度
            BigDecimal similarity = calculateTotalSimilarity(duplicateCheck);
            duplicateCheck.setSimilarity(similarity);

            // 6. 保存查重结果
            logger.info("查重完成: sourceId=[{}], similarity=[{}]", sourceId, similarity);
            synchronizedList.add(duplicateCheck);

        } catch (Exception e) {
            logger.error("查重过程发生异常: sourceId=[{}]", sourceId, e);
            throw new BusinessException(ErrorCode.DUPLICATE_CHECK_ERROR, "查重失败: " + e.getMessage());
        }
    }

    /**
     * 计算文本相似度
     */
    private CompletableFuture<BigDecimal> calculateTextSimilarity(String type, String text1, String text2) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("开始计算{}相似度", type);
                double similarity = ConsineSimilarityUtil.calculate(text1, text2);
                logger.debug("{}相似度计算完成: similarity=[{}]", type, similarity);
                return BigDecimal.valueOf(similarity);
            } catch (Exception e) {
                logger.error("{}相似度计算失败", type, e);
                throw new BusinessException(ErrorCode.DUPLICATE_CHECK_ERROR,
                        type + "相似度计算失败: " + e.getMessage());
            }
        }, duplicateCheckThreadPool);
    }

    /**
     * 计算文档相似度
     */
    private CompletableFuture<BigDecimal> calculateDocumentSimilarity(
            String type, String doc1, String doc2, String fileType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("开始计算{}文档相似度", type);
                double similarity = SimHashUtil.calculateGroupSimilarity(doc1, doc2, fileType);
                logger.debug("{}文档相似度计算完成: similarity=[{}]", type, similarity);
                return BigDecimal.valueOf(similarity);
            } catch (Exception e) {
                logger.error("{}文档相似度计算失败", type, e);
                throw new BusinessException(ErrorCode.DUPLICATE_CHECK_ERROR,
                        type + "文档相似度计算失败: " + e.getMessage());
            }
        }, duplicateCheckThreadPool);
    }

    /**
     * 计算源代码相似度
     */
    private CompletableFuture<BigDecimal> calculateSourceCodeSimilarity(String source1, String source2) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("开始计算源代码相似度");
                double similarity = CodeSimilarityUtil.calculateCodeSimilarity(source1, source2);
                logger.debug("源代码相似度计算完成: similarity=[{}]", similarity);
                return BigDecimal.valueOf(similarity);
            } catch (Exception e) {
                logger.error("源代码相似度计算失败", e);
                throw new BusinessException(ErrorCode.DUPLICATE_CHECK_ERROR,
                        "源代码相似度计算失败: " + e.getMessage());
            }
        }, duplicateCheckThreadPool);
    }

    /**
     * 计算总相似度
     * 各部分权重：
     * - 简介、背景各占10%
     * - 系统设计、Word文档、PDF文档、源代码各占20%
     */
    private BigDecimal calculateTotalSimilarity(DuplicateCheck duplicateCheck) {
        try {
            BigDecimal pct1 = new BigDecimal("0.1"); // 10%权重
            BigDecimal pct2 = new BigDecimal("0.2"); // 20%权重

            return duplicateCheck.getBriefValue().multiply(pct1)
                    .add(duplicateCheck.getBackgroundValue().multiply(pct1))
                    .add(duplicateCheck.getSystemDesignValue().multiply(pct2))
                    .add(duplicateCheck.getWordValue().multiply(pct2))
                    .add(duplicateCheck.getPdfValue().multiply(pct2))
                    .add(duplicateCheck.getSourceValue().multiply(pct2));
        } catch (Exception e) {
            logger.error("计算总相似度失败", e);
            throw new BusinessException(ErrorCode.DUPLICATE_CHECK_ERROR, "计算总相似度失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class,isolation = Isolation.READ_COMMITTED,timeout = 10)
    public void getSimilarity(Long homeworkId) {
        synchronizedList.clear();
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
        //删除旧的查重记录
        LambdaQueryWrapper<DuplicateCheck> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DuplicateCheck::getSourceId,homeworkId);
        this.remove(queryWrapper);


        int offset = 1;
        while(true){
            List<Homework> batch = homeworkService.page(new Page<>(offset, BATCH_SIZE)).getRecords();
            System.out.println("offset:"+offset+":batchSize:"+batch.size()+"  batch:"+ Arrays.toString(batch.toArray()));
            if(CollUtil.isEmpty(batch)){
                break;
            }

            List<CompletableFuture<Void>> futures = batch.stream().filter(homework -> !Objects.equals(homework.getId(), homeworkId)).map(homework->CompletableFuture.runAsync(()->duplicateCheck(source,homework), duplicateCheckThreadPool)).collect(Collectors.toList());
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            offset ++;
        }

        if(!synchronizedList.isEmpty()){
            this.saveBatch(synchronizedList,BATCH_SIZE);
            synchronizedList.clear();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(MessageExt message) {
        logger.info("\n\n\nmessageExt:{}\n\n\n",message);
        String msgId = message.getMsgId();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        Long homeworkId = Long.parseLong(body);

        String lockKey = "duplicate_check:"+msgId;

        String lockValue = UUID.randomUUID().toString();
        try {

            LambdaQueryWrapper<MqMessage> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(MqMessage::getMsgId, msgId);
            MqMessage mqMessage = mqMessageService.getOne(queryWrapper);

            if (mqMessage != null) {
                if (mqMessage.getStatus().equals(CheckStatusEnum.COMPLETED.getCode())) {
                    logger.info("消息已处理完成，忽略，msgId={}", msgId);
                    return;
                }
                if (mqMessage.getStatus().equals(CheckStatusEnum.PROCESSING.getCode())) {
                    // 检查锁是否存在，避免处理中状态的消息被重复处理
                    if(RedisUtil.KeyOps.hasKey(lockKey)) {
                        logger.info("消息正在处理中，忽略，msgId={}", msgId);
                        return;
                    }
                    // 锁不存在但状态是处理中，说明之前的处理异常终止，重置状态
                    mqMessage.setStatus(CheckStatusEnum.WAITING.getCode());
                    mqMessageService.updateById(mqMessage);
                }
            }

            // 尝试获取分布式锁，设置超时时间为30秒
            boolean locked = RedisUtil.LockOps.getLock(lockKey, lockValue, LOCK_TIMEOUT, LOCK_TIMEOUT_UNIT);
            if (!locked) {
                logger.info("获取锁失败，消息可能正在被其他实例处理，msgId={}", msgId);
                return;
            }

            // 双重检查锁，防止并发
                mqMessage = mqMessageService.getOne(queryWrapper);

            if (mqMessage == null) {
                mqMessage = new MqMessage();
                mqMessage.setMsgId(msgId);
                mqMessage.setMessageBody(String.valueOf(homeworkId));
                mqMessage.setBusinessKey("homework_" + homeworkId);
                mqMessage.setStatus(CheckStatusEnum.WAITING.getCode());
                mqMessageService.save(mqMessage);
            } else if (!mqMessage.getStatus().equals(CheckStatusEnum.WAITING.getCode())) {
                logger.info("消息状态已变更，不再处理，msgId={}, status={}", msgId, mqMessage.getStatus());
                return;
            }

            handleDuplicateCheck(homeworkId, mqMessage);

        } catch (Exception e) {
            logger.error("处理查重消息失败，msgId=" + msgId, e);
            handleError(message, msgId, e);
        }finally {
            RedisUtil.LockOps.releaseLock(lockKey, lockValue);
        }
    }

    private void handleError(MessageExt message, String msgId, Exception e) {
        int reconsumeTimes = message.getReconsumeTimes();
        if (reconsumeTimes >= 3) {
            try {
                MqMessage mqMessage = new MqMessage();
                mqMessage.setMsgId(msgId);
                mqMessage.setStatus(CheckStatusEnum.FAILED.getCode());
                mqMessage.setNextRetryTime(LocalDateTime.now().plusMinutes(3));

                LambdaUpdateWrapper<MqMessage> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(MqMessage::getMsgId, msgId);
                mqMessageService.update(mqMessage, updateWrapper);

                logger.error("查重消息处理失败超过最大重试次数，msgId={}", msgId);
            } catch (Exception ex) {
                logger.error("更新消息状态失败", ex);
            }
            return;
        }
        throw new BusinessException(ErrorCode.DUPLICATE_CHECK_ERROR, "查重失败：" + e.getMessage());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleDuplicateCheck(Long homeworkId, MqMessage mqMessage) {
        try {
            // 更新状态为处理中
            mqMessage.setStatus(CheckStatusEnum.PROCESSING.getCode());
            mqMessageService.updateById(mqMessage);

            // 更新作业查重状态
            updateHomeworkCheckStatus(homeworkId, CheckStatusEnum.PROCESSING);

            // 执行查重
            getSimilarity(homeworkId);

            // 更新状态为完成
            mqMessage.setStatus(CheckStatusEnum.COMPLETED.getCode());
            mqMessageService.updateById(mqMessage);
            updateHomeworkCheckStatus(homeworkId, CheckStatusEnum.COMPLETED);

            logger.info("作业查重完成，homeworkId={}", homeworkId);
        } catch (Exception e) {
            // 发生异常时回滚状态
            mqMessage.setStatus(CheckStatusEnum.WAITING.getCode());
            mqMessageService.updateById(mqMessage);
            updateHomeworkCheckStatus(homeworkId, CheckStatusEnum.WAITING);
            throw e; // 抛出异常触发事务回滚
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
            logger.error("更新作业查重状态失败，homeworkId=" + homeworkId, e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (duplicateCheckThreadPool != null) {
            duplicateCheckThreadPool.shutdown();
            try {
                if (!duplicateCheckThreadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                    duplicateCheckThreadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                duplicateCheckThreadPool.shutdownNow();
            }
        }
    }
}




