package com.sjxm.springbootinit.model.dto.device;

import com.sjxm.springbootinit.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/19
 * @Description:
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DeviceQueryRequest  extends PageRequest implements Serializable {

    private String deviceName;
}
