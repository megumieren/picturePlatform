package com.wjq.wjqpicturebackend.aop;

import com.wjq.wjqpicturebackend.annotation.AuthCheck;
import com.wjq.wjqpicturebackend.exception.BusinessException;
import com.wjq.wjqpicturebackend.exception.ErrorCode;
import com.wjq.wjqpicturebackend.model.domain.User;
import com.wjq.wjqpicturebackend.model.enums.UserRoleEnum;
import com.wjq.wjqpicturebackend.service.UserService;
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

    /**
     * 对标有注解@AuthCheck的方法进行环绕增强，用来对用户进行权限校验
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        UserRoleEnum mustRoleEnum = UserRoleEnum.getUserRoleEnumByValue(mustRole);
        //判断当前方法是否需要权限
        if(mustRoleEnum == null){
            //若不需要权限，则继续执行原方法
            return joinPoint.proceed();
        }

        //方法需要权限

        //获取当前登录用户
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        User loginUser = userService.getLoginUser(request);
        //获取用户的权限
        UserRoleEnum userRoleEnum = UserRoleEnum.getUserRoleEnumByValue(loginUser.getUserRole());
        if(userRoleEnum == null){
            //用户没有任何权限
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        //用户有权限，则需要判断是否为管理员权限
        if(UserRoleEnum.ADMIN.equals(mustRoleEnum) && !mustRoleEnum.equals(userRoleEnum)){
            //用户不是管理员,拒绝
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        //通过校验
        return joinPoint.proceed();
    }
}
