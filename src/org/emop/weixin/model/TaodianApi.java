package org.emop.weixin.model;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.emop.http.HTTPClient;
import org.emop.http.HTTPResult;
import org.json.simple.JSONValue;

public class TaodianApi {
	private Log log = LogFactory.getLog("wx.api");

    private String API_ROUTE = "http://fmei.sinaapp.com/api/route";

	public String appKey = "";
	public String appSecret = "";
	private DateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
	
	private HTTPClient http = HTTPClient.create();
	
	public TaodianApi(String appKey, String secret, String apiRoute){
		this.appKey = appKey;
		this.appSecret = secret;
		if(apiRoute != null && apiRoute.length() > 0){
			API_ROUTE = apiRoute;
		}
	}
	
	public Object call(String name, Map<String, Object> param){
		Map<String, Object> p = this.newDefaultParam();
		p.put("name", name);
		if(param == null){
			param = new HashMap<String, Object>();
		}
		
		if(param.containsKey("no_cache")){
			if(param.get("no_cache") != null){
				p.put("no_cache", param.get("no_cache"));
			}
			param.remove("no_cache");
		}
		
		p.put("params", JSONValue.toJSONString(param));
		
		HTTPResult r = http.post(API_ROUTE, p);
	 	if(r.getString("status").equals("ok")){
			   r.isOK = true;
		}else {
			r.errorMsg = r.getString("msg");
			r.errorCode = r.getString("code");
		}
	 	
		if(!r.isOK){
			log.warn("Taobian API error:" + r.errorCode + ",msg:" + r.errorMsg());
		}
		return r;		
	}
	
	protected Map<String, Object> newDefaultParam(){
		Map<String, Object> p = new HashMap<String, Object>();
		String stamp = this.timestamp();
		p.put("app_id", this.appKey);
		p.put("time", stamp);
		
		String sign = this.appKey + "," + stamp + "," + this.appSecret;
		sign = MD5(sign);
		p.put("sign", sign);
		
		return p;
	}
	
    public static String MD5(String str)  
    {  
        MessageDigest md5 = null;  
        try  
        {
            md5 = MessageDigest.getInstance("MD5"); 
        }catch(Exception e)  
        {  
            e.printStackTrace();  
            return "";  
        }  
          
        byte[] byteArray = null;
		try {
			byteArray = str.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
		}  
        
        byte[] md5Bytes = md5.digest(byteArray);  
          
        StringBuffer hexValue = new StringBuffer();  
        for( int i = 0; i < md5Bytes.length; i++)  
        {  
            int val = ((int)md5Bytes[i])&0xff;  
            if(val < 16)  
            {  
                hexValue.append("0");  
            }  
            hexValue.append(Integer.toHexString(val));  
        }  
        return hexValue.toString();  
    } 
    
    public static String SHA1(String str)  
    {  
        MessageDigest md5 = null;  
        try  
        {
            md5 = MessageDigest.getInstance("SHA1"); 
        }catch(Exception e)  
        {  
            e.printStackTrace();  
            return "";  
        }  
          
        
        byte[] byteArray = null;
		try {
			byteArray = str.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
		}  
		
        byte[] md5Bytes = md5.digest(byteArray);  
          
        StringBuffer hexValue = new StringBuffer();  
        for( int i = 0; i < md5Bytes.length; i++)  
        {  
            int val = ((int)md5Bytes[i])&0xff;  
            if(val < 16)  
            {  
                hexValue.append("0");  
            }  
            hexValue.append(Integer.toHexString(val));  
        }  
        return hexValue.toString();  
    }     
	
	private String timestamp(){
		Date date = new Date(System.currentTimeMillis());
		String time = sdf.format(date);
		return time;
	}	
}
