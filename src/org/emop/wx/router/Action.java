package org.emop.wx.router;

import java.util.HashMap;
import java.util.Map;

public class Action {
	/**
	 * 只是把当前消息转发到某个应用。
	 */
	public static final String FORWARD = "forward";
	
	/**
	 * 进入某个应用，后续的操作都直接转发到这个应用上去。
	 */
	public static final String ENTER = "enter";

	public static final String LOG = "log";
	
	/**
	 * 退出当前应用。
	 */
	public static final String EXITS = "exit";
	

	public String name = "";
	public String url = "";
	public boolean isLast = false;
	
	private static Map<String, Action> s = new HashMap<String, Action>();
	static{
		
	}
	
	public static Action get(String n){		
		return s.get(n);
	}
}
