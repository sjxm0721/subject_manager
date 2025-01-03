package com.sjxm.springbootinit.constant;

/**
 * 用户常量
 *
 * @author <a href="https://github.com/sjxm0721">四季夏目</a>
 
 */
public interface UserConstant {

    /**
     * 用户登录态键
     */
    String USER_LOGIN_STATE = "user_login";

    //  region 权限

    /**
     * 学生
     */
    String STUDENT_ROLE = "student";

    /**
     * 教师管理员
     */
    String TEACHER_ROLE = "teacher";

    /**
     * 游客
     */
    String TOURIST = "tourist";

    /**
     * 账号被封禁
     */
    String BAN = "ban";

    // endregion
}
