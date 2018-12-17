package com.pikaqiu.elastic.base;

import com.pikaqiu.elastic.entity.User;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @program: elastic
 * @description:
 * @author: xiaoye
 * @create: 2018-12-09 22:57
 **/
public class LoginUserUtil {

    /**
     * 获取当前登陆人
     *
     * @return
     */
    public static User load() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal != null && principal instanceof User) {
            return (User) principal;
        }
        return null;
    }

    public static Long getUserId() {
        User load = load();
        if (load != null) {
            return load.getId();
        }
        return -1L;
    }
}
