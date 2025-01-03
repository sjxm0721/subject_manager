package com.sjxm.springbootinit.controller;

import co.elastic.clients.elasticsearch.xpack.usage.Base;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sjxm.springbootinit.annotation.AuthCheck;
import com.sjxm.springbootinit.biz.SubjectBiz;
import com.sjxm.springbootinit.common.BaseResponse;
import com.sjxm.springbootinit.common.ErrorCode;
import com.sjxm.springbootinit.common.ResultUtils;
import com.sjxm.springbootinit.constant.UserConstant;
import com.sjxm.springbootinit.exception.BusinessException;
import com.sjxm.springbootinit.model.dto.subject.SubjectAddOrUpdateRequest;
import com.sjxm.springbootinit.model.dto.subject.SubjectPageRequest;
import com.sjxm.springbootinit.model.entity.Subject;
import com.sjxm.springbootinit.model.entity.User;
import com.sjxm.springbootinit.service.SubjectService;
import com.sjxm.springbootinit.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/19
 * @Description:
 */
@RestController
@RequestMapping("/subject")
public class SubjectController {

    @Resource
    private SubjectBiz subjectBiz;

    @GetMapping("/list")
    @AuthCheck(mustRole = UserConstant.TEACHER_ROLE)
    public BaseResponse<List<Subject>> list(HttpServletRequest request) {
        List<Subject> list = subjectBiz.list(request);
        return ResultUtils.success(list);
    }

    @GetMapping("/list-by-stu")
    @AuthCheck(mustRole = UserConstant.STUDENT_ROLE)
    public BaseResponse<List<Subject>> listByStu(HttpServletRequest request) {
        List<Subject> list = subjectBiz.listByStu(request);
        return ResultUtils.success(list);
    }

    @PostMapping("/page")
    @AuthCheck(mustRole = UserConstant.TEACHER_ROLE)
    public BaseResponse<Page<Subject>> page(@RequestBody SubjectPageRequest subjectPageRequest){
        if(subjectPageRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Page<Subject> page = subjectBiz.page(subjectPageRequest);
        return ResultUtils.success(page);
    }

    @PostMapping("/add-or-update")
    @AuthCheck(mustRole = UserConstant.TEACHER_ROLE)
    public BaseResponse<Boolean> addOrUpdateSubject(@RequestBody SubjectAddOrUpdateRequest subjectAddOrUpdateRequest,HttpServletRequest request){
        if(subjectAddOrUpdateRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        subjectBiz.addOrUpdate(subjectAddOrUpdateRequest,request);
        return ResultUtils.success(true);
    }

    @DeleteMapping("/del/{subjectId}")
    @AuthCheck(mustRole = UserConstant.TEACHER_ROLE)
    public BaseResponse<Boolean> delSubject(@PathVariable Long subjectId){
        subjectBiz.delSubject(subjectId);
        return ResultUtils.success(true);
    }

}
