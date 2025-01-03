package com.sjxm.springbootinit.model.dto.grade;

import lombok.Data;

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
    private Long homeworkId;

    private List<GradeStudentInfo> scores;

    @Data
    public static class GradeStudentInfo implements Serializable{
        private Long studentId;
        private BigDecimal score;
    }
}
