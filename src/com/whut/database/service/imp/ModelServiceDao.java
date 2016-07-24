package com.whut.database.service.imp;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.whut.database.MyDataBaseHelper;
import com.whut.database.entiy.Play_Model;
import com.whut.database.service.ModelService;

public class ModelServiceDao implements ModelService {

	private MyDataBaseHelper myDataBaseHelper;
	private SQLiteDatabase db;

	public ModelServiceDao(Context context) {
		myDataBaseHelper = MyDataBaseHelper.getInstance(context);
		db = myDataBaseHelper.getWritableDatabase();
	}

	/**
	 * 更新当前播放模式，默认值是列表循环
	 */
	@Override
	public void updateCurrentModel(int current_model) {
		db.beginTransaction();
		if (!isTableEmpty()) {
			db.execSQL("delete from current_model");
		}
		db.execSQL("insert into current_model (play_model) values(?)",
				new String[] { current_model + "" });
		db.setTransactionSuccessful();
		db.endTransaction();

	}

	// 判断表是否为空
	private boolean isTableEmpty() {
		Cursor cursor = db.rawQuery("select * from current_model", null);
		if (cursor == null || cursor.getCount() == 0) {
			return true;
		}
		return false;
	}

	// 获取当前播放模式
	@Override
	public int getCurrentModel() {
		db.beginTransaction();
		Cursor cursor = db.rawQuery("select * from current_model", null);

		int current_model = Play_Model.CYCLEALL;
		// cursor下标是从-1开始的,所以必须加上如下判断条件，否则会下标抛出异常
		if (cursor.moveToFirst()) {
			current_model = cursor.getInt(cursor.getColumnIndex("play_model"));
		}
		
		db.setTransactionSuccessful();
		db.endTransaction();

		return current_model;
	}

}
