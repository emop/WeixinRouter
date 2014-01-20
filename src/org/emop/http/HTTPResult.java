package org.emop.http;

import org.json.simple.JSONObject;



public class HTTPResult {
	public static final String ERR_JSON_PARSE = "err_json_parse";
	public static final String ERR_NETWORKING_UNKOWN = "err_networking_unkown";

	public String text = null;	
	public JSONObject json = null;
	public boolean isOK = false;
	public String errorMsg = "";
	public String errorCode = "";
	

	
	public String getString(String key){
		String str = null;
		if(json != null){
			JSONObject v = json;
			Object o = null;
			for(String k: key.split("\\.")){
				if(!v.containsKey(k)) {
					v = null;
					break;
				}
				o = v.get(k);
				if(o instanceof JSONObject){
					v = (JSONObject)o;
				}else{
					str = o.toString();
				}
			}
		}
		return str;
	}
	
	public JSONObject getJSONObject(String key){
		JSONObject data = null;
		if(json != null){
			JSONObject v = json;
			Object o = null;
			for(String k: key.split("\\.")){
				if(!v.containsKey(k)) {
					v = null;
					break;
				}
				o = v.get(k);
				if(o instanceof JSONObject){
					v = (JSONObject)o;
					data = v;
				}
			}
		}
		return data;
	}	
	
	public String errorMsg(){		
		if(this.errorMsg != null && this.errorMsg.trim().length() > 0){
			return this.errorMsg.trim();
		}else{
			return this.errorCode;			
		}
	}
	
	//private String getString(String key, )
}
