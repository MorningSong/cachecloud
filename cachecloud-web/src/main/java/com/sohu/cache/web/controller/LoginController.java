package com.sohu.cache.web.controller;

import com.sohu.cache.constant.AppUserTypeEnum;
import com.sohu.cache.entity.AppUser;
import com.sohu.cache.entity.LoginResult;
import com.sohu.cache.login.LoginComponent;
import com.sohu.cache.util.ConstUtils;
import com.sohu.cache.util.MD5Util;
import com.sohu.cache.utils.EnvCustomUtil;
import com.sohu.cache.web.enums.AdminEnum;
import com.sohu.cache.web.enums.LoginEnum;
import com.sohu.cache.web.service.UserLoginStatusService;
import com.sohu.cache.web.vo.GeneralResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录逻辑
 *
 * @author leifu
 * @Time 2014年6月12日
 */
@Controller
@RequestMapping("manage")
public class LoginController extends BaseController {

    @Resource(name = "userLoginStatusService")
    private UserLoginStatusService userLoginStatusService;

    @Autowired(required = false)
    private LoginComponent loginComponent;

    /**
     * 用户登录界面
     *
     * @param request
     * @return
     */
    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public ModelAndView init(HttpServletRequest request, HttpServletResponse response, Model model) {
        model.addAttribute(ConstUtils.RREDIRECT_URL_PARAM, request.getParameter(ConstUtils.RREDIRECT_URL_PARAM));
        model.addAttribute("pwdswitch", EnvCustomUtil.pwdswitch);
        return new ModelAndView("manage/login");
    }

    /**
     * 用户登录
     *
     * @param userName 用户名
     * @param password 密码
     * @param isAdmin  是否勾选超级管理员选项,1是0否
     * @return
     */
    @RequestMapping(value = "/loginIn", method = RequestMethod.POST)
    public ModelAndView loginIn(HttpServletRequest request,
                                HttpServletResponse response, Model model, String userName, String password, boolean isAdmin) {
        // 登录结果
        LoginResult loginResult = new LoginResult();
        loginResult.setAdminEnum((isAdmin == true ? AdminEnum.IS_ADMIN : AdminEnum.NOT_ADMIN));
        loginResult.setLoginEnum(LoginEnum.LOGIN_WRONG_USER_OR_PASSWORD);

        AppUser userModel = null;
        if (ConstUtils.SUPER_ADMIN_NAME.equals(userName)) {
            userModel = userService.getByName(userName);
            String checkPwd = ConstUtils.SUPER_ADMIN_PASS;
            if(EnvCustomUtil.pwdswitch){
                checkPwd = MD5Util.string2MD5(ConstUtils.SUPER_ADMIN_PASS);
            }
            if (userModel != null && checkPwd.equals(password)) {
                loginResult.setLoginEnum(LoginEnum.LOGIN_SUCCESS);
            } else {
                loginResult.setLoginEnum(LoginEnum.LOGIN_WRONG_USER_OR_PASSWORD);
            }
        } else {
            if (loginComponent != null && loginComponent.passportCheck(userName, password)) {
                // 同时要验证是否有cachecloud权限
                userModel = userService.getByName(userName);
                if (userModel != null && userModel.getType() != AppUserTypeEnum.NO_USER.value()) {
                    if (isAdmin) {
                        if (AppUserTypeEnum.ADMIN_USER.value().equals(userModel.getType())) {
                            loginResult.setLoginEnum(LoginEnum.LOGIN_SUCCESS);
                        } else {
                            loginResult.setLoginEnum(LoginEnum.LOGIN_NOT_ADMIN);
                        }
                    } else {
                        loginResult.setLoginEnum(LoginEnum.LOGIN_SUCCESS);
                    }
                } else {
                    // 用户不存在
                    loginResult.setLoginEnum(LoginEnum.LOGIN_USER_NOT_EXIST);
                }
            }
        }
        // 登录成功写入登录状态
        if (loginResult.getLoginEnum().equals(LoginEnum.LOGIN_SUCCESS)) {
            userLoginStatusService.addLoginStatus(request, response, userModel.getName());
        }
        model.addAttribute("success", loginResult.getLoginEnum().value());
        model.addAttribute("admin", loginResult.getAdminEnum().value());
        return new ModelAndView();
    }

    /**
     * 用户注销
     *
     * @return
     */
    @RequestMapping("/logout")
    public ModelAndView logout(HttpServletRequest request, HttpServletResponse response) {
        userLoginStatusService.removeLoginStatus(request, response);
        //String redirectUrl = userLoginStatusService.getLogoutUrl();
        return new ModelAndView("redirect:" + "/manage/login");
    }

    /**
     * 用户登录
     *
     * @param appUser 用户
     * @return
     */
    @RequestMapping(value = "/loginCheck", method = RequestMethod.POST)
    @ResponseBody
    public GeneralResponse<String> loginCheck(HttpServletRequest request,
                                              @RequestBody AppUser appUser) {
        AppUser user = userService.getByName(appUser.getName());
        if(user != null && user.getPassword() != null && user.getPassword().equals(appUser.getPassword())){
            return GeneralResponse.ok();
        }
        return GeneralResponse.error(1001, "用户名或密码错误");
    }

}
