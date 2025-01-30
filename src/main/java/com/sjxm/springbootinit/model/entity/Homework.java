package com.sjxm.springbootinit.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 
 * @TableName homework
 */
@TableName(value ="homework")
@Data
public class Homework implements Serializable {
    /**
     * 
     */
    @TableId
    private Long id;

    /**
     * 
     */
    private Long subjectId;

    /**
     * 
     */
    private Integer groupNum;

    /**
     * 
     */
    private String title;

    /**
     * 0-不推荐
1-推荐
     */
    private Integer commend;

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
     * 测试时间
     */
    private Date createTime;

    /**
     * 最后修改时间
     */
    private Date updateTime;

    /**
     * 
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 类型 0-嵌入式  1-物联网
     */
    private Integer subjectType;

    private String post;

    @TableField(updateStrategy = FieldStrategy.NEVER,
            insertStrategy = FieldStrategy.NEVER)
    private Integer submitYear;


    private Integer isCorrect;

    private Integer checkStatus;

    @Version
    private Integer version;


    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}