package com.pikaqiu.elastic.config;

import com.pikaqiu.elastic.security.AuthProvider;
import com.pikaqiu.elastic.security.LoginAuthFailHandler;
import com.pikaqiu.elastic.security.LoginUrlEntryPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;


/**
 * 注册中心安全认证类 <br>
 * 说明：spring-cloud使用了高版本，1.5.9以上及2.0.0及以上版本时，注册中心的基础校验必须程序控制 <br>
 *
 * @author 肖烨
 * @date 2018-08-24
 */
@Configuration
//开启webSecurity
@EnableWebSecurity
@EnableGlobalMethodSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    /**
     * 高版本的丢弃了security.basicenabled=true配置 <br>
     * 这里只能使用安全控制类设置启动http基础安全校验 <br>
     *
     * @param http HttpSecurity 校验对象
     * @throws Exception
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        //替换高版本的security.basic.enabled=true设置   //设置session创建方针
        http.csrf().disable();
        http.authorizeRequests().antMatchers("/admin/login").permitAll() // 管理员登录入口
                .antMatchers("/static/**").permitAll() // 静态资源
                .antMatchers("/user/login").permitAll() // 用户登录入口
                .antMatchers("/admin/**").hasRole("ADMIN")
                .antMatchers("/user/**").hasAnyRole("ADMIN", "USER")
                .antMatchers("/api/user/**").hasAnyRole("ADMIN", "USER")
                .and().formLogin().loginProcessingUrl("/login")// 配置角色登录处理入口
                //登陆失败是回到的页面
                .failureHandler(loginAuthFailHandler()).and()
                .logout()
                .logoutUrl("/logout")
                .logoutSuccessUrl("/logout/page")
                .deleteCookies("JSESSIONID")
                .invalidateHttpSession(true)
                .and()
                .exceptionHandling()
                //解决匿名用户访问无权限资源时的异常
                .authenticationEntryPoint(urlEntryPoint())
                //无权访问走的页面
                .accessDeniedPage("/403");

        http.headers().frameOptions().sameOrigin();

        http.headers().frameOptions().sameOrigin();
    }
/*    *//**
     * 自定义认证策略
     *//*
    @Autowired
    public void configGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication().passwordEncoder(new BCryptPasswordEncoder())
                .withUser("admin").password(new BCryptPasswordEncoder()
                .encode("admin")).roles("ADMIN").and();
    }*/

    /**
     * 自定义认证策略
     */
    @Autowired
    public void configGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(authProvider()).eraseCredentials(true);
    }
    @Bean
    public AuthProvider authProvider() {
        return new AuthProvider();
    }
    @Bean
    public LoginUrlEntryPoint urlEntryPoint(){
        //默认的匹配路劲
        return new LoginUrlEntryPoint("/admin/login");
    }
    @Bean
    public LoginAuthFailHandler loginAuthFailHandler(){
        return new LoginAuthFailHandler(urlEntryPoint());
    }
}
