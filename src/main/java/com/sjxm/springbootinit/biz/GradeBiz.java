package com.sjxm.springbootinit.biz;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sjxm.springbootinit.common.ErrorCode;
import com.sjxm.springbootinit.exception.BusinessException;
import com.sjxm.springbootinit.model.dto.grade.GradeSubmitRequest;
import com.sjxm.springbootinit.model.entity.Grade;
import com.sjxm.springbootinit.model.entity.Homework;
import com.sjxm.springbootinit.service.GradeService;
import com.sjxm.springbootinit.service.HomeworkService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/28
 * @Description:
 */
@Component
public class GradeBiz {

    @Resource
    private HomeworkService homeworkService;

    @Resource
    private GradeService gradeService;


    Grade dto2Obj(GradeSubmitRequest.GradeStudentInfo gradeStudentInfo, Long homeworkId){
        Grade grade = new Grade();
        BeanUtil.copyProperties(gradeStudentInfo,grade);
        grade.setHomeworkId(homeworkId);
        return grade;
    }

    @Transactional(rollbackFor = Exception.class)
    public void submitGrades(GradeSubmitRequest gradeSubmitRequest) {
        Long homeworkId = gradeSubmitRequest.getHomeworkId();
        List<GradeSubmitRequest.GradeStudentInfo> scores = gradeSubmitRequest.getScores();
        Homework homework = homeworkService.getById(homeworkId);
        if(homework==null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"作业信息不存在");
        }
        homework.setIsCorrect(1);
        homeworkService.updateById(homework);

        LambdaQueryWrapper<Grade> gradeLambdaQueryWrapper = new LambdaQueryWrapper<>();
        gradeLambdaQueryWrapper.eq(Grade::getHomeworkId,homeworkId);
        gradeService.remove(gradeLambdaQueryWrapper);
        List<Grade> list = scores.stream().map(score->this.dto2Obj(score,homeworkId)).collect(Collectors.toList());
        gradeService.saveBatch(list);
    }

}
