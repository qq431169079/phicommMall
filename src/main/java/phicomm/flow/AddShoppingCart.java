package phicomm.flow;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.cookie.BasicClientCookie;

import com.arronlong.httpclientutil.HttpClientUtil;
import com.arronlong.httpclientutil.common.HttpConfig;
import com.google.gson.Gson;

import phicomm.config.PhicommConstants;
import phicomm.logs.FlowUtils;
import phicomm.model.PhicommBuyInfo;
import phicomm.util.PhicommLogInfo;
import phicomm.util.PhicommThreadPool;

public class AddShoppingCart  extends FlowBase{

	private String execRequestResult = null;

	public String before(HttpConfig httpConfig , PhicommBuyInfo phicommBuyInfo, Map<String,Object> flowMap){
		String result = PhicommConstants.FLOW_CONTINUE;
		try{
			//判断是否跳过
			if((Boolean) flowMap.get("skip")){
				return PhicommConstants.FLOW_BREAK;
			}else if(null == PhicommThreadPool.PHICOMMHTREADPOOL.get(phicommBuyInfo.getPhone())){
				return PhicommConstants.FLOW_EXIT;
			}

			if(null != PhicommConstants.REBATE_MODEL && PhicommConstants.REBATE_MODEL && StringUtils.isNotBlank(phicommBuyInfo.getRebateVsid())){
				BasicClientCookie cookie = new BasicClientCookie("from_vshopid", phicommBuyInfo.getRebateVsid());
				String pattern = "[^//]*?\\.(com|cn|net|org|biz|info|cc|tv)" ;
		        Pattern p = Pattern.compile(pattern,Pattern.CASE_INSENSITIVE);
		        Matcher matcher = p.matcher(httpConfig.url());
		        matcher.find();
				cookie.setDomain(matcher.group());
				((HttpClientContext)httpConfig.context()).getCookieStore().addCookie(cookie);
			}

			//FlowUtils.info(String.format("IP:%s-手机:%s-流程:%s-Before-",phicommBuyInfo.getIp(),  phicommBuyInfo.getPhone() , flowMap.get("name")));
			PhicommLogInfo.logInfo("put", phicommBuyInfo.getPhone(), "开始添加到购物车" , phicommBuyInfo.getBuyType());
			result = PhicommConstants.FLOW_SUCCESS;
		}catch(Exception e){
			FlowUtils.errorException(String.format("IP:%s-手机:%s-流程:%s-Before失败-" , phicommBuyInfo.getIp(),  phicommBuyInfo.getPhone() , flowMap.get("name")) ,e);
			if("break".equals(flowMap.get("errorRepeatExec"))){
				result = PhicommConstants.FLOW_BREAK;
			}else if((Boolean) flowMap.get("errorRepeatExec")){
				result = PhicommConstants.FLOW_CONTINUE;
			}else{
				result = PhicommConstants.FLOW_EXIT;
			}
		}
		return result;
	}

	public String exec(HttpConfig httpConfig , PhicommBuyInfo phicommBuyInfo, Map<String,Object> flowMap , String beforeResult){
		try{
			if(PhicommConstants.FLOW_SUCCESS.equals(beforeResult)){
				FlowUtils.info(String.format("IP:%s-手机:%s-流程:%s-Exec-", phicommBuyInfo.getIp(),  phicommBuyInfo.getPhone() , flowMap.get("name")));
				PhicommLogInfo.logInfo("put", phicommBuyInfo.getPhone(), "添加到购物车中" , phicommBuyInfo.getBuyType());
				execRequestResult = HttpClientUtil.get(httpConfig.url(phicommBuyInfo.getFastBuyPage()));
			}
		}catch(Exception e){
			FlowUtils.errorException(String.format("IP:%s-手机:%s-流程:%s-Exec失败-" , phicommBuyInfo.getIp(),  phicommBuyInfo.getPhone() , flowMap.get("name")) ,e);
			PhicommLogInfo.logInfo("put", phicommBuyInfo.getPhone(), String.format("添加到购物车失败,错误信息:%s", e.getMessage()) , phicommBuyInfo.getBuyType());
			if("break".equals(flowMap.get("errorRepeatExec"))){
				beforeResult = PhicommConstants.FLOW_BREAK;
			}else if((Boolean) flowMap.get("errorRepeatExec")){
				beforeResult = PhicommConstants.FLOW_CONTINUE;
			}else{
				beforeResult = PhicommConstants.FLOW_EXIT;
			}
		}
		return beforeResult;
	}

	public String after(HttpConfig httpConfig , PhicommBuyInfo phicommBuyInfo, Map<String,Object> flowMap , String beforeResult){
		try{
			if(PhicommConstants.FLOW_SUCCESS.equals(beforeResult)){
				execRequestResult = unicodeToString(execRequestResult.trim());
				FlowUtils.info(String.format("IP:%s-手机:%s-流程:%s-After-%s-",phicommBuyInfo.getIp(),  phicommBuyInfo.getPhone() , flowMap.get("name"),execRequestResult));
				if(StringUtils.isNotBlank(execRequestResult)){
					if(execRequestResult.startsWith("{\"error\"")){
						Map map = new Gson().fromJson(execRequestResult , Map.class);
						execRequestResult = map.get("error").toString();
					}else if(execRequestResult.startsWith("{\"success\"")){
						beforeResult = PhicommConstants.FLOW_BREAK;
					}
				}
				if(PhicommConstants.FLOW_BREAK.equals(beforeResult)){
					PhicommLogInfo.logInfo("put", phicommBuyInfo.getPhone(), "添加到购物车成功" , phicommBuyInfo.getBuyType());
				}else{
					PhicommLogInfo.logInfo("put", phicommBuyInfo.getPhone(), String.format("添加到购物车失败返回:%s", execRequestResult) , phicommBuyInfo.getBuyType());
				}
			}
		}catch(Exception e){
			FlowUtils.errorException(String.format("IP:%s-手机:%s-流程:%s-After失败-" , phicommBuyInfo.getIp(),  phicommBuyInfo.getPhone() , flowMap.get("name")) ,e);
			PhicommLogInfo.logInfo("put", phicommBuyInfo.getPhone(), String.format("添加到购物车失败返回:", execRequestResult) , phicommBuyInfo.getBuyType());
			if("break".equals(flowMap.get("errorRepeatExec"))){
				beforeResult = PhicommConstants.FLOW_BREAK;
			}else if((Boolean) flowMap.get("errorRepeatExec")){
				beforeResult = PhicommConstants.FLOW_CONTINUE;
			}else{
				beforeResult = PhicommConstants.FLOW_EXIT;
			}
		}
		return beforeResult;
	}
	
}
