package com.sjxm.springbootinit.annotation;

import com.sjxm.springbootinit.model.dto.subject.SubjectAddOrUpdateRequest;
import com.sjxm.springbootinit.valid.TimeValidValidator;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * @Author: 四季夏目
 * @Date: 2025/1/21
 * @Description:
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TimeValidValidator.class)
public @interface ValidateTime {
    String message() default "结束时间必须晚于开始时间";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}


