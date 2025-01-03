package com.sjxm.springbootinit.model.dto.device;

import com.sjxm.springbootinit.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/23
 * @Description:
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DeviceBorrowPageRequest extends PageRequest implements Serializable {
    private Long subjectId;
    private Integer groupNum;
    private String deviceName;
    private Integer status;
}
