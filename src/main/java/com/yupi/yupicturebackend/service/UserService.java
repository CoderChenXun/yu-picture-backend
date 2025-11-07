package com.yupi.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yupi.yupicturebackend.models.dto.user.UserQueryRequest;
import com.yupi.yupicturebackend.models.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupicturebackend.models.vo.LoginUserVo;
import com.yupi.yupicturebackend.models.vo.UserVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 24636
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-11-07 10:29:43
*/
public interface UserService extends IService<User> {
    /**'
     * 用户注册功能
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @return
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    String getEncryptPassword(String userPassword);

    LoginUserVo userLogin(String userAccount, String userPassword, HttpServletRequest request);

    LoginUserVo getLoginUserVo(User user);

    User getLoginUser(HttpServletRequest request);

    boolean userLogout(HttpServletRequest request);

    UserVo getUserVo(User user);

    List<UserVo> getListUserVo(List<User> userList);

    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);
}
