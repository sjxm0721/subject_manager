package com.sjxm.springbootinit.biz;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sjxm.springbootinit.common.ErrorCode;
import com.sjxm.springbootinit.common.ResultUtils;
import com.sjxm.springbootinit.constant.UserConstant;
import com.sjxm.springbootinit.exception.BusinessException;
import com.sjxm.springbootinit.model.dto.student.StudentAddOrUpdateRequest;
import com.sjxm.springbootinit.model.dto.student.StudentGroupNumQueryRequest;
import com.sjxm.springbootinit.model.dto.student.StudentImportExcelDTO;
import com.sjxm.springbootinit.model.dto.student.StudentQueryRequest;
import com.sjxm.springbootinit.model.entity.Subject;
import com.sjxm.springbootinit.model.entity.SubjectStudent;
import com.sjxm.springbootinit.model.entity.User;
import com.sjxm.springbootinit.model.enums.UserRoleEnum;
import com.sjxm.springbootinit.model.vo.StudentVO;
import com.sjxm.springbootinit.service.SubjectService;
import com.sjxm.springbootinit.service.SubjectStudentService;
import com.sjxm.springbootinit.service.UserService;
import com.sjxm.springbootinit.utils.ExcelImportUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.sjxm.springbootinit.service.impl.UserServiceImpl.SALT;

/**
 * @Author: 四季夏目
 * @Date: 2024/12/19
 * @Description:
 */

@Component
@Slf4j
public class StudentBiz {

    @Resource
    private UserService userService;

    @Resource
    private SubjectService subjectService;

    @Resource
    private SubjectStudentService subjectStudentService;

    @Resource
    private SubjectStudentBiz subjectStudentBiz;

    StudentVO user2StudentVO(User user){
        if(ObjectUtil.isNull(user)){
            return null;
        }
        StudentVO studentVO = new StudentVO();
        BeanUtil.copyProperties(user,studentVO);
        Long studentId = user.getId();
        LambdaQueryWrapper<SubjectStudent> subjectStudentLambdaQueryWrapper = new LambdaQueryWrapper<>();
        subjectStudentLambdaQueryWrapper.eq(SubjectStudent::getStudentId,studentId);
        List<SubjectStudent> list = subjectStudentService.list(subjectStudentLambdaQueryWrapper);
        List<StudentVO.GroupDetail> groupDetails = new ArrayList<>();
        for (SubjectStudent subjectStudent : list) {
            StudentVO.GroupDetail groupDetail = new StudentVO.GroupDetail();
            Subject subject = subjectService.getById(subjectStudent.getSubjectId());
            if(subject!=null){
                groupDetail.setSubjectId(subject.getId());
                groupDetail.setSubjectName(subject.getTitle());
            }
            groupDetail.setSubjectStudentId(subjectStudent.getId());
            groupDetail.setGroupNum(subjectStudent.getGroupNum());
            groupDetails.add(groupDetail);
        }
        studentVO.setGroupDetails(groupDetails);
        return studentVO;
    }

    public Page<StudentVO> listUserByPage(StudentQueryRequest studentQueryRequest,
                                                        HttpServletRequest request) {
        String userName = studentQueryRequest.getUserName();
        String className = studentQueryRequest.getClassName();
        long current = studentQueryRequest.getCurrent();
        long size = studentQueryRequest.getPageSize();
        LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userLambdaQueryWrapper.eq(User::getUserRole, UserRoleEnum.STUDENT.getValue())
                .like(!StrUtil.isBlankIfStr(userName),User::getUserName,userName)
                .like(!StrUtil.isBlankIfStr(className),User::getClassName,className);
        Page<User> userPage = userService.page(new Page<>(current, size),
                userLambdaQueryWrapper);
        List<User> records = userPage.getRecords();
        Page<StudentVO> studentVOPage = new Page<>();
        BeanUtil.copyProperties(userPage,studentVOPage);
        List<StudentVO> studentVOList = records.stream().map(this::user2StudentVO).collect(Collectors.toList());
        studentVOPage.setRecords(studentVOList);
        return studentVOPage;
    }

    @Transactional(rollbackFor = Exception.class)
    public void addOrUpdateStudent(StudentAddOrUpdateRequest studentAddOrUpdateRequest, HttpServletRequest request) {
        User user = new User();
        BeanUtil.copyProperties(studentAddOrUpdateRequest,user);
        userService.addStudent(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteStudent(Long id) {
        userService.removeById(id);
        LambdaQueryWrapper<SubjectStudent> subjectStudentLambdaQueryWrapper = new LambdaQueryWrapper<>();
        subjectStudentLambdaQueryWrapper.eq(SubjectStudent::getStudentId,id);
        List<SubjectStudent> subjectStudentList = subjectStudentService.list(subjectStudentLambdaQueryWrapper);
        List<Long> subjectStudentIds = subjectStudentList.stream().map(SubjectStudent::getId).collect(Collectors.toList());
        for (Long subjectStudentId : subjectStudentIds) {
            subjectStudentBiz.delete(subjectStudentId);
        }
    }

    public int getGroupNum(StudentGroupNumQueryRequest request) {
        Long studentId = request.getStudentId();
        Long subjectId = request.getSubjectId();
        LambdaQueryWrapper<SubjectStudent> subjectStudentLambdaQueryWrapper = new LambdaQueryWrapper<>();
        subjectStudentLambdaQueryWrapper.eq(SubjectStudent::getStudentId,studentId).eq(SubjectStudent::getSubjectId,subjectId);
        SubjectStudent subjectStudent = subjectStudentService.getOne(subjectStudentLambdaQueryWrapper);
        if(subjectStudent==null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return subjectStudent.getGroupNum();
    }

    public void importStudents(InputStream inputStream) {
        ExcelImportUtil.importExcel(inputStream, StudentImportExcelDTO.class,this::excelAddStudents);
    }

    @Transactional(rollbackFor = Exception.class)
    public void excelAddStudents(List<StudentImportExcelDTO> list){
        List<String> errorMsgs = new ArrayList<>();
        for (StudentImportExcelDTO studentImportExcelDTO : list) {
            String errorMsg = validateStudentExcel(studentImportExcelDTO);
            if(!StrUtil.isBlankIfStr(errorMsg)){
                errorMsgs.add(String.format("用户%s:%s",studentImportExcelDTO.getUserAccount(),errorMsg));
            }
        }

        if(!errorMsgs.isEmpty()){
            log.info("Excel数据校验失败：\n{}", String.join("\n", errorMsgs));
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }

        //数据转换
        List<String> userAccounts = list.stream().map(StudentImportExcelDTO::getUserAccount).collect(Collectors.toList());

        LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userLambdaQueryWrapper.in(User::getUserAccount,userAccounts);
        List<User> existingUsers = userService.list(userLambdaQueryWrapper);
        Map<String,User> existingUserMap = existingUsers.stream().collect(Collectors.toMap(User::getUserAccount,user->user));

        for (StudentImportExcelDTO dto : list) {
            if(existingUserMap.containsKey(dto.getUserAccount())){
                //更新
                User existStudent = existingUserMap.get(dto.getUserAccount());
                User user = excelImportDTO2User(dto);
                user.setId(existStudent.getId());
                user.setCreateTime(existStudent.getCreateTime());
                userService.updateById(user);

                String title = dto.getTitle();
                String grade = dto.getGrade();
                Integer groupNum = dto.getGroupNum();
                LambdaQueryWrapper<Subject> subjectLambdaQueryWrapper = new LambdaQueryWrapper<>();
                subjectLambdaQueryWrapper.eq(Subject::getTitle,title).eq(Subject::getGrade,grade);
                Subject subject = subjectService.getOne(subjectLambdaQueryWrapper);
                if(subject==null){
                    throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"课程信息不存在");
                }
                LambdaQueryWrapper<SubjectStudent> subjectStudentLambdaQueryWrapper = new LambdaQueryWrapper<>();
                subjectStudentLambdaQueryWrapper.eq(SubjectStudent::getSubjectId,subject.getId()).eq(SubjectStudent::getStudentId,user.getId());
                SubjectStudent subjectStudent = subjectStudentService.getOne(subjectStudentLambdaQueryWrapper);
                if(subjectStudent==null){
                    //新增
                    SubjectStudent newSubjectStudent = new SubjectStudent();
                    newSubjectStudent.setStudentId(user.getId());
                    newSubjectStudent.setSubjectId(subject.getId());
                    newSubjectStudent.setGroupNum(groupNum);
                    subjectStudentService.save(newSubjectStudent);
                }
                else{
                    //修改
                    subjectStudent.setGroupNum(groupNum);
                    subjectStudentService.updateById(subjectStudent);
                }
            }else{
                //新增
                User user = excelImportDTO2User(dto);
                String defaultPassword = "123456";
                String encryptPassword = DigestUtils.md5DigestAsHex((SALT + defaultPassword).getBytes());
                user.setUserPassword(encryptPassword);
                user.setCheckAble(1);
                user.setUploadAble(1);
                userService.save(user);
                String title = dto.getTitle();
                String grade = dto.getGrade();
                Integer groupNum = dto.getGroupNum();
                LambdaQueryWrapper<Subject> subjectLambdaQueryWrapper = new LambdaQueryWrapper<>();
                subjectLambdaQueryWrapper.eq(Subject::getTitle,title).eq(Subject::getGrade,grade);
                Subject subject = subjectService.getOne(subjectLambdaQueryWrapper);
                if(subject==null){
                    throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"课程信息不存在");
                }
                SubjectStudent subjectStudent = new SubjectStudent();
                subjectStudent.setStudentId(user.getId());
                subjectStudent.setSubjectId(subject.getId());
                subjectStudent.setGroupNum(groupNum);
                subjectStudentService.save(subjectStudent);
            }
        }

    }

    private String validateStudentExcel(StudentImportExcelDTO studentImportExcelDTO) {
        StringBuilder errorMsg = new StringBuilder();
        String userAccount = studentImportExcelDTO.getUserAccount();
        String userName = studentImportExcelDTO.getUserName();
        String className = studentImportExcelDTO.getClassName();
        String phone = studentImportExcelDTO.getPhone();
        String title = studentImportExcelDTO.getTitle();
        String grade = studentImportExcelDTO.getGrade();
        Integer groupNum = studentImportExcelDTO.getGroupNum();

        // 校验必填字段
        if(StrUtil.isBlank(userAccount)){
            errorMsg.append("学号不能为空；");
        }

        if (StrUtil.isBlank(userName)) {
            errorMsg.append("姓名不能为空；");
        } else if (userName.length() > 50) {
            errorMsg.append("姓名长度不能超过50个字符；");
        }

        if(StrUtil.isBlank(className)){
            errorMsg.append("班级不能为空；");
        }

        if(StrUtil.isBlank(title)){
            errorMsg.append("课程不能为空；");
        }

        if(StrUtil.isBlank(grade)){
            errorMsg.append("开课年级不能为空；");
        }

        if(groupNum==null||groupNum<0){
            errorMsg.append("组号不正确；");
        }

        if (StrUtil.isNotBlank(phone)) {
            String phoneRegex = "^1[3-9]\\d{9}$";
            if (!phone.matches(phoneRegex)) {
                errorMsg.append("手机号格式不正确；");
            }
        }
        return errorMsg.toString();
    }

    User excelImportDTO2User(StudentImportExcelDTO studentImportExcelDTO){
        User user = new User();
        BeanUtil.copyProperties(studentImportExcelDTO,user);
        user.setUserRole(UserRoleEnum.STUDENT.getValue());
        return user;
    }

    public StudentVO detail(Long id) {
        User user = userService.getById(id);
        if(user==null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return user2StudentVO(user);
    }
}
