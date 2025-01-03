package com.sjxm.springbootinit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sjxm.springbootinit.common.ErrorCode;
import com.sjxm.springbootinit.exception.BusinessException;
import com.sjxm.springbootinit.mapper.SubjectStudentMapper;
import com.sjxm.springbootinit.model.entity.SubjectStudent;
import com.sjxm.springbootinit.model.vo.StudentVO;
import com.sjxm.springbootinit.service.SubjectStudentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author sijixiamu
* @description 针对表【subject_student】的数据库操作Service实现
* @createDate 2024-12-19 10:39:33
*/
@Service
public class SubjectStudentServiceImpl extends ServiceImpl<SubjectStudentMapper, SubjectStudent>
    implements SubjectStudentService {
}




