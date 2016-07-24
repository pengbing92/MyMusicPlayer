package com.whut.service;

import java.io.File;
import java.io.IOException;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.whut.application.MusicManager;
import com.whut.application.MyApplication;
import com.whut.database.entiy.Play_Model;
import com.whut.database.service.imp.ModelServiceDao;
import com.whut.database.service.imp.SongServiceDao;
import com.whut.music.LrcActivity;
import com.whut.music.SongListActivity;
import com.whut.util.Msg_Music;

public class MyMusicService extends Service {

	// 播放器
	private MediaPlayer mediaPlayer;
	private int secondPause = -1; // 播放中断位置，默认值为-1
	private boolean stopThread = false; // 中断线程的标志

	// 接收到命令
	private int msg;

	// 获取播放位置的线程
	private GetPosition getPosition;

	// 音乐文件路径
	private String mp3Path = "";

	// 切换歌曲
	private boolean other_music = false;

	// 广播接收器
	private SeekPositionReceiver spRev;
	private NotificationCreated notifRev;
	private CurrentPositonReceiver cpRev;

	// 是否有通知的标志
	private boolean notifCreated = false;

	private ModelServiceDao modelServiceDao;
	private SongServiceDao songServiceDao;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/**
	 * 只在初次启动Service时执行onCreate方法 后面再次启动，只执行onStart方法
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		mediaPlayer = new MediaPlayer();

		modelServiceDao = new ModelServiceDao(MyApplication.getContext());
		songServiceDao = new SongServiceDao(MyApplication.getContext());

		// 注册广播接收器
		initReceiver();

	}

	@Override
	public void onStart(Intent intent, int startId) {

		secondPause = intent.getIntExtra("secondPause", -1);
		msg = intent.getIntExtra("msg", -1);
		other_music = intent.getBooleanExtra("other_music", false);
		Log.i("other_music", other_music + "");

		initPlay();

		getPosition = new GetPosition();

		// 根据指令作相应处理
		if (msg == Msg_Music.PLAY) {
			play();
		} else if (msg == Msg_Music.PAUSE) {
			pause();
		} else if (msg == Msg_Music.PRE) {
			pre();
		} else if (msg == Msg_Music.NEXT) {
			next();
		}

	}

	/**
	 * 上一曲
	 * 
	 * 只有点击按钮切换一种情况
	 */
	public void pre() {

		MusicManager.setSeekPosition(-1);

		// 更新数据库
		songServiceDao.updateCurrentSong(songServiceDao
				.getPreSong(songServiceDao.getCurrentSong()));

		play();

		// 发送广播，通知Aty更新UI
		sendSwitchSongBroadcast();

	}

	/**
	 * 下一曲
	 * 
	 * 自动播放下一曲和点击按钮切换两种情况
	 */
	public void next() {

		MusicManager.setSeekPosition(-1);

		// 更新数据库
		songServiceDao.updateCurrentSong(songServiceDao
				.getNextSong(songServiceDao.getCurrentSong()));

		play();

		// 发送广播，通知Aty更新UI
		sendSwitchSongBroadcast();

	}

	/**
	 * 暂停
	 */
	public void pause() {
		mediaPlayer.pause();
		// 中断线程
		stopThread = true;
		recordSecondPause();
		// 发送播放状态广播
		sendIsPlayingBroadcast(false);

		// 更新通知栏
		if (notifCreated) {
			MusicManager.setPlaying(false);
			// 通知
			Intent intent = SongListActivity.gotoLrcAty(MyMusicService.this,
					false, true);
			MusicManager.musicNotification(1, MyMusicService.this,
					getPackageName(), intent, false, songServiceDao
							.getCurrentSong().getId(), modelServiceDao
							.getCurrentModel());
		}
	}

	/**
	 * 播放
	 */
	public void play() {

		// 先reset，再重新加载其他曲目
		if (other_music) {
			stopThread = true;
			mediaPlayer.reset();
		}

		mp3Path = songServiceDao.getCurrentSong().getMp3Path();

		File file = new File(mp3Path);
		Log.i("Service_currentPath", file.getPath());
		try {
			mediaPlayer.setDataSource(file.getPath());
			mediaPlayer.prepare();

			if (MusicManager.getSeekPosition() != -1) {
				secondPause = -1;
				mediaPlayer.seekTo(MusicManager.getSeekPosition());
			}

			// 跳转到上次播放中断位置
			if (secondPause != -1) {
				mediaPlayer.seekTo(secondPause);
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		mediaPlayer.start();
		// 发送播放状态广播
		sendIsPlayingBroadcast(true);
		// 启动线程，获取播放位置
		stopThread = false;
		new Thread(getPosition).start();

		// 更新通知栏
		if (notifCreated) {
			MusicManager.setPlaying(true);
			// 通知
			Intent intent = SongListActivity.gotoLrcAty(MyMusicService.this,
					true, true);
			MusicManager.musicNotification(1, MyMusicService.this,
					getPackageName(), intent, true, songServiceDao
							.getCurrentSong().getId(), modelServiceDao
							.getCurrentModel());
		}

	}

	// 发送切换歌曲广播
	public void sendSwitchSongBroadcast() {
		Intent intent = new Intent();
		intent.setAction("switchSong");
		sendBroadcast(intent);
	}

	// 发送播放状态广播
	public void sendIsPlayingBroadcast(boolean isplaying) {
		Intent intent = new Intent();
		intent.setAction("isplaying");
		intent.putExtra("isplaying", isplaying);
		sendBroadcast(intent);
		// 更新歌曲播放状态
		MusicManager.setPlaying(isplaying);
	}

	// 初始化播放器
	public void initPlay() {

		// 跳到指定位置播放,一种是暂停位置，一种是拖动后的位置
		mediaPlayer.setOnSeekCompleteListener(new OnSeekCompleteListener() {

			@Override
			public void onSeekComplete(MediaPlayer mp) {

				Intent intent = new Intent();
				intent.setAction("seekComplete");
				if (secondPause != -1) {
					intent.putExtra("seek", false);
					intent.putExtra("pause", secondPause);
				} else {
					intent.putExtra("seek", Msg_Music.SEEK);
				}
				sendBroadcast(intent);

			}
		});

	}

	// 记录播放暂停位置
	public void recordSecondPause() {
		// 发送广播
		Intent intent = new Intent();
		intent.setAction("recordSecondPause");
		intent.putExtra("secondPause", mediaPlayer.getCurrentPosition());
		sendBroadcast(intent);
	}

	// 获取当前播放位置
	class GetPosition implements Runnable {

		@Override
		public void run() {
			Log.i("MusicDemo", "GetPosition started");

			int temp = 0;
			Intent intent = new Intent();
			intent.setAction("GetPosition");

			while (!stopThread) {

				// 播放是否结束
				if (temp / 1000 == mediaPlayer.getDuration() / 1000) {

					stopThread = true;
					MusicManager.setSeekPosition(-1);

					// 如果有通知，播放结束切换下一曲
					if (notifCreated) {
						if (modelServiceDao.getCurrentModel() != Play_Model.CYCLEONE) {
							next();
						} else {
							// 单曲循环，自动重播
							intent.putExtra("isEnd", true);
							sendBroadcast(intent);
						}
					} else {
						// 没有通知栏的情况，发送广播，让Activity作播放结束处理
						intent.putExtra("isEnd", true);
						sendBroadcast(intent);
					}

				} else {
					// 播放未结束或暂停，不断获取播放位置
					temp = mediaPlayer.getCurrentPosition();
					intent.putExtra("position", temp);
					intent.putExtra("isEnd", false);
					sendBroadcast(intent);
				}
			}
		}
	}

	@Override
	public void onDestroy() {
		// 关闭线程
		stopThread = true;
		// 释放资源
		if (mediaPlayer != null) {
			mediaPlayer.stop();
			mediaPlayer.release();
		}
		// 注销广播接收器
		unregisterReceiver(spRev);
		unregisterReceiver(notifRev);
		unregisterReceiver(cpRev);
		super.onDestroy();
	}

	/**
	 * 注册广播接收器
	 */
	public void initReceiver() {
		spRev = new SeekPositionReceiver();
		IntentFilter spfFilter = new IntentFilter();
		spfFilter.addAction("seekBar");
		registerReceiver(spRev, spfFilter);

		notifRev = new NotificationCreated();
		IntentFilter notifFilter = new IntentFilter();
		notifFilter.addAction("notification");
		registerReceiver(notifRev, notifFilter);

		cpRev = new CurrentPositonReceiver();
		IntentFilter cpFilter = new IntentFilter();
		cpFilter.addAction("GetPosition");
		registerReceiver(cpRev, cpFilter);

	}

	/**
	 * 广播接收器
	 * 
	 * @author chenfu
	 * 
	 */
	// 接收MainActivity传过来的seekPosition
	public class SeekPositionReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			secondPause = -1;
			// 跳转到拖动后的位置
			mediaPlayer.seekTo(intent.getIntExtra("seekPosition", -1));
		}
	}

	// 是否有通知栏
	public class NotificationCreated extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			notifCreated = intent.getBooleanExtra("notification", false);
		}
	}

	// 当有通知栏，且播放模式为单曲循环，播放结束，自动重播
	public class CurrentPositonReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getBooleanExtra("isEnd", false)) {
				play();
			}
		}
	}
}
