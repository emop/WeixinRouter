package org.emop.weixin.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.emop.http.HTTPClient;
import org.emop.weixin.message.WeixinMessage;
import org.emop.weixin.router.RouteSession;

public class XmlWeixinApp extends WeixinApp {
	public boolean isEnabled(){
		return true;
	}

	public WeixinMessage forwardMessage(HTTPClient client, WeixinMessage msg, RouteSession session){
		String url = this.appUrl;
		if(url.indexOf('?') == -1){
			url += "?";
		}
		url += "&timestamp=" + session.timestamp;
		url += "&nonce=" + session.nonce;
		url += "&signature=" + getSignature(session); //.timestamp;
		
		URLConnection conn = null;
		InputStream ins = null;
		WeixinMessage resp = new WeixinMessage();
		resp.app = this;
		try{
			resp.formate = "xml";
			
			conn = new URL(url).openConnection();
			conn.setConnectTimeout(1000 * 30);
			conn.setReadTimeout(1000 * 30);
			
			conn.setRequestProperty("X-from-WexinGate", "xml");
			conn.setRequestProperty("Content-Type", "application/xml");
			conn.setDoOutput(true);
			OutputStream out = conn.getOutputStream(); //new PrintWriter(buffer);
			out.write(msg.rawData.getBytes("utf8"));
			out.close();
			
			ins = conn.getInputStream();
			ByteArrayOutputStream buffer = new ByteArrayOutputStream(64 * 1024);
			byte[] tmp = new byte[1024 * 16];
			for(int i = 0; i >= 0;){
				i = ins.read(tmp);
				if(i > 0){
					buffer.write(tmp, 0, i);
				}
			}
			buffer.close();
			
			//if(log.d)
			
			resp.rawData = new String(buffer.toByteArray(), "utf8");
			resp.isResponseOK = true;
			log.info("forward to url:" + url + "\nresp:" + resp.rawData);
			
		} catch (IOException e) {
			log.error(e.toString(), e);
		}finally{
			if(ins != null){
				try {
					ins.close();
				} catch (IOException e) {
					log.error(e.toString(), e);
				}
			}
		}

		return resp;
	}
	
	protected String getSignature(RouteSession s){
		List<String> list = new ArrayList<String>();
		if(s.timestamp != null){
			list.add(s.timestamp);
		}
		if(s.nonce != null){
			list.add(s.nonce);
		}
		if(this.appKey != null){
			list.add(appKey);
		}
		Collections.sort(list);
		
		String f = "";
		for(String term: list){
			f += term;
		}
		
		return TaodianApi.SHA1(f);	
	}
}
