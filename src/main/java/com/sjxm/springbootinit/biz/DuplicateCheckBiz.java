package com.sjxm.springbootinit.biz;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sjxm.springbootinit.model.entity.DuplicateCheck;
import com.sjxm.springbootinit.model.entity.Homework;
import com.sjxm.springbootinit.model.vo.DuplicateCheckVO;
import com.sjxm.springbootinit.service.DuplicateCheckService;
import com.sjxm.springbootinit.service.HomeworkService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: 四季夏目
 * @Date: 2025/1/3
 * @Description:
 */
@Component
public class DuplicateCheckBiz {

    @Resource
    private DuplicateCheckService duplicateCheckService;

    @Resource
    private HomeworkService homeworkService;

    DuplicateCheckVO obj2VO(DuplicateCheck duplicateCheck){
        DuplicateCheckVO duplicateCheckVO = new DuplicateCheckVO();
        BeanUtil.copyProperties(duplicateCheck,duplicateCheckVO);
        Homework source = homeworkService.getById(duplicateCheck.getSourceId());
        Homework target = homeworkService.getById(duplicateCheck.getTargetId());
        duplicateCheckVO.setSourceName(source.getTitle());
        duplicateCheckVO.setTargetName(target.getTitle());
        return duplicateCheckVO;
    }

    public List<DuplicateCheckVO> list(Long homeworkId) {
        LambdaQueryWrapper<DuplicateCheck> duplicateCheckLambdaQueryWrapper = new LambdaQueryWrapper<>();
        duplicateCheckLambdaQueryWrapper.eq(DuplicateCheck::getSourceId,homeworkId).orderBy(true,false,DuplicateCheck::getSimilarity).last("LIMIT 10");
        List<DuplicateCheck> list = duplicateCheckService.list(duplicateCheckLambdaQueryWrapper);
        return list.stream().map(this::obj2VO).collect(Collectors.toList());
    }
}
