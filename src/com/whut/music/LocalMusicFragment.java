package com.whut.music;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class LocalMusicFragment extends Fragment implements OnClickListener {

	private Context context;

	private TextView localContent;
	private RelativeLayout localBottom;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.fragment_local, container, false);
		return view;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		context = getActivity();

		localContent = (TextView) getActivity().findViewById(
				R.id.localFM_Content);
		localBottom = (RelativeLayout) getActivity().findViewById(
				R.id.bottomView);

		localContent.setOnClickListener(this);
		localBottom.setOnClickListener(this);

		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.bottomView:
			Intent gotoMainAty = new Intent(context, MainActivity.class);
			startActivity(gotoMainAty);
			break;
		case R.id.localFM_Content:
			Intent gotoSongListAty = new Intent(context, SongListActivity.class);
			startActivity(gotoSongListAty);
			break;
		default:
			break;
		}

	}

}
