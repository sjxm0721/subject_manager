package com.sjxm.springbootinit.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/23
 * @Description:
 */
@Data
public class DeviceBorrowPageVO implements Serializable {
    private Integer groupNum;
    private Long id;
    private List<UserVO> member;
    private Long subjectId;
    private String subjectName;
    private String deviceName;
    private Integer applyNum;
    private Integer status;
}
