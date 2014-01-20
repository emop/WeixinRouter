package org.emop.weixin.monitor.marks;

import org.emop.weixin.monitor.Benchmark;
import org.emop.weixin.monitor.StatusMark;


public class PendingRequest extends StatusMark{	
	public int timeoutCount = 0;
	
	public void markTimeOut(Benchmark mark){
		timeoutCount++;
		slowList.add(mark);
	}
}
