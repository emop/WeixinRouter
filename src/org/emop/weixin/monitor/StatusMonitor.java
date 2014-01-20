package org.emop.weixin.monitor;

import java.io.PrintWriter;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;

import org.emop.weixin.monitor.marks.HTTPRequest;
import org.emop.weixin.monitor.marks.PendingRequest;
import org.emop.weixin.monitor.marks.WeixinRequest;

public class StatusMonitor {
	public static StatusMonitor monitor = null;	
	protected ArrayBlockingQueue<Benchmark> marks = new ArrayBlockingQueue<Benchmark>(1024 * 5);
	protected Timer timer = new Timer();
	
	public WeixinRequest weixin = new WeixinRequest();
	public HTTPRequest http = new HTTPRequest();
	public PendingRequest pending = new PendingRequest();

	public Date uptime = null;

	private StatusMonitor(){
		timer.scheduleAtFixedRate(new TimerTask(){
			@Override
			public void run() {
				try{
					markAllItem();
				}catch(Throwable e){					
				}
			}
			
		}, 100, 100);
	}
	
	public static void startMonitor(){
		monitor = new StatusMonitor();
		monitor.uptime = new Date(System.currentTimeMillis());
	}
	
	public static void output(PrintWriter writer){
		if(monitor != null){
			TextStatusDumper.output(writer, monitor);
		}		
	}
	
	public static void hit(Benchmark mark){
		if(monitor != null){
			monitor.addStatus(mark);
		}
	}
	
	public void addStatus(Benchmark mark){
		if(marks.remainingCapacity() > 10){
			marks.add(mark);
		}
	}
	
	public void markAllItem(){
		if(marks.size() == 0) return;
		
		for(Benchmark m = marks.poll(); m != null; m = marks.poll()){
			if(m.type.equals(Benchmark.WX_REQUEST)){
				weixin.markRequest(m);
			}else if(m.type.equals(Benchmark.WX_REQUEST_TIMEOUT)){
				weixin.markTimeOut(m);
			}else if(m.type.equals(Benchmark.HTTP_REQUEST)){
				http.markRequest(m);
			}else if(m.type.equals(Benchmark.PEDDING_REQUEST)){
				pending.markRequest(m);
			}
		}
	}
}
