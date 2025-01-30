package com.sjxm.springbootinit.valid;

import com.sjxm.springbootinit.annotation.ValidateTime;
import com.sjxm.springbootinit.model.dto.subject.SubjectAddOrUpdateRequest;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * @Author: 四季夏目
 * @Date: 2025/1/21
 * @Description:
 */
public class TimeValidValidator implements ConstraintValidator<ValidateTime, SubjectAddOrUpdateRequest> {

    @Override
    public boolean isValid(SubjectAddOrUpdateRequest request, ConstraintValidatorContext context) {
        if (request.getStartTime() == null || request.getEndTime() == null) {
            return true;
        }

        try {
            LocalDate startDate = LocalDate.parse(request.getStartTime());
            LocalDate endDate = LocalDate.parse(request.getEndTime());
            return !endDate.isBefore(startDate);
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
