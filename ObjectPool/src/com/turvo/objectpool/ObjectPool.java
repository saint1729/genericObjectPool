package com.turvo.objectpool;

public interface ObjectPool<T> {
	
	public void releaseObject(T object);
	
}