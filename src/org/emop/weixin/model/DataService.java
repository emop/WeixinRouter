package org.emop.weixin.model;

import org.emop.weixin.message.WeixinMessage;
import org.emop.weixin.router.RouteRequest;
import org.emop.weixin.router.RouteTable;
import org.emop.weixin.router.WeixinAccount;
import org.emop.weixin.router.WeixinUser;

public interface DataService {
	
	public AppInstallInfo getInstallInfo(String sid);
	public WeixinApp getWeixinApp(String appId);
	
	public WeixinAccount getWeixinAccount(String name);

	public WeixinUser getWeixinUser(WeixinAccount account, String name);

	public WeixinMessage convertToUser(WeixinMessage msg, WeixinUser user);

	public RouteTable getRouteTable(String sessionId);	
	//public void updateWeixinAccountCache(WeixinAccount account, WeixinUser user, WeixinMessage msg);
	
	public void writeLog(RouteRequest req);
	
	public void cleanInstallInfo(String sid, boolean force);
	public void cleanWeixinApp(String appId, boolean force);	

	
	public TaodianApi getApi();
}
