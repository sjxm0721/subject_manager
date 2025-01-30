package com.sjxm.springbootinit.model.dto.device;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/19
 * @Description:
 */
@Data
public class DeviceBorrowRequest implements Serializable {

    @NotNull(message = "设备id不能为空")
    private Long id;

    @NotNull(message = "借用人信息不能为空")
    private Long subjectStudentId;

    @NotNull(message = "借用数量不能为空")
    @Min(value = 1, message = "借用数量不能小于1")
    private Integer applyNum;

}
