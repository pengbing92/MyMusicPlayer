package com.whut.music;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

import com.whut.application.MusicManager;
import com.whut.database.entiy.Song;
import com.whut.database.service.imp.SongServiceDao;
import com.whut.util.ToastUtil;

public class SearchActivity extends Activity implements OnClickListener,
		TextWatcher, OnItemClickListener {

	private static final String TAG = SearchActivity.class.getName();

	private ImageView backBtn;
	private ImageView searchBtn;
	private ImageView clearBtn;
	private EditText searchEt;

	private SongServiceDao songServiceDao;

	private ListView searchList;

	// 查询结果列表
	private List<Song> resultList = new ArrayList<Song>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_search);

		// 初始化数据
		initData();

		// 初始化组件
		initView();
	}

	private void initData() {
		songServiceDao = new SongServiceDao(this);
	}

	private void initView() {
		backBtn = (ImageView) findViewById(R.id.back);
		searchBtn = (ImageView) findViewById(R.id.searchBtn);
		clearBtn = (ImageView) findViewById(R.id.clearBtn);
		searchEt = (EditText) findViewById(R.id.search_content);

		searchList = (ListView) findViewById(R.id.searchList);

		backBtn.setOnClickListener(this);
		searchBtn.setOnClickListener(this);
		clearBtn.setOnClickListener(this);
		searchEt.addTextChangedListener(this);

		searchList.setOnItemClickListener(this);

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.back:
			finish();
			break;
		case R.id.searchBtn:
			search();
			break;
		case R.id.clearBtn:
			clearText();
			break;
		default:
			break;
		}

	}

	/**
	 * 点击搜索按钮查询
	 */
	private void search() {
		/**
		 * TODO 本来应该显示几个Fragment，此处为了方便，直接显示在搜索框下面
		 */
		String searchStr = searchEt.getText().toString().trim();
		Song song = new Song();
		song = songServiceDao.getSongByName(searchStr);
		if (song.getSongName() != null) {
			ToastUtil.toastInfo(this, song.getSongName());
			showSearchResult(song);
		} else {
			Log.i(TAG, "检索无结果..");
		}
	}

	// 显示搜索结果
	private void showSearchResult(Song song) {
		resultList.clear();
		resultList.add(song);
		searchList.setAdapter(new ArrayAdapter<Song>(this,
				android.R.layout.simple_expandable_list_item_1, resultList));
	}

	private void clearText() {
		searchEt.setText("");
		clearBtn.setVisibility(View.GONE);
	}

	/**
	 * 监听输入框变化，当有输入，显示清除按钮
	 */
	@Override
	public void afterTextChanged(Editable s) { // 展示模糊查询结果
		resultList = songServiceDao.getSongs(s.toString().trim());
		searchList.setAdapter(new ArrayAdapter<Song>(this,
				android.R.layout.simple_expandable_list_item_1, resultList));
		
		if (s.toString().trim().equals("")) { // 输入框中内容被清除
			resultList.clear();
			clearBtn.setVisibility(View.GONE);
		}

	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		clearBtn.setVisibility(View.GONE);
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		clearBtn.setVisibility(View.VISIBLE);
		
	}

	/**
	 * listView点击事件
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		// 更新当前播放歌曲
		songServiceDao.updateCurrentSong(resultList.get(position));
		// 更新当前播放状态
		MusicManager.setPlaying(true);
		// 进入歌词界面
		enterLrcAty();
	}

	private void enterLrcAty() {
		Intent intent = new Intent(this, LrcActivity.class);
		intent.putExtra("isPlaying", MusicManager.isPlaying());
		intent.putExtra("fromNotification", false);
		startActivity(intent);
		finish();
	}

}
