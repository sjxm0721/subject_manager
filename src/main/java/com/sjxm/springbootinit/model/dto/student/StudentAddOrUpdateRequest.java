package com.sjxm.springbootinit.model.dto.student;

import com.sjxm.springbootinit.model.vo.StudentVO;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/19
 * @Description:
 */
@Data
public class StudentAddOrUpdateRequest implements Serializable {

    private Long id;

    private String className;

    private String phone;

    private Integer checkAble;

    private Integer uploadAble;

    private String userAccount;

    private String userName;

}
