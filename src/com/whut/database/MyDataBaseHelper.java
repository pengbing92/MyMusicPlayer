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

	public MyDataBaseHelper(Context context) {
		super(context, "musicplayer.db", null, 1);
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
	}

	// 数据库更新
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		switch (oldVersion) {
		default:
		}

	}

}
