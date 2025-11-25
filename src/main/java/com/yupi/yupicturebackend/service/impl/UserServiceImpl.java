package com.yupi.yupicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicturebackend.constant.UserConstant;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.models.dto.user.UserQueryRequest;
import com.yupi.yupicturebackend.models.entity.User;
import com.yupi.yupicturebackend.models.enums.UserRoleEnum;
import com.yupi.yupicturebackend.models.vo.LoginUserVo;
import com.yupi.yupicturebackend.models.vo.UserVo;
import com.yupi.yupicturebackend.service.UserService;
import com.yupi.yupicturebackend.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author 24636
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-11-07 10:29:43
*/
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验参数是否合法
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword, checkPassword), ErrorCode.PARAMS_ERROR,"参数为空");
        if (userAccount.length() < 4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户账户过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户密码过短");
        }
        if (!userPassword.equals(checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"两次输入的密码不一致");
        }
        // 2. 检查用户账户名是否被占用
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        Long count = this.baseMapper.selectCount(queryWrapper);
        if(count > 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户账户名已存在");
        }
        // 3. 对用户密码进行加密
        String encryptPassword = getEncryptPassword(userPassword);
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("无名");
        user.setUserRole(UserRoleEnum.USER.getValue());
        // 4. 插入数据
        boolean saveResult = this.save(user);
        if (!saveResult){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"注册失败");
        }
        return user.getId();
    }

    @Override
    public String getEncryptPassword(String userPassword) {
        final String SALT = "coderLan";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    @Override
    public LoginUserVo userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 检验参数是否合法
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword), ErrorCode.PARAMS_ERROR,"账号或密码为空");
        if (userAccount.length() < 4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户账户过短");
        }
        if (userPassword.length() < 8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户密码过短");
        }
        // 2. 对密码进行加密处理
        String encryptPassword = getEncryptPassword(userPassword);
        // 3. 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        queryWrapper.eq("userPassword",encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 不存在抛出异常
        if(user == null){
            log.info("user login failed,userCount cannot match userPassword!");
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户名或密码错误");
        }
        // 4. 记录用户的登录态
        HttpSession session = request.getSession();
        session.setAttribute(UserConstant.USER_LOGIN_STATE,user);
        // 5. 返回脱敏后的用户信息
        LoginUserVo  loginUserVo = getLoginUserVo(user);
        return loginUserVo;
    }

    /**
     * 获取脱敏后的用户信息
     * @param user
     * @return
     */
    @Override
    public LoginUserVo getLoginUserVo(User user) {
        // 1. 检验参数是否合法
        if(user == null){
            return null;
        }
        LoginUserVo loginUserVo = new LoginUserVo();
        BeanUtil.copyProperties(user, loginUserVo);
        return loginUserVo;
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        if (request == null){
            return null;
        }
        Object obj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User user = (User) obj;
        if (ObjectUtil.isEmpty(user) || user.getId() == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 查询登录用户最好是从数据库查询，否则可能从登录态中查询到的数据是过时的数据
        user = this.getById(user.getId());
        if(user == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return user;
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Object userLoginState = session.getAttribute(UserConstant.USER_LOGIN_STATE);
        User loginUser = (User) userLoginState;
        // 1. 判断用户是否登录
        ThrowUtils.throwIf(loginUser == null || loginUser.getId() == null,ErrorCode.NOT_LOGIN_ERROR,"用户未登录");
        // 2. 用户已经登录，清除session信息
        session.removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }


    @Override
    public UserVo getUserVo(User user) {
        if(user == null){
            return null;
        }
        UserVo userVo = new UserVo();
        BeanUtils.copyProperties(user,userVo);
        return userVo;
    }

    @Override
    public List<UserVo> getListUserVo(List<User> userList) {
        // 1. 检验参数是否合法
        if(userList == null || userList.size() == 0){
            return new ArrayList<>();
        }
        List<UserVo> userVoList = userList.stream().map(this::getUserVo).collect(Collectors.toList());
        return userVoList;
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"查询参数为空");
        }
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        // 1.  id
        queryWrapper.eq(ObjectUtil.isNotNull(id), "id", id);
        // 2.  userName
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        // 3.  userAccount
        queryWrapper.eq(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        // 4.  userProfile
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        // 5.  userRole
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public boolean isAdmin(User user) {
        // 如果user不为null，并且user的userRole为管理员
        return user != null && UserConstant.ADMIN_ROLE.equals(user.getUserRole());
    }
}




