package com.sjxm.springbootinit.model.dto.student;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/30
 * @Description:
 */
@Data
public class StudentImportExcelDTO {

    @ExcelProperty("学号")
    private String userAccount;

    @ExcelProperty("姓名")
    private String userName;

    @ExcelProperty("班级")
    private String className;

    @ExcelProperty("联系电话")
    private String phone;

    @ExcelProperty("课程名称")
    private String title;

    @ExcelProperty("开课年级")
    private String grade;

    @ExcelProperty("组号")
    private Integer groupNum;

}
