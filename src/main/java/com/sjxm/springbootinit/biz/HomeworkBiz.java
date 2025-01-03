package com.sjxm.springbootinit.biz;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sjxm.springbootinit.common.ErrorCode;
import com.sjxm.springbootinit.exception.BusinessException;
import com.sjxm.springbootinit.model.dto.homework.*;
import com.sjxm.springbootinit.model.entity.*;
import com.sjxm.springbootinit.model.enums.CheckStatusEnum;
import com.sjxm.springbootinit.model.enums.UserRoleEnum;
import com.sjxm.springbootinit.model.vo.*;
import com.sjxm.springbootinit.service.*;
import com.sjxm.springbootinit.utils.AliOssUtil;
import com.sjxm.springbootinit.utils.ExcelExportUtil;
import com.sjxm.springbootinit.utils.VideoPostGenerateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/20
 * @Description:
 */

@Component
@Slf4j
public class HomeworkBiz {

    @Resource
    private SubjectStudentService subjectStudentService;

    @Resource
    private HomeworkService homeworkService;

    @Resource
    private SubjectService subjectService;

    @Resource
    private UserService userService;

    @Resource
    private AliOssUtil aliOssUtil;

    @Resource
    private GradeService gradeService;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Resource
    private MqMessageService mqMessageService;

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

    public void submitHomework(HomeworkAddRequest request) {
        Long id = request.getId();
            Long subjectId = request.getSubjectId();
            Long studentId = request.getStudentId();
            if(subjectId==null||studentId==null){
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
            }
            LambdaQueryWrapper<SubjectStudent> subjectStudentLambdaQueryWrapper = new LambdaQueryWrapper<>();
            subjectStudentLambdaQueryWrapper.eq(SubjectStudent::getStudentId,studentId).eq(SubjectStudent::getSubjectId,subjectId);
            SubjectStudent subjectStudent = subjectStudentService.getOne(subjectStudentLambdaQueryWrapper);
            Integer groupNum = subjectStudent.getGroupNum();
            Homework homework = new Homework();
            BeanUtil.copyProperties(request,homework);
            homework.setGroupNum(groupNum);

            homework.setSubjectType(request.getType());

            //设置封面
            if(id!=null){
                homework.setId(id);
            }
            homeworkService.saveOrUpdate(homework);

            //发送查重消息到MQ
        try {
            Message<String> message = MessageBuilder.withPayload(String.valueOf(homework.getId()))
                            .setHeader(RocketMQHeaders.KEYS,"homework_"+homework.getId()).build();
            rocketMQTemplate.asyncSend(
                    "homework_duplicate_check_topic",
                    message,
                    new SendCallback() {
                        @Override
                        public void onSuccess(SendResult sendResult) {
                            log.info("查重消息发送成功,homeworkId={},msgId={}",
                                    homework.getId(), sendResult.getMsgId());

                            // 记录消息
                            MqMessage mqMessage = new MqMessage();
                            mqMessage.setMsgId(sendResult.getMsgId());
                            mqMessage.setStatus(CheckStatusEnum.WAITING.getCode());
                            mqMessage.setMessageBody(String.valueOf(homework.getId()));
                            mqMessage.setBusinessKey("homework_"+homework.getId());
                            mqMessageService.save(mqMessage);
                        }

                        @Override
                        public void onException(Throwable throwable) {
                            log.error("查重消息发送失败,homeworkId=" + homework.getId(), throwable);
                        }
                    }
            );
        } catch (Exception e) {
            log.error("发送查重消息失败", e);
            throw new BusinessException(ErrorCode.DUPLICATE_CHECK_ERROR,
                    "构建查重请求失败：" + e.getMessage());
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

    public Page<HomeworkHistoryVO> getHomeworkHistoryPage(HomeworkHistoryPageQueryRequest homeworkHistoryPageQueryRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Integer userRole = loginUser.getUserRole();
        Integer year = homeworkHistoryPageQueryRequest.getYear();
        int current = homeworkHistoryPageQueryRequest.getCurrent();
        int pageSize = homeworkHistoryPageQueryRequest.getPageSize();
        LambdaQueryWrapper<Homework> homeworkLambdaQueryWrapper = new LambdaQueryWrapper<>();
        homeworkLambdaQueryWrapper.eq(year!=null,Homework::getSubmitYear,year);
        if(!UserRoleEnum.TEACHER.getValue().equals(userRole)){
            //学生、游客
            homeworkLambdaQueryWrapper.eq(Homework::getCommend,1);
        }
        Page<Homework> homeworkPage = homeworkService.page(new Page<>(current,pageSize),homeworkLambdaQueryWrapper);
        Page<HomeworkHistoryVO> homeworkHistoryVOPage = new Page<>();
        BeanUtil.copyProperties(homeworkPage,homeworkHistoryVOPage);
        List<Homework> homeworkList = homeworkPage.getRecords();
        List<HomeworkHistoryVO> homeworkHistoryVOList = homeworkList.stream().map(this::obj2HistoryVO).collect(Collectors.toList());
        homeworkHistoryVOPage.setRecords(homeworkHistoryVOList);
        return homeworkHistoryVOPage;
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
                    log.error("清理临时目录失败", e);
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
                log.error("下载OSS文件失败: " + url, e);
            }
        }
    }
}
