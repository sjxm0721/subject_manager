package com.sjxm.springbootinit.model.dto.subject;

import com.sjxm.springbootinit.annotation.ValidateTime;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/30
 * @Description:
 */
@Data
@ValidateTime
public class SubjectAddOrUpdateRequest implements Serializable {
    private Long id;
    @NotBlank(message = "标题不能为空")
    private String title;
    @NotBlank(message = "年级不能为空")
    private String grade;

    @NotBlank(message = "开始时间不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "开始时间格式不正确")
    private String startTime;

    @NotBlank(message = "结束时间不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "结束时间格式不正确")
    private String endTime;
}
