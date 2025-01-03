package com.sjxm.springbootinit.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sjxm.springbootinit.model.entity.Homework;
import com.sjxm.springbootinit.model.vo.HomeworkExportVO;
import com.sjxm.springbootinit.model.vo.HomeworkGradeVO;

import java.util.List;

/**
* @author sijixiamu
* @description 针对表【homework】的数据库操作Service
* @createDate 2024-12-20 09:59:12
*/
public interface HomeworkService extends IService<Homework> {

    Page<HomeworkGradeVO> selectHomeworkGradePage(Page<HomeworkGradeVO> page, Long studentId, Long subjectId, Integer isCorrect);

    List<HomeworkExportVO> export(String grade, String title,String homeworkTitle);
}
