package org.emop.wx.router;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.emop.weixin.message.WeixinMessage;


/**
 * 一个简单的路由表，
 * 
 * 配置命令：
 * route -[AD] chain matchcondition -j action [-next target] [-expire time]
 * 
 * chain => input|cpc
 * 
 * matchcondition => -[type|event|key|ticket|content]
 * action => forward|reject|ignore|redirect
 * target => URL
 * 
 * 例子：
 * #推广者74 的所有连接不跳转， 4小时后规则自动失效。
 * route -A cpc -user_id 74 -j reject -expire 4h
 * 
 * #店铺123456 暂停30分钟推广。
 * route -A cpc -shop_id 123456 -j redirect -next no_money -expire 30min
 * 
 * @author deonwu
 *
 */
public class DefaultRouter implements Router {
	/**
	 * 处理通用的路由规则，例如根据来源IP过滤等。
	 */
	protected RouteChain input = null;
	
	/**
	 * CPC 专用过滤规则。
	 */
	protected RouteChain cpcForward = null;
	
	private Log log = LogFactory.getLog("click.route");    	

	@Override
	public TargetURL route(WeixinMessage model) {
		Rule rule = new Rule();
		TargetURL next = new TargetURL();
		
		rule.type= model.msgType;
		rule.event = model.data.containsKey("Event") ?  model.data.get("Event") : "";
		rule.key = model.data.containsKey("EventKey") ?  model.data.get("EventKey") : "";
		rule.ticket = model.data.containsKey("Ticket") ?  model.data.get("Ticket") : "";
		rule.content = model.data.containsKey("Content") ?  model.data.get("Content") : "";
		
		Rule matched = input.match(rule);
		
		if(matched != null){
			if(log.isDebugEnabled()){
				log.debug("match rule:" + this.convertRuleToLine("na", matched));
			}
			next.actionName = matched.targetAction;
			if(matched.appURL != null && matched.appURL.trim().length() > 1){
				next.isOK = true;
				next.url = matched.appURL;
				next.token = matched.appToken;
				next.type = matched.appType;
			}
		}else {
			if(log.isDebugEnabled()){
				log.debug("not match:" + this.convertRuleToLine("na", rule));
			}
		}
		
		return next;
	}

	@Override
	public boolean updateRouteTable(String expr) throws RouteException{
		CLI c = this.parseCommandLine(expr);
		if(c.error != null){
			throw new RouteException(c.error);
		}
		
		if(c.action.equals("add")){
			if(c.chainName.equals("cpc")){
				return cpcForward.addRoute(c.rule);
			}else {
				return input.addRoute(c.rule);
			}
		}else {
			if(c.chainName.equals("cpc")){
				return cpcForward.delRoute(c.rule);
			}else {
				return input.delRoute(c.rule);
			}			
		}
	}


	@Override
	public void save(String path) throws IOException{
		// TODO Auto-generated method stub

	}

	@Override
	public void load(String path) throws RouteException{

		File f = new File(path);
		
		log.info("load route:" + path);
		if(f.isFile()){
			try {
				InputStream ins = new FileInputStream(path);
				BufferedReader reader = new BufferedReader(new InputStreamReader(ins, "utf8"));
				for(String l = reader.readLine(); l != null; l = reader.readLine()){
					if(l.trim().startsWith("#") || l.trim().length() == 0) continue;
					boolean r = updateRouteTable(l.trim());
					log.info("route:" + l + ", result:" + r);
				}
				reader.close();
			} catch (Exception e) {
				log.error(e, e);
			}
		}else {
			log.warn("not route config:" + path);
		}

	}

	@Override
	public void dump(PrintWriter writer) {
		
		writer.println();
		for(Rule r : input.rules()){
			writer.println(convertRuleToLine("input", r));
		}

		writer.println();
		for(Rule r : cpcForward.rules()){
			writer.println(convertRuleToLine("cpc", r));
		}
	}
	
	protected String convertRuleToLine(String chain, Rule r){		
		String c = "route -A " + chain;
		
		//type|event|key|ticket|content
		
		if(r.type.length() > 0){
			c += " -type " + r.type;
		}
		if(r.event.length() > 0){
			c += " -event " + r.event;
		}
		if(r.key.length() > 0){
			c += " -key " + r.key;
		}
		if(r.ticket.length() > 0){
			c += " -ticket " + r.ticket;
		}
		if(r.content.length() > 0){
			c += " -content " + r.content;
		}
		
		c += " -j " + r.targetAction;

		if(r.appType.length() > 0){
			c += " -app_type " + r.appType;
		}
		
		if(r.appURL.length() > 0){
			c += " -app_url " + r.appURL;
		}
		
		if(r.appToken.length() > 0){
			c += " -app_token " + r.appToken;
		}
		
		if(r.expired > 0){
			long t = r.expired - System.currentTimeMillis();
			c += " -expire " + (t / 1000) + "secs";			
		}
		
		return c;
	}

	@Override
	public void initRoute() throws RouteException{
		RuleMatcher m = new CPCChainRuleMatcher();
		
		/**
		 * @todo 通用的Chain和CPC Chain目前使用相同的配置。 因为通用的还没有特别的需求。
		 */
		input = new DefaultRouteChain(m);
		cpcForward = new DefaultRouteChain(m);
		
		//initCommandOption();
	}
	
	protected CLI parseCommandLine(String line) throws RouteException{
		CLI c = new CLI();
		String[] args = line.split(" ");
		CommandLine cmd = null;
		try{
			CommandLineParser parser = new PosixParser();
			Options options = createCommandOption();
			cmd = parser.parse(options, args);
			//cmd.h
			if(cmd.hasOption('A')){
				c.chainName = cmd.getOptionValue("add");
				c.action = "add";
			}else if(cmd.hasOption('D')){
				c.action = "del";
				c.chainName = cmd.getOptionValue("del");				
			}
			
			//type|event|key|ticket|content
			Rule r = new Rule();
			if(cmd.hasOption("type")){
				r.type = cmd.getOptionValue("type");
			}
			if(cmd.hasOption("event")){
				r.event = cmd.getOptionValue("event");
			}
			if(cmd.hasOption("key")){
				r.key = cmd.getOptionValue("key");
			}		
			if(cmd.hasOption("ticket")){
				r.ticket = cmd.getOptionValue("ticket");
			}		
			if(cmd.hasOption("jump")){
				r.targetAction = cmd.getOptionValue("jump");
			}

			if(cmd.hasOption("app_type")){
				r.appType = cmd.getOptionValue("app_type");
			}
			
			if(cmd.hasOption("app_url")){
				r.appURL = cmd.getOptionValue("app_url");
			}
			
			if(cmd.hasOption("app_token")){
				r.appToken = cmd.getOptionValue("app_token");
			}
			
			if(cmd.hasOption("expire")){
				r.expired = parseTime(cmd.getOptionValue("expire"));
			}			
			c.rule = r;
		}catch(ParseException e){
			c.error = e.getMessage();
			log.error("parse route command error, cmd:" + line, e);
		}
		
		return c;
	}
	
	private long parseTime(String t) throws RouteException{
		Pattern time = Pattern.compile("([0-9]+)(min|day|hour|sec)s?");
		Matcher m = time.matcher(t);		
		if(m != null && m.find()){
			int c = Integer.parseInt(m.group(1));
			String unit = m.group(2);
			long base = 1000;
			if(unit.equals("min")){
				base = base * 60;
			}else if(unit.equals("hour")){
				base = base * 60 * 60;
			}else if(unit.equals("day")){
				base = base * 60 * 60 * 24;
			}
			return System.currentTimeMillis() + base * c;
		}else {
			throw new RouteException("Error time:" + t);
		}		
	}
	
	
	private Options createCommandOption(){
		Option o = null;
		Options options = new Options();
		//cmdOptions = options;
		
		//o.setRequired(true);
		OptionGroup g = new OptionGroup();
		g.setRequired(true);

		o = new Option("A", "add", true, "add a rule in the route chain");
		g.addOption(o);
		
		o = new Option("D", "del", true, "delete a rule in the route chain");
		g.addOption(o);		
		options.addOptionGroup(g);

		/**
		 * 规则匹配命令行。
		 */		
		//g = new OptionGroup();
		//g.setRequired(true);
		//type|event|key|ticket|content
		
		o = new Option("t", "type", true, "match message type");
		options.addOption(o);
		
		o = new Option("e", "event", true, "match event name");
		options.addOption(o);		

		o = new Option("k", "key", true, "match event key");
		options.addOption(o);		

		o = new Option("i", "ticket", true, "match ticket");
		options.addOption(o);

		o = new Option("c", "content", true, "match content");
		options.addOption(o);
				
		/**
		 * 跳转和 过期命令参数。
		 */
		o = new Option("j", "jump", true, "the next action");
		o.setRequired(true);
		options.addOption(o);
		
		o = new Option("a", "app_url", true, "the next app url");
		options.addOption(o);
		
		o = new Option("N", "app_token", true, "the next app token");
		options.addOption(o);

		o = new Option("P", "app_type", true, "the app type(json|xml)");
		options.addOption(o);
		
		o = new Option("T", "expire", true, "the rule expire time.");
		options.addOption(o);
		
		return options;
	}
	
	public static class CLI{
		public String chainName = null;
		public String action = null;
		public Rule rule = null;
		public String error = null;
	}

}
