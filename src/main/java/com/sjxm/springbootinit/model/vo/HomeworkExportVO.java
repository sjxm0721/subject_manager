package com.sjxm.springbootinit.model.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/31
 * @Description:
 */
@Data
public class HomeworkExportVO {

    @ExcelProperty(value = "年级")
    @ColumnWidth(15)
    private String grade;

    @ExcelProperty(value = "课程名")
    @ColumnWidth(20)
    private String subjectName;

    @ExcelProperty(value = "组号")
    @ColumnWidth(15)
    private Integer groupNum;

    @ExcelProperty(value = "姓名")
    @ColumnWidth(25)
    private String names;

    @ExcelProperty(value = "题目")
    @ColumnWidth(25)
    private String title;

    @ExcelProperty(value = "简介")
    @ColumnWidth(40)
    private String brief;

    @ExcelProperty(value = "硬件技术")
    @ColumnWidth(40)
    private String hardwareTech;

    @ExcelProperty(value = "软件技术")
    @ColumnWidth(40)
    private String softwareTech;
}
