package org.emop.weixin.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.emop.http.HTTPClient;
import org.emop.http.HTTPResult;
import org.emop.weixin.message.ImageLinkItem;
import org.emop.weixin.message.WeixinMessage;
import org.emop.weixin.router.RouteSession;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * 微信应用的接口地址。
 * 
 * @author deonwu
 */
public class WeixinApp {
	public long loadTime = 0;
	
	protected static Log log = LogFactory.getLog("wx.app");

	public int appId = 0;
	public String appUrl = null;
	public String appKey = "";
	
	public String status = "";
	public int statusCode = 0;
	
	public String exitCommand = "q";
	
	public String exitKeyword = "";
	
	public boolean isEnabled(){
		return statusCode > 1000 && statusCode < 1999;
	}
	
	public WeixinMessage forwardMessage(HTTPClient client, WeixinMessage msg, RouteSession session){
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("ToUserName", msg.toUserName);
		param.put("FromUserName", msg.fromUserName);
		param.put("CreateTime", msg.data.get("CreateTime"));
		param.put("MsgType", msg.msgType);
		
		param.put("Content", msg.data.get(WeixinMessage.CONTENT));
		param.put("PicUrl", msg.data.get("PicUrl"));
		param.put("MsgId", msg.data.get("MsgId"));
		
		param.put("Location_X", msg.data.get("Location_X"));
		param.put("Location_Y", msg.data.get("Location_Y"));
		param.put("Scale", msg.data.get("Scale"));
		param.put("Label", msg.data.get("Label"));
		
		param.put("Title", msg.data.get("Title"));
		param.put("Description", msg.data.get("Description"));
		param.put("Url", msg.data.get("Url"));

		param.put("Event", msg.data.get("Event"));
		param.put("EventKey", msg.data.get("EventKey"));	

		param.put("wx_app_id", session.appId);
		param.put("wx_user_id", session.userId);
		param.put("wx_install_id", session.sessionId);
		param.put("wx_sign", getSignature(msg));
		
		HTTPResult r = client.post(this.appUrl, param);
		
		WeixinMessage resp = null;
		resp = new WeixinMessage();
		resp.fromUserName = msg.toUserName;
		resp.toUserName = msg.fromUserName;
		if(r != null && r.json != null){
			resp.rawData = r.json.toJSONString();
		}

		if(r != null && r.getString("wx_status") != null && r.getString("wx_status").equals("ok")){	
			resp.isResponseOK = true;
			resp.msgType = r.getString("MsgType");
			if(resp.isNews()){
				convertItems(resp, r);
			}else if(resp.isText()) {
				resp.text(r.getString(WeixinMessage.CONTENT));
			}else if(resp.isMusic()){
				resp.data.put("Title", r.getString("Title"));
				resp.data.put("Description", r.getString("Description"));
				resp.data.put("MusicUrl", r.getString("MusicUrl"));
				resp.data.put("HQMusicUrl", r.getString("HQMusicUrl"));
			}
			
			resp.command = r.getString("command");
		}else {
			String context = r.getString(WeixinMessage.CONTENT);
			if(context != null && context.length() > 0){
				resp.text(context);
			}else {
				resp = null;
			}
		}

		return resp;
	}
	
	private void convertItems(WeixinMessage resp, HTTPResult data){
		
		resp.items = new ArrayList<ImageLinkItem>();
		JSONArray items = (JSONArray)data.json.get("Articles");
		ImageLinkItem item = null;
		for(int i = 0; i < items.size(); i++){
			item = new ImageLinkItem();
			JSONObject o = (JSONObject)items.get(i);
			item.Title = o.get("Title") + "";
			item.PicUrl = o.get("PicUrl") + "";
			item.Url = o.get("Url") + "";
			item.Description = o.get("Description") + "";
			resp.items.add(item);
		}
	}
	
	private String getSignature(WeixinMessage msg){
		String url = msg.data.get("Url");
		String content = msg.data.get(WeixinMessage.CONTENT);
		String msgId = msg.data.get("MsgId");
		String key = String.format("%s,%s,%s,%s,%s,%s,%s", 
				msg.toUserName, 
				msg.fromUserName, 
				msg.data.get("CreateTime"),
				content == null ? "" : content,
				url == null ? "" : url,
				msgId == null ? "" : msgId,
				appKey);
		
		if(log.isDebugEnabled()){
			log.debug("signature str:" + key);
		}
		
		return TaodianApi.MD5(key);
	}
}
