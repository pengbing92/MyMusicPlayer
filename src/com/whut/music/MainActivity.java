package com.whut.music;

import java.util.ArrayList;
import java.util.List;

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
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.whut.application.MusicManager;
import com.whut.entiy.LrcContent;
import com.whut.entiy.Song;
import com.whut.service.MyMusicService;
import com.whut.util.Msg_Music;
import com.whut.util.Play_Model;
import com.whut.view.LrcProcess;
import com.whut.view.LrcView;

public class MainActivity extends Activity implements OnClickListener,
		OnSeekBarChangeListener {

	// 上一曲，暂停/播放，下一曲 按钮
	private ImageView pre_btn;
	private ImageView pau_btn;
	private ImageView next_btn;

	private ImageView play_model; // 播放模式:列表循环，单曲循环，顺序播放，随机播放

	// 进度条，歌曲时长
	private SeekBar lineOfTime;
	private TextView endTime;

	// 当前播放的时间
	private TextView currentTime;

	private int duration = 0; // 歌曲时长
	private int currentPosition = 0; // 当前播放位置

	// 播放中断位置，默认值为-1
	private int secondPause = -1;

	private int minute;
	private int second;
	private int millisecond;

	private Context context;

	// 保存播放中断位置
	private SharedPreferences sharedPreferences;
	private Editor editor;

	// 拖动位置
	private int seekPosition = 0;

	// 歌词相关
	private LrcView lrcView;
	private LrcProcess mLrcProcess;
	private List<LrcContent> lrcList = new ArrayList<LrcContent>();
	private int index = 0; // 歌词内容列表下标

	// 界面顶部展示歌曲名和歌手名
	private TextView song_singer;
	private TextView song_name;
	private String songName = "";
	private String singer = "";

	// 播放标志
	private boolean isPlaying = false;

	// 播放结束标志
	private boolean isEnd;

	// 广播接收器
	private CurrentPositonReceiver cpRev;
	private SecondPauseReceiver spRev;
	private IsplayingReceiver ipRev;
	private SeekCompleteReceiver scRev;
	private GetCurrentIndex gciRev;

	private ArrayList<Song> songList = new ArrayList<Song>(); // 歌曲列表
	private int currentIndex = 0; // 当前播放歌曲在列表中的下标
	private boolean switchSong = false; // 切换歌曲的标志

	private int currentModel = Play_Model.CYCLEALL; // 当前播放模式,默认为列表循环

	// 播放指令
	private int msg = -1;

	// 是否从通知栏进入
	private boolean fromNotification = false;

	// 通知栏管理
	private NotificationManager manager;

	// 是否已启动Service
	private boolean isServiceOpen = false;

	// 后退按钮
	private ImageView back_Btn;
	
	// 顶部平移动画
	private ObjectAnimator objectAnimatorLeft;
	private ObjectAnimator objectAnimatorRight;

	@SuppressLint("HandlerLeak")
	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case 0:
				// 更新滚动条
				updateSeekBar(msg.arg1);
				break;
			case 1:
				// 更新界面
				songName = songList.get(currentIndex).getSongName();
				singer = songList.get(currentIndex).getSinger();
				duration = songList.get(currentIndex).getDuration();
				song_name.setText(songName);
				song_singer.setText(singer);
				endTime.setText(getSongTime(duration));
				// 加载歌词
				initLrc();
				break;
			}
		};
	};

	// 格式化当前播放时间
	public String currentSecond2String(int currentSecond) {

		int minute = currentSecond / 60000;
		int second = (currentSecond - minute * 60000) / 1000;

		return formatTime(minute, second);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);

		context = this;

		initReceiver();

		// 通知栏管理
		manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// 去掉通知栏
		manager.cancelAll();
		// 发送广播，通知service，已经清除系统通知栏
		Intent intent = new Intent("notification");
		intent.putExtra("notification", false);
		sendBroadcast(intent);

		// 从Intent中获取数据
		isPlaying = getIntent().getBooleanExtra("isPlaying", false);
		currentIndex = getIntent().getIntExtra("currentIndex", 0);
		currentModel = getIntent().getIntExtra("currentModel",
				Play_Model.CYCLEALL);
		fromNotification = getIntent().getBooleanExtra("fromNotification",
				false);

		// 从SharedPreferences中读取数据
		sharedPreferences = getSharedPreferences("songInfo",
				Context.MODE_PRIVATE);
		secondPause = sharedPreferences.getInt("secondPause", -1);
		if (currentIndex != sharedPreferences.getInt("currentIndex", 0)) {
			secondPause = -1;
		}

		isServiceOpen = MusicManager.isServiceOpen();

		// 初始化歌曲信息
		songList = MusicManager.getSongsFromMediaDB(context);
		songName = songList.get(currentIndex).getSongName();
		singer = songList.get(currentIndex).getSinger();
		duration = songList.get(currentIndex).getDuration();

		initView();
		// 初始化歌词显示
		initLrc();

	}

	// 注册广播接收器
	public void initReceiver() {
		cpRev = new CurrentPositonReceiver();
		spRev = new SecondPauseReceiver();
		ipRev = new IsplayingReceiver();
		scRev = new SeekCompleteReceiver();
		gciRev = new GetCurrentIndex();

		IntentFilter cpFilter = new IntentFilter();
		cpFilter.addAction("GetPosition");
		registerReceiver(cpRev, cpFilter);

		IntentFilter spFilter = new IntentFilter();
		spFilter.addAction("recordSecondPause");
		registerReceiver(spRev, spFilter);

		IntentFilter ipFilter = new IntentFilter();
		ipFilter.addAction("isplaying");
		registerReceiver(ipRev, ipFilter);

		IntentFilter scFilter = new IntentFilter();
		scFilter.addAction("seekComplete");
		registerReceiver(scRev, scFilter);

		IntentFilter gciFilter = new IntentFilter();
		gciFilter.addAction("currentIndex");
		registerReceiver(gciRev, gciFilter);

	}

	public void initLrc() {
		mLrcProcess = new LrcProcess();
		mLrcProcess.readLRC(songName.trim());
		lrcList = mLrcProcess.getLrcList();
		lrcView.setmLrcList(lrcList);

		// lrcView.setAnimation(AnimationUtils.loadAnimation(context, ));
		handler.post(mRunnable);

	}

	Runnable mRunnable = new Runnable() {

		@Override
		public void run() {
			lrcView.setIndex(lrcIndex());
			lrcView.invalidate();
			handler.postDelayed(mRunnable, 100);

		}
	};

	// 取歌词list的下标
	public int lrcIndex() {

		if (currentPosition < duration) {
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

	public void initView() {

		// 按钮
		pre_btn = (ImageView) findViewById(R.id.previous);
		pau_btn = (ImageView) findViewById(R.id.pause);
		next_btn = (ImageView) findViewById(R.id.next);
		play_model = (ImageView) findViewById(R.id.play_model);
		back_Btn = (ImageView) findViewById(R.id.back);
		// 进度显示
		lineOfTime = (SeekBar) findViewById(R.id.time_line);
		endTime = (TextView) findViewById(R.id.end_time);
		currentTime = (TextView) findViewById(R.id.current_time);
		// 歌词显示
		lrcView = (LrcView) findViewById(R.id.lrcShowView);
		// 歌曲名与歌手名
		song_name = (TextView) findViewById(R.id.name);
		song_singer = (TextView) findViewById(R.id.singer);

		song_name.setText(songName);
		song_singer.setText(singer);

		// 设置歌曲时长
		endTime.setText(getSongTime(duration));
		// SeekBar长度
		lineOfTime.setMax(duration);

		if (secondPause != -1) {
			lineOfTime.setProgress(secondPause);
			currentTime.setText(currentSecond2String(secondPause));
		}

		if (MusicManager.getSeekPosition() != -1) {
			lineOfTime.setProgress(MusicManager.getSeekPosition());
			currentTime.setText(currentSecond2String(MusicManager
					.getSeekPosition()));
		}

		if (isPlaying) {
			pau_btn.setBackgroundResource(R.drawable.paubtn_selector);
		} else {
			pau_btn.setBackgroundResource(R.drawable.playbtn_selector);
		}

		switch (currentModel) {
		case Play_Model.CYCLEALL:
			play_model.setBackgroundResource(R.drawable.cycleall_selector);
			break;
		case Play_Model.CYCLEONE:
			play_model.setBackgroundResource(R.drawable.cycleone_selector);
			break;
		case Play_Model.RANDOM:
			play_model.setBackgroundResource(R.drawable.random_selector);
			break;
		}

		// 点击事件监听
		pre_btn.setOnClickListener(this);
		pau_btn.setOnClickListener(this);
		next_btn.setOnClickListener(this);
		play_model.setOnClickListener(this);
		back_Btn.setOnClickListener(this);

		// 进度条拖动事件监听
		lineOfTime.setOnSeekBarChangeListener(this);

		// 界面顶部歌曲名TextView,属性动画
		startTopAnimation();

	}

	// 实现顶部滚动动画效果
	public void startTopAnimation() {
		/**
		 * 测试，透明度动画，5秒内，将TextView由全透明变成非透明再变成全透明
		 */
		// ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(song_name,
		// "alpha", 1f, 0f, 1f);
		/**
		 * 测试，旋转动画，5秒内，将TextView旋转360度
		 */
		// ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(song_name,
		// "rotation", 0f, 360f);
		/**
		 * 测试，缩放动画，5秒内，将TextView在垂直方向上放大3倍再还原
		 */
		// ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(song_name,
		// "scaleY", 1f, 3f, 1f);
		/**
		 * 平移动画，歌曲名滚动效果
		 */
		float songNameX = song_name.getTranslationX();
		// 从当前位置向屏幕左侧平移
		objectAnimatorLeft = ObjectAnimator.ofFloat(song_name,
				"translationX", songNameX, -600f);
		objectAnimatorLeft.setDuration(2500);
		objectAnimatorLeft.start();
		// 从屏幕右侧向初始位置平移
		objectAnimatorRight = ObjectAnimator.ofFloat(song_name, 
				"translationX", 600f, songNameX);
		objectAnimatorRight.setDuration(2500);

		// 动画状态监听
		objectAnimatorLeft.addListener(new AnimatorListenerAdapter() {
			
			@Override
			public void onAnimationEnd(Animator animation) {
				super.onAnimationEnd(animation);
				/**
				 * 实现循环滚动效果，当向屏幕左侧平移结束，开始从屏幕右侧向左移
				 */
				objectAnimatorRight.start();			
			}	
		});
		
		objectAnimatorRight.addListener(new AnimatorListenerAdapter() {
			
			@Override
			public void onAnimationEnd(Animator animation) {
				super.onAnimationEnd(animation);
				/**
				 * 实现循环滚动效果，当从屏幕右侧向左移结束，开始向屏幕左侧平移
				 */
				objectAnimatorLeft.start();
			}
		});

	}

	// 格式化歌曲时长
	public String getSongTime(int songTime) {

		minute = songTime / 60000;
		second = (songTime - minute * 60000) / 1000;
		millisecond = (songTime - minute * 60000 - second * 1000) / 10;

		Log.i("MusicDemo", minute + ":" + second + ":" + millisecond);

		return formatTime(minute, second);
	}

	// 格式化时间显示
	public String formatTime(int minute, int second) {

		String min = "";
		String sec = "";

		if (minute < 10) {
			min = "0" + minute;
		} else {
			min = minute + min;
		}

		if (second < 10) {
			sec = "0" + second;
		} else {
			sec = sec + second;
		}

		return min + ":" + sec;

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.previous:
			playPreSong();
			// 切换歌曲的时候，重新设置 NOT_FOUND 的初始值为false
			LrcProcess.setNOT_FOUND(false);
			break;
		case R.id.pause:
			startMusicService();
			break;
		case R.id.next:
			playNextSong();
			// 切换歌曲的时候，重新设置 NOT_FOUND 的初始值为false
			LrcProcess.setNOT_FOUND(false);
			break;
		case R.id.play_model:
			switchModel();
			break;
		case R.id.back:
			// 结束当前Aty，返回上一个Aty
			finish();
			break;
		}

	}
	
	// 切换播放模式
	public void switchModel() {
		String showModel = "";
		switch (currentModel) {
		case Play_Model.CYCLEALL:
			currentModel = Play_Model.CYCLEONE;
			showModel = "单曲循环";
			play_model.setBackgroundResource(R.drawable.cycleone_selector);
			break;
		case Play_Model.CYCLEONE:
			currentModel = Play_Model.RANDOM;
			showModel = "随机播放";
			play_model.setBackgroundResource(R.drawable.random_selector);
			break;
		case Play_Model.RANDOM:
			currentModel = Play_Model.CYCLEALL;
			showModel = "列表循环";
			play_model.setBackgroundResource(R.drawable.cycleall_selector);
			break;
		}
		// 发送播放模式广播
		sendPlayModel();
		// 显示播放模式
		Toast.makeText(context, showModel, Toast.LENGTH_SHORT).show();

	}

	// 启动MyMusicService播放音乐
	public void startMusicService() {
		Intent intent = new Intent(context, MyMusicService.class);
		if (switchSong) {
			/**
			 * 切换上一曲或下一曲
			 */
			// 切换歌曲后，不用跳转到上一首歌的播放位置
			secondPause = -1;
			intent.putExtra("other_music", true);
			// 更新UI
			pau_btn.setBackgroundResource(R.drawable.paubtn_selector);
			// 重置
			switchSong = false;
		} else {
			// 播放按钮控制事件
			if (isPlaying) {
				msg = Msg_Music.PAUSE;
				secondPause = -1;
				pau_btn.setBackgroundResource(R.drawable.playbtn_selector);
			} else {
				msg = Msg_Music.PLAY;
				pau_btn.setBackgroundResource(R.drawable.paubtn_selector);
			}
			intent.putExtra("other_music", false);
		}

		intent.putExtra("msg", msg);
		intent.putExtra("secondPause", secondPause);
		intent.putExtra("currentIndex", currentIndex);
		intent.putExtra("currentModel", currentModel);
		// 启动Service
		startService(intent);
		isServiceOpen = true;
	}

	// 切换歌曲，发送广播，通知SongAty更新相关信息
	public void sendInfoToSongAty() {
		Intent intent = new Intent();
		intent.setAction("switchSong");
		intent.putExtra("currentIndex", currentIndex);
		sendBroadcast(intent);
	}

	// 记录暂停位置
	public void recordSecondPause() {
		Log.i("MusicDemo", "sharedPreferences");
		editor = sharedPreferences.edit();
		editor.putInt("secondPause", secondPause);
		editor.commit();
	}

	/**
	 * 点击通知栏进入，MainAty， 返回，进入SongAty，执行顺序是: Main_onPause -> Song_onCreate ->
	 * Main_onStop -> Main_onDestory
	 */
	@Override
	protected void onPause() {
		if (fromNotification) {
			MusicManager.setNotifMainToSong(true);
			MusicManager.setPlaying(isPlaying);
			MusicManager.setCurrentIndex(currentIndex);
			MusicManager.setCurrentModel(currentModel);
		} else {
			MusicManager.setNotifMainToSong(false);
		}

		super.onPause();
	}

	@Override
	protected void onStop() {
		// Toast.makeText(context, "Main_onStop", Toast.LENGTH_SHORT).show();
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		// Toast.makeText(context, "Main_onDestory", Toast.LENGTH_SHORT).show();

		// 发送广播，通知SongAty更新相关信息
		sendInfoToSongAty();
		// 注销广播接收器
		unRegisterBroadcastRev();

		super.onDestroy();
	}

	// 发送播放模式广播
	public void sendPlayModel() {
		Intent intent = new Intent();
		intent.setAction("playModel");
		intent.putExtra("currentModel", currentModel);
		sendBroadcast(intent);
	}

	// 注销广播接收器
	public void unRegisterBroadcastRev() {
		unregisterReceiver(cpRev);
		unregisterReceiver(ipRev);
		unregisterReceiver(scRev);
		unregisterReceiver(spRev);
		unregisterReceiver(gciRev);
	}

	// 更新UI
	public void updateSeekBar(int position) {
		lineOfTime.setProgress(position);
		currentTime.setText(currentSecond2String(position));
	}

	/**
	 * SeekBar 拖动事件监听
	 */
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		// 拖动时更新当前播放位置，更改歌词显示
		currentPosition = progress;
		// 实时更新UI
		updateSeekBar(progress);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {

		// 拖动后的位置
		seekPosition = seekBar.getProgress();

		MusicManager.setSeekPosition(seekPosition);

		if (isServiceOpen) {
			sendSeekPositionToService();
		}

	}

	// 把拖动后的位置通过广播发送到Service
	public void sendSeekPositionToService() {

		Intent seekIntent = new Intent();
		seekIntent.setAction("seekBar");
		seekIntent.putExtra("seekPosition", seekPosition);
		sendBroadcast(seekIntent);

	}

	// 播放结束后的处理
	public void playEnd() {
		if (currentModel != Play_Model.CYCLEONE) {
			// 播放下一首歌曲
			playNextSong();
		} else {
			// 单曲循环
			cycleOne();
		}

	}

	// 单曲循环
	public void cycleOne() {
		// 单曲循环需重新播放，设置switchSong为true
		switchSong = true;
		// 播放指令
		msg = Msg_Music.PLAY;
		startMusicService();
	}

	// 播放上一曲
	public void playPreSong() {

		// 切换歌曲
		switchSong = true;

		// 播放指令
		msg = Msg_Music.PRE;

		// 启动Service
		startMusicService();

	}

	// 播放下一曲
	public void playNextSong() {

		// 切换歌曲
		switchSong = true;

		// 播放指令
		msg = Msg_Music.NEXT;

		// 启动Service
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
			if (isEnd) {
				// 播放结束后的处理
				playEnd();
			} else {
				currentPosition = intent.getIntExtra("position", 0);
				Message msg = handler.obtainMessage();
				msg.what = 0;
				msg.arg1 = currentPosition;

				handler.sendMessage(msg);
			}
		}

	}

	// 获取暂停位置
	public class SecondPauseReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			secondPause = intent.getIntExtra("secondPause", -1);
		}

	}

	// 获取播放状态
	public class IsplayingReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			isPlaying = intent.getBooleanExtra("isplaying", false);
		}
	}

	// 跳转到指定位置后，更新UI
	public class SeekCompleteReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getBooleanExtra("seek", false)) {
				// 跳转到SeekBar拖动后的位置
				Message msg = handler.obtainMessage();
				msg.what = 0;
				msg.arg1 = seekPosition;
				handler.sendMessage(msg);
			} else {
				// 跳转到暂停时的位置
				Message msg = handler.obtainMessage();
				msg.what = 0;
				msg.arg1 = intent.getIntExtra("pause", -1);
				handler.sendMessage(msg);
			}
		}
	}

	// 获取当前播放歌曲的下标
	public class GetCurrentIndex extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			currentIndex = intent.getIntExtra("currentIndex", 0);
			// 更新界面
			handler.sendEmptyMessage(1);
		}

	}
}
