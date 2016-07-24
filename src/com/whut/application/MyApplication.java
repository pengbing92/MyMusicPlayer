package com.whut.application;

import android.app.Application;
import android.content.Context;

/**
 * 
 * @author chenfu
 *
 */
public class MyApplication extends Application{
	
	private static Context context;
	
	@Override
	public void onCreate() {
		context = getApplicationContext();
	}
	
	public static Context getContext() {
		return context;
	}

}
