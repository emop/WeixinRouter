package org.emop.wx.router;

/**
 * 短网址下一跳地址。
 * 
 * @author deonwu
 *
 */
public class TargetURL {
	//public static final String IGNORE = "ignore";
	
	public String url = null;
	public String token = null;
	public String retCmd = null;
	public String type = null;
	public boolean isOK = false;
	
	public String module = null;
	//public boolean writeLog = true;
	
	public String actionName = Action.FORWARD;
	
	public boolean isLast = false;
}
