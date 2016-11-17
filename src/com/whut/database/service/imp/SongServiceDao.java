package com.whut.database.service.imp;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.whut.database.MyDataBaseHelper;
import com.whut.database.entiy.Play_Model;
import com.whut.database.entiy.Song;
import com.whut.database.service.SongService;

public class SongServiceDao implements SongService {

	private MyDataBaseHelper dbHelper;
	private SQLiteDatabase db;
	private Context context;

	private static final String TAG = SongServiceDao.class.getName();

	/**
	 * 不加static关键字，则在调用getNextSong或者getPreSong方法时 出现songNum的值为0的情况。
	 * 这是因为，调用addMusicList2DB的对象跟调用另外两个方法的对象不同， 加了static关键字，其他对象，才能共享songNum的改变。
	 */
	private static int songNum = 0; // 歌曲总数

	public SongServiceDao(Context context) {
		dbHelper = MyDataBaseHelper.getInstance(context);
		db = dbHelper.getWritableDatabase();
		this.context = context;

	}

	/**
	 * 将歌曲信息保存到数据库中： 当媒体库中新增了歌曲或者删除了歌曲或者歌曲名改变了，不能排序。 此处采用的办法是，先删除表，再重新建表
	 */
	@Override
	public void addMusicList2DB(List<Song> songList) {

		songNum = songList.size();

		Log.i(TAG, "songNum is " + songNum);

		if (isMusicListTbEmpty()) { // 第一次启动程序
			Log.i(TAG, "第一次启动应用");
			insert2MusicList(songList);
		} else {
			if (songList.size() == getMusicCountInDB()) {
				// TODO 检查歌曲名是否更改
				for (Song song : songList) {
					if (!isSongInDB(song)) {
						/**
						 * 媒体库中的歌曲名已更改
						 * (经测试，即使修改了文件名，媒体库中的歌曲名并未修改，其他播放器也是一样的)
						 */
						Log.i(TAG, song.getSongName().split("\\(")[0]
								+ "不在数据库中");
						db.execSQL("drop table if exists music_list");
						db.execSQL(MyDataBaseHelper.CREATE_MUSIC_LIST);
						insert2MusicList(songList);
						break;
					}
				}
			} else { // 媒体库中新增或者删除了歌曲，都要更新表
				db.execSQL("drop table if exists music_list");
				db.execSQL(MyDataBaseHelper.CREATE_MUSIC_LIST);
				insert2MusicList(songList);
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
	 * 根据id查找歌曲是否已在数据库中，且歌曲名是否更改
	 * 
	 * @param song
	 * @return
	 */
	public boolean isSongInDB(Song song) {
		Cursor cursor = db.rawQuery("select * from music_list where m_id = ?",
				new String[] { song.getId() + "" });
		while (cursor.moveToNext()) {
			String nameInDB = cursor.getString(cursor.getColumnIndex("m_name"));
			if (song.getSongName().equals(nameInDB)) { // 歌曲名未更改
				return true;
			}
		}
		cursor.close();
		return false;

	}

	/**
	 * 获取下一曲
	 */
	@Override
	public Song getNextSong(Song currentSong) {
		ModelServiceDao modelServiceDao = new ModelServiceDao(context);
		int current_model = modelServiceDao.getCurrentModel();

		db.beginTransaction();
		Cursor cursor = db.rawQuery("select * from music_list where m_id = ?",
				new String[] { currentSong.getId() + "" });

		int currentPos = -1; // 当前播放歌曲的sql_id
		if (cursor.moveToFirst()) {
			// 获取在数据库中自增的id，改变id，来获取下一曲
			currentPos = cursor.getInt(cursor.getColumnIndex("id"));
			Log.i("当前播放歌曲的sql_id:", currentPos + "");
		}
		// int currentPos = songList.indexOf(currentSong);

		int nextSong_id = -1; // 下一首歌曲的sql_id
		switch (current_model) {
		case Play_Model.CYCLEALL: // 列表循环
			nextSong_id = currentPos + 1;
			if (nextSong_id > songNum) { // 最后一首歌曲
				Log.i("测试", "歌曲总数是：" + songNum + "");
				Log.i("测试", "跳转到第一首歌曲");
				nextSong_id = 1;
			}
			break;
		case Play_Model.CYCLEONE: // 点击按钮，单曲循环也切换下一曲
			nextSong_id = currentPos + 1;
			if (nextSong_id > songNum) { // 最后一首歌曲
				nextSong_id = 1;
			}
			break;
		case Play_Model.RANDOM: // 随机播放
			do {
				nextSong_id = getRand_Id();
			} while (nextSong_id == 0 || nextSong_id == currentPos);
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

	/**
	 * 获取上一曲
	 */
	@Override
	public Song getPreSong(Song currentSong) {
		ModelServiceDao modelServiceDao = new ModelServiceDao(context);
		int current_model = modelServiceDao.getCurrentModel();

		db.beginTransaction();
		Cursor cursor = db.rawQuery("select * from music_list where m_id = ?",
				new String[] { currentSong.getId() + "" });

		int currentPos = -1; // 当前播放歌曲的sql_id
		if (cursor.moveToFirst()) {
			// 获取在数据库中自增的id，改变id，来获取下一曲
			currentPos = cursor.getInt(cursor.getColumnIndex("id"));
			Log.i("当前播放歌曲的sql_id:", currentPos + "");
		}

		// int currentPos = songList.indexOf(currentSong);

		int preSong_id = -1; // 下一首歌曲的sql_id
		switch (current_model) {
		case Play_Model.CYCLEALL: // 列表循环
			preSong_id = currentPos - 1;
			if (preSong_id < 1) { // 第一首歌曲
				preSong_id = songNum;
			}
			break;
		case Play_Model.CYCLEONE: // 点击按钮，单曲循环也切换上一曲
			preSong_id = currentPos - 1;
			if (preSong_id < 1) { // 第一首歌曲
				preSong_id = songNum;
			}
			break;
		case Play_Model.RANDOM: // 随机播放
			do {
				preSong_id = getRand_Id();
			} while (preSong_id == 0 || preSong_id == currentPos);
			break;
		default:
		}

		cursor = db.rawQuery("select * from music_list where id = ?",
				new String[] { preSong_id + "" });
		db.setTransactionSuccessful();
		db.endTransaction();

		return getSongByCursor(cursor);
	}

	// 生成一个随机数
	private int getRand_Id() {
		Random random = new Random();
		int rand_id = random.nextInt(songNum + 1);

		return rand_id;
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

	private List<Song> getSongListByCursor(Cursor cursor) {
		List<Song> songList = new ArrayList<Song>();

		if (cursor != null && cursor.getCount() > 0) {
			while (cursor.moveToNext()) {
				Song song = new Song();
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
				song.setFirstLetter(cursor.getString(cursor
						.getColumnIndex("first_letter")));

				songList.add(song);

			}
		}
		cursor.close();
		return songList;
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

	/**
	 * 读取数据库中保存的歌曲列表
	 */
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
			song.setFirstLetter(cursor.getString(cursor
					.getColumnIndex("first_letter")));

			songList.add(song);

		}
		cursor.close();
		db.setTransactionSuccessful();
		db.endTransaction();

		return songList;
	}

	// 根据歌曲名检索歌曲
	@Override
	public Song getSongByName(String songName) {
		Song song = new Song();

		Cursor cursor = db.query("music_list", null, "m_name = ?",
				new String[] { songName }, null, null, null);
		song = getSongByCursor(cursor);

		return song;

	}

	/**
	 * 模糊查询 查询歌曲名或者歌手名中包含关键字的记录
	 */
	@Override
	public List<Song> getSongs(String str) {
		List<Song> songList = new ArrayList<Song>();
		Cursor cursor = db.query("music_list", null,
				"m_name like ? or m_singer like ?", new String[] {
						"%" + str + "%", "%" + str + "%" }, null, null, null);
		songList = getSongListByCursor(cursor);
		Log.i(TAG, "模糊查询结果记录有" + songList.size() + "条");
		return songList;
	}

	// 判断当前播放歌曲表是否为空
	@Override
	public boolean isCurrentSongTbEmpty() {
		Cursor cursor = db.rawQuery("select * from current_song", null);
		if (cursor == null || cursor.getCount() == 0) {
			return true;
		}
		cursor.close();
		return false;
	}

	// 判断歌曲表是否为空
	@Override
	public boolean isMusicListTbEmpty() {
		if (getMusicCountInDB() > 0) {
			return false;
		}
		return true;
	}

	// 添加记录到歌曲表
	@Override
	public void insert2MusicList(List<Song> songList) {
		for (Song song : songList) {
			db.beginTransaction();
			db.execSQL(
					"insert into music_list (m_id,m_dur,m_singer,m_name,m_path,m_size,m_album,m_album_id,first_letter) "
							+ "values(?,?,?,?,?,?,?,?,?)",
					new String[] { song.getId() + "", song.getDuration() + "",
							song.getSinger(), song.getSongName(),
							song.getMp3Path(), song.getSize() + "",
							song.getAlbum(), song.getAlbumId() + "",
							song.getFirstLetter() });
			db.setTransactionSuccessful();
			db.endTransaction();
		}

	}

	// 获取数据库中歌曲数目
	@Override
	public int getMusicCountInDB() {
		Cursor cursor = db.rawQuery("select * from music_list", null);
		if (cursor != null) {
			Log.i(TAG, "数据库中有" + cursor.getCount() + "首歌");
			cursor.close();
			return cursor.getCount();
		}

		return 0;
	}

}
