package org.emop.weixin.model.impl;

import java.util.HashMap;
import java.util.Map;

import org.emop.http.HTTPResult;
import org.emop.weixin.message.WeixinMessage;
import org.emop.weixin.model.AppInstallInfo;
import org.emop.weixin.model.DataService;
import org.emop.weixin.model.TaodianApi;
import org.emop.weixin.model.WeixinApp;
import org.emop.weixin.router.RouteRequest;
import org.emop.weixin.router.RouteTable;
import org.emop.weixin.router.WeixinAccount;
import org.emop.weixin.router.WeixinUser;
import org.emop.weixin.utils.Cache;
import org.emop.weixin.utils.impl.SimpleCache;

public class HTTPDataService implements DataService {
	private Cache cache = new SimpleCache();
	private TaodianApi api = null; // new TaodianApi();
	
	public HTTPDataService(TaodianApi api){
		this.api = api;
	}

	@Override
	public AppInstallInfo getInstallInfo(String sid) {
		
		AppInstallInfo info = null;
		Object tmp = null;
		String ck = "sid_" + sid;
		
		tmp = cache.get(ck, true);
		if(tmp == null){
			Map<String, Object> p = new HashMap<String, Object>();
			p.put("sid", sid);
			Object obj = api.call("weixin_install_session_get", p);
			
			info = new AppInstallInfo();
			if(obj != null && obj instanceof HTTPResult){
				HTTPResult r = (HTTPResult)obj;
				if(r.isOK){
					info.sessionId = Integer.parseInt(sid);
					info.token = r.getString("data.token");
					info.status = r.getString("data.sub_status");
					info.loadTime = System.currentTimeMillis();

					if(info.status != null){
						info.statusCode = Integer.parseInt(info.status);
					}
					cache.set(ck, info, 60 * 60 * 4);
				}
			}		
		}else {
			info = (AppInstallInfo)tmp;
		}
		
		return info;
	}

	@Override
	public WeixinApp getWeixinApp(String appId) {
		WeixinApp app = null;
		Object tmp = null;
		String ck = "app_" + appId;
		
		tmp = cache.get(ck, true);
		if(tmp == null){
			Map<String, Object> p = new HashMap<String, Object>();
			p.put("id", appId);
			Object obj = api.call("weixin_app_info_get", p);
			
			app = new WeixinApp();
			if(obj != null && obj instanceof HTTPResult){
				HTTPResult r = (HTTPResult)obj;
				if(r.isOK){
					
					app.appId = Integer.parseInt(appId);
					app.appKey = r.getString("data.app_key");
					app.appUrl = r.getString("data.app_url");
					app.status = r.getString("data.app_status");
					app.loadTime = System.currentTimeMillis();
					if(app.status != null){
						app.statusCode = Integer.parseInt(app.status);
					}
					
					cache.set(ck, app, 60 * 60 * 4);
				}
			}
		}else {
			app = (WeixinApp)tmp;
		}
		
		return app;
	}

	@Override
	public WeixinAccount getWeixinAccount(String name) {
		String ck = "wx_" + name;
		
		WeixinAccount account = null;
		Object obj = cache.get(ck, true);
		
		if(obj == null){
			account = new WeixinAccount();
			account.wxUUID = name;
			
			cache.set(ck, account, 30 * 60);
		}else {
			account = (WeixinAccount)obj;
		}
		
		return account;
	}

	@Override
	public WeixinUser getWeixinUser(WeixinAccount account, String name) {
		if(account != null){
			return account.getUser(name);
		}
		return null;
	}

	@Override
	public WeixinMessage convertToUser(WeixinMessage msg, WeixinUser user) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void writeLog(RouteRequest req) {
		if(req.msg == null) return;
		// TODO Auto-generated method stub
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("app_id", req.session.appId);
		param.put("user_id", req.session.userId);
		param.put("to_username", req.msg.toUserName);
		param.put("from_username", req.msg.fromUserName);
		param.put("content_type", req.msg.msgType);
		param.put("event", req.msg.data.get("Event"));
		param.put("keyword", req.msg.data.get(WeixinMessage.CONTENT));
		
		if(req.responseMsg != null){
			param.put("response", req.responseMsg.rawData);
		}
		
		api.call("weixin_route_log", param);
	}

	@Override
	public void cleanInstallInfo(String sid, boolean force) {
		String ck = "sid_" + sid;
		if(force){
			cache.remove(ck);
		}else {
			AppInstallInfo app = getInstallInfo(sid);
			if(app != null && System.currentTimeMillis() - app.loadTime > 1000 * 30){
				cache.remove(ck);
			}
		}
	}

	@Override
	public void cleanWeixinApp(String appId, boolean force) {
		String ck = "app_" + appId;
		if(force){
			cache.remove(ck);
		}else {
			WeixinApp app = getWeixinApp(appId);
			if(app != null && System.currentTimeMillis() - app.loadTime > 1000 * 30){
				cache.remove(ck);
			}
		}
	}

	@Override
	public TaodianApi getApi() {
		return api;
	}

	@Override
	public RouteTable getRouteTable(String sessionId) {
		Object obj = cache.get(sessionId, true);
		
		if(obj == null || !(obj instanceof RouteTable)){
			obj = new RouteTable();
			
			cache.set(sessionId, obj, 60 * 60 * 24);
		}
				
		return (RouteTable)obj;
	}

}
