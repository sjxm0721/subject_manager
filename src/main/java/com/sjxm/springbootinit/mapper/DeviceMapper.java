package com.sjxm.springbootinit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sjxm.springbootinit.model.entity.Device;
import com.sjxm.springbootinit.model.vo.DeviceBorrowExportVO;

import java.util.List;

/**
* @author sijixiamu
* @description 针对表【device】的数据库操作Mapper
* @createDate 2024-12-19 14:19:41
* @Entity generator.domain.Device
*/
public interface DeviceMapper extends BaseMapper<Device> {

    List<DeviceBorrowExportVO> getDeviceBorrowExportList();
}




