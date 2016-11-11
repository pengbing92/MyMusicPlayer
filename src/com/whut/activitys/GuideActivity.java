package com.whut.activitys;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;

import com.whut.application.MyApplication;
import com.whut.music.R;
import com.whut.util.HanZi2PinYin;
import com.whut.view.MyVideoView;

/**
 * 视频引导页，
 * 并初始化汉字转拼音对照Map
 * 
 * @author chenfu
 *
 */
public class GuideActivity extends Activity implements OnClickListener {

	private MyVideoView videoView;
	private Button enterAppBtn;

	@TargetApi(Build.VERSION_CODES.KITKAT)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_guide);
		
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

	private void initView() {
		videoView = (MyVideoView) findViewById(R.id.guideVideo);
		enterAppBtn = (Button) findViewById(R.id.enterAppBtn);

		//设置播放加载路径
		videoView.setVideoURI(Uri.parse("android.resource://"
				+ getPackageName() + "/" + R.raw.welcome_media));
		//播放
        videoView.start();
        //播放结束，进入主界面
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
            	// 播放结束，进入MainAty
                enterMainAty();
            }
        });

		enterAppBtn.setOnClickListener(this);

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
	 * 只有在Android 4.4及以上系统才支持沉浸式模式
	 * 隐藏状态栏，导航栏
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus && Build.VERSION.SDK_INT >= 19) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
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
			//释放掉占用的内存
			videoView.suspend();
		}
		super.onDestroy();
	}
}
