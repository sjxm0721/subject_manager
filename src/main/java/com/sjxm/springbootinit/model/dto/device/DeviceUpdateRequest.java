package com.sjxm.springbootinit.model.dto.device;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/19
 * @Description:
 */
@Data
public class DeviceUpdateRequest implements Serializable {

    @NotNull(message = "设备ID不能为空")
    private Long id;

    @NotBlank(message = "设备名称不能为空")
    private String deviceName;

    @NotNull(message = "设备总数不能为空")
    private Integer totalNum;

    private String description;


}
