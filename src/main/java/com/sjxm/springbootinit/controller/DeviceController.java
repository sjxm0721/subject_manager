package com.sjxm.springbootinit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sjxm.springbootinit.annotation.AuthCheck;
import com.sjxm.springbootinit.biz.DeviceBiz;
import com.sjxm.springbootinit.common.BaseResponse;
import com.sjxm.springbootinit.common.ErrorCode;
import com.sjxm.springbootinit.common.ResultUtils;
import com.sjxm.springbootinit.constant.UserConstant;
import com.sjxm.springbootinit.exception.BusinessException;
import com.sjxm.springbootinit.model.dto.device.*;
import com.sjxm.springbootinit.model.entity.Device;
import com.sjxm.springbootinit.model.vo.ApplyDeviceVO;
import com.sjxm.springbootinit.model.vo.DeviceBorrowPageVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/19
 * @Description:
 */
@RestController
@RequestMapping("/device")
public class DeviceController {

    @Resource
    private DeviceBiz deviceBiz;


    @PostMapping("/page")
    public BaseResponse<Page<Device>> page(@RequestBody DeviceQueryRequest request){
        if(request==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Page<Device> page = deviceBiz.page(request);
        return ResultUtils.success(page);
    }

    @AuthCheck(mustRole = UserConstant.TEACHER_ROLE)
    @PostMapping("/update")
    public BaseResponse<Boolean> update(@RequestBody DeviceUpdateRequest request){
        if(request==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        deviceBiz.update(request);
        return ResultUtils.success(true);
    }

    @AuthCheck(mustRole = UserConstant.STUDENT_ROLE)
    @PostMapping("/borrow")
    public BaseResponse<Boolean> borrow(@RequestBody DeviceBorrowRequest request, HttpServletRequest httpServletRequest){
        deviceBiz.borrowDevice(request,httpServletRequest);
        return ResultUtils.success(true);
    }

    @AuthCheck(mustRole = UserConstant.TEACHER_ROLE)
    @PostMapping("/borrow-page")
    public BaseResponse<Page<DeviceBorrowPageVO>> borrowPage(@RequestBody DeviceBorrowPageRequest deviceBorrowPageRequest){
        Page<DeviceBorrowPageVO> deviceBorrowPageVOPage = deviceBiz.borrowPage(deviceBorrowPageRequest);
        return ResultUtils.success(deviceBorrowPageVOPage);
    }


    @AuthCheck(mustRole = UserConstant.TEACHER_ROLE)
    @PostMapping("/store")
    public BaseResponse<Boolean> storeDevice(@RequestBody DeviceStoreRequest deviceStoreRequest){
        deviceBiz.storeDevice(deviceStoreRequest);
        return ResultUtils.success(true);
    }

    @AuthCheck(mustRole = UserConstant.STUDENT_ROLE)
    @PostMapping("/my-apply")
    public BaseResponse<Page<ApplyDeviceVO>> getMyDeviceApply(@RequestBody MyDeviceApplyPageRequest myDeviceApplyPageRequest,HttpServletRequest request){
        Page<ApplyDeviceVO> page = deviceBiz.getMyDeviceApply(myDeviceApplyPageRequest,request);
        return ResultUtils.success(page);
    }

    @AuthCheck(mustRole = UserConstant.STUDENT_ROLE)
    @GetMapping("/cancel-apply")
    public BaseResponse<Boolean> cancelApply(Long id){
        deviceBiz.cancelApply(id);
        return ResultUtils.success(true);
    }

    @AuthCheck(mustRole = UserConstant.STUDENT_ROLE)
    @GetMapping("/return")
    public BaseResponse<Boolean> returnDevice(Long id){
        deviceBiz.returnDevice(id);
        return ResultUtils.success(true);
    }

    @AuthCheck(mustRole = UserConstant.TEACHER_ROLE)
    @GetMapping("/approve-borrow")
    public BaseResponse<Boolean> approveBorrow(Long id){
        deviceBiz.approveBorrow(id);
        return ResultUtils.success(true);
    }


    @AuthCheck(mustRole = UserConstant.TEACHER_ROLE)
    @GetMapping("/reject-borrow")
    public BaseResponse<Boolean> rejectBorrow(Long id){
        deviceBiz.rejectBorrow(id);
        return ResultUtils.success(true);
    }


    @AuthCheck(mustRole = UserConstant.TEACHER_ROLE)
    @GetMapping("/confirm-return")
    public BaseResponse<Boolean> confirmReturn(Long id){
        deviceBiz.confirmReturn(id);
        return ResultUtils.success(true);
    }

    @GetMapping("/export")
    @AuthCheck(mustRole = UserConstant.TEACHER_ROLE)
    public void export(HttpServletResponse response){
        deviceBiz.export(response);
    }
}
