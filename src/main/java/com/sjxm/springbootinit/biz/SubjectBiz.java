package com.sjxm.springbootinit.biz;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sjxm.springbootinit.common.ErrorCode;
import com.sjxm.springbootinit.exception.BusinessException;
import com.sjxm.springbootinit.model.dto.subject.SubjectAddOrUpdateRequest;
import com.sjxm.springbootinit.model.dto.subject.SubjectPageRequest;
import com.sjxm.springbootinit.model.entity.*;
import com.sjxm.springbootinit.service.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/19
 * @Description:
 */
@Component
public class SubjectBiz {

    @Resource
    private UserService userService;

    @Resource
    private SubjectService subjectService;

    @Resource
    private SubjectStudentService subjectStudentService;

    @Resource
    private ApplyDeviceService applyDeviceService;

    @Resource
    private HomeworkService homeworkService;

    @Resource
    private GradeService gradeService;

    public List<Subject> list(HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        LambdaQueryWrapper<Subject> subjectLambdaQueryWrapper = new LambdaQueryWrapper<>();
        subjectLambdaQueryWrapper.eq(Subject::getTeacherId,loginUser.getId());
        return subjectService.list(subjectLambdaQueryWrapper);
    }

    public List<Subject> listByStu(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        LambdaQueryWrapper<SubjectStudent> subjectStudentLambdaQueryWrapper = new LambdaQueryWrapper<>();
        subjectStudentLambdaQueryWrapper.eq(SubjectStudent::getStudentId,loginUser.getId());
        List<SubjectStudent> list = subjectStudentService.list(subjectStudentLambdaQueryWrapper);
        if(!CollUtil.isEmpty(list)){
            Set<Long> set = list.stream().map(SubjectStudent::getSubjectId).collect(Collectors.toSet());
            LambdaQueryWrapper<Subject> subjectLambdaQueryWrapper = new LambdaQueryWrapper<>();
            subjectLambdaQueryWrapper.in(Subject::getId,set);
            return subjectService.list(subjectLambdaQueryWrapper);
        }
        return new ArrayList<>();
    }

    public Page<Subject> page(SubjectPageRequest subjectPageRequest) {
        int current = subjectPageRequest.getCurrent();
        int pageSize = subjectPageRequest.getPageSize();
        LambdaQueryWrapper<Subject> subjectLambdaQueryWrapper = new LambdaQueryWrapper<>();
        subjectLambdaQueryWrapper.orderBy(true,false,Subject::getCreateTime);
        return subjectService.page(new Page<>(current, pageSize), subjectLambdaQueryWrapper);
    }

    public void addOrUpdate(SubjectAddOrUpdateRequest subjectAddOrUpdateRequest,HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if(ObjectUtil.isNull(loginUser)){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        Subject subject = new Subject();
        BeanUtil.copyProperties(subjectAddOrUpdateRequest,subject);
        subject.setStartTime(Date.valueOf(subjectAddOrUpdateRequest.getStartTime()));
        subject.setEndTime(Date.valueOf(subjectAddOrUpdateRequest.getEndTime()));
        if(subjectAddOrUpdateRequest.getId()==null){
            subject.setTeacherId(loginUser.getId());
        }
        subjectService.saveOrUpdate(subject);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delSubject(Long subjectId) {
        subjectService.removeById(subjectId);
        LambdaQueryWrapper<SubjectStudent> subjectStudentLambdaQueryWrapper = new LambdaQueryWrapper<>();
        subjectStudentLambdaQueryWrapper.eq(SubjectStudent::getSubjectId,subjectId);
        List<SubjectStudent> subjectStudentList = subjectStudentService.list(subjectStudentLambdaQueryWrapper);
        if(!CollUtil.isEmpty(subjectStudentList)){
            List<Long> subjectStudentIds = subjectStudentList.stream().map(SubjectStudent::getId).collect(Collectors.toList());
            subjectStudentService.remove(subjectStudentLambdaQueryWrapper);
            LambdaQueryWrapper<ApplyDevice> applyDeviceLambdaQueryWrapper = new LambdaQueryWrapper<>();
            applyDeviceLambdaQueryWrapper.in(ApplyDevice::getSubjectStudentId,subjectStudentIds);
            List<ApplyDevice> applyDeviceList = applyDeviceService.list(applyDeviceLambdaQueryWrapper);
            if(!CollUtil.isEmpty(applyDeviceList)){
                for (ApplyDevice applyDevice : applyDeviceList) {
                    Integer status = applyDevice.getStatus();
                    if(status==0||status==1||status==3){
                        throw new BusinessException(ErrorCode.OPERATION_ERROR,"该课程存在设备外借，无法删除");
                    }
                }
                applyDeviceService.remove(applyDeviceLambdaQueryWrapper);
            }
        }

        LambdaQueryWrapper<Homework> homeworkLambdaQueryWrapper = new LambdaQueryWrapper<>();
        homeworkLambdaQueryWrapper.eq(Homework::getSubjectId,subjectId);
        List<Homework> homeworkList = homeworkService.list(homeworkLambdaQueryWrapper);
        if(!CollUtil.isEmpty(homeworkList)){
            List<Long> homeworkIds = homeworkList.stream().map(Homework::getId).collect(Collectors.toList());
            LambdaQueryWrapper<Grade> gradeLambdaQueryWrapper = new LambdaQueryWrapper<>();
            gradeLambdaQueryWrapper.in(Grade::getHomeworkId,homeworkIds);
            gradeService.remove(gradeLambdaQueryWrapper);
            homeworkService.remove(homeworkLambdaQueryWrapper);
        }

    }
}
