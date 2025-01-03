package com.sjxm.springbootinit.controller;

import com.sjxm.springbootinit.annotation.AuthCheck;
import com.sjxm.springbootinit.biz.SubjectStudentBiz;
import com.sjxm.springbootinit.common.BaseResponse;
import com.sjxm.springbootinit.common.ErrorCode;
import com.sjxm.springbootinit.common.ResultUtils;
import com.sjxm.springbootinit.constant.UserConstant;
import com.sjxm.springbootinit.exception.BusinessException;
import com.sjxm.springbootinit.model.dto.subjectStudent.SubjectStudentAddRequest;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Author: 四季夏目
 * @Date: 2025/1/1
 * @Description:
 */
@RestController
@RequestMapping("/subject-student")
public class SubjectStudentController{

    @Resource
    private SubjectStudentBiz subjectStudentBiz;

    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.TEACHER_ROLE)
    public BaseResponse<Boolean> add(@RequestBody SubjectStudentAddRequest subjectStudentAddRequest){
        if(subjectStudentAddRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        subjectStudentBiz.add(subjectStudentAddRequest);
        return ResultUtils.success(true);
    }


    @DeleteMapping("/delete/{id}")
    @AuthCheck(mustRole = UserConstant.TEACHER_ROLE)
    public BaseResponse<Boolean> delete(@PathVariable Long id){
        subjectStudentBiz.delete(id);
        return ResultUtils.success(true);
    }
}
