package com.sjxm.springbootinit.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 
 * @TableName duplicate_check
 */
@TableName(value ="duplicate_check")
@Data
public class DuplicateCheck implements Serializable {
    /**
     * 
     */
    @TableId
    private Long id;

    /**
     * 
     */
    private Long sourceId;

    /**
     * 
     */
    private Long targetId;

    /**
     * 
     */
    private BigDecimal similarity;

    /**
     * 
     */
    private BigDecimal briefValue;

    /**
     * 
     */
    private BigDecimal backgroundValue;

    /**
     * 
     */
    private BigDecimal systemDesignValue;

    /**
     * 
     */
    private BigDecimal wordValue;

    /**
     * 
     */
    private BigDecimal pdfValue;

    /**
     * 
     */
    private BigDecimal sourceValue;

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

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}