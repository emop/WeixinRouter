package org.emop.weixin.router;

import java.util.HashMap;
import java.util.Map;

import org.emop.weixin.message.WeixinMessage;
import org.emop.weixin.utils.Cache;
import org.emop.weixin.utils.impl.SimpleCache;

public class WeixinAccount {
	private Cache cache = new SimpleCache();

	public String wxUUID = "";
	
	public Map<String, WeixinMessage> keywords = new HashMap<String, WeixinMessage>();
	
	public WeixinMessage getCachedMessage(String keyword){
		return keywords.get(keyword.trim());
	}
	
	public WeixinUser getUser(String name){
		WeixinUser u = null;
		Object o = cache.get(name);
		if(o != null){
			u = (WeixinUser) o;
		}
		return u;
	}	
	
	public void setUser(String name, WeixinUser u){
		u.account = this;
		cache.set(name, u, 60 * 5);
	}
}
