package com.sjxm.springbootinit.model.dto.homework;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/21
 * @Description:
 */
@Data
public class HomeworkStatusUpdateRequest implements Serializable {

    @NotNull(message = "作业id不能为空")
    private Long id;
    @NotNull(message = "作业状态不能为空")
    private Integer suggested;

}
