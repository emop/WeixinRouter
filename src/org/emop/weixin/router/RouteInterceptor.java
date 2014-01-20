package org.emop.weixin.router;

import org.emop.weixin.message.WeixinMessage;
import org.emop.weixin.model.WeixinApp;

public interface RouteInterceptor {
	public void start(final RouteSession session, final WeixinApp app, final WeixinUser user, final WeixinMessage source);	
	public void response(final RouteSession session, final WeixinApp app, final WeixinUser user, final WeixinMessage source, final WeixinMessage resp);
}
