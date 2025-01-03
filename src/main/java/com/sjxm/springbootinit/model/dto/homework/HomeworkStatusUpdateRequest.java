package com.sjxm.springbootinit.model.dto.homework;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/21
 * @Description:
 */
@Data
public class HomeworkStatusUpdateRequest implements Serializable {

    private Long id;
    private Integer suggested;

}
