package com.sjxm.springbootinit.model.vo;

import com.sjxm.springbootinit.model.entity.Grade;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/28
 * @Description:
 */
@Data
public class HomeworkGradeVO implements Serializable {

    private Long id;

    /**
     *
     */
    private Long subjectId;

    /**
     * 课程名称
     */
    private String subjectName;

    /**
     * 组号
     */
    private Integer groupNum;

    /**
     *
     */
    private String title;

    private Integer isCorrect;

    private Grade score;

}
