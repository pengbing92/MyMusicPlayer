package com.whut.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * SQLite数据库
 * 
 * @author chenfu
 * 
 */
public class MyDataBaseHelper extends SQLiteOpenHelper {

	private static MyDataBaseHelper instance;

	// 歌曲列表Table
	private static final String CREATE_MUSIC_LIST = "create table if not exists music_list (id integer "
			+ "primary key autoincrement, m_id integer, m_dur integer, m_singer varchar(255), "
			+ "m_name varchar(255), m_path varchar(255), m_size integer, m_album varchar(255), "
			+ "m_album_id integer)";

	// 当前播放歌曲Table
	private static final String CREATE_CURRENT_SONG = "create table if not exists current_song (m_id integer, "
			+ "m_dur integer, m_singer varchar(255), m_name varchar(255), m_path varchar(255), m_size integer, "
			+ "m_album varchar(255), m_album_id integer)";
	
	// 当前播放模式Table
	private static final String CREATE_CURRENT_MODEL = "create table if not exists current_model (id integer "
			+ "primary key autoincrement, play_model integer)";
	
	// 拼音对照表
	private static final String CREATE_PINYIN_TABLE = "create table if not exists pinyin (m_index integer, " +
			"m_pinyin varchar(255))";
	
	// 汉字对照表
	private static final String CREATE_HANZI_TABLE = "create table if not exists hanzi (m_byte1 integer, " +
			"m_byte2 integer, m_index integer)";
	
	// 给歌曲列表Table新增一列first_letter
	private static final String ADD_COLUMN_FIRSTLETTER = "alter table music_list add column first_letter varchar(255)";

	public MyDataBaseHelper(Context context) {
		super(context, "musicplayer.db", null, 3);
	}

	public static MyDataBaseHelper getInstance(Context context) {
		if (instance == null) {
			synchronized (MyDataBaseHelper.class) {
				if (instance == null) {
					instance = new MyDataBaseHelper(context);
				}
			}
		}
		return instance;

	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_MUSIC_LIST);
		db.execSQL(CREATE_CURRENT_SONG);
		db.execSQL(CREATE_CURRENT_MODEL);
		db.execSQL(CREATE_PINYIN_TABLE);
		db.execSQL(CREATE_HANZI_TABLE);
		db.execSQL(ADD_COLUMN_FIRSTLETTER);
	}

	// 数据库更新
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		switch (oldVersion) {
		case 1:
			db.execSQL(CREATE_PINYIN_TABLE);
			db.execSQL(CREATE_HANZI_TABLE);
		case 2:
			db.execSQL(ADD_COLUMN_FIRSTLETTER);
		default:
		}

	}

}
