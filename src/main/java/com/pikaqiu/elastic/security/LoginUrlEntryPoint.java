package com.pikaqiu.elastic.security;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * @program: elastic
 * @description:
 * @author: xiaoye
 * @create: 2018-12-03 22:46
 **/
public class LoginUrlEntryPoint extends LoginUrlAuthenticationEntryPoint {

    private final Map<String, String> authEntryPointMap;

    private PathMatcher pathMatcher = new AntPathMatcher();


    public LoginUrlEntryPoint(String loginFormUrl) {

        super(loginFormUrl);
        //放入路径匹配规则
        authEntryPointMap = new HashMap<>();
        //用户登录入口
        authEntryPointMap.put("/user/**", "/user/login");
        //后台登陆人口
        authEntryPointMap.put("/admin/**", "/admin/login");

    }

    /**
     * 根据请求跳转到指定页面
     *限定路径到用户这个请求
     * @param request
     * @param response
     * @param exception
     * @return
     */

    @Override
    protected String determineUrlToUseForThisRequest(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
        request.getHeader("referer");

        //获取到纯路径
        String url = request.getRequestURI().replace(request.getContextPath(), "");

        for (Map.Entry<String, String> item : this.authEntryPointMap.entrySet()) {
            //spring自带的匹配器 来匹配我们的规则
            if (this.pathMatcher.match(item.getKey(), url)) {
                return item.getValue();
            }
        }

        return super.determineUrlToUseForThisRequest(request, response, exception);
    }
}
