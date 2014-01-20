package org.emop.weixin.message;

import java.io.PrintWriter;

public class ImageLinkItem {
	public String Title;
	public String Description;
	public String PicUrl;
	public String Url;
	
	public void writeTo(PrintWriter writer){
		writer.println("<item>");
		writer.println("<Title><![CDATA[" + Title + "]]></Title>");
		writer.println("<Description><![CDATA[" + Description + "]]></Description>");
		writer.println("<PicUrl><![CDATA[" + PicUrl + "]]></PicUrl>");
		writer.println("<Url><![CDATA[" + Url + "]]></Url>");
		writer.println("</item>");		
	}
}
