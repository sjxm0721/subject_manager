package com.sjxm.springbootinit.model.dto.subject;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/30
 * @Description:
 */
@Data
public class SubjectAddOrUpdateRequest implements Serializable {
    private Long id;
    private String title;
    private String grade;
    private String startTime;
    private String endTime;
}
