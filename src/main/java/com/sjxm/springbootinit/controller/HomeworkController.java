package com.sjxm.springbootinit.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sjxm.springbootinit.annotation.AuthCheck;
import com.sjxm.springbootinit.biz.GradeBiz;
import com.sjxm.springbootinit.biz.HomeworkBiz;
import com.sjxm.springbootinit.common.BaseResponse;
import com.sjxm.springbootinit.common.ErrorCode;
import com.sjxm.springbootinit.common.ResultUtils;
import com.sjxm.springbootinit.constant.UserConstant;
import com.sjxm.springbootinit.exception.BusinessException;
import com.sjxm.springbootinit.model.dto.homework.*;
import com.sjxm.springbootinit.model.entity.Grade;
import com.sjxm.springbootinit.model.entity.Homework;
import com.sjxm.springbootinit.model.vo.HomeworkGradeVO;
import com.sjxm.springbootinit.model.vo.HomeworkHistoryVO;
import com.sjxm.springbootinit.model.vo.HomeworkVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/20
 * @Description:
 */
@RestController
@RequestMapping("/homework")
public class HomeworkController {

    @Resource
    private HomeworkBiz homeworkBiz;

    @Resource
    private GradeBiz gradeBiz;

    @AuthCheck(mustRole = UserConstant.STUDENT_ROLE)
    @PostMapping("/submit")
    public BaseResponse<Boolean> submitHomework(@RequestBody HomeworkAddRequest request){
        if(request==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        homeworkBiz.submitHomework(request);
        return ResultUtils.success(true);
    }

    @AuthCheck(mustRole = UserConstant.TEACHER_ROLE)
    @PostMapping("/page")
    public BaseResponse<Page<HomeworkVO>> getHomeworkPage(@RequestBody HomeworkQueryRequest homeworkQueryRequest){
        if(homeworkQueryRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Page<HomeworkVO> page = homeworkBiz.getHomeworkPage(homeworkQueryRequest);
        return ResultUtils.success(page);
    }

    @PostMapping("/update-status")
    @AuthCheck(mustRole = UserConstant.TEACHER_ROLE)
    public BaseResponse<Boolean> updateHomeworkStatus(@RequestBody HomeworkStatusUpdateRequest homeworkStatusUpdateRequest){
        if(homeworkStatusUpdateRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        homeworkBiz.updateHomeworkStatus(homeworkStatusUpdateRequest);
        return ResultUtils.success(true);
    }


    @GetMapping("/homework-year")
    public BaseResponse<List<Integer>> getHomeworkYear(){
        List<Integer> years = homeworkBiz.getHomeworkYear();
        return ResultUtils.success(years);
    }

    @AuthCheck(mustRole = UserConstant.TEACHER_ROLE)
    @GetMapping("/homework-year-teacher")
    public BaseResponse<List<Integer>> getHomeworkYearTeacher(){
        List<Integer> years = homeworkBiz.getHomeworkYearTeacher();
        return ResultUtils.success(years);
    }

    @PostMapping("/page-homework-history")
    public BaseResponse<Page<HomeworkHistoryVO>> getHomeworkHistoryPage(@RequestBody HomeworkHistoryPageQueryRequest homeworkHistoryPageQueryRequest, HttpServletRequest request){
        if(homeworkHistoryPageQueryRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Page<HomeworkHistoryVO> homeworkHistoryVOPage = homeworkBiz.getHomeworkHistoryPage(homeworkHistoryPageQueryRequest,request);
        return ResultUtils.success(homeworkHistoryVOPage);
    }

    @GetMapping("/get")
    public BaseResponse<Homework> getHomeworkDetail(Long homeworkId,HttpServletRequest request){
        Homework homework = homeworkBiz.getHomeworkDetail(homeworkId,request);
        return ResultUtils.success(homework);
    }

    @PostMapping("/page-grade")
    @AuthCheck(mustRole = UserConstant.STUDENT_ROLE)
    public BaseResponse<Page<HomeworkGradeVO>> pageGrade(@RequestBody HomeworkGradePageRequest homeworkGradePageRequest, HttpServletRequest request){
        if(homeworkGradePageRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Page<HomeworkGradeVO> page = homeworkBiz.pageGrade(homeworkGradePageRequest,request);
        return ResultUtils.success(page);
    }

    @GetMapping("/get-my")
    @AuthCheck(mustRole = UserConstant.STUDENT_ROLE)
    public BaseResponse<Homework> getMyHomeworkDetail(Long homeworkId,HttpServletRequest request){
        Homework homework = homeworkBiz.getMyHomeworkDetail(homeworkId,request);
        return ResultUtils.success(homework);
    }


    @AuthCheck(mustRole = UserConstant.TEACHER_ROLE)
    @PostMapping("/export-xml")
    public void exportXml(@RequestBody HomeworkExportRequest homeworkExportRequest, HttpServletResponse response){
        homeworkBiz.exportXml(homeworkExportRequest,response);
    }

    @AuthCheck(mustRole = UserConstant.TEACHER_ROLE)
    @PostMapping("/export-zip")
    public void exportZip(@RequestBody HomeworkExportRequest homeworkExportRequest,HttpServletResponse response){
        homeworkBiz.exportZip(homeworkExportRequest,response);
    }




}
