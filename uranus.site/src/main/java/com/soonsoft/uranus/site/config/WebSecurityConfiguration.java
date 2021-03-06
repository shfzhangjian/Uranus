package com.soonsoft.uranus.site.config;

import java.util.Set;

import javax.annotation.Resource;

import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;

import com.soonsoft.uranus.data.IDatabaseAccess;
import com.soonsoft.uranus.security.SecurityManager;
import com.soonsoft.uranus.security.authentication.IUserManager;
import com.soonsoft.uranus.security.authentication.WebUserDetailsService;
import com.soonsoft.uranus.security.authorization.IFunctionManager;
import com.soonsoft.uranus.security.authorization.IRoleManager;
import com.soonsoft.uranus.security.config.WebApplicationConfig;

import com.soonsoft.uranus.security.entity.MenuInfo;
import com.soonsoft.uranus.security.entity.RoleInfo;
import com.soonsoft.uranus.security.entity.UserInfo;
import com.soonsoft.uranus.security.authentication.SimpleUserManager;
import com.soonsoft.uranus.security.authorization.SimpleRoleManager;
import com.soonsoft.uranus.security.authorization.SimpleFunctionManager;

import com.soonsoft.uranus.services.membership.authorization.MembershipRoleVoter;
import com.soonsoft.uranus.site.config.properties.MembershipProperties;
import com.soonsoft.uranus.site.config.properties.WebProperties;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * WebSecurityConfiguration
 */
@Configuration
@EnableConfigurationProperties(MembershipProperties.class)
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Resource
    private WebProperties webProperties;

    @Override
    public void configure(WebSecurity web) throws Exception {
        // 配置静态资源，这些资源不做安全验证
        web.ignoring()
            .antMatchers(
                HttpMethod.GET, 
                webProperties.getResourcePathArray())
            .antMatchers(HttpMethod.GET, "/CloudAtlas/**", "/page/**");
	}

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 初始化SecurityManager
        SecurityManager.init(this.getApplicationContext());
        // Web应用程序，身份验证配置
        WebApplicationConfig config = SecurityManager.webApplicationConfig(http);
        config.getWebAccessDecisionManager().addVoter(new MembershipRoleVoter());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean(name = "userManager")
    public IUserManager userManager(
        @Qualifier("securityAccess") IDatabaseAccess securityAccess, 
        PasswordEncoder passwordEncoder) {
            
        SimpleUserManager userManager = new SimpleUserManager(passwordEncoder);

        Set<GrantedAuthority> roles = new HashSet<>();
        roles.add(new RoleInfo("Admin", "管理员"));
        
        List<UserInfo> users = new ArrayList<>();
        String salt = null;
        UserInfo user = new UserInfo("admin", userManager.encryptPassword("1", salt), roles);
        user.setNickName("张三");
        user.setPasswordSalt(salt);
        user.setCellPhone("139-0099-8877");
        user.setCreateTime(new Date());
        users.add(user);

        userManager.addAll(users); 

        return userManager;
    }

    @Bean(name = "roleManager")
    public IRoleManager roleManager(@Qualifier("securityAccess") IDatabaseAccess securityAccess) {
        return new SimpleRoleManager();
    }

    @Bean(name = "functionManager")
    public IFunctionManager functionManager(@Qualifier("securityAccess") IDatabaseAccess securityAccess) {
        List<RoleInfo> allowRoles = new ArrayList<>();
        allowRoles.add(new RoleInfo("Admin"));

        List<MenuInfo> menus = new ArrayList<>();
        MenuInfo menu = new MenuInfo("1", "HOME");
        menu.setUrl("/index");
        menu.setIcon("/content/icons/sys-setting.png");
        menu.setAllowRoles(allowRoles);
        menus.add(menu);

        menu = new MenuInfo("2", "功能菜单2");
        menu.setUrl("/coming-soon");
        menu.setIcon("/content/icons/sys-setting.png");
        menu.setAllowRoles(allowRoles);
        menus.add(menu);

        menu = new MenuInfo("3", "功能菜单3");
        menu.setIcon("/content/icons/sys-setting.png");
        menu.setAllowRoles(allowRoles);
        menus.add(menu);

        menu = new MenuInfo("3_1", "菜单3-功能");
        menu.setParentResourceCode("3");
        menu.setUrl("/coming-soon");
        menu.setAllowRoles(allowRoles);
        menus.add(menu);

        menu = new MenuInfo("3_2", "菜单3-功能");
        menu.setParentResourceCode("3");
        menu.setUrl("/coming-soon");
        menu.setAllowRoles(allowRoles);
        menus.add(menu);

        menu = new MenuInfo("3_3", "菜单3-功能");
        menu.setParentResourceCode("3");
        menu.setUrl("/coming-soon");
        menu.setAllowRoles(allowRoles);
        menus.add(menu);

        menu = new MenuInfo("4", "系统管理");
        menu.setIcon("/content/icons/sys-setting.png");
        menu.setAllowRoles(allowRoles);
        menus.add(menu);

        menu = new MenuInfo("4_1", "用户管理");
        menu.setParentResourceCode("4");
        menu.setUrl("/coming-soon");
        menu.setAllowRoles(allowRoles);
        menus.add(menu);

        menu = new MenuInfo("4_2", "角色管理");
        menu.setParentResourceCode("4");
        menu.setUrl("/coming-soon");
        menu.setAllowRoles(allowRoles);
        menus.add(menu);

        menu = new MenuInfo("4_3", "模块管理");
        menu.setParentResourceCode("4");
        menu.setUrl("/coming-soon");
        menu.setAllowRoles(allowRoles);
        menus.add(menu);

        menu = new MenuInfo("4_4", "系统设置");
        menu.setParentResourceCode("4");
        menu.setUrl("/coming-soon");
        menu.setAllowRoles(allowRoles);
        menus.add(menu);

        return new SimpleFunctionManager(menus);
    }

    /**
     * 自定义身份验证管理器
     */
    @Bean
    public WebUserDetailsService getUserDetailsService(@Qualifier("userManager") IUserManager userManager) {
        WebUserDetailsService userDetailsService = new WebUserDetailsService();
        userDetailsService.setUserManager(userManager);
        return userDetailsService;
    }
}