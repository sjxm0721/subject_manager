package com.sjxm.springbootinit.biz;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sjxm.springbootinit.common.ErrorCode;
import com.sjxm.springbootinit.exception.BusinessException;
import com.sjxm.springbootinit.model.dto.homework.*;
import com.sjxm.springbootinit.model.dto.task.RetryTask;
import com.sjxm.springbootinit.model.entity.*;
import com.sjxm.springbootinit.model.enums.CheckStatusEnum;
import com.sjxm.springbootinit.model.enums.UserRoleEnum;
import com.sjxm.springbootinit.model.vo.*;
import com.sjxm.springbootinit.service.*;
import com.sjxm.springbootinit.utils.ExcelExportUtil;
import com.sjxm.springbootinit.utils.RedisUtil;
import com.sjxm.springbootinit.utils.RichTextXssFilterUtil;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/20
 * @Description:
 */

@Component
public class HomeworkBiz {

    private static final Logger logger = LoggerFactory.getLogger(HomeworkBiz.class);


    private final DelayQueue<RetryTask> retryQueue = new DelayQueue<>();



    @Resource
    private SubjectStudentService subjectStudentService;

    @Resource
    private HomeworkService homeworkService;

    @Resource
    private SubjectService subjectService;

    @Resource
    private UserService userService;


    @Resource
    private GradeService gradeService;


    @Resource
    private MqMessageService mqMessageService;

    @Resource
    private DuplicateCheckService duplicateCheckService;

    @PostConstruct
    public void init() {
        Thread retryThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    RetryTask task = retryQueue.take();
                    if (task != null) {
                        asyncDuplicateCheck(task.getHomeworkId(), task.getMessageId());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        retryThread.setName("duplicate-check-retry");
        retryThread.setDaemon(true);
        retryThread.start();
    }




    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    @Transactional(rollbackFor = Exception.class)
    public void processFailedTasks() {
        MqMessage mqMessage = null;
        try {
            RetryTask task = retryQueue.peek();
            if (task == null || !task.canRetry()) {
                return;
            }

            // 检查消息状态
            mqMessage = mqMessageService.getById(task.getMessageId());
            if (mqMessage == null || !mqMessage.getStatus().equals(CheckStatusEnum.WAITING.getCode())) {
                retryQueue.remove(task);
                return;
            }

            // 更新重试次数和状态
            mqMessage.setRetryTimes(task.getRetryCount());
            mqMessage.setStatus(CheckStatusEnum.PROCESSING.getCode());
            mqMessageService.updateById(mqMessage);

            // 执行查重
            duplicateCheckService.getSimilarity(task.getHomeworkId());

            // 更新状态为完成
            mqMessage.setStatus(CheckStatusEnum.COMPLETED.getCode());
            mqMessageService.updateById(mqMessage);

            // 移除成功的任务
            retryQueue.remove(task);

        } catch (Exception e) {
            logger.error("重试任务处理失败", e);
            if (mqMessage != null) {
                handleRetryFailure(mqMessage);
            }
        }
    }

    private void handleRetryFailure(MqMessage mqMessage) {
        try {
            RetryTask currentTask = retryQueue.peek();
            if (currentTask != null && currentTask.canRetry()) {
                currentTask.incrementRetryCount();
                mqMessage.setStatus(CheckStatusEnum.WAITING.getCode());
                mqMessage.setNextRetryTime(LocalDateTime.now().plusSeconds(5));
            } else {
                mqMessage.setStatus(CheckStatusEnum.FAILED.getCode());
                if (currentTask != null) {
                    retryQueue.remove(currentTask);
                }
                // 发送告警通知
                sendAlert(mqMessage);
            }
            mqMessageService.updateById(mqMessage);
        } catch (Exception e) {
            logger.error("处理重试失败异常", e);
        }
    }

    private void sendAlert(MqMessage message) {
        String alertMessage = String.format(
                "作业查重失败，已达到最大重试次数\n" +
                        "作业ID: %s\n" +
                        "消息ID: %s\n" +
                        "重试次数: %d\n",
                message.getMessageBody(),
                message.getMsgId(),
                message.getRetryTimes()
        );
        logger.error(alertMessage);
        // 这里可以添加其他告警方式，如邮件、钉钉等
    }

    @Scheduled(cron = "0 0 1 * * ?") // 每天凌晨1点执行
    public void cleanExpiredData() {
        try {
            // 清理过期消息记录
            LocalDateTime beforeTime = LocalDateTime.now().minusDays(7);
            LambdaQueryWrapper<MqMessage> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.lt(MqMessage::getCreateTime, beforeTime);
            mqMessageService.remove(queryWrapper);

            // 清理重试队列中的过期任务
            RetryTask task;
            while ((task = retryQueue.peek()) != null) {
                if (task.getStartTime() < beforeTime.toInstant(ZoneOffset.UTC).toEpochMilli()) {
                    retryQueue.remove(task);
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("清理过期数据失败", e);
        }
    }



    HomeworkVO obj2VO(Homework homework){
        HomeworkVO homeworkVO = new HomeworkVO();
        BeanUtil.copyProperties(homework,homeworkVO);
        Long subjectId = homework.getSubjectId();
        Subject subject = subjectService.getById(subjectId);
        if(subject!=null){
            homeworkVO.setGrade(subject.getGrade());
            homeworkVO.setSubjectName(subject.getTitle());
        }
        LambdaQueryWrapper<SubjectStudent> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(SubjectStudent::getSubjectId,subjectId).eq(SubjectStudent::getGroupNum,homework.getGroupNum());
        List<SubjectStudent> subjectStudentList = subjectStudentService.list(lambdaQueryWrapper);
        Set<Long> studentIds = subjectStudentList.stream().map(SubjectStudent::getStudentId).collect(Collectors.toSet());
        LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userLambdaQueryWrapper.in(!CollUtil.isEmpty(studentIds),User::getId,studentIds);
        List<User> list = userService.list(userLambdaQueryWrapper);
        List<UserVO> userVOList = list.stream().map(user -> userService.getUserVO(user)).collect(Collectors.toList());
        homeworkVO.setMember(userVOList);
        LambdaQueryWrapper<Grade> gradeLambdaQueryWrapper = new LambdaQueryWrapper<>();
        gradeLambdaQueryWrapper.eq(Grade::getHomeworkId,homework.getId());
        List<Grade> gradeList = gradeService.list(gradeLambdaQueryWrapper);
        homeworkVO.setScores(gradeList);
        return homeworkVO;
    }

    HomeworkHistoryVO obj2HistoryVO(Homework homework){
        HomeworkHistoryVO homeworkHistoryVO = new HomeworkHistoryVO();
        BeanUtil.copyProperties(homework,homeworkHistoryVO);
        Long subjectId = homework.getSubjectId();
        Subject subject = subjectService.getById(subjectId);
        if(subject!=null){
            homeworkHistoryVO.setSubjectName(subject.getTitle());
        }
        LambdaQueryWrapper<SubjectStudent> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(SubjectStudent::getSubjectId,subjectId).eq(SubjectStudent::getGroupNum,homework.getGroupNum());
        List<SubjectStudent> subjectStudentList = subjectStudentService.list(lambdaQueryWrapper);
        Set<Long> studentIds = subjectStudentList.stream().map(SubjectStudent::getStudentId).collect(Collectors.toSet());
        LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userLambdaQueryWrapper.in(!CollUtil.isEmpty(studentIds),User::getId,studentIds);
        List<User> list = userService.list(userLambdaQueryWrapper);
        List<UserVO> userVOList = list.stream().map(user -> userService.getUserVO(user)).collect(Collectors.toList());
        homeworkHistoryVO.setMember(userVOList);
        return homeworkHistoryVO;
    }

    @Transactional(rollbackFor = Exception.class)
    public void submitHomework(HomeworkAddRequest request) {
        // 参数校验
        Long subjectId = request.getSubjectId();
        Long studentId = request.getStudentId();
        if (subjectId == null || studentId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取小组信息
        LambdaQueryWrapper<SubjectStudent> subjectStudentLambdaQueryWrapper = new LambdaQueryWrapper<>();
        subjectStudentLambdaQueryWrapper.eq(SubjectStudent::getStudentId, studentId)
                .eq(SubjectStudent::getSubjectId, subjectId);
        SubjectStudent subjectStudent = subjectStudentService.getOne(subjectStudentLambdaQueryWrapper);
        Integer groupNum = subjectStudent.getGroupNum();

        // 构建作业实体
        Homework homework = new Homework();
        BeanUtil.copyProperties(request, homework);
        homework.setBackground(RichTextXssFilterUtil.clean(homework.getBackground()));
        homework.setSystemDesign(RichTextXssFilterUtil.clean(homework.getSystemDesign()));
        homework.setGroupNum(groupNum);
        homework.setCheckStatus(CheckStatusEnum.WAITING.getCode());  // 直接设置查重状态

        // 设置ID（如果是更新操作）
        Long id = request.getId();
        if (id != null) {
            homework.setId(id);
        }

        // 保存作业
        homeworkService.saveOrUpdate(homework);

        // 删除缓存
        redisDeleteByPrefix("homework-history:pagination:");

        // 创建本地消息记录
        MqMessage mqMessage = new MqMessage();
        mqMessage.setMsgId(UUID.randomUUID().toString());
        mqMessage.setStatus(CheckStatusEnum.WAITING.getCode());
        mqMessage.setMessageBody(String.valueOf(homework.getId()));
        mqMessage.setBusinessKey("homework_" + homework.getId());
        mqMessageService.save(mqMessage);
        asyncDuplicateCheck(homework.getId(), mqMessage.getId())
                .exceptionally(throwable -> {
                    logger.error("提交作业失败，homeworkId={}", homework.getId(), throwable);
                    retryQueue.offer(new RetryTask(homework.getId(), mqMessage.getId()));
                    return null;
                });
    }

    @Async("duplicateCheckThreadPool")
    public CompletableFuture<Void> asyncDuplicateCheck(Long homeworkId, Long messageId) {

        return CompletableFuture.runAsync(()->{
            String lockKey = "duplicate_check:" + messageId;
            String lockValue = UUID.randomUUID().toString();
            try{
                // 获取分布式锁
                boolean locked = RedisUtil.LockOps.getLock(lockKey, lockValue, 60, TimeUnit.SECONDS);
                if (!locked) {
                    logger.info("获取锁失败，任务可能正在被处理，messageId={}", messageId);
                    return;
                }

                // 检查消息状态
                MqMessage message = mqMessageService.getById(messageId);
                if (message == null || !message.getStatus().equals(CheckStatusEnum.WAITING.getCode())) {
                    logger.info("消息状态已变更，不再处理，messageId={}", messageId);
                    return;
                }

                // 更新状态为处理中
                message.setStatus(CheckStatusEnum.PROCESSING.getCode());
                mqMessageService.updateById(message);

                // 更新作业查重状态
                updateHomeworkCheckStatus(homeworkId, CheckStatusEnum.PROCESSING);

                // 执行查重
                duplicateCheckService.getSimilarity(homeworkId);

                // 更新状态为完成
                message.setStatus(CheckStatusEnum.COMPLETED.getCode());
                mqMessageService.updateById(message);
                updateHomeworkCheckStatus(homeworkId, CheckStatusEnum.COMPLETED);

                logger.info("作业查重完成，homeworkId={}", homeworkId);
            }catch (Exception e){
                logger.error("查重失败，homeworkId=" + homeworkId, e);
                handleError(homeworkId, messageId, e);
            }
            finally {
                RedisUtil.LockOps.releaseLock(lockKey, lockValue);
            }
        });
    }


    private void handleError(Long homeworkId, Long messageId, Exception e) {
        try {
            MqMessage message = mqMessageService.getById(messageId);
            if (message == null) {
                return;
            }

            int retryCount = message.getRetryTimes() != null ? message.getRetryTimes() : 0;
            if (retryCount >= 3) {
                message.setStatus(CheckStatusEnum.FAILED.getCode());
                message.setNextRetryTime(LocalDateTime.now().plusMinutes(3));
                mqMessageService.updateById(message);
                updateHomeworkCheckStatus(homeworkId, CheckStatusEnum.FAILED);
                logger.error("查重失败超过最大重试次数，homeworkId={}", homeworkId);
                return;
            }

            // 更新重试信息
            message.setStatus(CheckStatusEnum.WAITING.getCode());
            message.setRetryTimes(retryCount + 1);
            message.setNextRetryTime(LocalDateTime.now().plusSeconds(10));
            mqMessageService.updateById(message);
            updateHomeworkCheckStatus(homeworkId, CheckStatusEnum.WAITING);

            // 延迟重试
            CompletableFuture.delayedExecutor(10, TimeUnit.SECONDS)
                    .execute(() -> asyncDuplicateCheck(homeworkId, messageId));

        } catch (Exception ex) {
            logger.error("处理错误失败", ex);
        }
    }

    private void updateHomeworkCheckStatus(Long homeworkId, CheckStatusEnum status) {
        try {
            Homework homework = homeworkService.getById(homeworkId);
            if (homework != null) {
                homework.setCheckStatus(status.getCode());
                homeworkService.updateById(homework);
            }
        } catch (Exception e) {
            logger.error("更新作业查重状态失败，homeworkId=" + homeworkId, e);
        }
    }


    public void redisDeleteByPrefix(String prefix) {
        String pattern = prefix + "*";
        Set<String> keys = RedisUtil.KeyOps.keys(pattern);
        if (!CollUtil.isEmpty(keys)) {
            RedisUtil.KeyOps.delete(keys);
        }
    }


    public Page<HomeworkVO> getHomeworkPage(HomeworkQueryRequest homeworkQueryRequest) {

        String title = homeworkQueryRequest.getSubjectName();
        String grade = homeworkQueryRequest.getGrade();
        String homeworkTitle = homeworkQueryRequest.getTitle();
        int current = homeworkQueryRequest.getCurrent();
        int pageSize = homeworkQueryRequest.getPageSize();

        LambdaQueryWrapper<Homework> homeworkLambdaQueryWrapper = new LambdaQueryWrapper<>();
        homeworkLambdaQueryWrapper.like(!StrUtil.isBlankIfStr(homeworkTitle),Homework::getTitle,homeworkTitle);
        //获取该年级所有课程
        LambdaQueryWrapper<Subject> subjectLambdaQueryWrapper = new LambdaQueryWrapper<>();
        subjectLambdaQueryWrapper.like(!StrUtil.isBlankIfStr(grade),Subject::getGrade,grade).like(!StrUtil.isBlankIfStr(title),Subject::getTitle,title);
        List<Subject> subjectList = subjectService.list(subjectLambdaQueryWrapper);
        if(!CollUtil.isEmpty(subjectList)){
            Set<Long> subjectIds = subjectList.stream().map(Subject::getId).collect(Collectors.toSet());
            homeworkLambdaQueryWrapper.in(Homework::getSubjectId,subjectIds);

            Page<Homework> homeworkPage = homeworkService.page(new Page<>(current, pageSize),
                    homeworkLambdaQueryWrapper);

            Page<HomeworkVO> homeworkVOPage = new Page<>();
            BeanUtil.copyProperties(homeworkPage,homeworkVOPage);
            List<Homework> records = homeworkPage.getRecords();
            List<HomeworkVO> homeworkVOList = records.stream().map(this::obj2VO).collect(Collectors.toList());
            homeworkVOPage.setRecords(homeworkVOList);
            return homeworkVOPage;
        }
        return new Page<>();
    }

    public void updateHomeworkStatus(HomeworkStatusUpdateRequest homeworkStatusUpdateRequest) {
        Long id = homeworkStatusUpdateRequest.getId();
        Integer suggested = homeworkStatusUpdateRequest.getSuggested();
        if(suggested==null || suggested<0||suggested>1) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        LambdaUpdateWrapper<Homework> homeworkLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        homeworkLambdaUpdateWrapper.set(Homework::getCommend,suggested).eq(Homework::getId,id);
        homeworkService.update(homeworkLambdaUpdateWrapper);

        //删除缓存
        redisDeleteByPrefix("homework-history:pagination:");
    }

    /**
     * 教师获取历年作品年份
     * @return
     */
    public List<Integer> getHomeworkYearTeacher() {
        List<Homework> list = homeworkService.list();
        List<Integer> yearList = list.stream().map(Homework::getSubmitYear).collect(Collectors.toList());
        return yearList.stream().distinct().sorted((a,b)->b-a).collect(Collectors.toList());
    }

    /**
     * 一般获取历年作品年份
     * @return
     */
    public List<Integer> getHomeworkYear() {
        LambdaQueryWrapper<Homework> homeworkLambdaQueryWrapper = new LambdaQueryWrapper<>();
        homeworkLambdaQueryWrapper.eq(Homework::getCommend,1);
        List<Homework> list = homeworkService.list(homeworkLambdaQueryWrapper);
        List<Integer> yearList = list.stream().map(Homework::getSubmitYear).collect(Collectors.toList());
        return yearList.stream().distinct().sorted((a,b)->b-a).collect(Collectors.toList());
    }

    private static final String CACHE_KEY_PREFIX = "homework:history:page:";
    private static final String EMPTY_CACHE = "EMPTY";
    private static final long CACHE_EXPIRE_TIME = 5;
    private static final TimeUnit CACHE_EXPIRE_UNIT = TimeUnit.MINUTES;

    public Page<HomeworkHistoryVO> getHomeworkHistoryPage(
            HomeworkHistoryPageQueryRequest request,
            HttpServletRequest httpRequest) {
        // 1. 生成缓存key
        String cacheKey = generateCacheKey(request, httpRequest);

        // 2. 查询缓存
        String cachedData = RedisUtil.StringOps.get(cacheKey);

        // 3. 判断是否命中空值缓存（防止缓存穿透）
        if (EMPTY_CACHE.equals(cachedData)) {
            return new Page<>();
        }

        // 4. 缓存命中，返回数据
        if (!StrUtil.isBlankIfStr(cachedData)) {
            try {
                return JSON.parseObject(cachedData,
                        new TypeReference<Page<HomeworkHistoryVO>>() {});
            } catch (Exception e) {
                logger.error("Cache data parse error", e);
                RedisUtil.KeyOps.delete(cacheKey);
            }
        }

        // 5. 缓存未命中，使用本地锁（单机环境）防止缓存击穿
        synchronized (cacheKey.intern()) {
            // 双重检查
            cachedData = RedisUtil.StringOps.get(cacheKey);
            if (!StrUtil.isBlankIfStr(cachedData)) {
                return JSON.parseObject(cachedData,
                        new TypeReference<Page<HomeworkHistoryVO>>() {});
            }

            // 6. 查询数据库
            Page<HomeworkHistoryVO> result = queryFromDatabase(request, httpRequest);

            // 7. 写入缓存（防止缓存雪崩）
            if (result != null && !result.getRecords().isEmpty()) {
                // 添加随机过期时间，防止缓存雪崩
                long expireTime = CACHE_EXPIRE_TIME +
                        RandomUtil.randomLong(0, CACHE_EXPIRE_TIME);
                RedisUtil.StringOps.setEx(cacheKey,
                        JSON.toJSONString(result),
                        expireTime,
                        CACHE_EXPIRE_UNIT);
            } else {
                // 缓存空值，防止缓存穿透
                RedisUtil.StringOps.setEx(cacheKey,
                        EMPTY_CACHE,
                        60,
                        TimeUnit.SECONDS);
            }

            return result;
        }
    }

    private String generateCacheKey(
            HomeworkHistoryPageQueryRequest request,
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        Integer userRole = loginUser.getUserRole();
        Integer year = request.getYear();

        return new StringBuilder(CACHE_KEY_PREFIX)
                .append("role:").append(userRole)
                .append(":year:").append(year != null ? year : "all")
                .append(":page:").append(request.getCurrent())
                .append(":size:").append(request.getPageSize())
                .toString();
    }

    private Page<HomeworkHistoryVO> queryFromDatabase(
            HomeworkHistoryPageQueryRequest request,
            HttpServletRequest httpRequest) {
        // 原有的数据库查询逻辑
        User loginUser = userService.getLoginUser(httpRequest);
        Integer userRole = loginUser.getUserRole();
        Integer year = request.getYear();

        LambdaQueryWrapper<Homework> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(year != null, Homework::getSubmitYear, year);
        if (!UserRoleEnum.TEACHER.getValue().equals(userRole)) {
            wrapper.eq(Homework::getCommend, 1);
        }

        Page<Homework> homeworkPage = homeworkService.page(
                new Page<>(request.getCurrent(), request.getPageSize()),
                wrapper);

        // 转换为VO
        Page<HomeworkHistoryVO> resultPage = new Page<>();
        BeanUtil.copyProperties(homeworkPage, resultPage);
        List<HomeworkHistoryVO> voList = homeworkPage.getRecords().stream()
                .map(this::obj2HistoryVO)
                .collect(Collectors.toList());
        resultPage.setRecords(voList);

        return resultPage;
    }

    public Homework getHomeworkDetail(Long homeworkId, HttpServletRequest request) {
        Homework homework = homeworkService.getById(homeworkId);
        if(homework==null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        User user = userService.getLoginUser(request);
        if(homework.getCommend()==0&& !Objects.equals(user.getUserRole(), UserRoleEnum.TEACHER.getValue())){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return homework;
    }

    public Page<HomeworkGradeVO> pageGrade(HomeworkGradePageRequest homeworkGradePageRequest, HttpServletRequest request) {
        Long subjectId = homeworkGradePageRequest.getSubjectId();
        Integer isCorrect = homeworkGradePageRequest.getIsCorrect();
        int current = homeworkGradePageRequest.getCurrent();
        int pageSize = homeworkGradePageRequest.getPageSize();

        User loginUser = userService.getLoginUser(request);
        if(ObjectUtil.isNull(loginUser)){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        Long studentId = loginUser.getId();

        Page<HomeworkGradeVO> page = new Page<>(current,pageSize);

        Page<HomeworkGradeVO> homeworkGradePage = homeworkService.selectHomeworkGradePage(page, studentId, subjectId, isCorrect);
        List<HomeworkGradeVO> records = homeworkGradePage.getRecords();
        for (HomeworkGradeVO record : records) {
            if(record.getIsCorrect()==1){
                LambdaQueryWrapper<Grade> gradeLambdaQueryWrapper = new LambdaQueryWrapper<>();
                gradeLambdaQueryWrapper.eq(Grade::getHomeworkId,record.getId()).eq(Grade::getStudentId,studentId);
                Grade grade = gradeService.getOne(gradeLambdaQueryWrapper);
                record.setScore(grade);
            }
        }
        homeworkGradePage.setRecords(records);
        return homeworkGradePage;
    }

    public Homework getMyHomeworkDetail(Long homeworkId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Long id = loginUser.getId();
        Homework homework = homeworkService.getById(homeworkId);
        Long subjectId = homework.getSubjectId();
        Integer groupNum = homework.getGroupNum();
        LambdaQueryWrapper<SubjectStudent> subjectStudentLambdaQueryWrapper = new LambdaQueryWrapper<>();
        subjectStudentLambdaQueryWrapper.eq(SubjectStudent::getSubjectId,subjectId).eq(SubjectStudent::getGroupNum,groupNum);
        List<SubjectStudent> subjectStudentList = subjectStudentService.list(subjectStudentLambdaQueryWrapper);
        Set<Long> set = subjectStudentList.stream().map(SubjectStudent::getStudentId).collect(Collectors.toSet());
        if(!set.contains(id))
        {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return homework;
    }

    public void exportXml(HomeworkExportRequest homeworkExportRequest, HttpServletResponse response) {
        try{
            String grade = homeworkExportRequest.getGrade();
            String title = homeworkExportRequest.getTitle();
            String homeworkTitle = homeworkExportRequest.getHomeworkTitle();
            List<HomeworkExportVO> list = homeworkService.export(grade,title,homeworkTitle);
            String fileName = "学生作业信息表-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));
            ExcelExportUtil.exportExcel(response,fileName,list, HomeworkExportVO.class);
        }catch (IOException e){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,e.getMessage());
        }

    }

    public void exportZip(HomeworkExportRequest homeworkExportRequest, HttpServletResponse response) {
        File tempDir = null;
        try {
            String grade = homeworkExportRequest.getGrade();
            String title = homeworkExportRequest.getTitle();
            String homeworkTitle = homeworkExportRequest.getHomeworkTitle();
            LambdaQueryWrapper<Subject> subjectLambdaQueryWrapper = new LambdaQueryWrapper<>();
            subjectLambdaQueryWrapper.like(!StrUtil.isBlankIfStr(grade), Subject::getGrade, grade).like(!StrUtil.isBlankIfStr(title), Subject::getTitle, title);
            List<Subject> subjectList = subjectService.list(subjectLambdaQueryWrapper);
            List<Long> subjectIds = subjectList.stream().map(Subject::getId).collect(Collectors.toList());
            if (CollUtil.isEmpty(subjectIds)) {
                return;
            }
            LambdaQueryWrapper<Homework> homeworkLambdaQueryWrapper = new LambdaQueryWrapper<>();
            homeworkLambdaQueryWrapper.in(Homework::getSubjectId, subjectIds);
            homeworkLambdaQueryWrapper.like(!StrUtil.isBlankIfStr(homeworkTitle),Homework::getTitle,homeworkTitle);
            List<Homework> homeworkList = homeworkService.list(homeworkLambdaQueryWrapper);
            List<HomeworkVO> homeworkVOList = new ArrayList<>();
            for (Homework homework : homeworkList) {
                HomeworkVO homeworkVO = new HomeworkVO();
                BeanUtil.copyProperties(homework, homeworkVO);
                Subject subject = subjectService.getById(homework.getSubjectId());
                if (subject != null) {
                    homeworkVO.setSubjectName(subject.getTitle());
                    homeworkVO.setGrade(subject.getGrade());
                }
                homeworkVOList.add(homeworkVO);
            }

            // 创建临时目录
            tempDir = Files.createTempDirectory("homework_export_").toFile();

            // 准备压缩包文件名
            String zipFileName = "作业信息表-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".zip";

            // 设置响应头
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(zipFileName, "UTF-8"));

            try (ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(response.getOutputStream()))) {
                // 遍历每个作业创建对应文件夹和文件
                for (HomeworkVO homework : homeworkVOList) {
                    // 创建作业文件夹
                    String folderName = String.format("%s-%s-%d-%s",
                            homework.getGrade(),
                            homework.getSubjectName(),
                            homework.getGroupNum(),
                            homework.getTitle());

                    // 创建普通文本文件
                    createTextFile(zipOut, folderName + "/背景.html", homework.getBackground());
                    createTextFile(zipOut, folderName + "/系统设计.html", homework.getSystemDesign());
                    createTextFile(zipOut, folderName + "/内容简介.txt", homework.getBrief());
                    createTextFile(zipOut, folderName + "/硬件内容.txt", homework.getHardwareTech());
                    createTextFile(zipOut, folderName + "/软件内容.txt", homework.getSoftwareTech());

                    // 处理OSS文件
                    downloadAndZipOssFiles(zipOut, folderName, "Word文件", homework.getAttachmentWord());
                    downloadAndZipOssFiles(zipOut, folderName, "PDF文件", homework.getAttachmentPdf());
                    downloadAndZipOssFiles(zipOut, folderName, "源代码", homework.getAttachmentSource());
                    downloadAndZipOssFiles(zipOut, folderName, "视频", homework.getAttachmentMp4());
                }

                zipOut.finish();
                zipOut.flush();
            }

        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, e.getMessage());
        } finally {
            // 清理临时目录
            if (tempDir != null) {
                try {
                    FileUtils.deleteDirectory(tempDir);
                } catch (IOException e) {
                    logger.error("清理临时目录失败", e);
                }
            }
        }
    }

    // 创建文本文件并添加到压缩包
    private void createTextFile(ZipOutputStream zipOut, String path, String content) throws IOException {
        if (StrUtil.isBlank(content)) {
            return;
        }
        ZipEntry entry = new ZipEntry(path);
        zipOut.putNextEntry(entry);
        zipOut.write(content.getBytes(StandardCharsets.UTF_8));
        zipOut.closeEntry();
    }

    // 下载并添加OSS文件到压缩包
    private void downloadAndZipOssFiles(ZipOutputStream zipOut, String folderPath, String subFolder, String ossUrls) {
        if (StrUtil.isBlank(ossUrls)) {
            return;
        }

        String[] urls = ossUrls.split(",");
        for (String url : urls) {
            url = url.trim();
            if (!url.startsWith("http")) {
                continue;
            }

            try {
                String fileName = url.substring(url.lastIndexOf('/') + 1);
                String entryPath = folderPath + "/" + subFolder + "/" + fileName;

                ZipEntry entry = new ZipEntry(entryPath);
                zipOut.putNextEntry(entry);

                // 下载OSS文件并写入压缩包
                URL fileUrl = new URL(url);
                try (BufferedInputStream bis = new BufferedInputStream(fileUrl.openStream())) {
                    byte[] buffer = new byte[1024 * 1024];
                    int bytesRead;
                    while ((bytesRead = bis.read(buffer)) != -1) {
                        zipOut.write(buffer, 0, bytesRead);
                    }
                }

                zipOut.closeEntry();

            } catch (Exception e) {
                logger.error("下载OSS文件失败: " + url, e);
            }
        }
    }
}
