package com.sjxm.springbootinit.model.dto.homework;

import lombok.Data;

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
    private Long subjectId;

    /**
     *
     */
    private Long studentId;

    /**
     *
     */
    private String title;


    /**
     *
     */
    private String brief;

    /**
     *
     */
    private String hardwareTech;

    /**
     *
     */
    private String softwareTech;

    /**
     *
     */
    private String background;

    /**
     *
     */
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
    private Integer type;

}
