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
 * @TableName apply_device
 */
@TableName(value ="apply_device")
@Data
public class ApplyDevice implements Serializable {
    /**
     * 
     */
    @TableId
    private Long id;

    /**
     * 
     */
    private Long deviceId;

    /**
     * 
     */
    private Long subjectStudentId;

    /**
     * 
     */
    private Integer applyNum;

    /**
     * 0-申请中
1-已同意
2-拒绝
3-已归还
     */
    private Integer status;

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