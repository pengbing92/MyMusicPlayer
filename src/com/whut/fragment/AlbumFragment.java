package com.whut.fragment;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader.TileMode;
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
		imageView.setImageBitmap(createReflectedImage(bitmap));

	}

	@Override
	public void onDestroy() {
		// 注销广播接收器
		context.unregisterReceiver(myreceiver);
		super.onDestroy();
	}
	
	/**
	 * 网上找的方法，实现图片倒影效果
	 * 
	 * @param originalImage
	 * @return
	 */
	public Bitmap createReflectedImage(Bitmap originalImage)
	{
		int width = originalImage.getWidth();
		int height = originalImage.getHeight();
		Matrix matrix = new Matrix();
		// 实现图片翻转90度
		matrix.preScale(1, -1);
		// 创建倒影图片（是原始图片的一半大小）
		Bitmap reflectionImage = Bitmap.createBitmap(originalImage, 0, height / 2, width, height / 2, matrix, false);
		// 创建总图片（原图片 + 倒影图片）
		Bitmap finalReflection = Bitmap.createBitmap(width, (height + height / 2), Config.ARGB_8888);
		// 创建画布
		Canvas canvas = new Canvas(finalReflection);
		canvas.drawBitmap(originalImage, 0, 0, null);
		//把倒影图片画到画布上
		canvas.drawBitmap(reflectionImage, 0, height + 5, null);
		Paint shaderPaint = new Paint();
		//创建线性渐变LinearGradient对象
		LinearGradient shader = new LinearGradient(0, originalImage.getHeight(), 0, finalReflection.getHeight() + 5, 0x70ffffff,
				0x00ffffff, TileMode.MIRROR);
		shaderPaint.setShader(shader);
		shaderPaint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
		//画布画出反转图片大小区域，然后把渐变效果加到其中，就出现了图片的倒影效果。
		canvas.drawRect(0, height + 1, width, finalReflection.getHeight() + 5, shaderPaint);
		return finalReflection;
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
