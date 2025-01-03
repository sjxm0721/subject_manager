package com.sjxm.springbootinit.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 
 * @TableName device
 */
@TableName(value ="device")
@Data
public class Device implements Serializable {
    /**
     * 
     */
    @TableId
    private Long id;

    /**
     * 
     */
    private String deviceName;

    /**
     * 
     */
    private String pic;

    /**
     * 
     */
    private String description;

    /**
     * 
     */
    private String helpB;

    /**
     * 
     */
    private Integer totalNum;

    /**
     * 
     */
    private Integer outerNum;

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