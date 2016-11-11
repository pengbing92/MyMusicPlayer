package com.whut.activitys;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.whut.adapter.MyFragmentAdapter;
import com.whut.fragment.LocalFragment;
import com.whut.fragment.OnlineFragment;
import com.whut.music.R;

/**
 * 程序主界面
 * 
 * @author chenfu
 * 
 */
public class MainActivity extends FragmentActivity implements OnClickListener,
		OnPageChangeListener {

	private TextView onlineTab;
	private TextView localTab;

	private ViewPager viewPager;

	private List<Fragment> fragmentList = new ArrayList<Fragment>();
	private OnlineFragment onlineFM;
	private LocalFragment localFM;
	private MyFragmentAdapter myFragmentAdapter;

	// 底部导航条
	private View tab_online;
	private View tab_local;
	
	// 按钮
	private ImageView searchBtn;
	private ImageView settingBtn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.activity_main);

		// 初始化组件
		initView();
		
	}

	public void initFragments() {

		onlineFM = new OnlineFragment();
		localFM = new LocalFragment();

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
		
		// 按钮
		searchBtn = (ImageView) findViewById(R.id.searchBtn);
		settingBtn = (ImageView) findViewById(R.id.setting);

		// 点击事件监听
		onlineTab.setOnClickListener(this);
		localTab.setOnClickListener(this);
		searchBtn.setOnClickListener(this);
		settingBtn.setOnClickListener(this);

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
		case R.id.searchBtn:
			Intent intent = new Intent(this, SearchActivity.class);
			startActivity(intent);
			break;
		case R.id.setting:
			// TODO
			break;

		default:
			break;
		}

	}

	@Override
	protected void onRestart() {
		Log.i("MainAty", "onRestart");
		// 更新UI
		LocalFragment.handler.sendEmptyMessage(0);
		super.onRestart();
	}

	@Override
	protected void onStop() {
		Log.i("MainAty", "onStop");
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		Log.i("MainAty", "onDestory");
		super.onDestroy();
	}

	

	/**
	 * state滑动中的状态 有三种状态（0，1，2） 1：正在滑动 2：滑动完毕 0：什么都没做。
	 */
	@Override
	public void onPageScrollStateChanged(int state) {

	}

	/**
	 * position :当前页面，即你点击滑动的页面 offset:当前页面偏移的百分比 offsetPixels:当前页面偏移的像素位置
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
