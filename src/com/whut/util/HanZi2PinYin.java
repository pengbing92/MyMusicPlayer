package com.whut.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import android.content.Context;

import com.whut.application.MyApplication;
import com.whut.music.R;

/**
 * 汉字转拼音
 * 
 * @author chenfu
 * 
 */
public class HanZi2PinYin {

	private static Map<Integer, String> pinyinMap = new HashMap<>();
	private static Map<Map<Integer, Integer>, Integer> hanziMap = new HashMap<Map<Integer, Integer>, Integer>();

	/**
	 * 如果是中文歌曲，则返回拼音 如果是英文歌曲，原样返回
	 * 
	 * @param inputStr
	 *            中文歌曲是第一个汉字，英文歌曲是首字母
	 * @return
	 */
	public static String getPinYin(String inputStr) {

		String returnStr = "";

		byte[] inputByte;
		try {
			inputByte = inputStr.getBytes("gbk");

			if (inputByte.length == 1) { // 英文歌曲
				returnStr = inputStr;
			} else { // 中文歌曲
				// IdentityHashMap，允许有相同的key，key的存储地址不同
				 Map<Integer, Integer> inputMap = new IdentityHashMap<Integer,
				 Integer>();
				 inputMap.put(Integer.parseInt(inputByte[0] + ""),
				 Integer.parseInt(inputByte[1] + ""));
				 returnStr = pinyinMap.get(hanziMap.get(inputMap));
				
			}

			if (returnStr == null || returnStr.equals("")) {
				returnStr = "error";
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		return returnStr;
	}

	/**
	 * 根据raw目录下的文本文件建立汉字与拼音的映射关系
	 */
	public static void readTxtFile(Context context) {

		try {
			InputStream inputStream = MyApplication.getContext().getResources()
					.openRawResource(R.raw.hanzi2pinyin);
			InputStreamReader inputStreamReader = new InputStreamReader(
					inputStream, "gbk");
			BufferedReader bufferedReader = new BufferedReader(
					inputStreamReader);
			String lineText = null;
			int j = 0;
			while ((lineText = bufferedReader.readLine()) != null) {
				String pinyinContent = lineText.split(":")[0].replace("\"", "")
						.trim();
				String hanziContent = lineText.split(":")[1].replace("\"", "")
						.replace(",", "").trim();
				byte[] byteArray = hanziContent.getBytes("gbk");

				// 建立映射关系
				pinyinMap.put(j, pinyinContent);
				for (int i = 0; i < byteArray.length; i += 2) {
					 Map<Integer, Integer> map = new IdentityHashMap<Integer,
					 Integer>();
					 map.put(Integer.parseInt(byteArray[i] + ""),
					 Integer.parseInt(byteArray[i + 1] + ""));
					 hanziMap.put(map, j);
				}
				j++;
			}
			bufferedReader.close();
		} catch (IOException e) {

			e.printStackTrace();
		}

	}

}
