package com.sohu.cache.interceptor;

import com.sohu.cache.constant.AppUserTypeEnum;
import com.sohu.cache.entity.AppToUser;
import com.sohu.cache.entity.AppUser;
import com.sohu.cache.entity.InstanceInfo;
import com.sohu.cache.stats.instance.InstanceStatsCenter;
import com.sohu.cache.web.service.AppService;
import com.sohu.cache.web.service.UserLoginStatusService;
import com.sohu.cache.web.service.UserService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

/**
 * 应用和实例权限验证
 *
 * @author leifu
 * @Date 2014年10月29日
 * @Time 下午3:18:00
 */
public class AppAndInstanceAuthorityInterceptor extends HandlerInterceptorAdapter {
    private Logger logger = LoggerFactory.getLogger(AppAndInstanceAuthorityInterceptor.class);
    @Autowired
    private AppService appService;
    @Autowired
    private UserService userService;
    @Autowired
    private InstanceStatsCenter instanceStatsCenter;
    @Autowired
    private UserLoginStatusService userLoginStatusService;

    @Override
    public boolean preHandle(HttpServletRequest request,
            HttpServletResponse response, Object handler) throws Exception {
        // 1. 获取用户
        String userName = userLoginStatusService.getUserNameFromLoginStatus(request);
        //未登录
        if (StringUtils.isBlank(userName)) {
            String redirectUrl = userLoginStatusService.getRedirectUrl(request);
            response.sendRedirect(redirectUrl);
            return false;
        }
        AppUser user = userService.getByName(userName);
        if (user == null || user.getType() == -1) {
            String redirectUrl = userLoginStatusService.getRegisterUrl(user);
            response.sendRedirect(redirectUrl);
            return false;
        }

        // 2. 管理员直接跳过
        if (AppUserTypeEnum.ADMIN_USER.value().equals(user.getType())) {
            return true;
        }

        // 3. 应用id
        String appId = request.getParameter("appId");
        if (StringUtils.isNotBlank(appId)) {
            checkUserAppPower(response, request.getSession(true), user, NumberUtils.toLong(appId));
        }

        // 4. 实例权限检测(其实也是应用)
        String instanceId = request.getParameter("instanceId");
        if (StringUtils.isNotBlank(instanceId)) {
            InstanceInfo instanceInfo = instanceStatsCenter.getInstanceInfo(Long.parseLong(instanceId));
            checkUserAppPower(response, request.getSession(true), user, instanceInfo.getAppId());
        }

        return true;
    }

    /**
     * 检查用户应用的权限
     *
     * @param response
     * @param session
     * @param user
     * @param appId
     * @return
     */
    private void checkUserAppPower(HttpServletResponse response, HttpSession session, AppUser user, Long appId) {
        // 应用下的用户
        List<AppToUser> appToUsers = appService.getAppToUserList(appId);
        if (CollectionUtils.isNotEmpty(appToUsers)) {
            for (AppToUser tempAppToUser : appToUsers) {
                if (user.getId().equals(tempAppToUser.getUserId())) {
                    return;
                }
            }
            // 没权限
            String path = session.getServletContext().getContextPath();
            try {
                response.sendRedirect(path + "/web/resource/noPower?appId=" + appId);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void postHandle(HttpServletRequest request,
            HttpServletResponse response, Object handler,
            ModelAndView modelAndView) throws Exception {
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
            HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
    }
}