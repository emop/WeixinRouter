package org.emop.weixin.message;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.emop.weixin.model.WeixinApp;
import org.emop.weixin.router.WeixinAccount;
import org.emop.weixin.router.WeixinUser;

public class WeixinMessage implements Cloneable {
	public static final String MSG_TEXT = "text";
	public static final String CONTENT = "Content";
	public static final String EVENT = "Event";
	
	public boolean isResponseOK = false;
	public WeixinAccount account = null;
	public WeixinUser user = null;
	public WeixinApp app = null;
	
	
	public String toUserName = null;
	public String fromUserName = null;
	public String msgId = null;
	public String msgType = "text";
	public List<ImageLinkItem> items = null;
	
	
	public Map<String, String> data = new HashMap<String, String>();
	public String rawData = "";
	
	public String command = null;
	public String formate = "json";
	
	public String getMessageFormate(){
		return this.formate;
	}
	
	public void text(String text){
		data.put(CONTENT, text);
		this.msgType = MSG_TEXT;
	}
	
	public boolean isText(){
		return msgType != null && msgType.equals("text");
	}
	
	public boolean isNews(){
		return msgType != null && msgType.equals("news");
	}
	
	public boolean isMusic(){
		return msgType != null && msgType.equals("music");
	}

	public boolean isClick(){
		return false;
	}
	
	public String keyword(){
		return data.get(CONTENT);
	}
	
	
	public void writeTo(PrintWriter writer){
		long writeTime = System.currentTimeMillis() / 1000;
		writer.println("<xml>");
		writer.println("<ToUserName><![CDATA[" + toUserName + "]]></ToUserName>");
		writer.println("<FromUserName><![CDATA[" + fromUserName + "]]></FromUserName>");
		writer.println("<CreateTime><![CDATA[" + writeTime + "]]></CreateTime>");
		if(msgType == null || msgType.equals("")){
			msgType = "text";
		}
		writer.println("<MsgType><![CDATA[" + msgType + "]]></MsgType>");
		writeMessageBody(writer);
		
		writer.println("</xml>");		
	}
	
	protected void writeMessageBody(PrintWriter writer){
		if(msgType != null && msgType.equals("news")){
			writeNewBody(writer);
		}else if(msgType != null && msgType.equals("music")){
			writeMusicBody(writer);
		}else {
			writeTextBody(writer);
		}
	}
	
	protected void writeNewBody(PrintWriter writer){
		if(items != null){
			writer.println("<ArticleCount>" + items.size() + "</ArticleCount>");
			writer.println("<Articles>");
			for(ImageLinkItem item: items){
				item.writeTo(writer);
			}
			writer.println("</Articles>");			
		}
	}
	
	protected void writeMusicBody(PrintWriter writer){		
		writer.println("<Music>");
		
		writer.println("<Title><![CDATA[" + data.get("Title") +"]]></Title>");
		writer.println("<Description><![CDATA[" + data.get("Description") + "]]></Description>");
		writer.println("<MusicUrl><![CDATA[" + data.get("MusicUrl") + "]]></MusicUrl>");
		writer.println("<HQMusicUrl><![CDATA[" + data.get("HQMusicUrl") +"]]></HQMusicUrl>");
		
		writer.println("</Music>");
	}
	
	protected void writeTextBody(PrintWriter writer){
		String content = data.get(CONTENT);
		if(content == null || content.equals("")){
			content = "APP no response";
		}
		writer.println("<Content><![CDATA[" + content + "]]></Content>");
	}
	
	public WeixinMessage copy(){
		WeixinMessage m = null;
		try {
			m = (WeixinMessage)clone();
			m.data = new HashMap<String, String>();
			m.data.putAll(data);
		} catch (CloneNotSupportedException e) {
		}
		
		return m;
	}
}
