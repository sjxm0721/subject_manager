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
public class DeviceBorrowExportVO {

    @ExcelProperty(value = "设备名称")
    @ColumnWidth(20)
    private String deviceName;

    @ExcelProperty(value = "课程名称")
    @ColumnWidth(20)
    private String title;

    @ExcelProperty(value = "年级")
    @ColumnWidth(15)
    private String grade;

    @ExcelProperty(value = "组号")
    @ColumnWidth(15)
    private Integer groupNum;

    @ExcelProperty(value = "外借数量")
    @ColumnWidth(15)
    private Integer applyNum;

}
