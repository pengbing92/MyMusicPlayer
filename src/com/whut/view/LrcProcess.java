package com.whut.view;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import android.os.Environment;
import android.util.Log;

import com.whut.entiy.LrcContent;
import com.whut.entiy.OnLineLrc;
import com.whut.httpclient.MyClient;
import com.whut.util.JsonUtil;

/**
 * 2013/6/1
 * 
 * @author wwj 处理歌词的类
 */
public class LrcProcess {
	private List<LrcContent> lrcList; // List集合存放歌词内容对象
	private LrcContent mLrcContent; // 声明一个歌词内容对象

	private List<LrcContent> repeatLrcList; // 重复出现的歌词

	private String songName = ""; // 要检索歌词的歌曲名
	private boolean DOWNLOAD = false; // 下载是否完成
	private static boolean NOT_FOUND = false; // 是否检索到歌词
	
	private String rootPath = Environment.getExternalStorageDirectory()
			.getPath() + "/PengBing_lrcs";

	private File prcFile; // 歌词文件
	
	private MyClient myClient; // httpClient
	
	
	public static void setNOT_FOUND(boolean isFound) {
		NOT_FOUND = isFound;
	}

	public static boolean isNOT_FOUND() {
		return NOT_FOUND;
	}

	/**
	 * 无参构造函数用来实例化对象
	 */
	public LrcProcess() {
		mLrcContent = new LrcContent();
		lrcList = new ArrayList<LrcContent>();
		repeatLrcList = new ArrayList<LrcContent>();
	}

	/**
	 * 读取歌词
	 * 
	 * @param path
	 * @return
	 */
	public void readLRC(String song) {

		this.songName = song;

		// 去掉歌曲名中的()
		String nameArray[] = songName.split("\\(");

		songName = nameArray[0].trim();

		// File prcFile = new File(path.replace(".mp3", ".lrc"));
		prcFile = new File(rootPath, songName + ".prc");
		Log.i("lrc_path", prcFile.getPath());

		if (!prcFile.exists()) {
			myClient = MyClient.getInstance();
			// 本地不存在，则开启线程从网络上下载歌词
			new Thread(new GetLrcFromInternet()).start();
		} else {
			// 本地存在歌词文件，直接读取歌词文件
			readPrc();
			DOWNLOAD = true;
			NOT_FOUND = false;
		}

	}

	/**
	 * 读取歌词文件内容，prc是自己指定的歌词文件后缀名
	 */
	public void readPrc() {
		try {
			// 创建一个文件输入流对象
			FileInputStream fis = new FileInputStream(prcFile);
			InputStreamReader isr = new InputStreamReader(fis, "utf-8");
			BufferedReader br = new BufferedReader(isr);
			String s = "";
			while ((s = br.readLine()) != null) {
				// 替换字符
				s = s.replace("[", "");
				s = s.replace("]", "@");

				// 分离“@”字符
				String splitLrcData[] = s.split("@");

				int arrayLength = splitLrcData.length;

				// 只读取了包含时间戳和歌词内容的行
				if (arrayLength > 1) {

					String contentStr = splitLrcData[arrayLength - 1];

					// 读取含多个时间戳的行
					for (int i = 0; i < arrayLength - 1; i++) {

						mLrcContent.setLrcStr(contentStr);

						// 处理歌词取得歌曲的时间
						int lrcTime = time2Str(splitLrcData[i]);
						mLrcContent.setLrcTime(lrcTime);

						if (arrayLength > 2) {
							// 至少有两个时间戳
							if (i == arrayLength - 2) {
								// 将最后一个时间戳先添加进列表
								lrcList.add(mLrcContent);
							} else {
								// 将其他时间戳添加进重复出现的歌词列表
								repeatLrcList.add(mLrcContent);
							}
						} else {
							// 只有一个时间戳，直接添加进列表
							lrcList.add(mLrcContent);
						}

						// 新创建歌词内容对象
						mLrcContent = new LrcContent();
					}

				}
			}
			// 释放资源
			br.close();
			isr.close();
			fis.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * 解析歌词时间
	 */
	public int time2Str(String timeStr) {
		timeStr = timeStr.replace(":", ".");
		timeStr = timeStr.replace(".", "@");

		String timeData[] = timeStr.split("@"); // 将时间分隔成字符串数组

		// 分离出分、秒并转换为整型
		int minute = Integer.parseInt(timeData[0]);
		int second = Integer.parseInt(timeData[1]);
		int millisecond = 0;
		if (timeData.length > 2) {
			millisecond = Integer.parseInt(timeData[2]);
		}

		// 计算上一行与下一行的时间转换为毫秒数
		int currentTime = (minute * 60 + second) * 1000 + millisecond * 10;
		return currentTime;
	}

	public List<LrcContent> getLrcList() {

		List<LrcContent> myLrcList = new ArrayList<LrcContent>();

		// 整合 lrcList 与 repeatLrcList
		if (repeatLrcList.size() > 0) {
			// 有重复歌词

			int j = 0;
			int repeatTime = 0;
			int preTime = 0;
			int nextTime = 0;

			while (j < lrcList.size() - 1) {

				myLrcList.add(lrcList.get(j));
				preTime = lrcList.get(j).getLrcTime();
				nextTime = lrcList.get(j + 1).getLrcTime();

				for (int i = 0; i < repeatLrcList.size(); i++) {

					repeatTime = repeatLrcList.get(i).getLrcTime();

					if (repeatTime > preTime && repeatTime < nextTime) {
						myLrcList.add(repeatLrcList.get(i));
						repeatLrcList.remove(i);
					}

				}
				j++;
			}

			myLrcList.add(lrcList.get(j));

		} else {
			// 无重复歌词
			myLrcList = lrcList;
		}

		return myLrcList;
	}

	/**
	 * 下载歌词
	 * 
	 * @author chenfu
	 * 
	 */
	public class GetLrcFromInternet implements Runnable {

		@Override
		public void run() {

			String url = "http://geci.me/api/lyric/" + songName;

			String response = myClient.doGet(url);

			if (response == null) {
				// 网络无响应
				Log.i("response", "网络请求无响应");
			} else {
				// 网络请求成功
				if (JsonUtil.getonLineLrcList(response).size() > 0) {
					/**
					 * 测试，下载返回结果中的第一个条记录， 后续可以加入切换歌词的功能
					 */
					OnLineLrc onLineLrc = new OnLineLrc();
					onLineLrc = JsonUtil.getonLineLrcList(response).get(0);
					String lrcUrl = onLineLrc.getDownloadUrl(); // 下载歌词的链接

					DOWNLOAD = myClient.createPrcFile(rootPath, lrcUrl, songName);
				} else {
					Log.i("search", "没有搜索结果");
				}

				if (DOWNLOAD) {
					// 歌词下载完成，读取歌词文件
					readPrc();
					NOT_FOUND = false;
				} else {
					// 未完成下载，未检索到相关歌词
					DOWNLOAD = false;
					NOT_FOUND = true;
					Log.i("search_lrc", "未检索到歌词...");
				}
			}

		}
	}
}
