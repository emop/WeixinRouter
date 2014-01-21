package org.emop.weixin.utils.impl;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.emop.weixin.utils.Cache;


public class SimpleCache implements Cache{
	private Map<String, CacheItem> cache = new ConcurrentHashMap<String, CacheItem>();

	private long lastCleanUp = System.currentTimeMillis();

	public void set(String key, Object obj, int expired) {
		CacheItem item = new CacheItem();
		item.ref = new SoftReference<Object>(obj);
		item.expiredTime = expired * 1000;
		cache.put(key, item);
	}

	@Override
	public Object get(String key) {
		return get(key, false);
	}

	public Object get(String key, boolean update) {
		if (System.currentTimeMillis() - lastCleanUp > 1000 * 30) {
			lastCleanUp = System.currentTimeMillis();
			cleanObject();
		}

		CacheItem item = cache.get(key);
		if (item != null) {
			if (System.currentTimeMillis() - item.lastAccess < item.expiredTime) {
				if (update) {
					item.lastAccess = System.currentTimeMillis();
				}
				return item.ref.get();
			} else {
				cache.remove(key);
			}
		}
		return null;
	}

	@Override
	public boolean remove(String key) {
		if (cache.containsKey(key)) {
			cache.remove(key);
			return true;
		}
		return false;
	}

	@Override
	public boolean add(String key, Object data, int expired) {
		if (cache.containsKey(key)) {
			return false;
		} else {
			this.set(key, data, expired);
		}
		return true;
	}

	private synchronized void cleanObject() {
		Vector<String> keys = new Vector<String>();
		keys.addAll(cache.keySet());
		for (String key : keys) {
			CacheItem item = cache.get(key);
			if (item != null
					&& System.currentTimeMillis() - item.lastAccess > item.expiredTime) {
				cache.remove(key);
			}
		}
	}

	class CacheItem {
		SoftReference<Object> ref = null;
		long lastAccess = System.currentTimeMillis();
		long expiredTime = 0;
	}

	@Override
	public List<String> keys() {
		return new ArrayList<String>(cache.keySet());
	}
}
