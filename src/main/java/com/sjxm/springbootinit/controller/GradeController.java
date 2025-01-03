package com.sjxm.springbootinit.controller;

import com.sjxm.springbootinit.annotation.AuthCheck;
import com.sjxm.springbootinit.biz.GradeBiz;
import com.sjxm.springbootinit.common.BaseResponse;
import com.sjxm.springbootinit.common.ErrorCode;
import com.sjxm.springbootinit.common.ResultUtils;
import com.sjxm.springbootinit.constant.UserConstant;
import com.sjxm.springbootinit.exception.BusinessException;
import com.sjxm.springbootinit.model.dto.grade.GradeSubmitRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/28
 * @Description:
 */
@RestController
@RequestMapping("/grade")
public class GradeController {

    @Resource
    private GradeBiz gradeBiz;

    @PostMapping("/submit")
    @AuthCheck(mustRole = UserConstant.TEACHER_ROLE)
    public BaseResponse<Boolean> submitGrades(@RequestBody GradeSubmitRequest gradeSubmitRequest){
        if(gradeSubmitRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        gradeBiz.submitGrades(gradeSubmitRequest);
        return ResultUtils.success(true);
    }



}
