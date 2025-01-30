package com.sjxm.springbootinit.model.dto.homework;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/20
 * @Description:
 */
@Data
public class HomeworkAddRequest {

    private Long id;


    /**
     *
     */
    @NotNull(message = "课程不能为空")
    private Long subjectId;

    /**
     *
     */
    @NotNull(message = "学生不能为空")
    private Long studentId;

    /**
     *
     */
    @NotBlank(message = "标题不能为空")
    private String title;


    /**
     *
     */
    @NotBlank(message = "简介不能为空")
    @Size(max = 300,message = "简介不能超过300字")
    private String brief;

    /**
     *
     */
    @NotBlank(message = "硬件技术不能为空")
    @Size(max = 500,message = "硬件技术不能超过500字")
    private String hardwareTech;

    /**
     *
     */
    @NotBlank(message = "软件技术不能为空")
    @Size(max = 500,message = "软件技术不能超过500字")
    private String softwareTech;

    /**
     *
     */
    @NotBlank(message = "背景不能为空")
    private String background;

    /**
     *
     */
    @NotBlank(message = "系统设计不能为空")
    private String systemDesign;

    /**
     *
     */
    private String attachmentWord;

    /**
     *
     */
    private String attachmentPdf;

    /**
     *
     */
    private String attachmentSource;

    /**
     *
     */
    private String attachmentMp4;

    /**
     * 类型 0-嵌入式  1-物联网
     */
    @NotNull(message = "类型选择不能为空")
    private Integer subjectType;

}
