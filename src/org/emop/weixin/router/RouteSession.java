package org.emop.weixin.router;

import org.emop.weixin.message.WeixinMessage;
import org.emop.weixin.model.WeixinApp;

public class RouteSession {
	public String appId = null;
	public String userId = null;
	public String sessionId = null;
	
	public String signature = null;
	public String timestamp = null;
	public String nonce = null;
	public String echostr = null;
	
	public String hostUrl = null;
	
	public String toString(){
		return "" + appId + "/" + userId + "/" + sessionId;
	}
	
}
