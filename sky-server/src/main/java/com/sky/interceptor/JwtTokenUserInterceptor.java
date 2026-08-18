package com.sky.interceptor;


import com.sky.constant.JwtClaimsConstant;
import com.sky.context.BaseContext;
import com.sky.properties.JwtProperties;
import com.sky.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@Slf4j
public class JwtTokenUserInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties ;

    /**
     * jwt校验
     * @param request current HTTP request
     * @param response current HTTP response
     * @param handler chosen handler to execute, for type and/or instance evaluation
     * @return
     * @throws Exception
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response ,Object handler) throws Exception{

        //先判断当前拦截的方法是不是controller方法
        if(!(handler instanceof HandlerMethod)){
            return true ;

        }
        //获取令牌
        String token = request.getHeader(jwtProperties.getUserTokenName()) ;
        //校验令牌

        try{
            log.info("令牌校验: {}" , token) ;
            Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey() , token) ;
            Long userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString()) ;
            log.info("当前用户的ID: {}" ,userId);
            BaseContext.setCurrentId(userId) ;
            return true ;
        }
        catch(Exception ex){
            response.setStatus(401) ;
            return false ;
        }


    }


}
