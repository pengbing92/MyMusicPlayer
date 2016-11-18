package com.whut.util;

import java.io.IOException;
import java.io.InputStream;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.net.Uri;
import android.util.LruCache;

import com.whut.music.R;

/**
 * 图片工具类，对图片做缓存，缩放操作
 * 
 * @author chenfu
 * 
 */
public class ImageUtil {

	/**
	 * 图片缓存技术的核心类，在程序内存达到设定值时会将最少最近使用的图片移除掉。
	 */
	private static LruCache<Long, Bitmap> mMemoryCache;

	/**
	 * ImageUtil的实例
	 */
	private static ImageUtil imageUtil;

	private Context context;
	
	private final Uri sArtworkUri = Uri
			.parse("content://media/external/audio/albumart");

	private ImageUtil(Context context) {
		// 获取应用程序最大可用内存
		int maxMemory = (int) Runtime.getRuntime().maxMemory();
		int cacheSize = maxMemory / 8;
		// 设置图片缓存大小为程序最大可用内存的1/8
		mMemoryCache = new LruCache<Long, Bitmap>(cacheSize) {
			@Override
			protected int sizeOf(Long key, Bitmap bitmap) {
				return bitmap.getByteCount();
			}
		};
		this.context = context;
	}

	public static ImageUtil getInstance(Context context) {
		if (imageUtil == null) {
			imageUtil = new ImageUtil(context);
		}
		return imageUtil;
	}

	/**
	 * 将一张图片存储到LruCache中。
	 * 
	 * @param key
	 *            LruCache的键，这里传入album_id。
	 * @param bitmap
	 *            LruCache的值，这里传入Bitmap对象。
	 */
	public void addBitmapToMemoryCache(Long key, Bitmap bitmap) {
		if (bitmap == null) {
			bitmap = getDefaultArtworkInList();
		}
		if (getBitmapFromMemoryCache(key) == null) {
			mMemoryCache.put(key, bitmap);
		}
	}

	/**
	 * 从LruCache中获取一张图片，如果不存在就返回null。
	 * 
	 * @param key
	 *            LruCache的键，这里传入album_id。
	 * @return 对应传入键的Bitmap对象，或者null。
	 */
	public Bitmap getBitmapFromMemoryCache(Long key) {
		return mMemoryCache.get(key);
	}

	/**
	 * 获取歌曲列表默认专辑图片
	 * 
	 * @param context
	 * @return
	 */
	public Bitmap getDefaultArtworkInList() {
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inPreferredConfig = Bitmap.Config.RGB_565;

		return BitmapFactory.decodeResource(context.getResources(),
				R.drawable.albumart_mp_unknown_list, opts);
	}

	/**
	 * 获取歌词显示区域默认专辑图片
	 * 
	 * @return
	 */
	public Bitmap getDefaultArtWork() {
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inPreferredConfig = Bitmap.Config.RGB_565;

		return BitmapFactory.decodeResource(context.getResources(),
				R.drawable.albumart_mp_unknown, opts);
	}

	/**
	 * 获取原始的专辑封面位图
	 * 
	 * @param album_id
	 * @return
	 */
	public Bitmap getLargeArtwork(Long album_id) {
		Bitmap bm = null;

		ContentResolver res = context.getContentResolver();
		Uri uri = ContentUris.withAppendedId(sArtworkUri, album_id);
		if (uri != null) {
			InputStream in = null;
			try {
				in = res.openInputStream(uri);
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inPreferredConfig = Bitmap.Config.RGB_565;
				bm = BitmapFactory.decodeStream(in, null, options);
				in.close();
			} catch (IOException e) {
				getDefaultArtWork();
				e.printStackTrace();
			}

		}

		return bm;
	}

	/**
	 * 获取专辑封面位图对象(按比例缩小的)
	 * 
	 * @param albumPath
	 * @return 位图对象
	 */
	public Bitmap getSmallArtwork(Long album_id) {

		if (album_id < 0) {
			return null;
		}

		Bitmap bm = null;

		ContentResolver res = context.getContentResolver();
		Uri uri = ContentUris.withAppendedId(sArtworkUri, album_id);
		if (uri != null) {
			InputStream in = null;
			try {
				in = res.openInputStream(uri);
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inPreferredConfig = Bitmap.Config.RGB_565;
				// 先制定原始大小
				options.inSampleSize = 1;
				// 只进行大小判断
				options.inJustDecodeBounds = true;
				// 调用此方法得到options得到图片的大小
				BitmapFactory.decodeStream(in, null, options);
				/** 我们的目标是在你N pixel的画面上显示。 所以需要调用computeSampleSize得到图片缩放的比例 **/
				/** 这里的target为800是根据默认专辑图片大小决定的，800只是测试数字但是试验后发现完美的结合 **/
				options.inSampleSize = computeSampleSize(options, 60);
				// if(small){
				// options.inSampleSize = computeSampleSize(options, 40);
				// } else{
				// options.inSampleSize = computeSampleSize(options, 600);
				// }

				// 我们得到了缩放比例，现在开始正式读入Bitmap数据
				options.inJustDecodeBounds = false;
				options.inDither = false;
				options.inPreferredConfig = Bitmap.Config.ARGB_8888;
				in = res.openInputStream(uri);
				bm = BitmapFactory.decodeStream(in, null, options);
				in.close();
			} catch (IOException e) {
				// 返回默认专辑图片
				getDefaultArtworkInList();
				e.printStackTrace();
			}
		}

		return bm;
	}

	/**
	 * 对图片进行合适的缩放
	 * 
	 * @param options
	 * @param target
	 * @return
	 */
	private static int computeSampleSize(Options options, int target) {
		int w = options.outWidth;
		int h = options.outHeight;
		int candidateW = w / target;
		int candidateH = h / target;
		int candidate = Math.max(candidateW, candidateH);
		if (candidate == 0) {
			return 1;
		}
		if (candidate > 1) {
			if ((w > target) && (w / candidate) < target) {
				candidate -= 1;
			}
		}
		if (candidate > 1) {
			if ((h > target) && (h / candidate) < target) {
				candidate -= 1;
			}
		}
		return candidate;
	}
}
