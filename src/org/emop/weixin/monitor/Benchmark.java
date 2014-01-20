package org.emop.weixin.monitor;

public class Benchmark {
	public static final String HTTP_REQUEST = "http_request";
	public static final String WX_REQUEST = "wx_request";
	public static final String WX_REQUEST_TIMEOUT = "wx_request_timeout";

	public static final String PEDDING_REQUEST = "wx_pedding_request";
	
	public long start = 0;
	public long end = 0;
	public long elapsed = 0;
	public String type = "";
	public Object obj = null;

	private Benchmark(String type){
		this.type = type;
	}
	
	public static Benchmark start(String type, Object obj){
		Benchmark mark = new Benchmark(type);
		mark.start = System.currentTimeMillis();
		mark.obj = obj;
		
		return mark;
	}

	public static Benchmark start(String type){
		return start(type, null);
	}
	
	
	public void attachObject(Object obj){
		this.obj = obj;
	}
	
	public void done(){
		end = System.currentTimeMillis();
		elapsed = end - start;
		StatusMonitor.hit(this);
	}
	
	public void done(long value){
		elapsed = value;
		StatusMonitor.hit(this);
	}	
	
	public Benchmark copy(){
		Benchmark m = new Benchmark(type);
		m.type = type;
		m.start = start;
		m.end = end;
		m.obj = obj;
		
		return m;
	}
}
