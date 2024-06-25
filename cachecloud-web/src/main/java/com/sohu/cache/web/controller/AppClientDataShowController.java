package com.sohu.cache.web.controller;

import com.alibaba.fastjson.JSONObject;
import com.sohu.cache.client.service.*;
import com.sohu.cache.dao.AppClientStatisticGatherDao;
import com.sohu.cache.entity.*;
import com.sohu.cache.stats.instance.InstanceStatsCenter;
import com.sohu.cache.util.NumberUtil;
import com.sohu.cache.web.service.AppService;
import com.sohu.cache.web.util.DateUtil;
import com.sohu.cache.web.util.Page;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 应用客户端统计相关
 *
 * @author leifu
 * @Time 2014年8月31日
 */
@Controller
@RequestMapping("/client/show")
public class AppClientDataShowController extends BaseController {

    /**
     * 收集数据时间format
     */
    private final static String COLLECT_TIME_FORMAT = "yyyyMMddHHmmss";

    @Autowired
    private AppClientReportCommandService appClientReportCommandService;

    @Autowired
    private AppClientReportExceptionService appClientReportExceptionService;
    /**
     * 客户端异常服务
     */
    @Resource(name = "clientReportExceptionService")
    private ClientReportExceptionService clientReportExceptionService;

    /**
     * 应用基本服务
     */
    @Resource(name = "appService")
    private AppService appService;
    /**
     * 实例信息
     */
    @Resource(name = "instanceStatsCenter")
    private InstanceStatsCenter instanceStatsCenter;

    @Autowired
    private AppClientStatisticGatherDao appClientStatisticGatherDao;

    /**
     * 应用客户端统计首页
     */
    @RequestMapping("/index")
    public ModelAndView doIndex(HttpServletRequest request, HttpServletResponse response, Model model) {
        Long appId = NumberUtils.toLong(request.getParameter("appId"));
        if (appId == null || appId <= 0) {
            return new ModelAndView("");
        }
        AppDesc appDesc = appService.getByAppId(appId);
        model.addAttribute("appId", appId);
        model.addAttribute("appDesc", appDesc);
        model.addAttribute("tabTag", request.getParameter("tabTag"));
        model.addAttribute("type", request.getParameter("type"));
        model.addAttribute("searchDate", request.getParameter("searchDate"));
        model.addAttribute("commandStatisticsStartDate", request.getParameter("commandStatisticsStartDate"));
        model.addAttribute("commandStatisticsEndDate", request.getParameter("commandStatisticsEndDate"));
        model.addAttribute("exceptionStartDate", request.getParameter("exceptionStartDate"));
        model.addAttribute("exceptionEndDate", request.getParameter("exceptionEndDate"));
        model.addAttribute("valueDistriStartDate", request.getParameter("valueDistriStartDate"));
        model.addAttribute("valueDistriEndDate", request.getParameter("valueDistriEndDate"));
        model.addAttribute("costDistriStartDate", request.getParameter("costDistriStartDate"));
        model.addAttribute("costDistriEndDate", request.getParameter("costDistriEndDate"));
        model.addAttribute("clientIp", request.getParameter("clientIp"));
        model.addAttribute("pageNo", request.getParameter("pageNo"));
        model.addAttribute("firstCommand", request.getParameter("firstCommand"));
        model.addAttribute("timeDimensionality", request.getParameter("timeDimensionality"));
        return new ModelAndView("client/appClientIndex");
    }

    /**
     * 客户端异常查询
     */
    @RequestMapping("/exception")
    public ModelAndView doException(HttpServletRequest request, HttpServletResponse response, Model model) {
        // 1.1 应用信息
        Long appId = NumberUtils.toLong(request.getParameter("appId"));
        if (appId <= 0) {
            return new ModelAndView("");
        }
        AppDesc appDesc = appService.getByAppId(appId);
        model.addAttribute("appDesc", appDesc);

        // 1.2 异常类型
        int type = NumberUtil.toInt(request.getParameter("type"));
        model.addAttribute("type", type);

        // 1.3 客户端ip
        String clientIp = request.getParameter("clientIp");
        model.addAttribute("clientIp", clientIp);

        // 1.4 日期格式转换
        TimeBetween timeBetween = new TimeBetween();
        try {
            timeBetween = fillWithClientExceptionTime(request, model);
        } catch (ParseException e) {
            logger.error(e.getMessage(), e);
        }

        // 2. 分页查询异常
        int totalCount = clientReportExceptionService.getAppExceptionCount(appId, timeBetween.getStartTime(), timeBetween.getEndTime(), type, clientIp);
        int pageNo = NumberUtils.toInt(request.getParameter("pageNo"), 1);
        int pageSize = NumberUtils.toInt(request.getParameter("pageSize"), 10);
        Page page = new Page(pageNo, pageSize, totalCount);
        model.addAttribute("page", page);

        List<AppClientExceptionStat> appClientExceptionList = clientReportExceptionService.getAppExceptionList(appId,
                timeBetween.getStartTime(), timeBetween.getEndTime(), type, clientIp, page);
        model.addAttribute("appClientExceptionList", appClientExceptionList);

        return new ModelAndView("client/clientException");
    }

    /**
     * 异常查询日期格式
     */
    private TimeBetween fillWithClientExceptionTime(HttpServletRequest request, Model model) throws ParseException {
        final String exceptionDateFormat = "yyyy-MM-dd";
        String exceptionStartDateParam = request.getParameter("exceptionStartDate");
        String exceptionEndDateParam = request.getParameter("exceptionEndDate");
        Date startDate;
        Date endDate;
        if (StringUtils.isBlank(exceptionStartDateParam) || StringUtils.isBlank(exceptionEndDateParam)) {
            // 如果为空默认取昨天和今天
            SimpleDateFormat sdf = new SimpleDateFormat(exceptionDateFormat);
            startDate = sdf.parse(sdf.format(new Date()));
            endDate = DateUtils.addDays(startDate, 1);
            exceptionStartDateParam = DateUtil.formatDate(startDate, exceptionDateFormat);
            exceptionEndDateParam = DateUtil.formatDate(endDate, exceptionDateFormat);
        } else {
            endDate = DateUtil.parse(exceptionEndDateParam, exceptionDateFormat);
            startDate = DateUtil.parse(exceptionStartDateParam, exceptionDateFormat);
            //限制不能超过7天
            if (endDate.getTime() - startDate.getTime() > TimeUnit.DAYS.toMillis(7)) {
                startDate = DateUtils.addDays(endDate, -7);
            }
        }
        // 前端需要
        model.addAttribute("exceptionStartDate", exceptionStartDateParam);
        model.addAttribute("exceptionEndDate", exceptionEndDateParam);
        // 查询后台需要
        long startTime = NumberUtils.toLong(DateUtil.formatDate(startDate, COLLECT_TIME_FORMAT));
        long endTime = NumberUtils.toLong(DateUtil.formatDate(endDate, COLLECT_TIME_FORMAT));
        return new TimeBetween(startTime, endTime, startDate, endDate);
    }

    @RequestMapping("/commandStatistics")
    public ModelAndView doCommandStatistics(HttpServletRequest request, HttpServletResponse response, Model model) {
        // 1.应用信息
        Long appId = NumberUtils.toLong(request.getParameter("appId"));
        if (appId <= 0) {
            return new ModelAndView("");
        }
        AppDesc appDesc = appService.getByAppId(appId);
        model.addAttribute("appDesc", appDesc);
        model.addAttribute("appId", appId);

        // 2.获取时间区间
        String searchDate = request.getParameter("searchDate");
        TimeBetween timeBetween = new TimeBetween();
        try {
            timeBetween = fillWithDateFormat(searchDate);
        } catch (ParseException e) {
            logger.error(e.getMessage(), e);
        }
        long startTime = timeBetween.getStartTime();
        long endTime = timeBetween.getEndTime();
        searchDate = timeBetween.getFormatStartDate();
        model.addAttribute("searchDate", searchDate);

        try {
            List<Map<String, Object>> appClientGatherStatList = appClientStatisticGatherDao.getAppClientStatisticByGatherTime(appId, searchDate);
            model.addAttribute("appClientGatherStat", CollectionUtils.isNotEmpty(appClientGatherStatList) && appClientGatherStatList.size() > 0 ? appClientGatherStatList.get(0) : null);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        // 3.客户端的统计信息
        Map<String, List<Map<String, Object>>> appClientCommandStatisticsMap = appClientReportCommandService.getAppCommandClientStatistics(appId, null, startTime, endTime, null);
        model.addAttribute("appClientCommandStatisticsJson", JSONObject.toJSONString(appClientCommandStatisticsMap));

        return new ModelAndView("client/clientCommandStatistics");
    }

    @RequestMapping("/commandStatistics/client")
    public ModelAndView getCommandStatisticsByClient(HttpServletRequest request, HttpServletResponse response, Model model) {
        // 1.应用信息
        Long appId = NumberUtils.toLong(request.getParameter("appId"));
        if (appId <= 0) {
            return new ModelAndView("");
        }
        model.addAttribute("appId", appId);
        // 2.获取时间区间
        String searchDate = request.getParameter("searchDate");
        TimeBetween timeBetween = new TimeBetween();
        try {
            timeBetween = fillWithDateFormat(searchDate);
        } catch (ParseException e) {
            logger.error(e.getMessage(), e);
        }
        long startTime = timeBetween.getStartTime();
        long endTime = timeBetween.getEndTime();
        model.addAttribute("searchDate", timeBetween.getFormatStartDate());
        List<String> clientList = appClientReportCommandService.getAppDistinctClients(appId, startTime, endTime);
        model.addAttribute("clientList", clientList);
        List<String> commandList = appClientReportCommandService.getAppDistinctCommand(appId, startTime, endTime);
        model.addAttribute("commandList", commandList);

        String firstClient = request.getParameter("firstClient");
        if (StringUtils.isBlank(firstClient) && CollectionUtils.isNotEmpty(clientList)) {
            firstClient = clientList.get(0);
        }
        model.addAttribute("firstClient", firstClient);

        String firstCommand = request.getParameter("firstCommand");
        if (StringUtils.isBlank(firstCommand) && CollectionUtils.isNotEmpty(commandList)) {
            firstCommand = commandList.get(0);
        }
        model.addAttribute("firstCommand", firstCommand);

        if ("all".equals(firstClient)) {
            List<Map<String, Object>> sumCommandStatList = appClientReportCommandService.getSumCmdStatByCmd(appId, startTime, endTime, firstCommand);
            model.addAttribute("sumCommandStatJson", JSONObject.toJSONString(sumCommandStatList));
        } else if ("all".equals(firstCommand)) {
            List<Map<String, Object>> sumClientStatList = appClientReportCommandService.getSumCmdStatByClient(appId, startTime, endTime, firstClient);
            model.addAttribute("sumClientStatJson", JSONObject.toJSONString(sumClientStatList));
        } else {
            Map<String, List<Map<String, Object>>> appClientCommandStatisticsMap = appClientReportCommandService.getAppCommandClientStatistics(appId, firstCommand, startTime, endTime, firstClient);
            model.addAttribute("appClientCommandStatisticsJson", JSONObject.toJSONString(appClientCommandStatisticsMap));
        }
        return new ModelAndView("client/commandStatisticsByClient");
    }

    @RequestMapping("/exceptionStatistics")
    public ModelAndView doExceptionStatistics(HttpServletRequest request, HttpServletResponse response, Model model) {
        // 1.应用信息
        Long appId = NumberUtils.toLong(request.getParameter("appId"));
        if (appId <= 0) {
            return new ModelAndView("");
        }
        AppDesc appDesc = appService.getByAppId(appId);
        model.addAttribute("appDesc", appDesc);
        model.addAttribute("appId", appId);

        // 2.获取时间区间
        String searchDate = request.getParameter("searchDate");
        TimeBetween timeBetween = new TimeBetween();
        try {
            timeBetween = fillWithDateFormat(searchDate);
        } catch (ParseException e) {
            logger.error(e.getMessage(), e);
        }
        long startTime = timeBetween.getStartTime();
        long endTime = timeBetween.getEndTime();
        searchDate = timeBetween.getFormatStartDate();
        model.addAttribute("searchDate", searchDate);

        try {
            List<Map<String, Object>> appClientGatherStatList = appClientStatisticGatherDao.getAppClientStatisticByGatherTime(appId, searchDate);
            model.addAttribute("appClientGatherStat", CollectionUtils.isNotEmpty(appClientGatherStatList) && appClientGatherStatList.size() > 0 ? appClientGatherStatList.get(0) : null);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        // 3.客户端的统计信息
        Map<String, List<Map<String, Object>>> appClientExceptionStatisticsMap = appClientReportExceptionService.getAppExceptionStatisticsMap(appId, null, startTime, endTime, null);
        model.addAttribute("appClientExceptionStatisticsJson", JSONObject.toJSONString(appClientExceptionStatisticsMap));

        return new ModelAndView("client/clientExceptionStatistics");
    }

    @RequestMapping("/exceptionStatistics/client")
    public ModelAndView getExceptionStatisticsByClient(HttpServletRequest request, HttpServletResponse response, Model model) {
        // 1.应用信息
        Long appId = NumberUtils.toLong(request.getParameter("appId"));
        if (appId <= 0) {
            return new ModelAndView("");
        }
        model.addAttribute("appId", appId);

        Integer type = NumberUtils.toInt(request.getParameter("exceptionType"));
        String viewName = type == 0 ? "connExceptionStatisticsByClient" : "cmdExceptionStatisticsByClient";

        // 2.获取时间区间
        String searchDate = request.getParameter("searchDate");
        TimeBetween timeBetween = new TimeBetween();
        try {
            timeBetween = fillWithDateFormat(searchDate);
        } catch (ParseException e) {
            logger.error(e.getMessage(), e);
        }
        long startTime = timeBetween.getStartTime();
        long endTime = timeBetween.getEndTime();
        model.addAttribute("searchDate", timeBetween.getFormatStartDate());
        Map<String, List<String>> clientConfigMap = appClientReportExceptionService.getAppClientConfigs(appId, type, startTime, endTime);
        model.addAttribute("clientConfigMap", clientConfigMap);

        String firstClient = request.getParameter("firstClient");
        if (StringUtils.isBlank(firstClient) && CollectionUtils.isNotEmpty(clientConfigMap.keySet())) {
            firstClient = clientConfigMap.keySet().iterator().next();
        }
        model.addAttribute("firstClient", firstClient);

        Map<String, List<Map<String, Object>>> appClientExceptionStatisticsMap = appClientReportExceptionService.getAppExceptionStatisticsMap(appId, firstClient, startTime, endTime, type);
        model.addAttribute("appClientExceptionStatisticsJson", JSONObject.toJSONString(appClientExceptionStatisticsMap));
        List<Map<String, Object>> appNodeExceptionStatisticsList = appClientReportExceptionService.getDistinctClientNodeStatistics(appId, firstClient, startTime, endTime, type);
        model.addAttribute("appNodeExceptionStatisticsList", appNodeExceptionStatisticsList);

        return new ModelAndView("client/" + viewName);
    }

    @RequestMapping(value = "/sumCommandStat/command")
    public void getSumCmdStatByCmd(HttpServletRequest request, HttpServletResponse response) {
        Long appId = NumberUtils.toLong(request.getParameter("appId"));
        String command = request.getParameter("command");
        String searchDate = request.getParameter("searchDate");
        TimeBetween timeBetween = new TimeBetween();
        try {
            timeBetween = fillWithDateFormat(searchDate);
        } catch (ParseException e) {
            logger.error(e.getMessage(), e);
        }
        long startTime = timeBetween.getStartTime();
        long endTime = timeBetween.getEndTime();

        JSONObject json = new JSONObject();
        String result = "success";
        List<Map<String, Object>> sumCommandStatMap = appClientReportCommandService.getSumCmdStatByCmd(appId, startTime, endTime, command);
        if (CollectionUtils.isEmpty(sumCommandStatMap)) {
            result = "sumCommandStatMap is empty";
        }
        json.put("result", result);
        json.put("sumCommandStatMap", sumCommandStatMap);
        sendMessage(response, json.toString());
    }

    @RequestMapping(value = "/latencyCommandDetails")
    public ModelAndView getLatencyCommandDetails(HttpServletRequest request, HttpServletResponse response, Model model) {
        long appId = NumberUtils.toLong(request.getParameter("appId"));
        model.addAttribute("appId", appId);
        long timestamp = NumberUtils.toLong(request.getParameter("searchTime")); //毫秒
        Date date = new Date(timestamp);
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmm00");
        long searchTime = NumberUtils.toLong(format.format(date));
        model.addAttribute("searchTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:00").format(date));

        Map<String, Map<String, Object>> sumCmdExpStatMap = appClientReportExceptionService.getSumCmdExpStatGroupByNode(appId, searchTime);
        model.addAttribute("sumCmdExpStatMap", sumCmdExpStatMap);

        Set<String> nodeSet = sumCmdExpStatMap.keySet();
        Map<String, List<Map<String, Object>>> latencyCommandDetailMap = appClientReportExceptionService.getLatencyCommandDetails(nodeSet, searchTime);
        model.addAttribute("latencyCommandDetailMap", latencyCommandDetailMap);
        return new ModelAndView("/client/cmdExceptionCommandDetail");
    }

    @RequestMapping(value = "/latencyCommandDetail/node")
    public void getLatencyCommandDetailByNode(HttpServletRequest request, HttpServletResponse response) {
        JSONObject json = new JSONObject();
        String result = "success";
        String client = request.getParameter("client");
        String node = request.getParameter("node");
        String searchDate = request.getParameter("searchDate");
        TimeBetween timeBetween = new TimeBetween();
        try {
            timeBetween = fillWithDateFormat(searchDate);
        } catch (ParseException e) {
            logger.error(e.getMessage(), e);
        }
        long startTime = timeBetween.getStartTime();
        long endTime = timeBetween.getEndTime();
        List<Map<String, Object>> latencyCommandDetailList = appClientReportExceptionService.getLatencyCommandDetailByNode(client, node, startTime, endTime);
        if (CollectionUtils.isEmpty(latencyCommandDetailList)) {
            result = "latencyCommandDetailList is empty";
        }
        json.put("result", result);
        json.put("latencyCommandDetailList", latencyCommandDetailList);
        sendMessage(response, json.toString());
    }

    private TimeBetween fillWithDateFormat(String searchDate) throws ParseException {

        final String dateFormat = "yyyy-MM-dd";
        Date startDate;
        Date endDate;
        if (StringUtils.isBlank(searchDate)) {
            // 如果为空默认取今天
            SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
            startDate = sdf.parse(sdf.format(new Date()));
        } else {
            startDate = DateUtil.parse(searchDate, dateFormat);
        }
        endDate = DateUtils.addDays(startDate, 1);
        // 查询后台需要
        long startTime = NumberUtils.toLong(DateUtil.formatDate(startDate, COLLECT_TIME_FORMAT));
        long endTime = NumberUtils.toLong(DateUtil.formatDate(endDate, COLLECT_TIME_FORMAT));
        return new TimeBetween(startTime, endTime, startDate, endDate);
    }
}
