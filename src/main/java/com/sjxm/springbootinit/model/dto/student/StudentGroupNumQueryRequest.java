package com.sjxm.springbootinit.model.dto.student;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/24
 * @Description:
 */
@Data
public class StudentGroupNumQueryRequest implements Serializable {

    @NotNull(message = "学生ID不能为空")
    private Long studentId;
    @NotNull(message = "科目ID不能为空")
    private Long subjectId;

}
