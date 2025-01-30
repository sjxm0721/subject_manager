package com.sjxm.springbootinit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sjxm.springbootinit.annotation.AuthCheck;
import com.sjxm.springbootinit.biz.StudentBiz;
import com.sjxm.springbootinit.common.BaseResponse;
import com.sjxm.springbootinit.common.ErrorCode;
import com.sjxm.springbootinit.common.ResultUtils;
import com.sjxm.springbootinit.constant.UserConstant;
import com.sjxm.springbootinit.exception.BusinessException;
import com.sjxm.springbootinit.model.dto.student.StudentAddOrUpdateRequest;
import com.sjxm.springbootinit.model.dto.student.StudentGroupNumQueryRequest;
import com.sjxm.springbootinit.model.dto.student.StudentQueryRequest;
import com.sjxm.springbootinit.model.vo.StudentVO;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/18
 * @Description:
 */
@RestController
@RequestMapping("/student")
public class StudentController {


    @Resource
    private StudentBiz studentBiz;




    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.TEACHER_ROLE)
    public BaseResponse<Page<StudentVO>> listUserByPage(@RequestBody StudentQueryRequest studentQueryRequest,
                                                   HttpServletRequest request) {
        if(studentQueryRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(studentBiz.listUserByPage(studentQueryRequest,request));
    }

    @PostMapping("/add-update")
    @AuthCheck(mustRole = UserConstant.TEACHER_ROLE)
    public BaseResponse<Boolean> addOrUpdateStudent(@RequestBody @Valid StudentAddOrUpdateRequest studentAddOrUpdateRequest, HttpServletRequest request){
        if(studentAddOrUpdateRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        studentBiz.addOrUpdateStudent(studentAddOrUpdateRequest,request);
        return ResultUtils.success(true);

    }

    @DeleteMapping("/delete/{id}")
    @AuthCheck(mustRole = UserConstant.TEACHER_ROLE)
    public BaseResponse<Boolean> deleteStudent(@PathVariable Long id){
        studentBiz.deleteStudent(id);
        return ResultUtils.success(true);
    }

    @PostMapping("/group-num")
    @AuthCheck(mustRole = UserConstant.STUDENT_ROLE)
    public BaseResponse<Integer> getGroupNum(@RequestBody @Valid StudentGroupNumQueryRequest request){
        int groupNum = studentBiz.getGroupNum(request);
        return ResultUtils.success(groupNum);
    }

    @PostMapping("/import")
    @AuthCheck(mustRole = UserConstant.TEACHER_ROLE)
    public BaseResponse<Boolean> importStudents(@RequestParam("file") MultipartFile file){
        try{
            studentBiz.importStudents(file.getInputStream());
            return ResultUtils.success(true);
        }catch (IOException e){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,e.getMessage());
        }
    }


    @AuthCheck(mustRole = UserConstant.TEACHER_ROLE)
    @GetMapping("/detail")
    public BaseResponse<StudentVO> detail(Long id){
        StudentVO studentVO = studentBiz.detail(id);
        return ResultUtils.success(studentVO);
    }

}
