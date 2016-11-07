package com.whut.fragment;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.whut.database.entiy.Song;
import com.whut.database.service.imp.SongServiceDao;
import com.whut.music.R;
import com.whut.util.ImageUtil;

public class AlbumFragment extends Fragment {

	private Context context;
	private SongServiceDao songServiceDao;

	private Song currentSong;

	private MyBroadcastReceiver myreceiver;

	private ImageView imageView;

	private ImageUtil imageUtil;

	@SuppressLint("HandlerLeak")
	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case 0:
				initAlbum();
				break;

			default:
				break;
			}
		};
	};

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		context = getActivity();
		currentSong = new Song();
		imageUtil = ImageUtil.getInstance(context);

		songServiceDao = new SongServiceDao(context);
		currentSong = songServiceDao.getCurrentSong();

		myreceiver = new MyBroadcastReceiver();

		IntentFilter ssFilter = new IntentFilter();
		ssFilter.addAction("switchSong");
		context.registerReceiver(myreceiver, ssFilter);

		super.onCreate(savedInstanceState);
	}

	@Override
	@Nullable
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_album, container, false);
		return view;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		imageView = (ImageView) getActivity().findViewById(R.id.showAlbum);
		initAlbum();
		super.onActivityCreated(savedInstanceState);
	}

	// 展示专辑封面图片
	private void initAlbum() {
		Bitmap bitmap = imageUtil.getLargeArtwork(currentSong.getAlbumId());
		if (bitmap == null) {
			bitmap = imageUtil.getDefaultArtWork();
		}
		imageView.setImageBitmap(bitmap);

	}

	@Override
	public void onDestroy() {
		// 注销广播接收器
		context.unregisterReceiver(myreceiver);
		super.onDestroy();
	}

	public class MyBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals("switchSong")) {
				currentSong = songServiceDao.getCurrentSong();
				handler.sendEmptyMessage(0);
			}
		}
	}

}
