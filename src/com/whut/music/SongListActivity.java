package com.whut.music;

import java.io.File;
import java.util.ArrayList;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.whut.adapter.SongListAdapter;
import com.whut.application.MusicManager;
import com.whut.entiy.Song;
import com.whut.service.MyMusicService;
import com.whut.util.Msg_Music;
import com.whut.util.Play_Model;
import com.whut.util.ToastUtil;
import com.whut.view.LrcProcess;

public class SongListActivity extends Activity implements OnClickListener,
		OnItemClickListener {

	private ArrayList<Song> songList;
	private ListView songListView;
	private RelativeLayout bottomLayout;
	private ImageView songImage;
	private TextView songName;
	private TextView singer;
	private ImageView playBtn;
	private ImageView nextBtn;

	private Context context;

	private static SharedPreferences preferences;
	private static Editor editor;

	public static Editor getEditor() {
		return editor;
	}

	private String songName_str = ""; // 歌曲名
	private String singer_str = ""; // 歌手名
	private int secondPause = -1; // 播放中断位置，默认值为-1

	private SongListAdapter songListAdapter;

	// 播放标志
	private static boolean isPlaying;

	// 广播接收器
	private IsplayingReceiver ipRev;
	private SecondPauseReceiver spRev;
	private CurrentPositonReceiver cpRev;
	private SwitchSongByMainAty ssbmRev;
	private GetCurrentModel gcmRev;
	private GetCurrentIndex gciRev;
	private HeadSetStatus hssRev; // 接收耳机插拔状态的广播
	private static int HEADSET_DISCONNECT = 0; // 未插耳机
	private static int HEADSET_CONNECT = 1; // 插入耳机

	// 当前正在播放的歌曲
	private static int currentPosition = 0; // 当前播放的位置
	private static int currentIndex = 0; // 当前播放歌曲的下标
	//private static int currentId    = 0; // 当前播放歌曲的id
	private static int currentModel = Play_Model.CYCLEALL; // 当前播放模式,默认为列表循环

	// 播放是否结束
	private boolean isEnd = false;

	// 发送指令
	private int msg = -1;

	// 播放下一曲
	private boolean playNext = false;

	// 通知栏管理
	private NotificationManager manager;

	// 是否已启动Service
	private static boolean isServiceOpen = false;

	// 是否有通知栏
	private boolean notification = false;

	// 手机存储根目录
	private String rootPath = Environment.getExternalStorageDirectory()
			.getPath();

	// 歌曲缩略图旋转动画
	private ObjectAnimator objectAnimatorPre; // 先从0旋转到180
	private ObjectAnimator objectAnimatorNext; // 再从180旋转到360，周而复始

	@SuppressLint("HandlerLeak")
	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case 0:
				// 更新UI
				// song_progressBar.setProgress(msg.arg1);
				// Log.i("currentPosition", msg.arg1+"");
				break;
			case 1:
				songName_str = songList.get(currentIndex).getSongName();
				singer_str = songList.get(currentIndex).getSinger();
				songName.setText(songName_str);
				singer.setText(singer_str);
				// 正在播放的字体变红
				songListAdapter.setCurrentItem(currentIndex);
				songListView.setSelection(currentIndex);
				songListAdapter.notifyDataSetChanged();

				MusicManager.setCurrentIndex(currentIndex);
				break;
			case 2:
				// 播放模式
				MusicManager.setCurrentModel(currentModel);
				break;
			case 3:
				if (msg.arg1 == HEADSET_DISCONNECT) {
					ToastUtil.toastInfo(context, "未插入耳机");
				} else {
					ToastUtil.toastInfo(context, "已插入耳机");
				}
				break;
			}
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_song_list);

		context = this;

		Toast.makeText(context, "Song_onCreate", Toast.LENGTH_SHORT).show();

		// 创建歌词文件夹
		createLrcFolder();

		// 通知管理
		manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// 去掉通知
		manager.cancelAll();

		// 发送广播，通知service，已经清除系统通知栏
		Intent intent = new Intent("notification");
		intent.putExtra("notification", notification);
		sendBroadcast(intent);

		initBroadcastReceiver();

		initData();
		initView();

	}

	// 创建保存歌词文件的文件夹
	public void createLrcFolder() {

		File lrcDir = new File(rootPath + "/PengBing_lrcs");
		if (!lrcDir.exists()) {
			lrcDir.mkdirs();
		}
	}

	/**
	 * 从MainAty返回,执行onStart->onResume
	 */
	@Override
	protected void onStart() {
		super.onStart();
	}

	/**
	 * 执行流程onCreate->onStart->onResume, 从MainAty执行onStart->onResume
	 */
	@Override
	protected void onResume() {
		super.onResume();
		if (isPlaying) {
			playBtn.setBackgroundResource(R.drawable.paubtn_selector);
		} else {
			playBtn.setBackgroundResource(R.drawable.playbtn_selector);
		}
		// 若MainAty中切换了歌曲，更新UI
		handler.sendEmptyMessage(1);

		// 开启旋转动画效果
		startImageAnimation();

	}

	// 注册广播接收器
	public void initBroadcastReceiver() {

		ipRev = new IsplayingReceiver();
		IntentFilter ipFilter = new IntentFilter();
		ipFilter.addAction("isplaying");
		registerReceiver(ipRev, ipFilter);

		spRev = new SecondPauseReceiver();
		IntentFilter spFilter = new IntentFilter();
		spFilter.addAction("recordSecondPause");
		registerReceiver(spRev, spFilter);

		cpRev = new CurrentPositonReceiver();
		IntentFilter cpFilter = new IntentFilter();
		cpFilter.addAction("GetPosition");
		registerReceiver(cpRev, cpFilter);

		ssbmRev = new SwitchSongByMainAty();
		IntentFilter ssbmFilter = new IntentFilter();
		ssbmFilter.addAction("switchSong");
		registerReceiver(ssbmRev, ssbmFilter);

		gcmRev = new GetCurrentModel();
		IntentFilter gcmFilter = new IntentFilter();
		gcmFilter.addAction("playModel");
		registerReceiver(gcmRev, gcmFilter);

		gciRev = new GetCurrentIndex();
		IntentFilter gciFilter = new IntentFilter();
		gciFilter.addAction("currentIndex");
		registerReceiver(gciRev, gciFilter);

		hssRev = new HeadSetStatus();
		IntentFilter hssFilter = new IntentFilter();
		hssFilter.addAction("android.intent.action.HEADSET_PLUG");
		registerReceiver(hssRev, hssFilter);

	}

	// 初始化数据
	@SuppressWarnings("deprecation")
	public void initData() {

		songList = new ArrayList<Song>();
		songList = MusicManager.getSongsFromMediaDB(context);

		if (songList.size() > 0) {

			if (songName_str.equals("")) {
				songName_str = songList.get(0).getSongName();
			}

			if (singer_str.equals("")) {
				singer_str = songList.get(0).getSinger();
			}

		}

		// 从SharedPreferences中获取数据
		preferences = getSharedPreferences("songInfo", Context.MODE_PRIVATE);

		editor = preferences.edit();

		secondPause = preferences.getInt("secondPause", -1);
		currentIndex = preferences.getInt("currentIndex", 0);
		currentModel = preferences.getInt("currentModel", Play_Model.CYCLEALL);
		songName_str = songList.get(currentIndex).getSongName();
		singer_str = songList.get(currentIndex).getSinger();

		isPlaying = MusicManager.isPlaying();
		// currentIndex = MusicManager.getCurrentIndex();
		currentModel = MusicManager.getCurrentModel();
		isServiceOpen = MusicManager.isServiceOpen();

		/**
		 * 从通知栏进入MainAty，再返回到SongAty
		 */
		// if (MusicManager.isNotifMainToSong()) {
		// isPlaying = MusicManager.isPlaying();
		// currentIndex = MusicManager.getCurrentIndex();
		// currentModel = MusicManager.getCurrentModel();
		// isServiceOpen = MusicManager.isServiceOpen();
		// }

		songListAdapter = new SongListAdapter(songList, context);

	}

	public void initView() {

		songListView = (ListView) findViewById(R.id.songList);
		bottomLayout = (RelativeLayout) findViewById(R.id.bottomView);
		songImage = (ImageView) findViewById(R.id.songImg);
		songName = (TextView) findViewById(R.id.songName);
		singer = (TextView) findViewById(R.id.singer);
		playBtn = (ImageView) findViewById(R.id.play_btn);
		nextBtn = (ImageView) findViewById(R.id.next_btn);
//		song_progressBar = (ProgressBar) findViewById(R.id.song_progress);
//		song_progressBar.setMax(duration);

		songListView.setAdapter(songListAdapter);
		songListView.setOnItemClickListener(this);

		songImage.setBackgroundResource(R.drawable.app_music);

		if (isPlaying) {
			playBtn.setBackgroundResource(R.drawable.paubtn_selector);
		} else {
			playBtn.setBackgroundResource(R.drawable.playbtn_selector);
		}

		songName.setText(songName_str);
		singer.setText(singer_str);

		// 上次退出时播放的歌曲显示为红色
		handler.sendEmptyMessage(1);

		// 点击事件监听
		bottomLayout.setOnClickListener(this);
		playBtn.setOnClickListener(this);
		nextBtn.setOnClickListener(this);

		// 歌曲缩略图旋转动画效果
		objectAnimatorPre = ObjectAnimator.ofFloat(songImage, "rotation", 0f,
				180f);
		objectAnimatorNext = ObjectAnimator.ofFloat(songImage, "rotation",
				180f, 360f);

		// 动画状态监听
		objectAnimatorPre.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				super.onAnimationEnd(animation);
				objectAnimatorNext.start();
			}
		});
		objectAnimatorNext.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				super.onAnimationEnd(animation);
				objectAnimatorPre.start();
			}
		});

		// 开启动画效果
		startImageAnimation();

	}

	// 360旋转动画
	public void startImageAnimation() {

		objectAnimatorPre.setDuration(3000);
		objectAnimatorPre.start();

		objectAnimatorNext.setDuration(3000);

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.bottomView:
			// 从SongAty跳转到MainAty
			startActivity(gotoMainAty(context, isPlaying, currentIndex,
					currentModel, false));
			break;
		case R.id.play_btn:
			startMusicService();
			break;
		case R.id.next_btn:
			playNextSong();
			// 切换歌曲的时候，重新设置 NOT_FOUND 的初始值为false
			LrcProcess.setNOT_FOUND(false);
			break;
		}
	}

	// 通过Btn启动service
	public void startMusicService() {
		Intent gotoService = new Intent(context, MyMusicService.class);
		if (playNext) {
			secondPause = -1;
			gotoService.putExtra("other_music", true);
			// 重置
			playNext = false;
			playBtn.setBackgroundResource(R.drawable.paubtn_selector);
		} else {
			// 没有切换歌曲
			if (isPlaying) {
				msg = Msg_Music.PAUSE;
				secondPause = -1;
				playBtn.setBackgroundResource(R.drawable.playbtn_selector);
			} else {
				msg = Msg_Music.PLAY;
				playBtn.setBackgroundResource(R.drawable.paubtn_selector);
			}
			gotoService.putExtra("other_music", false);
		}

		gotoService.putExtra("msg", msg);
		gotoService.putExtra("secondPause", secondPause);
		gotoService.putExtra("currentModel", currentModel);
		gotoService.putExtra("currentIndex", currentIndex);
		// 启动Service
		startService(gotoService);
		isServiceOpen = true;
	}

	@Override
	protected void onDestroy() {

		/**
		 * 播放时退出，通知栏
		 */
		if (isPlaying) {
			MusicManager.setPlaying(isPlaying);
			MusicManager
					.musicNotification(
							1,
							context,
							getPackageName(),
							gotoMainAty(context, true, currentIndex,
									currentModel, true), true, currentIndex,
							currentModel);

			// 发送广播，通知service，已经构建系统通知栏
			notification = true;
			Intent intent = new Intent("notification");
			intent.putExtra("notification", notification);
			sendBroadcast(intent);
		} else {

			// 关闭MyMusicService
			Intent intent = new Intent(context, MyMusicService.class);
			stopService(intent);

		}

		// 保存退出时的歌曲信息
		recordPause(currentPosition, currentIndex, currentModel);
		// 注销广播接收器
		unRegisterBroadcastRev();
		super.onDestroy();
	}

	/**
	 * 进入MainAty的intent，包含相关信息
	 * 
	 * @return intent
	 */
	public static Intent gotoMainAty(Context context, boolean isPlaying,
			int currentIndex, int currentModel, boolean fromNotification) {

		Intent intent = new Intent(context, MainActivity.class);
		intent.putExtra("isPlaying", isPlaying);
		intent.putExtra("currentIndex", currentIndex);
		intent.putExtra("currentModel", currentModel);
		intent.putExtra("fromNotification", fromNotification);
		if (fromNotification) {
			if (isPlaying) {
				MusicManager.setServiceOpen(true);
			} else {
				MusicManager.setServiceOpen(false);
			}
		} else {
			MusicManager.setServiceOpen(isServiceOpen);
		}

		return intent;
	}

	// 注销广播接收器
	public void unRegisterBroadcastRev() {
		unregisterReceiver(ipRev);
		unregisterReceiver(spRev);
		unregisterReceiver(cpRev);
		unregisterReceiver(ssbmRev);
		unregisterReceiver(gcmRev);
		unregisterReceiver(gciRev);
		unregisterReceiver(hssRev);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {

		if (currentIndex == position) {
			// 点击的是同一首歌曲
			playNext = false;
		} else {
			// 点击的不是同一首歌曲
			MusicManager.setSeekPosition(-1);

			playNext = true;
			msg = Msg_Music.PLAY;
			
			// 切换歌曲的时候，重新设置 NOT_FOUND 的初始值为false
			LrcProcess.setNOT_FOUND(false);
		}

		currentIndex = position;

		// 更新UI
		handler.sendEmptyMessage(1);

		startMusicService();

	}

	// 保存退出时的歌曲信息
	public static void recordPause(int currentPosition, int currentIndex,
			int currentModel) {
		Log.i("MusicDemo", "sharedPreferences");
		// editor = preferences.edit();
		editor.putInt("secondPause", currentPosition);
		editor.putInt("currentIndex", currentIndex);
		editor.putInt("currentModel", currentModel);
		editor.commit();

		MusicManager.setPlaying(isPlaying);
	}

	// 播放结束后的处理
	public void playEnd(boolean isEnd) {
		if (isEnd) {
			if (currentModel != Play_Model.CYCLEONE) {
				// 播放下一首歌曲
				playNextSong();
			} else {
				// 单曲循环
				cycleOne();
			}
		}
	}

	// 单曲循环
	public void cycleOne() {
		// 单曲循环需重新播放，设置playNext为true
		playNext = true;
		// 播放指令
		msg = Msg_Music.PLAY;
		startMusicService();
	}

	// 播放下一曲
	public void playNextSong() {
		playNext = true;
		msg = Msg_Music.NEXT;
		startMusicService();
	}

	/**
	 * 定义广播接收器
	 * 
	 * @author chenfu
	 * 
	 */
	// 获取当前播放位置
	public class CurrentPositonReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			isEnd = intent.getBooleanExtra("isEnd", false);
			// 播放结束后的处理
			playEnd(isEnd);
			if (isPlaying) {
				if (!isEnd) {
					currentPosition = intent.getIntExtra("position", 0);

					Message msg = handler.obtainMessage();
					msg.what = 0;
					msg.arg1 = currentPosition;

					handler.sendMessage(msg);
				}
			}

		}

	}

	// 获取播放状态
	public class IsplayingReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			isPlaying = intent.getBooleanExtra("isplaying", false);
		}
	}

	// 获取暂停位置
	public class SecondPauseReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			secondPause = intent.getIntExtra("secondPause", -1);
		}

	}

	// MainAty中切换了歌曲,SongAty作相应改变
	public class SwitchSongByMainAty extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			currentIndex = intent.getIntExtra("currentIndex", 0);
			songName_str = songList.get(currentIndex).getSongName();
			singer_str = songList.get(currentIndex).getSinger();
		}

	}

	// 获取当前播放模式
	public class GetCurrentModel extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			currentModel = intent.getIntExtra("currentModel",
					Play_Model.CYCLEALL);
			handler.sendEmptyMessage(2);

		}
	}

	// 获取当前播放歌曲的下标
	public class GetCurrentIndex extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			currentIndex = intent.getIntExtra("currentIndex", 0);
			handler.sendEmptyMessage(1);
		}

	}

	// 实时监测耳机插拔状态
	public class HeadSetStatus extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Message msg = handler.obtainMessage();
			msg.what = 3;
			if (intent.hasExtra("state")) {

				if (intent.getIntExtra("state", 0) == 0) {
					// 未插入耳机
					msg.arg1 = HEADSET_DISCONNECT;
				}

				if (intent.getIntExtra("state", 0) == 1) {
					// 插入耳机
					msg.arg1 = HEADSET_CONNECT;
				}
			}
			handler.sendMessage(msg);

		}

	}

}
