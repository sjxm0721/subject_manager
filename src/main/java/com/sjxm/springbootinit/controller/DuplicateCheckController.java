package com.sjxm.springbootinit.controller;

import com.sjxm.springbootinit.annotation.AuthCheck;
import com.sjxm.springbootinit.biz.DuplicateCheckBiz;
import com.sjxm.springbootinit.common.BaseResponse;
import com.sjxm.springbootinit.common.ResultUtils;
import com.sjxm.springbootinit.constant.UserConstant;
import com.sjxm.springbootinit.model.vo.DuplicateCheckVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author: 四季夏目
 * @Date: 2025/1/3
 * @Description:
 */
@RestController
@RequestMapping("/duplicate-check")
public class DuplicateCheckController {

    @Resource
    private DuplicateCheckBiz duplicateCheckBiz;

    @GetMapping("/list")
    @AuthCheck(mustRole = UserConstant.TEACHER_ROLE)
    public BaseResponse<List<DuplicateCheckVO>> list(Long homeworkId){
        List<DuplicateCheckVO> list = duplicateCheckBiz.list(homeworkId);
        return ResultUtils.success(list);
    }



}
