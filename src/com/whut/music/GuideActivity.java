package com.whut.music;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;

import com.whut.application.MyApplication;
import com.whut.database.entiy.Song;
import com.whut.database.service.imp.SongServiceDao;
import com.whut.music.R;
import com.whut.util.HanZi2PinYin;
import com.whut.util.ImageUtil;
import com.whut.view.MyVideoView;

/**
 * 视频引导页， 并初始化汉字转拼音对照Map
 * 
 * @author chenfu
 * 
 */
public class GuideActivity extends Activity implements OnClickListener,
		OnCompletionListener {

	private MyVideoView videoView;
	private Button enterAppBtn;

	// 图片管理
	private ImageUtil imageUtil;

	private SongServiceDao songServiceDao;
	private List<Song> songList = new ArrayList<Song>();
	
	private static final String TAG = GuideActivity.class.getName();

	@SuppressLint("HandlerLeak")
	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case 0:
				enterAppBtn.setVisibility(View.VISIBLE);
				// 开启动画效果
				startAnimator();
				break;

			default:
				break;
			}

		};
	};

	@TargetApi(Build.VERSION_CODES.KITKAT)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_guide);

		initData();
		initView();

		/**
		 * 首先根据raw目录下的文本文件建立汉字与拼音的映射关系
		 */
		new Thread(new Runnable() {

			@Override
			public void run() {
				HanZi2PinYin.readTxtFile(MyApplication.getContext());
			}

		}).start();

	}

	private void initData() {
		imageUtil = ImageUtil.getInstance(this);
		songServiceDao = new SongServiceDao(this);

		songList = songServiceDao.getAllSong();
		
		if (songList.size() > 0) {
			// 把歌曲缩略图加入到缓存中
			loadBitmap();
		} else {
			Log.i(TAG, "数据库中没有歌曲..");
		}

	}

	private void initView() {
		videoView = (MyVideoView) findViewById(R.id.guideVideo);
		enterAppBtn = (Button) findViewById(R.id.enterAppBtn);

		// 设置引导视频加载路径
		videoView.setVideoURI(Uri.parse("android.resource://"
				+ getPackageName() + "/" + R.raw.welcome_media));
		// 播放
		videoView.start();
		// 播放结束，进入主界面
		videoView.setOnCompletionListener(this);

		/**
		 * 定时器，让进入按钮6秒后再显示出来
		 */
		Timer timer = new Timer();
		TimerTask task = new TimerTask() {

			@Override
			public void run() {
				handler.sendEmptyMessage(0);

			}
		};
		timer.schedule(task, 6000);

		enterAppBtn.setOnClickListener(this);

	}

	// 初始化操作，将缩略图加入缓存
	private void loadBitmap() {

		new Thread(new Runnable() {

			@Override
			public void run() {
				for (int i = 0; i < songList.size(); i++) {
					Long album_id = songList.get(i).getAlbumId();
					imageUtil.addBitmapToMemoryCache(album_id,
							imageUtil.getSmallArtwork(album_id));
				}

			}
		}).start();

	}

	/**
	 * 开启进入按钮的动画
	 */
	private void startAnimator() {
		ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(enterAppBtn,
				"alpha", 0f, 1f);
		ObjectAnimator rotationAnimator = ObjectAnimator.ofFloat(enterAppBtn,
				"rotation", 0f, 360f);
		AnimatorSet animatorSet = new AnimatorSet();
		animatorSet.setDuration(3000);
		animatorSet.play(rotationAnimator).with(alphaAnimator);
		animatorSet.start();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.enterAppBtn:
			// 点击按钮，进入MainAty
			enterMainAty();
			break;
		default:
			break;
		}

	}

	// 进入MainAty
	private void enterMainAty() {
		Intent intent = new Intent(this, MainActivity.class);
		startActivity(intent);
		finish();
	}

	/**
	 * 只有在Android 4.4及以上系统才支持沉浸式模式 隐藏状态栏，导航栏
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus && Build.VERSION.SDK_INT >= 19) {
			View decorView = getWindow().getDecorView();
			decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
					| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
					| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_FULLSCREEN
					| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
		}
	}

	@Override
	protected void onDestroy() {
		if (videoView != null) {
			// 释放掉占用的内存
			videoView.suspend();
		}
		super.onDestroy();
	}

	@Override
	public void onCompletion(MediaPlayer mediaPlayer) {
		mediaPlayer.start();

	}
}
