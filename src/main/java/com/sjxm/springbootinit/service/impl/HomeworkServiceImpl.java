package com.sjxm.springbootinit.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sjxm.springbootinit.mapper.HomeworkMapper;
import com.sjxm.springbootinit.model.entity.Homework;
import com.sjxm.springbootinit.model.vo.HomeworkExportVO;
import com.sjxm.springbootinit.model.vo.HomeworkGradeVO;
import com.sjxm.springbootinit.service.HomeworkService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
* @author sijixiamu
* @description 针对表【homework】的数据库操作Service实现
* @createDate 2024-12-20 09:59:12
*/
@Service
public class HomeworkServiceImpl extends ServiceImpl<HomeworkMapper, Homework>
    implements HomeworkService {

    @Resource
    private HomeworkMapper homeworkMapper;


    @Override
    public Page<HomeworkGradeVO> selectHomeworkGradePage(Page<HomeworkGradeVO> page, Long studentId, Long subjectId, Integer isCorrect) {
        return homeworkMapper.selectHomeworkGradePage(page,studentId,subjectId,isCorrect);
    }

    @Override
    public List<HomeworkExportVO> export(String grade, String title,String homeworkTitle) {
        return homeworkMapper.export(grade,title,homeworkTitle);
    }
}




