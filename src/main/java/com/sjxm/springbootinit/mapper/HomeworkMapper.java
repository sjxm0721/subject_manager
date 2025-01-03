package com.sjxm.springbootinit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sjxm.springbootinit.model.entity.Homework;
import com.sjxm.springbootinit.model.vo.HomeworkExportVO;
import com.sjxm.springbootinit.model.vo.HomeworkGradeVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author sijixiamu
* @description 针对表【homework】的数据库操作Mapper
* @createDate 2024-12-20 09:59:12
* @Entity generator.domain.Homework
*/
public interface HomeworkMapper extends BaseMapper<Homework> {

    Page<HomeworkGradeVO> selectHomeworkGradePage(Page<HomeworkGradeVO> page, @Param("studentId") Long studentId, @Param("subjectId") Long subjectId, @Param("isCorrect") Integer isCorrect);

    List<HomeworkExportVO> export(@Param("grade") String grade,@Param("title") String title,@Param("homeworkTitle") String homeworkTitle);
}




