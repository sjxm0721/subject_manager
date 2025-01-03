package com.sjxm.springbootinit.biz;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sjxm.springbootinit.common.ErrorCode;
import com.sjxm.springbootinit.exception.BusinessException;
import com.sjxm.springbootinit.model.dto.device.*;
import com.sjxm.springbootinit.model.entity.*;
import com.sjxm.springbootinit.model.enums.ApplyDeviceStatusEnum;
import com.sjxm.springbootinit.model.vo.ApplyDeviceVO;
import com.sjxm.springbootinit.model.vo.DeviceBorrowExportVO;
import com.sjxm.springbootinit.model.vo.DeviceBorrowPageVO;
import com.sjxm.springbootinit.model.vo.UserVO;
import com.sjxm.springbootinit.service.*;
import com.sjxm.springbootinit.utils.ExcelExportUtil;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/19
 * @Description:
 */

@Component
public class DeviceBiz {

    @Resource
    private DeviceService deviceService;

    @Resource
    private ApplyDeviceService applyDeviceService;

    @Resource
    private SubjectStudentService subjectStudentService;

    @Resource
    private UserService userService;

    @Resource
    private SubjectService subjectService;

    DeviceBorrowPageVO obj2DeviceBorrowPageVO(ApplyDevice applyDevice){
        Long deviceId = applyDevice.getDeviceId();
        Long subjectStudentId = applyDevice.getSubjectStudentId();
        Integer applyNum = applyDevice.getApplyNum();
        Integer status = applyDevice.getStatus();


        DeviceBorrowPageVO deviceBorrowPageVO = new DeviceBorrowPageVO();
        SubjectStudent subjectStudent = subjectStudentService.getById(subjectStudentId);
        if(subjectStudent!=null){
            deviceBorrowPageVO.setGroupNum(subjectStudent.getGroupNum());
            deviceBorrowPageVO.setSubjectId(subjectStudent.getSubjectId());
            Subject subject = subjectService.getById(subjectStudent.getSubjectId());
            if(subject!=null){
                deviceBorrowPageVO.setSubjectName(subject.getTitle());
            }
            LambdaQueryWrapper<SubjectStudent> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(SubjectStudent::getSubjectId,subjectStudent.getSubjectId()).eq(SubjectStudent::getGroupNum,subjectStudent.getGroupNum());
            List<SubjectStudent> subjectStudentList = subjectStudentService.list(lambdaQueryWrapper);
            Set<Long> studentIds = subjectStudentList.stream().map(SubjectStudent::getStudentId).collect(Collectors.toSet());
            LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
            userLambdaQueryWrapper.in(!CollUtil.isEmpty(studentIds),User::getId,studentIds);
            List<User> list = userService.list(userLambdaQueryWrapper);
            List<UserVO> userVOList = list.stream().map(user -> userService.getUserVO(user)).collect(Collectors.toList());
            deviceBorrowPageVO.setMember(userVOList);

        }
        Device device = deviceService.getById(deviceId);
        if(device!=null){
            deviceBorrowPageVO.setDeviceName(device.getDeviceName());
        }
        deviceBorrowPageVO.setId(applyDevice.getId());
        deviceBorrowPageVO.setApplyNum(applyNum);
        deviceBorrowPageVO.setStatus(status);
        return deviceBorrowPageVO;
    }

    ApplyDeviceVO ApplyDevice2VO(ApplyDevice applyDevice){
        ApplyDeviceVO applyDeviceVO = new ApplyDeviceVO();
        Long deviceId = applyDevice.getDeviceId();
        BeanUtil.copyProperties(applyDevice,applyDeviceVO);
        Device device = deviceService.getById(deviceId);
        if(device!=null){
            applyDeviceVO.setDeviceName(device.getDeviceName());
            applyDeviceVO.setPic(device.getPic());
            applyDeviceVO.setHelpB(device.getHelpB());
            applyDeviceVO.setDescription(device.getDescription());
        }
        applyDeviceVO.setStatusValue(Objects.requireNonNull(ApplyDeviceStatusEnum.getEnumByValue(applyDevice.getStatus())).getText());
        return applyDeviceVO;
    }


    public Page<Device> page(DeviceQueryRequest request) {

        long current = request.getCurrent();
        long size = request.getPageSize();
        String deviceName = request.getDeviceName();
        LambdaQueryWrapper<Device> deviceLambdaQueryWrapper = new LambdaQueryWrapper<>();
        deviceLambdaQueryWrapper.like(!StrUtil.isBlankIfStr(deviceName),Device::getDeviceName,deviceName);
        return deviceService.page(new Page<>(current, size),
                deviceLambdaQueryWrapper);
    }

    public void update(DeviceUpdateRequest request) {
        Long id = request.getId();
        String deviceName = request.getDeviceName();
        Integer totalNum = request.getTotalNum();
        String description = request.getDescription();
        if(id==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LambdaUpdateWrapper<Device> deviceLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        deviceLambdaUpdateWrapper.set(!StrUtil.isBlankIfStr(deviceName),Device::getDeviceName,deviceName)
                .set(totalNum!=null,Device::getTotalNum,totalNum)
                .set(Device::getDescription,description);
        deviceService.update(deviceLambdaUpdateWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public void borrowDevice(DeviceBorrowRequest request, HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        Long id = request.getId();
        Long subjectId = request.getSubjectStudentId();
        Integer applyNum = request.getApplyNum();

        Device device = deviceService.getById(id);
        if(device==null){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"设备不存在");
        }
        int leftNum = device.getTotalNum()-device.getOuterNum();
        if(applyNum>leftNum){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"申请数量超过可用数量");
        }
        ApplyDevice applyDevice = new ApplyDevice();
        applyDevice.setDeviceId(id);

        LambdaQueryWrapper<SubjectStudent> subjectStudentLambdaQueryWrapper = new LambdaQueryWrapper<>();
        subjectStudentLambdaQueryWrapper.eq(SubjectStudent::getSubjectId,subjectId).eq(SubjectStudent::getStudentId,loginUser.getId());
        SubjectStudent subjectStudent = subjectStudentService.getOne(subjectStudentLambdaQueryWrapper);
        if(subjectStudent!=null){
            applyDevice.setSubjectStudentId(subjectStudent.getId());
        }
        applyDevice.setApplyNum(applyNum);
        applyDevice.setStatus(0);
        applyDeviceService.save(applyDevice);

        //修改器材数量
        device.setOuterNum(device.getOuterNum()+applyNum);
        deviceService.updateById(device);
    }

    public Page<DeviceBorrowPageVO> borrowPage(DeviceBorrowPageRequest deviceBorrowPageRequest) {
        Long subjectId = deviceBorrowPageRequest.getSubjectId();
        Integer groupNum = deviceBorrowPageRequest.getGroupNum();
        String deviceName = deviceBorrowPageRequest.getDeviceName();
        Integer status = deviceBorrowPageRequest.getStatus();
        int current = deviceBorrowPageRequest.getCurrent();
        int pageSize = deviceBorrowPageRequest.getPageSize();

        LambdaQueryWrapper<ApplyDevice> applyDeviceLambdaQueryWrapper = new LambdaQueryWrapper<>();
        applyDeviceLambdaQueryWrapper.eq(status!=null,ApplyDevice::getStatus,status);

        LambdaQueryWrapper<Device> deviceLambdaQueryWrapper = new LambdaQueryWrapper<>();
        deviceLambdaQueryWrapper.like(!StrUtil.isBlankIfStr(deviceName),Device::getDeviceName,deviceName);
        List<Device> deviceList = deviceService.list(deviceLambdaQueryWrapper);
        if(CollUtil.isEmpty(deviceList)){
            return new Page<>();
        }
        Set<Long> deviceIdSet = deviceList.stream().map(Device::getId).collect(Collectors.toSet());
        applyDeviceLambdaQueryWrapper.in(!CollUtil.isEmpty(deviceIdSet),ApplyDevice::getDeviceId,deviceIdSet);

        LambdaQueryWrapper<SubjectStudent> subjectStudentLambdaQueryWrapper = new LambdaQueryWrapper<>();
        subjectStudentLambdaQueryWrapper.eq(subjectId!=null,SubjectStudent::getSubjectId,subjectId).eq(groupNum!=null,SubjectStudent::getGroupNum,groupNum);
        List<SubjectStudent> subjectStudentList = subjectStudentService.list(subjectStudentLambdaQueryWrapper);
        if(CollUtil.isEmpty(subjectStudentList)){
            return new Page<>();
        }
        Set<Long> subjectStudentSet = subjectStudentList.stream().map(SubjectStudent::getId).collect(Collectors.toSet());
        applyDeviceLambdaQueryWrapper.in(!CollUtil.isEmpty(subjectStudentSet),ApplyDevice::getSubjectStudentId,subjectStudentSet);

        Page<ApplyDevice> applyDevicePage = applyDeviceService.page(new Page<>(current, pageSize), applyDeviceLambdaQueryWrapper);
        Page<DeviceBorrowPageVO> deviceBorrowPageVOPage = new Page<>();
        BeanUtil.copyProperties(applyDevicePage,deviceBorrowPageVOPage);

        List<ApplyDevice> records = applyDevicePage.getRecords();
        deviceBorrowPageVOPage.setRecords(records.stream().map(this::obj2DeviceBorrowPageVO).collect(Collectors.toList()));
        return deviceBorrowPageVOPage;
    }

    public void storeDevice(DeviceStoreRequest deviceStoreRequest) {
        Device device = new Device();
        BeanUtil.copyProperties(deviceStoreRequest,device);
        device.setOuterNum(0);
        deviceService.save(device);
    }

    public Page<ApplyDeviceVO> getMyDeviceApply(MyDeviceApplyPageRequest myDeviceApplyPageRequest,HttpServletRequest httpServletRequest) {
        int current = myDeviceApplyPageRequest.getCurrent();
        int pageSize = myDeviceApplyPageRequest.getPageSize();

        User loginUser = userService.getLoginUser(httpServletRequest);
        LambdaQueryWrapper<SubjectStudent> subjectStudentLambdaQueryWrapper = new LambdaQueryWrapper<>();
        subjectStudentLambdaQueryWrapper.eq(SubjectStudent::getStudentId,loginUser.getId());
        List<SubjectStudent> subjectStudentList = subjectStudentService.list(subjectStudentLambdaQueryWrapper);
        if(CollUtil.isEmpty(subjectStudentList)){
            return new Page<>();
        }
        Set<Long> set = subjectStudentList.stream().map(SubjectStudent::getId).collect(Collectors.toSet());
        LambdaQueryWrapper<ApplyDevice> applyDeviceLambdaQueryWrapper = new LambdaQueryWrapper<>();
        applyDeviceLambdaQueryWrapper.in(!CollUtil.isEmpty(set),ApplyDevice::getSubjectStudentId,set);
        Page<ApplyDevice> page = applyDeviceService.page(new Page<>(current,pageSize),applyDeviceLambdaQueryWrapper);
        List<ApplyDevice> list = page.getRecords();
        List<ApplyDeviceVO> applyDeviceVOS = list.stream().map(this::ApplyDevice2VO).collect(Collectors.toList());
        Page<ApplyDeviceVO> voPage = new Page<>();
        BeanUtil.copyProperties(page,voPage);
        voPage.setRecords(applyDeviceVOS);
        return voPage;
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelApply(Long id) {
        ApplyDevice applyDevice = applyDeviceService.getById(id);
        if(applyDevice==null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"租借状态不存在");
        }
        if(!Objects.equals(applyDevice.getStatus(), ApplyDeviceStatusEnum.APPLYING.getValue())){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"租借状态异常");
        }
        Device device = deviceService.getById(applyDevice.getDeviceId());
        if(device==null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"该器材不存在");
        }
        device.setOuterNum(device.getOuterNum()-applyDevice.getApplyNum());
        deviceService.updateById(device);
        applyDeviceService.removeById(id);
    }


    public void returnDevice(Long id) {
        ApplyDevice applyDevice = applyDeviceService.getById(id);
        if(applyDevice==null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"租借信息不存在");
        }
        applyDevice.setStatus(3);
        applyDeviceService.updateById(applyDevice);
    }

    public void approveBorrow(Long id) {
        ApplyDevice applyDevice = applyDeviceService.getById(id);
        if(applyDevice==null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"租借信息不存在");
        }
        if(!Objects.equals(applyDevice.getStatus(), ApplyDeviceStatusEnum.APPLYING.getValue())){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"租借状态异常");
        }
        applyDevice.setStatus(ApplyDeviceStatusEnum.APPROVE.getValue());
        applyDeviceService.updateById(applyDevice);
    }

    @Transactional(rollbackFor = Exception.class)
    public void rejectBorrow(Long id) {
        ApplyDevice applyDevice = applyDeviceService.getById(id);
        if(applyDevice==null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"租借信息不存在");
        }
        if(!Objects.equals(applyDevice.getStatus(), ApplyDeviceStatusEnum.APPLYING.getValue())){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"租借状态异常");
        }
        applyDevice.setStatus(ApplyDeviceStatusEnum.REFUSE.getValue());
        applyDeviceService.updateById(applyDevice);
        Device device = deviceService.getById(applyDevice.getDeviceId());
        if(device==null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"该器材不存在");
        }
        device.setOuterNum(device.getOuterNum()-applyDevice.getApplyNum());
        deviceService.updateById(device);
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmReturn(Long id) {
        ApplyDevice applyDevice = applyDeviceService.getById(id);
        if(applyDevice==null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"租借信息不存在");
        }
        if(!Objects.equals(applyDevice.getStatus(), ApplyDeviceStatusEnum.RETURNING.getValue())){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"租借状态异常");
        }
        applyDevice.setStatus(ApplyDeviceStatusEnum.RETURNED.getValue());
        applyDeviceService.updateById(applyDevice);
        Device device = deviceService.getById(applyDevice.getDeviceId());
        if(device==null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"该器材不存在");
        }
        device.setOuterNum(device.getOuterNum()-applyDevice.getApplyNum());
        deviceService.updateById(device);
    }

    public void export(HttpServletResponse response) {
        try{
            List<DeviceBorrowExportVO> list = deviceService.getDeviceBorrowExportList();
            String fileName = "器材外借信息表-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));
            ExcelExportUtil.exportExcel(response,fileName,list,DeviceBorrowExportVO.class);
        }catch (IOException e){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,e.getMessage());
        }

    }
}
