package com.whut.application;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.RemoteViews;

import com.whut.activitys.MainActivity;
import com.whut.database.entiy.Song;
import com.whut.database.service.imp.SongServiceDao;
import com.whut.fragment.LocalFragment;
import com.whut.music.R;
import com.whut.service.MyMusicService;
import com.whut.util.HanZi2PinYin;
import com.whut.util.Msg_Music;

/**
 * 歌曲综合管理
 * 
 * @author chenfu
 * 
 */
public class MusicManager {

	// 点击通知栏进入LrcAty，再返回SongAty的标志
	private static boolean notifMainToSong = false;

	private static boolean isPlaying = false;

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

	public static boolean isPlaying() {
		return isPlaying;
	}

	public static void setPlaying(boolean isPlaying) {
		MusicManager.isPlaying = isPlaying;
	}

	/**
	 * 从媒体库中获取本机上的MP3文件
	 * 
	 * @param context
	 * @return 歌曲列表
	 */
	public static List<Song> getSongsFromMediaDB(Context context) {

		List<Song> songList = new ArrayList<Song>();

		// 扫描媒体库中的音乐文件
		Cursor cursor = context.getContentResolver().query(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				new String[] { MediaStore.Audio.Media.TITLE,
						MediaStore.Audio.Media.DURATION,
						MediaStore.Audio.Media.ARTIST,
						MediaStore.Audio.Media._ID,
						MediaStore.Audio.Media.DATA,
						MediaStore.Audio.Media.ALBUM_ID,
						MediaStore.Audio.Media.DISPLAY_NAME,
						MediaStore.Audio.Media.SIZE,
						MediaStore.Audio.Media.ALBUM }, null, null, null);

		for (int i = 0; i < cursor.getCount(); i++) {
			Song song = new Song();
			cursor.moveToNext();
			String songName = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.TITLE));
			int duration = cursor.getInt(cursor
					.getColumnIndex(MediaStore.Audio.Media.DURATION));
			String singer = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.ARTIST));
			long id = cursor.getLong(cursor
					.getColumnIndex(MediaStore.Audio.Media._ID));
			String mp3Path = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.DATA));
			long albumId = cursor.getInt(cursor
					.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
			long size = cursor.getLong(cursor
					.getColumnIndex(MediaStore.Audio.Media.SIZE));
			String album = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.ALBUM));

			if (size / 1000000 > 0) {

				song.setId(id);
				song.setDuration(duration);
				song.setMp3Path(mp3Path);
				song.setSongName(songName);
				song.setSinger(singer);
				song.setSize(size);
				song.setAlbum(album);
				song.setAlbumId(albumId);

				// 设置首字母
				song.setFirstLetter(getFirstLetter(song.getSongName()));

				// System.out.println(songName);

				songList.add(song);
			}

		}

		Log.i("song_num", "媒体库中包含" + songList.size() + "首歌曲");
		cursor.close();

		return songList;
	}

	/**
	 * 显示通知栏
	 * 
	 * @param id
	 * @param context
	 * @param packageName
	 * @param AtyIntent
	 * @param isPlaying
	 * @param currentId
	 * @param currentModel
	 */
	public static void musicNotification(int id, Context context,
			String packageName, Intent AtyIntent, Boolean isPlaying,
			long currentId, int currentModel) {

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
		Song currentSong = new Song();
		SongServiceDao songServiceDao = new SongServiceDao(context);
		currentSong = songServiceDao.getCurrentSong();
		String songName = currentSong.getSongName();
		String singer = currentSong.getSinger();
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
		PendingIntent playBtn = PendingIntent.getService(context, 1,
				intentPlayBtn, PendingIntent.FLAG_UPDATE_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.notif_pause, playBtn);
		// 下一曲按钮
		Intent intentNextBtn = new Intent(context, MyMusicService.class);
		intentNextBtn.putExtra("msg", Msg_Music.NEXT);
		intentNextBtn.putExtra("secondPause", -1);
		intentNextBtn.putExtra("other_music", true);
		PendingIntent nextBtn = PendingIntent.getService(context, 2,
				intentNextBtn, PendingIntent.FLAG_UPDATE_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.notif_next, nextBtn);
		// 上一曲按钮
		Intent intentPreBtn = new Intent(context, MyMusicService.class);
		intentPreBtn.putExtra("msg", Msg_Music.PRE);
		intentPreBtn.putExtra("secondPause", -1);
		intentPreBtn.putExtra("other_music", true);
		PendingIntent preBtn = PendingIntent.getService(context, 3,
				intentPreBtn, PendingIntent.FLAG_UPDATE_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.notif_previous, preBtn);

		/**
		 * 设置通栏的点击事件
		 */
		Intent[] intents = new Intent[2];
		// 创建activity栈的根activity
		Intent gotoMainAty = new Intent(context, MainActivity.class);
		gotoMainAty.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		if (MusicManager.isPlaying) {
			isServiceOpen = true;
		} else {
			isServiceOpen = false;
		}

		intents[0] = gotoMainAty;
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
		LocalFragment.getEditor().putLong("currentId", currentId);
		LocalFragment.getEditor().putInt("currentModel", currentModel);
		LocalFragment.getEditor().commit();

	}

	// 得到歌曲名首字母(大写)
	@SuppressLint("DefaultLocale")
	private static String getFirstLetter(String songName) {

		String firstLetter = "";

		firstLetter = HanZi2PinYin.getPinYin(songName.substring(0, 1))
				.substring(0, 1).toUpperCase();

		if (firstLetter == null) {
			firstLetter = "#";
		}

		return firstLetter;
	}

}
