package com.whut.application;

import java.util.ArrayList;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.MediaStore;
import android.widget.RemoteViews;

import com.whut.entiy.Song;
import com.whut.music.R;
import com.whut.music.SongListActivity;
import com.whut.service.MyMusicService;
import com.whut.util.Msg_Music;
import com.whut.util.Play_Model;

public class MusicManager {

	private static ArrayList<Song> songList = new ArrayList<Song>();

	private static boolean notifMainToSong = false; // 从通知栏进入MainAty再返回SongAty的标志

	private static boolean isPlaying = false;
	private static int currentIndex = 0;
	private static int currentModel = Play_Model.CYCLEALL;

	private static int seekPosition = -1;

	private static boolean isServiceOpen = true;

	public static boolean isServiceOpen() {
		return isServiceOpen;
	}

	public static void setServiceOpen(boolean isServiceOpen) {
		MusicManager.isServiceOpen = isServiceOpen;
	}

	public static int getSeekPosition() {
		return seekPosition;
	}

	public static void setSeekPosition(int seekPosition) {
		MusicManager.seekPosition = seekPosition;
	}

	public static boolean isNotifMainToSong() {
		return notifMainToSong;
	}

	public static void setNotifMainToSong(boolean notifMainToSong) {
		MusicManager.notifMainToSong = notifMainToSong;
	}

	public static int getCurrentIndex() {
		return currentIndex;
	}

	public static void setCurrentIndex(int currentIndex) {
		MusicManager.currentIndex = currentIndex;
	}

	public static int getCurrentModel() {
		return currentModel;
	}

	public static void setCurrentModel(int currentModel) {
		MusicManager.currentModel = currentModel;
	}

	public static boolean isPlaying() {
		return isPlaying;
	}

	public static void setPlaying(boolean isPlaying) {
		MusicManager.isPlaying = isPlaying;
	}

	// 从媒体库中获取本机上的MP3文件
	public static ArrayList<Song> getSongsFromMediaDB(Context context) {

		songList.clear();

		Cursor cursor = context.getContentResolver().query(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null,
				MediaStore.Audio.Media.DEFAULT_SORT_ORDER);

		for (int i = 0; i < cursor.getCount(); i++) {
			Song song = new Song();
			cursor.moveToNext();
			long id = cursor.getLong(cursor
					.getColumnIndex(MediaStore.Audio.Media._ID));
			int duration = cursor.getInt(cursor
					.getColumnIndex(MediaStore.Audio.Media.DURATION));
			String mp3Path = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.DATA));
			String songName = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.TITLE));
			String singer = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.ARTIST));
			long size = cursor.getLong(cursor
					.getColumnIndex(MediaStore.Audio.Media.SIZE));

			int isMusic = cursor.getInt(cursor
					.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC));

			if (isMusic != 0 && size / 1000000 > 0) {
				song.setId(id);
				song.setDuration(duration);
				song.setMp3Path(mp3Path);
				song.setSongName(songName);
				song.setSinger(singer);
				song.setSize(size);
				songList.add(song);

				// Log.i("song_list", mp3Path + ";" + songName + ";" + singer);
			}

		}

		cursor.close();

		return songList;
	}

	/**
	 * 显示通知栏
	 * 
	 * @param id
	 *            通知的id
	 * @param context
	 * @param packageName
	 *            包名
	 * @param songName
	 *            通知栏显示的歌曲名
	 * @param singer
	 *            通知栏显示的歌手名
	 * @param intent
	 *            点击通知栏跳转到Aty的intent
	 */
	@SuppressWarnings("null")
	public static void musicNotification(int id, Context context,
			String packageName, Intent AtyIntent, Boolean isPlaying,
			int currentIndex, int currentModel) {

		NotificationManager manager = null;
		Notification notification = null;

		// 通知管理
		manager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);

		// 构建通知
		Notification.Builder builder = new Builder(context);
		builder.setSmallIcon(R.drawable.stat_notify_musicplayer);

		// 自定义通知栏布局
		RemoteViews remoteViews = new RemoteViews(packageName,
				R.layout.notification);
		// 设置通知栏的歌曲名和歌手名
		String songName = getSongsFromMediaDB(context).get(currentIndex)
				.getSongName();
		String singer = getSongsFromMediaDB(context).get(currentIndex)
				.getSinger();
		remoteViews.setTextViewText(R.id.notif_name, songName);
		remoteViews.setTextViewText(R.id.notif_singer, singer);

		/**
		 * 设置按钮点击事件
		 */
		// 播放按钮
		Intent intentPlayBtn = new Intent(context, MyMusicService.class);
		if (MusicManager.isPlaying()) {
			remoteViews
					.setImageViewResource(R.id.notif_pause, R.drawable.pause);
			intentPlayBtn.putExtra("msg", Msg_Music.PAUSE);
			intentPlayBtn.putExtra("secondPause", -1);
		} else {
			remoteViews
					.setImageViewResource(R.id.notif_pause, R.drawable.start);
			intentPlayBtn.putExtra("msg", Msg_Music.PLAY);
			intentPlayBtn.putExtra("secondPause", -1); // 需要改
		}
		intentPlayBtn.putExtra("other_music", false);
		intentPlayBtn.putExtra("currentModel", currentModel);
		intentPlayBtn.putExtra("currentIndex", currentIndex);
		PendingIntent playBtn = PendingIntent.getService(context, 1,
				intentPlayBtn, PendingIntent.FLAG_UPDATE_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.notif_pause, playBtn);
		// 下一曲按钮
		Intent intentNextBtn = new Intent(context, MyMusicService.class);
		intentNextBtn.putExtra("msg", Msg_Music.NEXT);
		intentNextBtn.putExtra("secondPause", -1);
		intentNextBtn.putExtra("other_music", true);
		intentNextBtn.putExtra("currentModel", currentModel);
		intentNextBtn.putExtra("currentIndex", currentIndex);
		PendingIntent nextBtn = PendingIntent.getService(context, 2,
				intentNextBtn, PendingIntent.FLAG_UPDATE_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.notif_next, nextBtn);
		// 上一曲按钮
		Intent intentPreBtn = new Intent(context, MyMusicService.class);
		intentPreBtn.putExtra("msg", Msg_Music.PRE);
		intentPreBtn.putExtra("secondPause", -1);
		intentPreBtn.putExtra("other_music", true);
		intentPreBtn.putExtra("currentModel", currentModel);
		intentPreBtn.putExtra("currentIndex", currentIndex);
		PendingIntent preBtn = PendingIntent.getService(context, 3,
				intentPreBtn, PendingIntent.FLAG_UPDATE_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.notif_previous, preBtn);

		/**
		 * 设置通栏的点击事件
		 */
		Intent[] intents = new Intent[2];
		// 创建activity栈的根activity
		Intent gotoSongListAty = new Intent(context, SongListActivity.class);
		gotoSongListAty.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		if (MusicManager.isPlaying) {
			isServiceOpen = true;
		} else {
			isServiceOpen = false;
		}

		intents[0] = gotoSongListAty;
		intents[1] = AtyIntent;

		PendingIntent pendingIntent = PendingIntent.getActivities(context, 0,
				intents, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(pendingIntent);

		/**
		 * 正在播放时，不可清除，否则可清除
		 */
		if (MusicManager.isPlaying()) {
			builder.setOngoing(true);
			// notification.flags = Notification.FLAG_ONGOING_EVENT;
			builder.setAutoCancel(false);
		} else {
			builder.setOngoing(false);
			builder.setAutoCancel(true); // 设置可以清除
		}

		notification = builder.build();
		// 这样才能显示自定义高度，不然是系统默认通知栏高度
		notification.bigContentView = remoteViews;
		// 更新通知栏
		manager.notify(id, notification);

		// 保存数据
		SongListActivity.getEditor().putInt("currentIndex", currentIndex);
		SongListActivity.getEditor().putInt("currentModel", currentModel);
		SongListActivity.getEditor().commit();

	}

}
