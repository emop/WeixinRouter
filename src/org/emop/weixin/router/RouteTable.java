package org.emop.weixin.router;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.emop.weixin.message.WeixinMessage;
import org.emop.weixin.model.WeixinApp;
import org.emop.weixin.model.XmlWeixinApp;
import org.emop.weixin.utils.Cache;
import org.emop.weixin.utils.impl.SimpleCache;
import org.emop.wx.router.Action;
import org.emop.wx.router.DefaultRouter;
import org.emop.wx.router.RouteException;
import org.emop.wx.router.Router;
import org.emop.wx.router.TargetURL;

/**
 * 微信消息转发路由表。每个应用会话有一个自己的转发规则。 
 * 可以根据不同的消息类型转发，到不同的URL上面去。
 * 
 * @author deonwu
 *
 */
public class RouteTable {
	private static Log log = LogFactory.getLog("wx.route");
	
	private boolean isnew = true;
	public Router router = new DefaultRouter();
	public Cache cache = new SimpleCache();
	
	public boolean isNew(){
		return isnew;
	}
	
	public  WeixinApp route(WeixinMessage msg, WeixinApp root, WeixinAccount account, WeixinUser user){
		String curApp = user.userID;
		Object obj = cache.get(curApp, true);
		if(obj != null){
			log.debug("get session app for user:" + user.userID + ", app:" + obj);
		}
		
		if(obj == null || !(obj instanceof WeixinApp)){
			TargetURL t = router.route(msg);
			if(t != null && t.isOK){
				obj = createApp(t);
				if(t.actionName.equals(Action.ENTER)){
					cache.set(user.userID, obj, 60 * 30);
				}
			}
		}else {	//如果存在已经关联的会话，检查是否遇到退出会话的命令。如果是退出命令，就删除会话回到根菜单。
			WeixinApp tmp = (WeixinApp)obj;
			String content = msg.data.get(WeixinMessage.CONTENT);
			if(msg.isText() && content != null && content.trim().equalsIgnoreCase(tmp.exitCommand)){
				cache.remove(user.userID);
				return root;
			}			
		}
		
		if(obj == null || !(obj instanceof WeixinApp)){
			return root;
		}
		
		return (WeixinApp)obj;
	}
	
	public WeixinMessage postProcess(WeixinMessage resp, WeixinApp app, WeixinAccount account, WeixinUser user){
		if(isnew){
			try {
				router.initRoute();
			} catch (RouteException e) {
				log.error("init route:" + e.toString(), e);
			}
		}
		isnew = false;
		String s = resp.getMessageFormate();
		if(s != null && s.equals("json")){
			if(resp.command != null && resp.command.length() > 0){
				if(log.isDebugEnabled()){
					log.debug("process command user:" + user.userID + ", command:" + resp.command);
				}
				for(String c : resp.command.split("\\n")){
					c = c.trim();
					if(c.length() == 0 || c.startsWith("#")) continue;
					processCommandLine(c, user.userID);
				}
			}
		}
		
		return resp;
	}
	
	protected void processCommandLine(String cli, String userId){
		if(log.isDebugEnabled()){
			log.debug("cmd:" + cli);
		}
		if(cli.startsWith("route")){
			try {
				router.updateRouteTable(cli);
			} catch (RouteException e) {
				log.warn("command error:" + cli, e);
			}
		}else if(cli.equals("exit")){
			cache.remove(userId);
		}else if(cli.startsWith("next")){
			TargetURL n = new TargetURL();
			Map<String, String> p = convertMap(cli);
			n.url = p.get("url");
			n.type = p.get("type");
			n.token = p.get("token");
			n.retCmd = p.get("ret_code");
			
			WeixinApp app = createApp(n);
			cache.set(userId, app, 60 * 30);
		}
	}
	
	protected Map<String, String> convertMap(String m){
		Map<String, String> p = new HashMap<String, String>();
		for(String s : m.split(" ")){
			s = s.trim();
			if(s.indexOf('=') <= 1) continue;
			String[] tmp = s.split("=", 2);
			p.put(tmp[0], tmp[1]);
		}
		return p;
	}
	
	protected WeixinApp createApp(TargetURL r){
		WeixinApp app = null;
		if(r.type != null && r.type.equals("xml")){
			app = new XmlWeixinApp();
		}else {
			app = new WeixinApp();
		}
		
		app.appUrl = r.url;
		app.appKey = r.token;
		if(r.retCmd != null && r.retCmd.length() > 0){
			app.exitCommand = r.retCmd;
		}else {
			app.exitCommand = "q";
		}
		
		return app;
	}
}
