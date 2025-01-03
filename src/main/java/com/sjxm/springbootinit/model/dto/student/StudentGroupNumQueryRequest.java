package com.sjxm.springbootinit.model.dto.student;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/24
 * @Description:
 */
@Data
public class StudentGroupNumQueryRequest implements Serializable {
    private Long studentId;
    private Long subjectId;

}
