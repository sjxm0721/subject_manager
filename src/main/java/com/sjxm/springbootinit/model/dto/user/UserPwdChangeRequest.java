package com.sjxm.springbootinit.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/18
 * @Description:
 */
@Data
public class UserPwdChangeRequest implements Serializable {

    private String newPwd;

}
