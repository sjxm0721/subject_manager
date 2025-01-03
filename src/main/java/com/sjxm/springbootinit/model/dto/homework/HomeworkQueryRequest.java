package com.sjxm.springbootinit.model.dto.homework;

import com.sjxm.springbootinit.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/18
 * @Description:
 */

@EqualsAndHashCode(callSuper = true)
@Data
public class HomeworkQueryRequest extends PageRequest implements Serializable {

    private String subjectName;

    private String grade;

    private String title;

    private static final long serialVersionUID = 1L;

}
