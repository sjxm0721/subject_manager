package com.sjxm.springbootinit.biz;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sjxm.springbootinit.common.ErrorCode;
import com.sjxm.springbootinit.exception.BusinessException;
import com.sjxm.springbootinit.model.dto.subjectStudent.SubjectStudentAddRequest;
import com.sjxm.springbootinit.model.entity.*;
import com.sjxm.springbootinit.service.*;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @Author: 四季夏目
 * @Date: 2025/1/1
 * @Description:
 */
@Component
public class SubjectStudentBiz {

    @Resource
    private SubjectStudentService subjectStudentService;

    @Resource
    private UserService userService;

    @Resource
    private ApplyDeviceService applyDeviceService;

    @Resource
    private HomeworkService homeworkService;

    @Resource
    private GradeService gradeService;

    public void add(SubjectStudentAddRequest subjectStudentAddRequest) {

        SubjectStudent subjectStudent = new SubjectStudent();
        BeanUtils.copyProperties(subjectStudentAddRequest,subjectStudent);
        subjectStudentService.save(subjectStudent);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {

        SubjectStudent subjectStudent = subjectStudentService.getById(id);
        if(subjectStudent==null){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"分组记录不存在");
        }
        LambdaQueryWrapper<ApplyDevice> applyDeviceLambdaQueryWrapper = new LambdaQueryWrapper<>();
        applyDeviceLambdaQueryWrapper.eq(ApplyDevice::getSubjectStudentId,id);
        List<ApplyDevice> applyDeviceList = applyDeviceService.list(applyDeviceLambdaQueryWrapper);
        if(!CollUtil.isEmpty(applyDeviceList)){
            for (ApplyDevice applyDevice : applyDeviceList) {
                Integer status = applyDevice.getStatus();
                if(status==0||status==1||status==3){
                    throw new BusinessException(ErrorCode.OPERATION_ERROR,"存在器材外借，删除失败");
                }
            }
            applyDeviceService.remove(applyDeviceLambdaQueryWrapper);
        }

        Long subjectId = subjectStudent.getSubjectId();
        Integer groupNum = subjectStudent.getGroupNum();
        LambdaQueryWrapper<Homework> homeworkLambdaQueryWrapper = new LambdaQueryWrapper<>();
        homeworkLambdaQueryWrapper.eq(Homework::getSubjectId,subjectId).eq(Homework::getGroupNum,groupNum);

        List<Homework> homeworkList = homeworkService.list(homeworkLambdaQueryWrapper);
        if(!CollUtil.isEmpty(homeworkList)){
            List<Long> homeworkIds = homeworkList.stream().map(Homework::getId).collect(Collectors.toList());
            LambdaQueryWrapper<Grade> gradeLambdaQueryWrapper = new LambdaQueryWrapper<>();
            gradeLambdaQueryWrapper.eq(Grade::getStudentId,subjectStudent.getStudentId()).in(Grade::getHomeworkId,homeworkIds);
            gradeService.remove(gradeLambdaQueryWrapper);
        }

        subjectStudentService.removeById(id);

        //特殊处理:当该分组没有学生，删除作业
        LambdaQueryWrapper<SubjectStudent> subjectStudentLambdaQueryWrapper = new LambdaQueryWrapper<>();
        subjectStudentLambdaQueryWrapper.eq(SubjectStudent::getSubjectId,subjectStudent.getSubjectId()).eq(SubjectStudent::getGroupNum,subjectStudent.getGroupNum());
        List<SubjectStudent> subjectStudentList = subjectStudentService.list(subjectStudentLambdaQueryWrapper);
        if(CollUtil.isEmpty(subjectStudentList)){
            homeworkService.remove(homeworkLambdaQueryWrapper);
        }

    }
}
