package com.whut.fragment;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.whut.application.MusicManager;
import com.whut.database.entiy.Play_Model;
import com.whut.database.entiy.Song;
import com.whut.database.service.imp.ModelServiceDao;
import com.whut.database.service.imp.SongServiceDao;
import com.whut.music.R;
import com.whut.music.SongListActivity;
import com.whut.service.MyMusicService;
import com.whut.util.ToastUtil;

public class LocalMusicFragment extends Fragment implements OnClickListener {

	private Context context;

	private TextView localContent;
	private RelativeLayout localBottom;

	private List<Song> songList = new ArrayList<Song>();

	private String songName_str = "";
	private String singer_str = "";

	private static SharedPreferences preferences;
	private static Editor editor;

	public static Editor getEditor() {
		return editor;
	}

	private int secondPause;
	private Song currentSong;
	private long currentId;
	private int currentModel;

	private ImageView songImage;
	private TextView songName;
	private TextView singer;

	// 播放状态标志
	private static boolean isPlaying;
	private MyBroadcastReceiver myreceiver;

	// 通知栏管理
	private NotificationManager manager;
	private boolean notification = false; // 是否有通知栏

	private SongServiceDao songServiceDao;
	private ModelServiceDao modelServiceDao;

	@SuppressLint("HandlerLeak")
	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case 0:
				// currentId发生变化,更新UI
				currentSong = songServiceDao.getCurrentSong();
				songName.setText(currentSong.getSongName());
				singer.setText(currentSong.getSinger());
				break;
			case 1:
				ToastUtil.toastInfo(context, "test");
				break;

			default:
				break;
			}
		};
	};

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		/**
		 * onCreate方法只执行一次
		 */
		context = getActivity();

		currentSong = new Song();
		songServiceDao = new SongServiceDao(context);
		modelServiceDao = new ModelServiceDao(context);

		// 通知栏管理
		manager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		// 去掉通知栏
		manager.cancelAll();

		// 发送广播，通知service，已经清除系统通知栏
		Intent intent = new Intent("notification");
		intent.putExtra("notification", notification);
		context.sendBroadcast(intent);

		initData();
		// 初始化广播接收器
		initBroadcastReceiver();
		Log.i("localFM", "onCreate");
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		Log.i("localFM", "onCreateView");
		View view = inflater.inflate(R.layout.fragment_local, container, false);

		handler.sendEmptyMessage(0);
		return view;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {

		localContent = (TextView) getActivity().findViewById(
				R.id.localFM_Content);
		localBottom = (RelativeLayout) getActivity().findViewById(
				R.id.bottomView);

		localContent.setOnClickListener(this);
		localBottom.setOnClickListener(this);

		songImage = (ImageView) getActivity().findViewById(R.id.songImg);
		songName = (TextView) getActivity().findViewById(R.id.songName);
		singer = (TextView) getActivity().findViewById(R.id.singer);

		songImage.setBackgroundResource(R.drawable.app_music);
		songName.setText(songName_str);
		singer.setText(singer_str);

		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.bottomView:
			startActivity(SongListActivity
					.gotoLrcAty(context, isPlaying, false));
			break;
		case R.id.localFM_Content:
			Intent gotoSongListAty = new Intent(context, SongListActivity.class);
			startActivity(gotoSongListAty);
			break;
		default:
			break;
		}

	}

	public void initData() {

		songList = MusicManager.getSongsFromMediaDB(context);

		// 从SharedPreferences中获取数据
		preferences = context.getSharedPreferences("songInfo",
				Context.MODE_PRIVATE);
		editor = preferences.edit();
		secondPause = preferences.getInt("secondPause", -1);
		currentId = preferences.getLong("currentId", 0);
		currentModel = preferences.getInt("currentModel", Play_Model.CYCLEALL);

		if (songList.size() > 0) {
			songServiceDao.addMusicList2DB(songList);

			if (currentId != 0) {
				songServiceDao.updateCurrentSong(songServiceDao
						.getSongById(currentId));
			} else {
				songServiceDao.updateCurrentSong(songList.get(0));
			}

			currentSong = songServiceDao.getCurrentSong();
			songName_str = currentSong.getSongName();
			singer_str = currentSong.getSinger();

		}

		modelServiceDao.updateCurrentModel(currentModel);

		/**
		 * 点击通知栏进入LrcAty，后退进入MainAty 此时LocalFM的广播接收器才注册，接收不到播放状态广播
		 */
		isPlaying = MusicManager.isPlaying();

	}

	// 注册广播接收器
	public void initBroadcastReceiver() {

		myreceiver = new MyBroadcastReceiver();

		IntentFilter ipFilter = new IntentFilter();
		ipFilter.addAction("isplaying");
		context.registerReceiver(myreceiver, ipFilter);

	}

	// 保存退出时的歌曲信息
	public void recordPause(int currentPosition) {
		Log.i("MusicDemo", "sharedPreferences");

		editor.putInt("secondPause", currentPosition);
		editor.putLong("currentId", songServiceDao.getCurrentSong().getId());
		editor.putInt("currentModel", modelServiceDao.getCurrentModel());
		editor.commit();

		MusicManager.setPlaying(isPlaying);
	}

	@Override
	public void onDestroy() {
		Log.i("LocalFragement", "onDestory");
		/**
		 * 播放时退出，通知栏
		 */
		if (isPlaying) {
			MusicManager.setPlaying(isPlaying);
			MusicManager.musicNotification(1, context,
					context.getPackageName(),
					SongListActivity.gotoLrcAty(context, true, true), true,
					songServiceDao.getCurrentSong().getId(),
					modelServiceDao.getCurrentModel());

			// 发送广播，通知service，已经构建系统通知栏
			notification = true;
			Intent intent = new Intent("notification");
			intent.putExtra("notification", notification);
			context.sendBroadcast(intent);
		} else {
			// 关闭MyMusicService
			Intent intent = new Intent(context, MyMusicService.class);
			context.stopService(intent);
		}
		// 保存退出时的歌曲信息
		recordPause(SongListActivity.getCurrentPosition());
		// 注销广播接收器
		context.unregisterReceiver(myreceiver);
		super.onDestroy();
	}

	@Override
	public void onDestroyView() {
		Log.i("LocalFragement", "onDestoryView");
		super.onDestroyView();
	}

	@Override
	public void onStop() {
		/**
		 * 跳转到另一个Aty后，先执行Aty的onStop方法， 再执行Fragment的onStop方法
		 */
		Log.i("LocalFragement", "onStop");
		super.onStop();
	}

	public class MyBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals("isplaying")) {
				isPlaying = intent.getBooleanExtra("isplaying", false);
				handler.sendEmptyMessage(1);
			}
		}
	}

}