package com.whut.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

public class NetworkCheck {
	/**   
     * 网络是否可用   
     */   
    public static boolean isNetworkAvailable(Context context) {     
        ConnectivityManager connectivity = (ConnectivityManager) context     
                    .getSystemService(Context.CONNECTIVITY_SERVICE);     
        if (connectivity == null) {     
        } else {
        	//如果仅仅是用来判断网络连接?
        	//则可以使用 connectivity.getActiveNetworkInfo().isAvailable();
            NetworkInfo[] info = connectivity.getAllNetworkInfo();     
            if (info != null) {     
                for (int i = 0; i < info.length; i++) {     
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {     
                        return true;     
                    }     
                }     
            }     
        }     
        return false;     
    }     
      
    /**   
     * wifi是否打开   
     */     
    public static boolean isWifiEnabled(Context context) {     
        ConnectivityManager mgrConn = (ConnectivityManager) context     
                    .getSystemService(Context.CONNECTIVITY_SERVICE);     
        TelephonyManager mgrTel = (TelephonyManager) context     
                    .getSystemService(Context.TELEPHONY_SERVICE);
        // 返回值相当于false
        return ((mgrConn.getActiveNetworkInfo() != null && mgrConn     
                    .getActiveNetworkInfo().getState() == NetworkInfo.State.CONNECTED) || mgrTel     
                    .getNetworkType() == TelephonyManager.NETWORK_TYPE_UMTS);  
    }  
      
     /**   
     * 判断当前网络是否是wifi网络   
     * if(activeNetInfo.getType()==ConnectivityManager.TYPE_MOBILE) { //判断3G?   
     *    
     * @param context   
     * @return boolean   
     */     
    public static boolean isWifi(Context context) {     
        ConnectivityManager connectivityManager = (ConnectivityManager) context     
                .getSystemService(Context.CONNECTIVITY_SERVICE);     
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();     
        if (activeNetInfo != null     
                && activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {     
            return true;     
        }     
        return false;     
    }     
      
    /**   
     * 判断当前网络是否是3G网络   
     *    
     * @param context   
     * @return boolean   
     */     
    public static boolean is3G(Context context) {     
        ConnectivityManager connectivityManager = (ConnectivityManager) context     
                .getSystemService(Context.CONNECTIVITY_SERVICE);     
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();     
        if (activeNetInfo != null     
                && activeNetInfo.getType() == ConnectivityManager.TYPE_MOBILE) {     
            return true;     
        }     
        return false;     
    }     
  
}
