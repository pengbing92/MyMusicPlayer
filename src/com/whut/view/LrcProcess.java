package com.whut.view;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import android.os.Environment;
import android.util.Log;

import com.whut.entiy.LrcContent;
import com.whut.httpclient.MyClient;

/**
 * 
 * @author chenfu
 * 
 */
public class LrcProcess {
	private List<LrcContent> lrcList; // List集合存放歌词内容对象
	private LrcContent mLrcContent; // 声明一个歌词内容对象

	private List<LrcContent> repeatLrcList; // 重复出现的歌词

	private String songName = ""; // 要检索歌词的歌曲名
	private String singer = ""; // 要检索歌词的歌手
	private boolean DOWNLOAD = false; // 下载是否完成
	private static boolean NOT_FOUND = true; // 是否检索到歌词

	private String rootPath = Environment.getExternalStorageDirectory()
			.getPath() + "/MyMusicPlayer/PengBing_lrcs";

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
	 * 读歌词
	 * 
	 * @param song
	 * @param singer
	 */
	public void readLRC(String song, String singer) {

		this.songName = song;
		this.singer = singer;

		// 去掉歌曲名中的()
		String nameArray[] = songName.split("\\(");

		songName = nameArray[0].trim();

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
				s = s.replace("]", "%");

				// 分离“%”字符
				String splitLrcData[] = s.split("%");

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

		int minute = 0;
		int second = 0;
		int millisecond = 0;

		// 分离出分、秒并转换为整型
		// minute = Integer.parseInt(timeData[0]);
		// second = Integer.parseInt(timeData[1]);
		// if (timeData.length > 2) {
		// millisecond = Integer.parseInt(timeData[2]);
		// }
		try {
			// 分离出分、秒并转换为整型
			minute = Integer.parseInt(timeData[0]);
			second = Integer.parseInt(timeData[1]);
			if (timeData.length > 2) {
				millisecond = Integer.parseInt(timeData[2]);
			}
		} catch (NumberFormatException e) {
			/**
			 * 少数情况下，乱码导致不能正常解析时间，从而导致程序崩溃 所以要捕捉异常
			 */
			e.printStackTrace();
			NOT_FOUND = true;
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

			// String url = "http://geci.me/api/lyric/" + songName;
			try {
				String url = "http://www.cnlyric.com/search.php?k="
						+ URLEncoder.encode(singer + " " + songName, "gb2312")
						+ "&t=s";

				String response = myClient.doGet(url);

				if (response == null) {
					// 网络无响应
					Log.i("response", "网络请求无响应");
				} else {

					if (response.contains("geci/")) {
						String str = response.split("geci/")[1];
						String numStr = str.split(".html")[0];

						String downloadUrl = "http://www.cnlyric.com/LrcDown/"
								+ numStr + ".lrc";

						DOWNLOAD = myClient.createPrcFile(rootPath, downloadUrl,
								songName);

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
					} else { // 15秒内只能下载一次
						// 未完成下载，未检索到相关歌词
						DOWNLOAD = false;
						NOT_FOUND = true;
						Log.i("错误信息", "未检索到歌词...");
					}
					
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

		}
	}
}
