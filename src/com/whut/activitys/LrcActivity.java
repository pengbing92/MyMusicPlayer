package com.whut.activitys;

import java.util.ArrayList;
import java.util.List;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.whut.adapter.MyFragmentAdapter;
import com.whut.application.MusicManager;
import com.whut.database.entiy.Play_Model;
import com.whut.database.service.imp.ModelServiceDao;
import com.whut.database.service.imp.SongServiceDao;
import com.whut.fragment.AlbumFragment;
import com.whut.fragment.LrcFragment;
import com.whut.music.R;
import com.whut.service.MyMusicService;
import com.whut.util.Msg_Music;
import com.whut.util.ToastUtil;
import com.whut.view.LrcProcess;

/**
 * 歌词界面
 * 
 * @author chenfu
 * 
 */
public class LrcActivity extends FragmentActivity implements OnClickListener,
		OnSeekBarChangeListener, OnPageChangeListener {

	// 上一曲，暂停/播放，下一曲 按钮
	private ImageView pre_btn;
	private ImageView pau_btn;
	private ImageView next_btn;

	private ImageView play_model; // 播放模式:列表循环，单曲循环，随机播放

	// 进度条，歌曲时长
	private static SeekBar lineOfTime;
	private static TextView endTime;

	// 当前播放的时间
	private static TextView currentTime;

	private static int duration = 0; // 歌曲时长
	public static int currentPosition = 0; // 当前播放位置

	// 播放中断位置，默认值为-1
	private int secondPause = -1;

	private static int minute;
	private static int second;
	private static int millisecond;

	private Context context;

	// 拖动位置
	private int seekPosition = 0;

	// 界面顶部展示歌曲名和歌手名
	private static TextView song_singer;
	private static TextView song_name;
	private static String songName = "";
	private static String singer = "";

	// 播放标志
	private boolean isPlaying = false;

	// 播放结束标志
	private boolean isEnd;

	// 广播接收器
	private CurrentPositonReceiver cpRev;
	private SecondPauseReceiver spRev;
	private IsplayingReceiver ipRev;
	private SeekCompleteReceiver scRev;
	private SwitchSongReceiver ssRev;

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

	private ModelServiceDao modelServiceDao;
	private static SongServiceDao songServiceDao;

	// 歌词与专辑图片切换
	private LrcFragment lrcFragment;
	private AlbumFragment albumFragment;
	private List<Fragment> fragmentList = new ArrayList<Fragment>();
	private MyFragmentAdapter myFragmentAdapter;
	private ViewPager viewPager;
	
	// 右上角分享按钮
	private ImageView shareBtn;

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
				songName = songServiceDao.getCurrentSong().getSongName();
				singer = songServiceDao.getCurrentSong().getSinger();
				duration = songServiceDao.getCurrentSong().getDuration();
				song_name.setText(songName);
				song_singer.setText(singer);
				endTime.setText(getSongTime(duration));
				break;
			}
		};
	};

	// 格式化当前播放时间
	public static String currentSecond2String(int currentSecond) {

		int minute = currentSecond / 60000;
		int second = (currentSecond - minute * 60000) / 1000;

		return formatTime(minute, second);
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_lrc);
		if (Build.VERSION.SDK_INT >= 19) { // 设置系统状态栏透明，因此背景图片可以全屏显示
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
		}
		
		context = this;

		modelServiceDao = new ModelServiceDao(context);
		songServiceDao = new SongServiceDao(context);

		currentModel = modelServiceDao.getCurrentModel();

		// 注册广播接收器
		initBroadcastReceiver();

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
		fromNotification = getIntent().getBooleanExtra("fromNotification",
				false);

		isServiceOpen = MusicManager.isServiceOpen();

		// 初始化歌曲信息
		songName = songServiceDao.getCurrentSong().getSongName();
		singer = songServiceDao.getCurrentSong().getSinger();
		duration = songServiceDao.getCurrentSong().getDuration();

		initView();

	}

	// 注册广播接收器
	public void initBroadcastReceiver() {
		cpRev = new CurrentPositonReceiver();
		spRev = new SecondPauseReceiver();
		ipRev = new IsplayingReceiver();
		scRev = new SeekCompleteReceiver();
		ssRev = new SwitchSongReceiver();

		IntentFilter ssFilter = new IntentFilter();
		ssFilter.addAction("switchSong");
		registerReceiver(ssRev, ssFilter);

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
		// 歌曲名与歌手名
		song_name = (TextView) findViewById(R.id.name);
		song_singer = (TextView) findViewById(R.id.singer);
		// 分享按钮
		shareBtn = (ImageView) findViewById(R.id.shareBtn);

		/**
		 * 歌词与专辑图片的切换
		 */
		initFragMents();
		viewPager = (ViewPager) findViewById(R.id.viewPager_lrc_album);
		viewPager.addOnPageChangeListener(this);
		viewPager.setAdapter(myFragmentAdapter);
		viewPager.setCurrentItem(0);

		// 歌曲名和歌手
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

		// 播放模式按钮背景图
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
		shareBtn.setOnClickListener(this);

		// 进度条拖动事件监听
		lineOfTime.setOnSeekBarChangeListener(this);

		// 界面顶部歌曲名TextView,属性动画
		//startTopAnimation();

	}

	private void initFragMents() {
		lrcFragment = new LrcFragment();
		albumFragment = new AlbumFragment();

		fragmentList.add(lrcFragment);
		fragmentList.add(albumFragment);

		myFragmentAdapter = new MyFragmentAdapter(getSupportFragmentManager(),
				fragmentList);

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
		objectAnimatorLeft = ObjectAnimator.ofFloat(song_name, "translationX",
				songNameX, -600f);
		objectAnimatorLeft.setDuration(2500);
		if (isPlaying) {
			objectAnimatorLeft.start();
		}
		// 从屏幕右侧向初始位置平移
		objectAnimatorRight = ObjectAnimator.ofFloat(song_name, "translationX",
				600f, songNameX);
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
	public static String getSongTime(int songTime) {

		minute = songTime / 60000;
		second = (songTime - minute * 60000) / 1000;
		millisecond = (songTime - minute * 60000 - second * 1000) / 10;

		Log.i("MusicDemo", minute + ":" + second + ":" + millisecond);

		return formatTime(minute, second);
	}

	// 格式化时间显示
	public static String formatTime(int minute, int second) {

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
		case R.id.shareBtn:
			// 分享
			
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
		// 更新数据库
		modelServiceDao.updateCurrentModel(currentModel);
		// 显示播放模式
		ToastUtil.toastInfo(context, showModel);

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
		// 启动Service
		startService(intent);
		isServiceOpen = true;

	}

	/**
	 * 点击通知栏进入，LrcAty， 返回，进入SongAty，执行顺序是: Lrc_onPause -> Song_onCreate ->
	 * Lrc_onStop -> Lrc_onDestory
	 */
	@Override
	protected void onPause() {
		if (fromNotification) {
			MusicManager.setNotifMainToSong(true);
			MusicManager.setPlaying(isPlaying);
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

		// 注销广播接收器
		unRegisterBroadcastRev();

		super.onDestroy();
	}

	// 注销广播接收器
	public void unRegisterBroadcastRev() {
		unregisterReceiver(cpRev);
		unregisterReceiver(ipRev);
		unregisterReceiver(scRev);
		unregisterReceiver(spRev);
		unregisterReceiver(ssRev);
	}

	// 更新UI
	public static void updateSeekBar(int position) {
		lineOfTime.setProgress(position);
		currentTime.setText(currentSecond2String(position));
	}

	/**
	 * SeekBar 进度条拖动事件监听
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

		// 发送广播，service更新播放位置
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

	// 切换音乐
	public class SwitchSongReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			handler.sendEmptyMessage(1);

		}

	}

	/**
	 * 滑动切换歌词与专辑图片显示
	 */
	@Override
	public void onPageScrollStateChanged(int arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPageSelected(int arg0) {
		// TODO Auto-generated method stub

	}
}
