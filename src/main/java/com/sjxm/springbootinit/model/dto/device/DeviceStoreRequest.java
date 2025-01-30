package com.sjxm.springbootinit.model.dto.device;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/24
 * @Description:
 */
@Data
public class DeviceStoreRequest implements Serializable {

    @NotBlank(message = "设备名称不能为空")
    private String deviceName;
    private String pic;
    private String description;
    private String helpB;

    @NotNull(message = "设备总数不能为空")
    @Min(value = 1, message = "设备总数必须大于0")
    private Integer totalNum;
}
