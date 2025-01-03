package com.sjxm.springbootinit.model.dto.device;

import com.sjxm.springbootinit.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/27
 * @Description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MyDeviceApplyPageRequest extends PageRequest implements Serializable {
}
