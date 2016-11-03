package com.whut.database.service.imp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.whut.database.MyDataBaseHelper;
import com.whut.database.entiy.Play_Model;
import com.whut.database.entiy.Song;
import com.whut.database.service.SongService;
import com.whut.util.HanZi2PinYin;
import com.whut.util.PinyinComparator;

public class SongServiceDao implements SongService {

	private MyDataBaseHelper dbHelper;
	private SQLiteDatabase db;
	private Context context;
	
	// 根据首字母来排列ListView里面的数据类
	private PinyinComparator pinyinComparator;

	/**
	 * 不加static关键字，则在调用getNextSong或者getPreSong方法时 出现songNum的值为0的情况。
	 * 这是因为，调用addMusicList2DB的对象跟调用另外两个方法的对象不同， 加了static关键字，其他对象，才能共享songNum的改变。
	 */
	private static int songNum = 0; // 歌曲总数

	public SongServiceDao(Context context) {
		dbHelper = MyDataBaseHelper.getInstance(context);
		db = dbHelper.getWritableDatabase();
		this.context = context;
		pinyinComparator = new PinyinComparator();

	}

	// 扫描媒体库，将歌曲信息保存到数据库中
	@Override
	public void addMusicList2DB(List<Song> songList) {

		songNum = songList.size();

		for (Song song : songList) {
			// 数据库中已有的歌曲不用添加
			if (!isSongInList(song)) {
				db.beginTransaction();
				db.execSQL(
						"insert into music_list (m_id,m_dur,m_singer,m_name,m_path,m_size,m_album,m_album_id) "
								+ "values(?,?,?,?,?,?,?,?)",
						new String[] { song.getId() + "",
								song.getDuration() + "", song.getSinger(),
								song.getSongName(), song.getMp3Path(),
								song.getSize() + "", song.getAlbum(),
								song.getAlbumId() + "" });
				db.setTransactionSuccessful();
				db.endTransaction();
				Log.i("msg", "歌曲列表已保存到数据库中");
			}

		}
	}

	/**
	 * 如果current_song表不为空，则更新(先删除，再添加新的)，为空，则添加记录
	 */
	@Override
	public void updateCurrentSong(Song song) {
		db.beginTransaction();
		if (!isCurrentSongTbEmpty()) {
			db.execSQL("delete from current_song");
		}
		db.execSQL(
				"insert into current_song (m_id,m_dur,m_singer,m_name,m_path,m_size,m_album,m_album_id) "
						+ "values(?,?,?,?,?,?,?,?)",
				new String[] { song.getId() + "", song.getDuration() + "",
						song.getSinger(), song.getSongName(),
						song.getMp3Path(), song.getSize() + "",
						song.getAlbum(), song.getAlbumId() + "" });
		db.setTransactionSuccessful();
		db.endTransaction();

	}

	// 判断当前播放歌曲表是否为空
	private boolean isCurrentSongTbEmpty() {
		Cursor cursor = db.rawQuery("select * from current_song", null);
		if (cursor == null || cursor.getCount() == 0) {
			return true;
		}
		cursor.close();
		return false;
	}

	// 获取当前播放的歌曲
	@Override
	public Song getCurrentSong() {
		db.beginTransaction();
		Cursor cursor = db.rawQuery("select * from current_song", null);
		db.setTransactionSuccessful();
		db.endTransaction();

		// 错误写法 ： Song song = getSongByCursor(cursor);

		return getSongByCursor(cursor);
	}

	/**
	 * 歌曲是否已在数据库中
	 * 
	 * @param song
	 * @return
	 */
	public boolean isSongInList(Song song) {
		Cursor cursor = db.rawQuery("select * from music_list where m_id = ?",
				new String[] { song.getId() + "" });
		while (cursor.moveToNext()) {
			return true;
		}
		cursor.close();
		return false;
	}

	// 获取下一曲
	@Override
	public Song getNextSong(Song currentSong) {
		ModelServiceDao modelServiceDao = new ModelServiceDao(context);
		int current_model = modelServiceDao.getCurrentModel();

		db.beginTransaction();
		Cursor cursor = db.rawQuery("select * from music_list where m_id = ?",
				new String[] { currentSong.getId() + "" });

		int currentSong_id = -1; // 当前播放歌曲的sql_id
		if (cursor.moveToFirst()) {
			// 获取在数据库中自增的id，改变id，来获取下一曲
			currentSong_id = cursor.getInt(cursor.getColumnIndex("id"));
			Log.i("当前播放歌曲的sql_id:", currentSong_id + "");
		}

		int nextSong_id = -1; // 下一首歌曲的sql_id
		switch (current_model) {
		case Play_Model.CYCLEALL: // 列表循环
			nextSong_id = currentSong_id + 1;
			if (nextSong_id > songNum) { // 最后一首歌曲
				Log.i("测试", "歌曲总数是：" + songNum + "");
				Log.i("测试", "跳转到第一首歌曲");
				nextSong_id = 1;
			}
			break;
		case Play_Model.CYCLEONE: // 点击按钮，单曲循环也切换下一曲
			nextSong_id = currentSong_id + 1;
			if (nextSong_id > songNum) { // 最后一首歌曲
				nextSong_id = 1;
			}
			break;
		case Play_Model.RANDOM: // 随机播放
			do {
				nextSong_id = getRand_Id();
			} while (nextSong_id == 0 || nextSong_id == currentSong_id);
			break;
		default:
		}

		Log.i("下一首歌曲的sql_id:", nextSong_id + "");
		cursor = db.rawQuery("select * from music_list where id = ?",
				new String[] { nextSong_id + "" });
		db.setTransactionSuccessful();
		db.endTransaction();

		return getSongByCursor(cursor);
	}

	// 生成一个随机数
	private int getRand_Id() {
		Random random = new Random();
		int rand_id = random.nextInt(songNum);

		return rand_id;
	}

	// 获取上一曲
	@Override
	public Song getPreSong(Song currentSong) {
		ModelServiceDao modelServiceDao = new ModelServiceDao(context);
		int current_model = modelServiceDao.getCurrentModel();

		db.beginTransaction();
		Cursor cursor = db.rawQuery("select * from music_list where m_id = ?",
				new String[] { currentSong.getId() + "" });

		int currentSong_id = -1; // 当前播放歌曲的sql_id
		if (cursor.moveToFirst()) {
			// 获取在数据库中自增的id，改变id，来获取下一曲
			currentSong_id = cursor.getInt(cursor.getColumnIndex("id"));
			Log.i("当前播放歌曲的sql_id:", currentSong_id + "");
		}

		int preSong_id = -1; // 下一首歌曲的sql_id
		switch (current_model) {
		case Play_Model.CYCLEALL: // 列表循环
			preSong_id = currentSong_id - 1;
			if (preSong_id < 1) { // 第一首歌曲
				preSong_id = songNum;
			}
			break;
		case Play_Model.CYCLEONE: // 点击按钮，单曲循环也切换上一曲
			preSong_id = currentSong_id - 1;
			if (preSong_id < 1) { // 第一首歌曲
				preSong_id = songNum;
			}
			break;
		case Play_Model.RANDOM: // 随机播放
			do {
				preSong_id = getRand_Id();
			} while (preSong_id == 0 || preSong_id == currentSong_id);
			break;
		default:
		}

		cursor = db.rawQuery("select * from music_list where id = ?",
				new String[] { preSong_id + "" });
		db.setTransactionSuccessful();
		db.endTransaction();

		return getSongByCursor(cursor);
	}

	@SuppressWarnings("null")
	private Song getSongByCursor(Cursor cursor) {

		Song song = new Song();
		if (cursor != null || cursor.getCount() > 0) {
			if (cursor.moveToFirst()) {

				song.setId(cursor.getLong(cursor.getColumnIndex("m_id")));
				song.setDuration(cursor.getInt(cursor.getColumnIndex("m_dur")));
				song.setSinger(cursor.getString(cursor
						.getColumnIndex("m_singer")));
				song.setSongName(cursor.getString(cursor
						.getColumnIndex("m_name")));
				song.setMp3Path(cursor.getString(cursor
						.getColumnIndex("m_path")));
				song.setSize(cursor.getLong(cursor.getColumnIndex("m_size")));
				song.setAlbum(cursor.getString(cursor.getColumnIndex("m_album")));
				song.setAlbumId(cursor.getInt(cursor
						.getColumnIndex("m_album_id")));
			}
		}
		cursor.close();
		return song;
	}

	@Override
	public Song getFirstSong() {
		db.beginTransaction();
		Cursor cursor = db.rawQuery("select * from music_list where id=?",
				new String[] { 1 + "" });
		db.setTransactionSuccessful();
		db.endTransaction();

		return getSongByCursor(cursor);

	}

	/**
	 * 启动播放器后，根据上次退出时保存的m_id 在数据库中查询歌曲，并返回
	 */
	@Override
	public Song getSongById(long m_id) {

		db.beginTransaction();
		Cursor cursor = db.rawQuery("select * from music_list where m_id=?",
				new String[] { m_id + "" });
		db.setTransactionSuccessful();
		db.endTransaction();

		return getSongByCursor(cursor);
	}

	@Override
	public List<Song> getAllSong() {
		List<Song> songList = new ArrayList<Song>();
		db.beginTransaction();
		Cursor cursor = db.rawQuery("select * from music_list", null);
		while (cursor.moveToNext()) {
			Song song = new Song();
			song.setId(cursor.getLong(cursor.getColumnIndex("m_id")));
			song.setDuration(cursor.getInt(cursor.getColumnIndex("m_dur")));
			song.setSinger(cursor.getString(cursor.getColumnIndex("m_singer")));
			song.setSongName(cursor.getString(cursor.getColumnIndex("m_name")));
			song.setMp3Path(cursor.getString(cursor.getColumnIndex("m_path")));
			song.setSize(cursor.getLong(cursor.getColumnIndex("m_size")));
			song.setAlbum(cursor.getString(cursor.getColumnIndex("m_album")));
			song.setAlbumId(cursor.getInt(cursor.getColumnIndex("m_album_id")));
			// 设置首字母
			song.setFirstLetter(getFirstLetter(song.getSongName()));
			
			songList.add(song);
			Log.i("songList_inDB",
					cursor.getString(cursor.getColumnIndex("m_name")));
		}
		cursor.close();
		db.setTransactionSuccessful();
		db.endTransaction();
		
		sortedSongList(songList);

		return songList;
	}

	// 得到歌曲名首字母(大写)
	@SuppressLint("DefaultLocale")
	private String getFirstLetter(String songName) {

		String firstLetter = "";

		firstLetter = HanZi2PinYin.getPinYin(songName.substring(0, 1))
				.substring(0, 1).toUpperCase();

		if (firstLetter == null) {
			firstLetter = "#";
		}

		return firstLetter;
	}
	
	// 按照歌曲名首字母进行排序
	private void sortedSongList(List<Song> songList) {
		
		Collections.sort(songList, pinyinComparator);
		
		for (int i=0;i<songList.size();i++) {
			System.out.println(songList.get(i).getFirstLetter());
		}
		
	}

	// 删除
	@Override
	public void deleteSongById(long m_id) {
		// TODO Auto-generated method stub

	}

}
