package com.sjxm.springbootinit.model.dto.homework;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/31
 * @Description:
 */
@Data
public class HomeworkExportRequest implements Serializable {

    private String homeworkTitle;

    private String grade;

    private String title;
}
