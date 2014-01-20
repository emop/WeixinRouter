package org.emop.weixin.router;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.emop.weixin.model.DataService;
import org.emop.weixin.model.TaodianApi;
import org.emop.weixin.model.impl.HTTPDataService;
import org.emop.weixin.monitor.StatusMonitor;
import org.emop.weixin.router.settings.Settings;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.ServletHandler;


public class HttpServer {
	private Settings settings = null;
	private Log log = LogFactory.getLog("wx.http");

	public HttpServer(Settings s){
		this.settings = s;

	}	
	
	public void run(){
		StatusMonitor.startMonitor();
		
		ThreadPoolExecutor threadPool = null;
		/**
		 * 小于coreSize自动增加新线程，
		 * 大于coreSize放到Queue里面，
		 * Queue满后开始创建新线程，至到maxSize
		 */
		int core_thread_count = 200;
		threadPool = new ThreadPoolExecutor(
				core_thread_count,
				settings.getInt(Settings.MAX_ROUTE_THREAD_COUNT, 500),
				10, 
				TimeUnit.SECONDS, 
				new LinkedBlockingDeque<Runnable>(core_thread_count * 2)
				);
		
		String appKey = settings.getString(Settings.TD_API_ID, "23");
		String appSecret = settings.getString(Settings.TD_API_SECRET, "5e969f648a49364841936ad6da0b18a9");

		TaodianApi api = new TaodianApi(appKey, appSecret, null);
		
		DataService service = new HTTPDataService(api);
		WeixinRouter.router = new WeixinRouter(threadPool, service);
		
		WeixinRouter.router.imageDomain = settings.getString(Settings.REMOTE_DOMAIN, null);
		
		startHTTPServer();
	}
	
	private void startHTTPServer(){
		int httpPort = settings.getInt(Settings.HTTP_PORT, -1);
		Server server = new Server(httpPort);
        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);
        //handler.addServletWithMapping("org.http.channel.server.servlet.CommandServlet", "/~/*");
        handler.addServletWithMapping("org.emop.weixin.router.servlet.ImageProxyServlet", "/img/*");
        handler.addServletWithMapping("org.emop.weixin.router.servlet.RouterServlet", "/route/*");
        handler.addServletWithMapping("org.emop.weixin.router.servlet.ApiServlet", "/api/*");
        handler.addServletWithMapping("org.emop.weixin.router.servlet.StatusServlet", "/status/*");
        try {
        	log.info("Start http server at " + httpPort);
			server.start();
			server.join();
		} catch (Exception e) {
			log.error(e.toString(), e);
		}
	}	

}
