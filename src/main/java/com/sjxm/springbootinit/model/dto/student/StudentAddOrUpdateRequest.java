package com.sjxm.springbootinit.model.dto.student;

import com.sjxm.springbootinit.model.vo.StudentVO;
import lombok.Data;

import javax.validation.constraints.NotBlank;
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

    private Integer checkAble =1;

    private Integer uploadAble =0;

    @NotBlank(message = "学号不能为空")
    private String userAccount;

    @NotBlank(message = "姓名不能为空")
    private String userName;

}
