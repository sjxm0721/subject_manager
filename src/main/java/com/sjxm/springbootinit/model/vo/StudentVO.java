package com.sjxm.springbootinit.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/18
 * @Description:
 */
@Data
public class StudentVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    private String userAccount;


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
    private String userRole;

    /**
     * 创建时间
     */
    private Date createTime;

    private Integer uploadAble;

    private Integer checkAble;

    private String phone;

    private String className;

    private List<GroupDetail> groupDetails;

    @Data
    public static class GroupDetail{
        private Long subjectStudentId;
        private Long subjectId;
        private String subjectName;
        private Integer groupNum;
    }

    private static final long serialVersionUID = 1L;

}
