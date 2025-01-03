package com.sjxm.springbootinit.model.dto.homework;

import com.sjxm.springbootinit.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/28
 * @Description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HomeworkGradePageRequest extends PageRequest implements Serializable{
    private Long subjectId;
    private Integer isCorrect;
}
