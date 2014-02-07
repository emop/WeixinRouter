package org.emop.wx.router;

public class CPCChainRuleMatcher implements RuleMatcher {

	/**
	 * 用于CPC的路由规则检查，因为CPC的规则，大部分是和推广者有关的。 所以优先检查userId，能快速
	 * 的判断两个规则，是否匹配。
	 * 
	 type = "";
	 event = "";
	 key = "";
	 ticket = "";
	 content = "";
	 */
	@Override
	public boolean isMatch(Rule r, Rule r2, boolean isStrict) {
		//!(r.sourceUserId == r2.sourceUserId || (r.sourceUserId == 0 && !isStrict))
		if(!r.type.equalsIgnoreCase(r2.type) && (!r.type.equals("") || isStrict)) return false;
		if(!r.event.equalsIgnoreCase(r2.event) && (!r.event.equals("") || isStrict)) return false;
		if(!r.key.equalsIgnoreCase(r2.key) && (!r.key.equals("") || isStrict)) return false;
		if(!r.ticket.equalsIgnoreCase(r2.ticket) && (!r.ticket.equals("") || isStrict)) return false;
		if(!checkContent(r.content, r2.content) && (!r.content.equals("") || isStrict)) return false;
		if((isStrict && r.pollModule.equals(r2.pollModule)) || (!isStrict && r.pollModule.length() > 0)){
			return true;
		}
		
		return true;
	}
	
	private boolean checkContent(String s1, String s2){
		if(s1.startsWith("/") && s1.endsWith("/")){
			String t = s1.substring(1, s1.length() - 2);
			if(!t.startsWith("^")){
				t = ".*" + t;
			}
			if(!t.endsWith("$")){
				t = t + ".*";
			}
			return s2.matches(t);
		}
		if(s1.equalsIgnoreCase(s2)){
			return true;
		}
		return false;
	}
}
