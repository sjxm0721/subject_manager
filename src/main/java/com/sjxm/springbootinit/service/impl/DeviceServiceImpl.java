package com.sjxm.springbootinit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sjxm.springbootinit.mapper.DeviceMapper;
import com.sjxm.springbootinit.model.entity.Device;
import com.sjxm.springbootinit.model.vo.DeviceBorrowExportVO;
import com.sjxm.springbootinit.service.DeviceService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
* @author sijixiamu
* @description 针对表【device】的数据库操作Service实现
* @createDate 2024-12-19 14:19:41
*/
@Service
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device>
    implements DeviceService {

    @Resource
    private DeviceMapper deviceMapper;


    @Override
    public List<DeviceBorrowExportVO> getDeviceBorrowExportList() {
        return deviceMapper.getDeviceBorrowExportList();
    }
}




