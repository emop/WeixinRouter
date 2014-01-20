package org.emop.weixin.router.interceptor;

import org.emop.weixin.message.WeixinMessage;
import org.emop.weixin.model.WeixinApp;
import org.emop.weixin.router.RouteInterceptor;
import org.emop.weixin.router.RouteSession;
import org.emop.weixin.router.WeixinUser;

public class AppStatus implements RouteInterceptor {

	@Override
	public void start(RouteSession session, WeixinApp app, WeixinUser user,
			WeixinMessage source) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void response(RouteSession session, WeixinApp app, WeixinUser user,
			WeixinMessage source, WeixinMessage resp) {
		// TODO Auto-generated method stub
		
	}


}
