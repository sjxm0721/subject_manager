package com.sjxm.springbootinit.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import org.apache.poi.ss.usermodel.HorizontalAlignment;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

public class ExcelExportUtil {

    /**
     * 导出Excel
     * @param response HTTP响应
     * @param fileName 文件名
     * @param data 数据列表
     * @param clazz 导出数据类的Class对象
     */
    public static <T> void exportExcel(HttpServletResponse response,
                                       String fileName,
                                       List<T> data,
                                       Class<T> clazz) throws IOException {
        try {
            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            fileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            // 设置表头样式
            WriteCellStyle headWriteCellStyle = new WriteCellStyle();
            headWriteCellStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);

            // 设置内容样式
            WriteCellStyle contentWriteCellStyle = new WriteCellStyle();
            contentWriteCellStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);

            // 设置样式策略
            HorizontalCellStyleStrategy horizontalCellStyleStrategy = new HorizontalCellStyleStrategy(
                    headWriteCellStyle, contentWriteCellStyle);

            // 导出Excel
            EasyExcel.write(response.getOutputStream(), clazz)
                    .registerWriteHandler(horizontalCellStyleStrategy)
                    .sheet("Sheet1")
                    .doWrite(data);

        } catch (Exception e) {
            response.reset();
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            response.getWriter().println("导出失败：" + e.getMessage());
        }
    }
}