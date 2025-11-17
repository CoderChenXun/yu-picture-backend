package com.yupi.yupicturebackend.aop;

import com.yupi.yupicturebackend.annotation.AuthCheck;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.models.entity.User;
import com.yupi.yupicturebackend.models.enums.UserRoleEnum;
import com.yupi.yupicturebackend.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        // 1. 取出角色参数
        String mustRole = authCheck.mustRole();
        // 2. 获取当前登录用户
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest httpServletRequest = ((ServletRequestAttributes) requestAttributes).getRequest();
        User loginUser = userService.getLoginUser(httpServletRequest);
        // 3. 如果MustRole为空，则直接放行
        UserRoleEnum mustUserRole = UserRoleEnum.getUserRoleEnumByValue(mustRole);
        if (mustUserRole == null) {
            return joinPoint.proceed();
        }
        // 4. 校验权限
        UserRoleEnum userRole = UserRoleEnum.getUserRoleEnumByValue(loginUser.getUserRole());
        if(userRole == null){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        if(UserRoleEnum.ADMIN.equals(mustUserRole) && !UserRoleEnum.ADMIN.equals(userRole)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        return joinPoint.proceed();
    }
}
