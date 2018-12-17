package com.pikaqiu.elastic.security;

import com.pikaqiu.elastic.entity.User;
import com.pikaqiu.elastic.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.DigestUtils;

//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.authentication.encoding.Md5PasswordEncoder;

/**
 * 自定义认证实现
 * Created by 瓦力.
 */

public class AuthProvider implements AuthenticationProvider {

    @Autowired
    private IUserService userService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        String userName = authentication.getName();
        String password = (String) authentication.getCredentials();

        User user = userService.findUserByName(userName);

        if (user == null) {
            throw new AuthenticationCredentialsNotFoundException("authError");
        }
        if (DigestUtils.md5DigestAsHex(password.getBytes()).equals(user.getPassword())) {

            return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        }

        throw new BadCredentialsException("authError");

    }

    @Override
    public boolean supports(Class<?> authentication) {
        return true;
    }
}
