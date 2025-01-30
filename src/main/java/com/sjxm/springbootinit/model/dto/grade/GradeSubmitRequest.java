package com.sjxm.springbootinit.model.dto.grade;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/28
 * @Description:
 */
@Data
public class GradeSubmitRequest implements Serializable {

    @NotNull(message = "作业ID不能为空")
    private Long homeworkId;

    @NotEmpty(message = "成绩列表不能为空")
    @Valid
    private List<GradeStudentInfo> scores;

    @Data
    public static class GradeStudentInfo implements Serializable{
        @NotNull(message = "学生ID不能为空")
        private Long studentId;
        @NotNull(message = "成绩不能为空")
        @DecimalMin(value = "0.0", message = "成绩不能小于0")
        @DecimalMax(value = "100.0", message = "成绩不能大于100")
        private BigDecimal score;
    }
}
