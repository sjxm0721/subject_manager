package com.sjxm.springbootinit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sjxm.springbootinit.model.entity.Device;
import com.sjxm.springbootinit.model.vo.DeviceBorrowExportVO;

import java.util.List;

/**
* @author sijixiamu
* @description 针对表【device】的数据库操作Service实现
* @createDate 2024-12-19 14:19:41
*/
public interface DeviceService extends IService<Device> {

    List<DeviceBorrowExportVO> getDeviceBorrowExportList();
}




