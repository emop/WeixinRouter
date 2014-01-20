package org.emop.weixin.router.interceptor;

import java.util.HashMap;
import java.util.Map;

import org.emop.weixin.message.WeixinMessage;
import org.emop.weixin.model.DataService;
import org.emop.weixin.model.WeixinApp;
import org.emop.weixin.router.RouteInterceptor;
import org.emop.weixin.router.RouteSession;
import org.emop.weixin.router.WeixinUser;

public class FansCount implements RouteInterceptor{
	private DataService service = null;
	
	public FansCount(DataService service){
		this.service = service;
	}

	@Override
	public void start(RouteSession session, WeixinApp app, WeixinUser user,
			WeixinMessage source) {

		if(source == null) return;
		String e = source.data.get(WeixinMessage.EVENT);
		if(e == null) return;
		
		Map<String, Object> param = new HashMap<String, Object>();
		
		param.put("sid", session.sessionId);
		if(e.equals("subscribe") || e.equals("unsubscribe")){
			if(e.equals("subscribe")){
				param.put("value", 1);
			}else {
				param.put("value", -1);
			}
			
			service.getApi().call("weixin_account_fans_update", param);
		}
	}

	@Override
	public void response(RouteSession session, WeixinApp app, WeixinUser user,
			WeixinMessage source, WeixinMessage resp) {
		// TODO Auto-generated method stub
		
	}

}
