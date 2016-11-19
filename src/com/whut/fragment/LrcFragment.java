package com.whut.fragment;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.whut.database.entiy.Song;
import com.whut.database.service.imp.SongServiceDao;
import com.whut.entiy.LrcContent;
import com.whut.music.LrcActivity;
import com.whut.music.R;
import com.whut.view.LrcProcess;
import com.whut.view.LrcView;

public class LrcFragment extends Fragment {

	private static LrcView lrcView;
	private static LrcProcess mLrcProcess;
	private static List<LrcContent> lrcList = new ArrayList<LrcContent>();
	private static int index = 0; // 歌词内容列表下标

	private SongServiceDao songServiceDao;
	private Song lastSong; // 上一次播放的歌曲
	private Song currentSong;

	private boolean firstShow = true; // 第一次显示

	private Context context;
	private MyBroadcastReceiver myreceiver;

	@SuppressLint("HandlerLeak")
	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case 0:
				// 重新加载歌词
				initLrcOfSong();
				break;

			default:
				break;
			}
		};
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		currentSong = new Song();
		lastSong = new Song();

		context = getActivity();
		songServiceDao = new SongServiceDao(context);
		// 初始化广播接收器
		initBroadcastReceiver();

		initData();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_lrc, container, false);

		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		lrcView = (LrcView) getActivity().findViewById(R.id.lrcShowView);

		// 加载歌词
		initLrcOfSong();
	}

	// 注册广播接收器
	public void initBroadcastReceiver() {

		myreceiver = new MyBroadcastReceiver();

		IntentFilter ssFilter = new IntentFilter();
		ssFilter.addAction("switchSong");
		context.registerReceiver(myreceiver, ssFilter);

	}

	private void initData() {
		currentSong = songServiceDao.getCurrentSong();
		lastSong = currentSong;
	}

	public void initLrcOfSong() {
		
		mLrcProcess = new LrcProcess();
		mLrcProcess.readLRC(currentSong.getSongName().trim(), currentSong
				.getSinger().trim());
		lrcList = mLrcProcess.getLrcList();
	
		if (firstShow) {
			showLrc();
		} else {
			if (lastSong.getId() != currentSong.getId()) {
				showLrc();
				lastSong = currentSong;
			}
		}

		firstShow = false;

	}

	private void showLrc() {
		
		lrcView.setText("歌词加载中..");
		lrcView.setmLrcList(lrcList);
		handler.post(mRunnable);

	}

	Runnable mRunnable = new Runnable() {

		@Override
		public void run() {
			lrcView.setIndex(lrcIndex(LrcActivity.currentPosition));
			lrcView.invalidate();
			handler.postDelayed(mRunnable, 100);

		}
	};

	public int lrcIndex(int currentPosition) {

		if (currentPosition < currentSong.getDuration()) {
			for (int i = 0; i < lrcList.size(); i++) {
				if (i < lrcList.size() - 1) {
					if (currentPosition < lrcList.get(i).getLrcTime() && i == 0) {
						index = i;
					}
					if (currentPosition > lrcList.get(i).getLrcTime()
							&& currentPosition < lrcList.get(i + 1)
									.getLrcTime()) {
						index = i;
					}
				}
				if (i == lrcList.size() - 1
						&& currentPosition > lrcList.get(i).getLrcTime()) {
					index = i;
				}
			}
		}

		return index;
	}

	@Override
	public void onDestroy() {
		// 注销广播接收器
		context.unregisterReceiver(myreceiver);
		super.onDestroy();
	}

	public class MyBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals("switchSong")) {
				currentSong = songServiceDao.getCurrentSong();
				handler.sendEmptyMessage(0);
			}
		}
	}

}
