package org.emop.weixin.router;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.emop.weixin.Version;
import org.emop.weixin.router.settings.Settings;

public class Main {
	private Log log = LogFactory.getLog("wx.main");
	public static final String version = Version.getVersion();
	
	public static final String VERSION = "version";
	public static final String PREFIX = "prefix";
	public static final String HTTPPORT = "http_port";	
	public static final String HTTP_URL = "http_url";
	
	public static void main(String[] args) throws IOException{
		Options options = new Options();
		options.addOption(VERSION, false, "show version.");
		options.addOption(PREFIX, true, "the prefix of HTTP service.");
		options.addOption(HTTPPORT, true, "http listen port.");

		CommandLine cmd = null;
		
		try{
			CommandLineParser parser = new PosixParser();
			cmd = parser.parse(options, args);			
		}catch(ParseException e){
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("WeixinRouter", options);
			System.exit(-1);
		}
		
		if(cmd.hasOption(VERSION)){
			System.out.println("WeixinRouter " + Version.getVersion());
			return;
		}else {
			String httpPort = cmd.getOptionValue(HTTPPORT, "8925");
			initLog4jFile("server.log");
			Settings s = new Settings("route.conf");
			s.putSetting(Settings.HTTP_PORT, httpPort);
			s.putSetting(Settings.REMOTE_DOMAIN, cmd.getOptionValue(HTTP_URL, "http://wx.zaol.cn"));
			new HttpServer(s).run();
		}		

		System.out.println("Stopped.");
	}	
	
	private static void initLog4jFile(String name){
		//LogFactory.getLog("main");
		org.apache.log4j.Logger root = org.apache.log4j.Logger.getRootLogger();
		try {
			root.addAppender(new org.apache.log4j.DailyRollingFileAppender(root.getAppender("S").getLayout(),
					"logs/" + name, 
					".yy-MM-dd"));
		} catch (IOException e) {
			System.out.println("Failed to add file appender.");
			// TODO Auto-generated catch block
		}
		
		root.info(Version.getName() + " " + Version.getVersion());
		root.info("build at " + Version.getBuildDate());
		root.info("java.home:" + System.getProperty("java.home"));
		root.info("java.runtime.version:" + System.getProperty("java.runtime.version"));
		root.info("java.runtime.name:" + System.getProperty("java.runtime.name"));
		
	}
	
	private static void startCleanLog(final Settings s, final String name){
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask(){
			@Override
			public void run() {
				org.apache.log4j.Logger root = org.apache.log4j.Logger.getRootLogger();
				try{
					//updateLog4jLevel(s, name);
				}catch(Throwable e){
					root.info(e.toString());
				}
			}
		}, 100, 1000 * 3600 * 12);		
	}	
}
