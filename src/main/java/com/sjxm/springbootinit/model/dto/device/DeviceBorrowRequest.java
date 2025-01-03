package com.sjxm.springbootinit.model.dto.device;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/19
 * @Description:
 */
@Data
public class DeviceBorrowRequest implements Serializable {

    private Long id;

    private Long subjectStudentId;

    private Integer applyNum;

}
