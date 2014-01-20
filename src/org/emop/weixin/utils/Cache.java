package org.emop.weixin.utils;

import java.util.List;

public interface Cache {
	public void set(String key, Object data, int expired);
	public Object get(String key);
    public Object get(String key, boolean update);

	public boolean remove(String key);
	
	public boolean add(String key, Object data, int expired);
	
	public List<String> keys();
}
