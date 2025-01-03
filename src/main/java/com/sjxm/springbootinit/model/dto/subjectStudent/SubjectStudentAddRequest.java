package com.sjxm.springbootinit.model.dto.subjectStudent;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: 四季夏目
 * @Date: 2025/1/1
 * @Description:
 */
@Data
public class SubjectStudentAddRequest implements Serializable {

    private Long subjectId;

    private Integer groupNum;

    private Long studentId;

}
