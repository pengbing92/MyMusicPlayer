package com.whut.music;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.TextView;

import com.whut.adapter.MyFragmentAdapter;
import com.whut.database.service.imp.ModelServiceDao;
import com.whut.database.service.imp.SongServiceDao;
import com.whut.fragment.LocalMusicFragment;
import com.whut.fragment.OnlineMusicFragment;

public class MainActivity extends FragmentActivity implements OnClickListener,
		OnPageChangeListener {

	private TextView onlineTab;
	private TextView localTab;

	private ViewPager viewPager;

	private List<Fragment> fragmentList = new ArrayList<Fragment>();
	private OnlineMusicFragment onlineFM;
	private LocalMusicFragment localFM;
	private MyFragmentAdapter myFragmentAdapter;

	// 底部导航条
	private View tab_online;
	private View tab_local;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);

		// 初始化组件
		initView();

	}

	public void initFragments() {

		onlineFM = new OnlineMusicFragment();
		localFM = new LocalMusicFragment();

		fragmentList.add(onlineFM);
		fragmentList.add(localFM);

		myFragmentAdapter = new MyFragmentAdapter(getSupportFragmentManager(),
				fragmentList);

	}

	// 初始化view，并设置监听器
	public void initView() {

		// Tab标签
		onlineTab = (TextView) findViewById(R.id.onlineTab);
		localTab = (TextView) findViewById(R.id.localTab);

		onlineTab.setOnClickListener(this);
		localTab.setOnClickListener(this);

		initFragments();

		viewPager = (ViewPager) findViewById(R.id.viewPager);
		viewPager.addOnPageChangeListener(this);

		viewPager.setAdapter(myFragmentAdapter);
		viewPager.setCurrentItem(0);

		// 导航条
		tab_online = (View) findViewById(R.id.tab_selected_0);
		tab_local = (View) findViewById(R.id.tab_selected_1);

	}

	/**
	 * 单击事件
	 */
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.onlineTab:
			viewPager.setCurrentItem(0);
			break;
		case R.id.localTab:
			viewPager.setCurrentItem(1);
			break;

		default:
			break;
		}

	}
	
	@Override
	protected void onRestart() {
		Log.i("MainAty", "onRestart");
		// 更新UI
		LocalMusicFragment.handler.sendEmptyMessage(0);
		super.onRestart();
	}

	@Override
	protected void onStop() {
		Log.i("CFAty", "onStop");
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		Log.i("CFAty", "onDestory");
		super.onDestroy();
	}

	/**
	 * state滑动中的状态 有三种状态（0，1，2） 1：正在滑动 2：滑动完毕 0：什么都没做。
	 */
	@Override
	public void onPageScrollStateChanged(int state) {

	}

	/**
	 * position :当前页面，及你点击滑动的页面 offset:当前页面偏移的百分比 offsetPixels:当前页面偏移的像素位置
	 */
	@Override
	public void onPageScrolled(int position, float offset, int offsetPixels) {

	}

	@Override
	public void onPageSelected(int arg0) {
		switch (arg0) {
		case 0:
			tab_online.setVisibility(View.VISIBLE);
			tab_local.setVisibility(View.GONE);
			break;
		case 1:
			tab_online.setVisibility(View.GONE);
			tab_local.setVisibility(View.VISIBLE);
			break;

		default:
			break;
		}
	}

}
