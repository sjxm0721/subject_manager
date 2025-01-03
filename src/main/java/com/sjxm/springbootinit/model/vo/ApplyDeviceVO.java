package com.sjxm.springbootinit.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/27
 * @Description:
 */
@Data
public class ApplyDeviceVO implements Serializable {
    private Long id;

    private Long deviceId;

    private String deviceName;

    private Integer applyNum;

    private Integer status;

    private String statusValue;

    private Long subjectStudentId;

    private String pic;

    private String description;

    private String helpB;
}
