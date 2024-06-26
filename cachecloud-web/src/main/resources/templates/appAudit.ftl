<!DOCTYPE html>
<head>
    <meta charset=UTF-8/>
    <title>CacheCloud审核邮件</title>
</head>
<body>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<p>
<table style="width:100%; font-size:12px;" width="100%" cellpadding="0" cellspacing="0">
    <colgroup>
        <col style="width: 5px;">
    </colgroup>
    <tr>
        <td style="width:5px;"></td>
        <td style="color:#3f3f3f; font-weight: bold;font-size:12px; font-family: '宋体'; padding: 5px 0 15px 0">
            ${appAudit.userName!}，您好，欢迎使用CacheCloud平台，您的申请处理进度如下：
        </td>
    </tr>
    <tr>
        <td></td>
        <td style="padding-top:20px; padding-left:27px;">
            <ul>
                <li><span style="font-weight: bold; padding-top:20px; color:#3f3f3f;">申请处理信息：</span></li>
            </ul>
            <table style="table-layout:fixed;width: 872px;border-collapse: collapse;word-break: break-all;word-wrap:break-word;border-top: 1px dotted #676767;text-align: center;color: #000; font-family:'宋体'; font-size:12px; margin-top:10px; margin-left: 24px">
                <tr>
                    <td style="border-right: 1px dotted #676767; border-bottom: 1px dotted #676767;text-align: left; height:33px; width: 50px;">
                        申请类型：
                    </td>
                    <td style="border-right: 1px dotted #676767; border-bottom: 1px dotted #676767; height:33px; width: 140px;">
                        ${appAudit.typeDesc!}
                    </td>
                    <td style="border-right: 1px dotted #676767; border-bottom: 1px dotted #676767; text-align: left;height:33px; width: 50px;">
                        申请描述：
                    </td>
                    <td style="border-right: 1px dotted #676767; border-bottom: 1px dotted #676767; height:33px; width: 140px;">
                        ${appAudit.info!}
                    </td>
                    <td style="border-right: 1px dotted #676767; border-bottom: 1px dotted #676767; text-align: left;height:33px; width: 50px;">
                        申请时间：
                    </td>
                    <td style="border-right: 1px dotted #676767; border-bottom: 1px dotted #676767; height:33px; width: 140px;">
                        ${appAudit.createTimeFormat!}
                    </td>
                </tr>
                <tr>
                    <td style="border-right: 1px dotted #676767; border-bottom: 1px dotted #676767;text-align: left; height:33px; width: 50px;">
                        处理状态：
                    </td>
                    <td style="border-right: 1px dotted #676767; border-bottom: 1px dotted #676767; height:33px; width: 140px; color: red;">
                        ${appAudit.statusDesc!}
                    </td>
                    <td style="border-right: 1px dotted #676767; border-bottom: 1px dotted #676767;text-align: left; height:33px; width: 50px;">
                        处理描述：
                    </td>
                    <td style="border-right: 1px dotted #676767; border-bottom: 1px dotted #676767; height:33px; width: 140px; color: red;">
                        ${appAudit.refuseReason!}
                    </td>
                    <td style="border-right: 1px dotted #676767; border-bottom: 1px dotted #676767;text-align: left; height:33px; width: 50px;">
                        处理时间：
                    </td>
                    <td style="border-right: 1px dotted #676767; border-bottom: 1px dotted #676767; height:33px; width: 140px;">
                        ${appAudit.modifyTime?string("yyyy-MM-dd HH:mm:ss")!}
                    </td>
                </tr>
            </table>
            <br/>
            <#if appDesc??>
                <ul>
                    <li><span style="font-weight: bold; padding-top:20px; color:#3f3f3f;">应用详细信息：</span></li>
                </ul>
                <table style="table-layout:fixed;width: 872px;border-collapse: collapse;word-break: break-all;word-wrap:break-word;border-top: 1px dotted #676767;text-align: center;color: #000; font-family:'宋体'; font-size:12px; margin-top:10px; margin-left: 24px">
                    <tr>
                        <td style="border-right: 1px dotted #676767; border-bottom: 1px dotted #676767;text-align: left; height:33px; width: 50px;">
                            应用名称：
                        </td>
                        <td style="border-right: 1px dotted #676767; border-bottom: 1px dotted #676767; height:33px; width: 140px;">
                            ${appDesc.name!}
                        </td>
                        <td style="border-right: 1px dotted #676767; border-bottom: 1px dotted #676767;text-align: left; height:33px; width: 50px;">
                            应用类型：
                        </td>
                        <td style="border-right: 1px dotted #676767; border-bottom: 1px dotted #676767; height:33px; width: 140px;">
                            ${appDesc.typeDesc!}
                        </td>
                        <td style="border-right: 1px dotted #676767; border-bottom: 1px dotted #676767;text-align: left; height:33px; width: 50px;">
                            应用状态：
                        </td>
                        <td style="border-right: 1px dotted #676767; border-bottom: 1px dotted #676767; height:33px; width: 140px; color: red;">
                            ${appDesc.statusDesc!}
                        </td>
                    </tr>
                    <tr>
                        <td style="border-right: 1px dotted #676767; border-bottom: 1px dotted #676767;text-align: left; height:33px; width: 50px;">
                            是否测试：
                        </td>
                        <td style="border-right: 1px dotted #676767; border-bottom: 1px dotted #676767; height:33px; width: 140px;">
                            <#if appDesc.isTest == 1>
                                是
                            <#else>
                                否
                            </#if>
                        </td>
                        <td style="border-right: 1px dotted #676767; border-bottom: 1px dotted #676767;text-align: left; height:33px; width: 50px;">
                            是否持久化：
                        </td>
                        <td style="border-right: 1px dotted #676767; border-bottom: 1px dotted #676767; height:33px; width: 140px;">
                            <#if appDesc.needPersistence == 1>
                                是
                            <#else>
                                否
                            </#if>
                        </td>
                        <td style="border-right: 1px dotted #676767; border-bottom: 1px dotted #676767;text-align: left; height:33px; width: 50px;">
                            是否有后端数据源：
                        </td>
                        <td style="border-right: 1px dotted #676767; border-bottom: 1px dotted #676767; height:33px; width: 140px;">
                            <#if appDesc.hasBackStore == 1>
                                是
                            <#else>
                                否
                            </#if>
                        </td>
                    </tr>
                    <tr>
                        <td style="border-right: 1px dotted #676767; border-bottom: 1px dotted #676767;text-align: left; height:33px; width: 50px;">
                            应用描述：
                        </td>
                        <td style="border-right: 1px dotted #676767; border-bottom: 1px dotted #676767; height:33px; width: 140px;">
                            ${appDesc.intro!}
                        </td>
                        <td style="border-right: 1px dotted #676767; border-bottom: 1px dotted #676767;text-align: left; height:33px; width: 50px;">
                            应用负责人：
                        </td>
                        <td style="border-right: 1px dotted #676767; border-bottom: 1px dotted #676767; height:33px; width: 140px;">
                            ${appDesc.officer!}
                        </td>
                        <td style="border-right: 1px dotted #676767; border-bottom: 1px dotted #676767;text-align: left; height:33px; width: 50px;">
                            应用申请时间：
                        </td>
                        <td style="border-right: 1px dotted #676767; border-bottom: 1px dotted #676767; height:33px; width: 140px;">
                            ${appDesc.createTime?string("yyyy-MM-dd HH:mm:ss")!}
                        </td>
                    </tr>
                </table>
            </#if>
        </td>
    </tr>

</table>
</p>
</body>
</html>