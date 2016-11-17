package com.whut.util;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.whut.entiy.OnLineLrc;

/**
 * 
 * 新版的下载歌词已不再使用此类
 * 
 * 解析网络返回的json数据
 * @author chenfu
 *
 */
public class JsonUtil {
	
	// 获取在线歌词列表
	public static List<OnLineLrc> getonLineLrcList(String httpResponse) {
	
		// 保存返回结果中的Json数组的内容
		List<OnLineLrc> onLineLrcList = new ArrayList<OnLineLrc>();
		
		try {
			JSONObject jsonObject = new JSONObject(httpResponse);
			
			if (jsonObject.getInt("code") == 0) {
				
				int count = jsonObject.getInt("count");
				Log.i("搜索到的歌词有", count + "条");
				
				JSONArray jsonArray = jsonObject.getJSONArray("result");
				for (int i=0;i<jsonArray.length();i++) {
					
					JSONObject jsonItem = jsonArray.getJSONObject(i);
					
					OnLineLrc onLineLrc = new OnLineLrc();
					onLineLrc.setAid(jsonItem.getInt("aid"));
					onLineLrc.setDownloadUrl(jsonItem.getString("lrc"));
					onLineLrc.setArtist_id(jsonItem.getInt("artist_id"));
					onLineLrc.setSid(jsonItem.getInt("sid"));
					onLineLrc.setSongName(jsonItem.getString("song"));
					
					onLineLrcList.add(onLineLrc);
					
				}
				
			} else {
				Log.i("failed", "没有搜索到相关歌词");
			}
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return onLineLrcList;
		
	}

}
