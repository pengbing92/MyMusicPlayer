package com.whut.util;

import android.content.Context;
import android.widget.Toast;


/**
 * 弹出通知信息
 * @author chenfu
 *
 */
public class ToastUtil {

	public static void toastInfo(Context context, String info) {
		
		Toast.makeText(context, info, Toast.LENGTH_SHORT).show();
		
	}
}
