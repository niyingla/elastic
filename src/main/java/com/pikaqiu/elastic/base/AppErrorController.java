package com.pikaqiu.elastic.base;

import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * web错误 全局配置
 * @program: elastic
 * @description:
 * @author: xiaoye
 * @create: 2018-11-25 22:51
 **/
@Controller
public class AppErrorController implements ErrorController {

    private static final String ERROR_PATH = "/error";

    private ErrorAttributes errorAttributes;


    @Override
    public String getErrorPath() {
        return ERROR_PATH;
    }

    /**
     * Web页面错误处理
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = ERROR_PATH, produces = "text/html")
    public String errorPageHandler(HttpServletRequest request, HttpServletResponse response) {
        int status = response.getStatus();
        switch (status) {
            case 403:
                return "403";
            case 404:
                return "404";
            case 500:
                return "500";
            default:
                return "index";
        }

    }

   /* *//**
     * 除Web页面外的错误处理，比如Json/XML等
     *//*
    @RequestMapping(value = ERROR_PATH)
    @ResponseBody
    public ApiResponse errorApiHandler(HttpServletRequest request) {


        Map<String, Object> attr = this.errorAttributes.getErrorAttributes(request, false);
        int status = getStatus(request);

        return ApiResponse.ofMessage(status, String.valueOf(attr.getOrDefault("message", "error")));
    }

    private int getStatus(HttpServletRequest request) {
        Integer status = (Integer) request.getAttribute("javax.servlet.error.status_code");
        if (status != null) {
            return status;
        }

        return 500;
    }*/
}
