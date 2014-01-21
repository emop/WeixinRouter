package org.emop.weixin.message;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.emop.weixin.model.WeixinApp;
import org.emop.weixin.router.WeixinAccount;
import org.emop.weixin.router.WeixinUser;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

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
		if(this.formate.equals("xml")){
			writer.write(rawData);
		}else {
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
	
	public void parseXMLData() throws IOException{
		SAXBuilder builder = new SAXBuilder();

		Document doc = null;
		try {
			InputStream ins = new ByteArrayInputStream(rawData.getBytes("utf8"));
			doc = builder.build(new InputStreamReader(ins, "utf8"));
		} catch (JDOMException e1) {
		}
		List<Element> elements = null;
		if(doc != null){
			elements = doc.getRootElement().getChildren();
			for(Element e: elements){
				data.put(e.getName(), e.getText());
			}
			fromUserName = data.get("FromUserName");
			toUserName = data.get("ToUserName");
			msgId = data.get("MsgId");
			msgType = data.get("MsgType");
		}
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
