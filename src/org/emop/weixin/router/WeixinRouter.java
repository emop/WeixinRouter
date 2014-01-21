package org.emop.weixin.router;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.emop.http.HTTPClient;
import org.emop.weixin.message.ImageLinkItem;
import org.emop.weixin.message.WeixinMessage;
import org.emop.weixin.model.AppInstallInfo;
import org.emop.weixin.model.DataService;
import org.emop.weixin.model.TaodianApi;
import org.emop.weixin.model.WeixinApp;
import org.emop.weixin.monitor.Benchmark;
import org.emop.weixin.router.interceptor.FansCount;
import org.emop.weixin.utils.Cache;
import org.emop.weixin.utils.impl.SimpleCache;
import org.jdom2.input.SAXBuilder;
import org.mortbay.util.ajax.ContinuationSupport;

/**
 * 微信消息路由器。把从HTTP 进来的请求，封装到一个RouteRequest， 然后交给路由线程池处理。这样可以
 * 避免HTTP 线程阻塞导致服务无响应。 也可以更好的控制请求的超时。
 *  
 * @author deonwu
 *
 */
public class WeixinRouter {
	private Log log = LogFactory.getLog("wx.router");
	
	public static final long REQUEST_TIMEOUT = 4500; //4.5 秒后超时请求，避免被微信断掉。
	
	public static WeixinRouter router = null;
	protected ThreadPoolExecutor workerPool = null;
	protected CopyOnWriteArrayList<RouteRequest> pendingTask = new CopyOnWriteArrayList<RouteRequest>();
	protected HTTPClient httpClient = null;
	//protected ArrayBlockingQueue<RouteRequest> logWriteQueue = new ArrayBlockingQueue<RouteRequest>(1024);
	protected DataService dataService = null;
	protected RouteInterceptor routeInterceptor = null;
	protected List<RouteInterceptor> interceptors = new ArrayList<RouteInterceptor>();
	protected Timer timer = null;

	public static Cache imageShortUrl = new SimpleCache();
	public String imageDomain = null;
	protected int imageCount = 1;
	
	public WeixinRouter(ThreadPoolExecutor workers, DataService service){
		this.workerPool = workers;
		this.dataService = service;
		
		httpClient = HTTPClient.create();
		timer = new Timer();
		
		/**
		 * 每隔 0.1 秒检查一次超时的请求。
		 */
		timer.scheduleAtFixedRate(new TimerTask(){
			@Override
			public void run() {
				try{
					checkTimeoutRequest();
				}catch(Throwable e){
					log.error(e.toString(), e);
				}
			}
			
		}, 100L, 100L);
		
		
		routeInterceptor = new RouteInterceptorProxy();	
		
		interceptors.add(new FansCount(dataService));
	}
	
	public void route(final HttpServletRequest request, final HttpServletResponse response) throws IOException{
		response.setStatus(HttpServletResponse.SC_OK);
		response.setCharacterEncoding("utf8");
		response.setContentType("text/xml");
		
		if(log.isDebugEnabled()){
			log.debug("start request:" + request.getRequestURI());
		}
		
		final RouteRequest req = new RouteRequest();
		req.mark = Benchmark.start(Benchmark.WX_REQUEST, req);
		req.writer = response.getWriter();
		req.session = parseSession(request);
		
		if(request.getMethod().equalsIgnoreCase("post")){
			req.msg = parseMessage(request);
		}
		
		/**
		 * 避免XML转发规则，把消息转发给自己。导致递归调用。在XmlWexinApp里面设置这个调用头信息。
		 */
		String from =request.getHeader("X-from-WexinGate");
		boolean isRecurse = from != null && from.equals("xml");
		
		if(workerPool.getQueue().remainingCapacity() > 10 && !isRecurse){
			req.continuation =  ContinuationSupport.getContinuation(request, null); 
			pendingTask.add(req);		
			workerPool.execute(new Runnable(){
				public void run(){
					try{
						forwardRequest(req); // req.processRequest();
					}catch(Throwable e){
						log.error(e.toString(), e);
					}
				}
			});
							
			/**
			 * Servlet的方法已经会返回，但是Response的会话是一直保留。直到
			 * continuation.resume()， 被调用。
			 * 
			 * 这个其实是一个线程复用的设计模式。
			 * 
			 * 异步处理转发请求，避免超过响应时间还没用返回。微信接口5秒没用响应就要放弃响应。所有
			 * 消息应该在5秒以内回复。
			 */
			req.continuation.suspend(6 * 1000);
		}else {
			WeixinMessage responseMsg = req.createResponse();
			responseMsg.text("服务器繁忙，请稍后再试。");
			
			responseMsg.writeTo(req.writer);
		}
	}
	

	
	protected WeixinMessage parseMessage(HttpServletRequest request){
		WeixinMessage msg = new WeixinMessage();
		try {
			InputStream ins = request.getInputStream();
			ByteArrayOutputStream buffer = new ByteArrayOutputStream(64 * 1024);
			byte[] tmp = new byte[1024 * 16];
			for(int i = 0; i >= 0;){
				i = ins.read(tmp);
				if(i > 0){
					buffer.write(tmp, 0, i);
				}else {
					break;
				}
			}
			buffer.close();
			msg.rawData = new String(buffer.toByteArray(), "utf8");
			if(log.isDebugEnabled()){
				log.info("read msg:" + msg.rawData);
			}
			msg.parseXMLData();
		} catch (Exception e) {
			log.error(e.toString(), e);
		}
		
		return msg;
	}
	
	protected RouteSession parseSession(HttpServletRequest request){
		RouteSession session = new RouteSession();
		
        Pattern pa = Pattern.compile("/(\\d+)/(\\d+)/(\\d+)");
        Matcher ma = pa.matcher(request.getRequestURI());
        if(ma.find()){
        	session.appId = ma.group(1);
        	session.userId = ma.group(2);
        	session.sessionId = ma.group(3);
        }else {
        	throw new Error("Not found path from:" + request.getRequestURI());
        }
        
        session.signature = request.getParameter("signature");
        session.timestamp = request.getParameter("timestamp");
        session.nonce = request.getParameter("nonce");
        session.echostr = request.getParameter("echostr");
        
        int port = request.getServerPort();
        String name = request.getServerName();
        String url = "http://" + name;
        if(port != 80){
        	url += ":" + port;
        }
        if(url.indexOf("127.0.0.1") > 0 && imageDomain != null){
        	session.hostUrl = imageDomain;
        }else {
        	session.hostUrl = url;
        }
		return session;
	}	
	
	public void timeoutRequest(RouteRequest req){
		WeixinMessage responseMsg = req.createResponse();
		responseMsg.text("应用响应超时");
		
		responseMsg.writeTo(req.writer);
		
		Benchmark mark = Benchmark.start(Benchmark.WX_REQUEST_TIMEOUT, req);
		mark.done();
		
		doneRequest(req);
	}
	
	public void doneRequest(RouteRequest req){
		if(log.isDebugEnabled()){
			log.debug("done request:" + req.session.toString());
		}
		synchronized(req){
			if(!req.isDone){
				req.isDone = true;
				pendingTask.remove(req);
				req.writer.flush();
				req.continuation.resume();
				//req.continuation.
			}
		}
	}
	
	/**
	 * 检查已经超时的转发请求。
	 */
	public void checkTimeoutRequest(){
		Benchmark mark = Benchmark.start(Benchmark.PEDDING_REQUEST);
		mark.done(pendingTask.size());

		if(pendingTask.size() == 0) return;
		
		long curTime = System.currentTimeMillis();
		for(RouteRequest req : pendingTask){
			if(curTime - req.mark.start > REQUEST_TIMEOUT){
				final RouteRequest r = req; 
				workerPool.execute(new Runnable(){
					public void run(){
						try{
							timeoutRequest(r);
						}catch(Throwable e){
							log.error(e, e);
						}
					}
				});
			}
		}
	}
	
	/**
	 * 处理微信请求，步骤：
	 * 
	 * 1. 通过sessionId 查询接口的token。
	 * 2. 检查请求的签名是否正确。
	 * 3. 如果没有消息内容，就显示接入成功。
	 * 4. 如果有消息内容，转发到具体的应用接口。
	 *    a. 用户会话是否已经，切换到app。
	 *    a1. 如果不是#exit# 关键字，就直接转发
	 *    a2. 退出消息状态。
	 *    b. 关键字路由，消息类型，用户ID路由，
	 *    c. 切换APP。
	 *    d. 处理消息返回结果，更新路由表。
	 *    
	 *    route chain: forward, log
	 *    
	 * 5. 转换APP的图片链接到网关的链接。 （回复的图片地址必须和接口的域名相同)
	 * 6. 完成回复消息，
	 * 		8.1 日志更新
	 * 		8.2 消息是否可以缓存。
	 * 
	 * @param eq
	 */
	public void forwardRequest(RouteRequest req){
		AppInstallInfo install = dataService.getInstallInfo(req.session.sessionId);
		WeixinMessage msg = req.createResponse();
		WeixinApp app = dataService.getWeixinApp(req.session.appId);
		try{
			if(install != null){
				if(checkSignature(req.session, install)){
					this.routeInterceptor.start(req.session, msg.app, msg.user, req.msg);
					if(req.msg != null){
						if(app != null && app.isEnabled()){
							WeixinMessage tmp = forwardWeixinMessage(req.session, req.msg, app);
							if(tmp != null) {
								msg = tmp;
								this.routeInterceptor.response(req.session, msg.app, msg.user, req.msg, tmp);
							}else {
								msg.text("应用没有正确响应，请与管理员联系。");								
							}
						}else {
							msg.text("应用不能使用，请与管理员联系。状态:" + app.status);
						}
					}else {
						msg = null;
						req.writer.print(req.session.echostr);
					}
				}
			}else {
				msg.text("没有找到接口信息:" + req.session.sessionId + ", 请与管理员联系");
				log.info("Not found app install:" + req.session.sessionId);
			}
			req.responseMsg = msg;
			formatLinkUrl(msg, req.session);
			if(msg != null && !req.isDone){
				msg.writeTo(req.writer);
			}
			writeForwardLog(req);
		}finally{
			doneRequest(req);
			
			/*
			整个请求完成时间。可能前面请求已经超时了。但是还是要记录完成状态。用于统计分析
			*/
			req.mark.done();
		}
	}
	
	protected void formatLinkUrl(WeixinMessage msg, RouteSession s){
		if(msg != null && msg.items != null){
			
			int index = 0;
			for(ImageLinkItem item : msg.items){
				String url = item.PicUrl;
				if(url != null && url.trim().length() > 0){
					/*
					try {
						url = URLEncoder.encode(url.trim(), "UTF-8");
					} catch (UnsupportedEncodingException e) {
					}
					*/
					
					int width = (index == 0) ? 400 : 80;
					item.PicUrl = s.hostUrl + convertImageUrl(url, width); // "/img/?img=" + url;
				}
				index++;
			}
		}
	}
	
	protected synchronized String convertImageUrl(String image, int width){
		imageCount = (imageCount + 1) % 10000000 + 1;
		
		imageShortUrl.set(imageCount + "", image, 60 * 60 * 24 * 7);
		
		return String.format("/img/%s_%s.jpg", imageCount, width);
		//return image;
	}
	
	
	/**
	 * 根据当前会话，和应用安装信息，检查请求的签名是否正确。
	 */
	protected boolean checkSignature(RouteSession s, AppInstallInfo info){
		List<String> list = new ArrayList<String>();
		if(s.timestamp != null){
			list.add(s.timestamp);
		}
		if(s.nonce != null){
			list.add(s.nonce);
		}
		if(info.token != null){
			list.add(info.token);
		}
		Collections.sort(list);
		
		String f = "";
		for(String term: list){
			f += term;
		}
		
		String signature = TaodianApi.SHA1(f);
		if(log.isDebugEnabled()){
			log.debug("signature check, param:" + s.signature + ", local:" + signature);
		}
		
		boolean result = (s.signature != null && signature != null && s.signature.equals(signature));
		
		if(!result){
			dataService.cleanInstallInfo(s.sessionId, false);
		}
		
		return result;
	}
	
	/**
	 * 把微信消息转发给，APP 的接口。
	 * 
	 * 1. 检查是否有缓存消息。
	 * 2. 恢复用户会话，如果当前会话是有状态模式，进行会话状态处理。
	 * 3. 消息是否是文本消息，是否匹配的关键字缓存。
	 * 4. 转发消息到app
	 */
	protected WeixinMessage forwardWeixinMessage(RouteSession session, WeixinMessage msg, WeixinApp app){		
		WeixinAccount account = dataService.getWeixinAccount(msg.toUserName);
		WeixinUser user = dataService.getWeixinUser(account, msg.fromUserName);
		
		RouteTable routeTable = dataService.getRouteTable(session.toString());
		//if		
		String keyword = null;
		if(msg.isText() || msg.isClick()){
			keyword = msg.keyword();			
		}
		
		WeixinApp nextApp = app;
		WeixinMessage resp = null;
		
		/**
		 * 初始化默认的路由信息表。
		 */
		if(routeTable != null && routeTable.isNew()){
			WeixinMessage tmp = msg.copy();
			tmp.msgType = "event";
			tmp.data.put("MsgType", "event");
			tmp.data.put("Event", "init_route_table");
			
			WeixinMessage routeMsg = app.forwardMessage(httpClient, tmp, session);
			if(routeMsg != null){
				routeTable.postProcess(routeMsg, app, account, user);
			}else {
				log.warn("Failed to init app route table, session:" + session.toString());
			}
		}

		if(routeTable != null && account != null && user != null && keyword != null){
			nextApp = routeTable.route(msg, app, account, user);			
		}else {
			nextApp = app;
		}
		
		resp = nextApp.forwardMessage(httpClient, msg, session);
		
		if(resp != null){
			resp.account = account;
			resp.app = nextApp;
			resp.user = user;	
			
			if(routeTable != null) {
				resp = routeTable.postProcess(resp, nextApp, account, user);
			}
		}
		if(resp == null || !resp.isResponseOK){
			dataService.cleanWeixinApp(session.appId, false);
		}
		
		return resp;
	}
	
	/**
	 * 写消息转发日志，为了提高消息响应速度。这个方法只是把要写的操作放到一个队列。具体的日志操作
	 * 由其他线程异步完成。
	 * 
	 * @param req
	 */
	protected void writeForwardLog(final RouteRequest req){
		workerPool.execute(new Runnable(){
			public void run(){
				dataService.writeLog(req);
			}
		});
	}
	
	
	class RouteInterceptorProxy implements RouteInterceptor{

		@Override
		public void start(final RouteSession s, final WeixinApp app, final WeixinUser user,
				final WeixinMessage source) {
			workerPool.execute(new Runnable(){
				public void run(){
					try{
						for(RouteInterceptor iter : interceptors){
							iter.start(s, app, user, source);
						}
					}catch(Throwable e){
						log.error(e, e);
					}
				}
			});			
		}

		@Override
		public void response(final RouteSession s, final WeixinApp app, final WeixinUser user,
				final WeixinMessage source, final WeixinMessage resp) {
			workerPool.execute(new Runnable(){
				public void run(){
					try{
						for(RouteInterceptor iter : interceptors){
							iter.response(s, app, user, source, resp);
						}
					}catch(Throwable e){
						log.error(e, e);
					}
				}
			});			
		}
		
	}
}
