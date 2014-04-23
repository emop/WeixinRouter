package org.emop.weixin.router.servlet;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.emop.http.HTTPResult;
import org.emop.weixin.model.TaodianApi;
import org.emop.weixin.router.WeixinRouter;
import org.emop.weixin.utils.Cache;
import org.emop.weixin.utils.impl.SimpleCache;

public class ImageProxyServlet extends HttpServlet{
	private Log log = LogFactory.getLog("wx.image");

	private static Cache cache = new SimpleCache();
	
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
    	doPost(request, response);
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		
		DataInfo info = getDataInfo(request);
		
		response.setContentType("image/jpg");
		response.addHeader("Cache-Control", "max-age=7200");
		response.addDateHeader("Expires", System.currentTimeMillis() + 7200 * 1000 + 8 * 60 * 60 * 1000);
		
		byte[] data = getImageData(info.url, info.intWidth);
		if(data != null && data.length > 100){
			response.getOutputStream().write(data);
		}else {
			response.setContentType("text/plain");
			response.getWriter().println("图片链接过期。");
		}
    }
    
    protected byte[] getImageData(String url, int width){
    	log.debug("get image:" + url + ", width:" + width);
    	String ck = TaodianApi.MD5(url + width);
    	byte[] data = null;
    	Object tmp = cache.get(ck, true);
    	if(tmp == null && url != null && url.startsWith("http://")){
    		try {
    			BufferedImage image = ImageIO.read(new URL(url));
				int oldWidth = image.getWidth(null);  
				int oldHeight = image.getHeight(null);
				
				double rate = width * 1.0 / oldWidth;
				int newHeight = (int) (oldHeight * rate);
				
				log.debug("old width:" + oldWidth + ",old height:" + oldHeight + ", new height:" + newHeight);
				
				Image tmpImage = image.getScaledInstance(width, newHeight, BufferedImage.SCALE_SMOOTH);
				BufferedImage newImage = new BufferedImage(width, newHeight, BufferedImage.TYPE_3BYTE_BGR);
				
				newImage.getGraphics().drawImage(tmpImage, 0, 0, null);
				
				int cropHeight = 0;
				if(width > 300){
					cropHeight = width / 2;
				}else {
					cropHeight = width;
				}
				cropHeight = cropHeight > newHeight ? newHeight : cropHeight;
				
				int startHeight = (newHeight - cropHeight) / 2;
								
				newImage = newImage.getSubimage(0, startHeight, width, cropHeight);	
				
				newImage.flush();
				ByteArrayOutputStream out = new ByteArrayOutputStream();				
				ImageIO.write(newImage, "JPEG", out);
				out.close();				
				data = out.toByteArray();
				
				cache.set(ck, data, 60 * 10);
			} catch (IOException e) {
				log.error(e.toString(), e);
			}
    		
    	}else if(tmp != null){
    		log.info("hit in cache:" + url + ", ck:" + ck);
    		data = (byte[])tmp;
    	}else {
    		data = new byte[]{};
    	}
    	
    	return data;
    }
    
	private DataInfo getDataInfo(HttpServletRequest request){
		
		DataInfo info = new DataInfo();
		
		info.url = request.getParameter("img");
        //S request.getPathInfo()
		info.width = request.getParameter("width");

		if(info.url == null){
            Pattern pa = Pattern.compile("(v|img)/([^\\.]+)");
            Matcher ma = pa.matcher(request.getRequestURI());
            if(ma.find()){
            	info.url = ma.group(2);
            }else {
            	throw new Error("Not found path from:" + request.getRequestURI());
            }
            String tmp[] = info.url.split("_");
            if(tmp.length == 2){
            	info.url = tmp[0];
            	info.width = tmp[1];
            }else {
            	info.url = tmp[0];       	
            	info.width = "80";
            }
            String shortKey = info.url;
            info.url = WeixinRouter.router.imageShortUrl.get(shortKey) + "";
            log.info("Convert short url to:" + shortKey + ", long url:" + info.url);
		}

		int iWidth = 0;
		if(info.width != null && info.width.length() > 0){
			try{
				iWidth = Integer.parseInt(info.width);
			}catch(Exception e){				
			}
		}
		info.intWidth = iWidth > 60 ? iWidth : 80;
		
		return info;
    }
	
	class DataInfo{
		public String url = null;
		public String width = "";

		public int intWidth = 80;
	}
    
}
