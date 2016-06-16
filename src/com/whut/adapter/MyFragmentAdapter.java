package com.whut.adapter;

import java.util.ArrayList;
import java.util.List;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;



public class MyFragmentAdapter extends FragmentPagerAdapter {

	private List<Fragment> fragmentList = new ArrayList<Fragment>();
	
	public MyFragmentAdapter(FragmentManager fm, List<Fragment> fragments) {
		super(fm);
		this.fragmentList = fragments;
	}

	@Override
	public Fragment getItem(int position) {
		return fragmentList.get(position);
	}

	@Override
	public int getCount() {
		return fragmentList.size();
	}

}
