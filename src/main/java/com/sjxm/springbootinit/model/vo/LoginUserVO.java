package com.sjxm.springbootinit.model.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 已登录用户视图（脱敏）
 *
 * @author <a href="https://github.com/sjxm0721">四季夏目</a>
 
 **/
@Data
public class LoginUserVO implements Serializable {

    /**
     * 用户 id
     */
    private Long id;

    /**
     * 用户名
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin/ban
     */
    private Integer userRole;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 班级名称
     */
    private String className;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 令牌
     */
    private String token;

    /**
     * 手机
     */
    private String phone;

    private static final long serialVersionUID = 1L;
}