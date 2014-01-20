package org.emop.weixin.router;

import java.io.PrintWriter;

import org.emop.weixin.message.WeixinMessage;
import org.emop.weixin.monitor.Benchmark;
import org.mortbay.util.ajax.Continuation;

/**
 * 封一个消息转发请求。
 * @author deonwu
 */
public class RouteRequest{
	public WeixinMessage msg = null;
	public WeixinMessage responseMsg = null;
	public RouteSession session = null;
	
	public Benchmark mark = null;
	public boolean isDone = false;
	//public long startTime = System.currentTimeMillis();
	//public long endTime = 0;

	//public transient WeixinRouter router = null;	
	public transient Continuation continuation = null;
	public transient PrintWriter writer = null;	

	public WeixinMessage createResponse(){
		WeixinMessage responseMsg = new WeixinMessage();
		if(msg != null){
			responseMsg.fromUserName = msg.toUserName;
			responseMsg.toUserName = msg.fromUserName;
		}
		responseMsg.msgType = "text";
		responseMsg.data.put(WeixinMessage.CONTENT, "应用响应超时");
		
		return responseMsg;
	}
	
	
}
