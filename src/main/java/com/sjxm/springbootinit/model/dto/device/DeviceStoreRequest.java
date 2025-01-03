package com.sjxm.springbootinit.model.dto.device;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/24
 * @Description:
 */
@Data
public class DeviceStoreRequest implements Serializable {

    private String deviceName;
    private String pic;
    private String description;
    private String helpB;
    private Integer totalNum;
}
