package com.whut.view;

import java.util.ArrayList;
import java.util.List;

import android.R;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import com.whut.application.MyApplication;
import com.whut.entiy.LrcContent;

/**
 * 自定义绘画歌词，产生滚动效果
 * 
 * @author wwj
 * 
 */
public class LrcView extends android.widget.TextView {
	private float width; // 歌词视图宽度
	private float height; // 歌词视图高度
	private Paint currentPaint; // 当前画笔对象
	private Paint notCurrentPaint; // 非当前画笔对象
	private int textWidth;
	private float textHeight = 100; // 文本高度
	private float textSize = 45; // 文本大小
	private int index = 0; // list集合下标

	private static final String TAG = LrcView.class.getName();

	private List<LrcContent> mLrcList = new ArrayList<LrcContent>();
	private int drawCount; // onDraw方法执行次数

	public void setmLrcList(List<LrcContent> mLrcList) {
		drawCount = 0;
		this.mLrcList = mLrcList;
	}

	public LrcView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public LrcView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {

		setFocusable(true); // 设置可对焦

		// 高亮部分
		currentPaint = new Paint();
		currentPaint.setAntiAlias(true); // 设置抗锯齿，让文字美观饱满
		currentPaint.setTextAlign(Paint.Align.CENTER);// 设置文本对齐方式

		// 非高亮部分
		notCurrentPaint = new Paint();
		notCurrentPaint.setAntiAlias(true);
		notCurrentPaint.setTextAlign(Paint.Align.CENTER);

	}

	/**
	 * 绘画歌词
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (canvas == null) {
			return;
		}

		currentPaint.setColor(getResources().getColor(R.color.holo_red_light));
		notCurrentPaint.setColor(Color.argb(140, 255, 255, 255));

		currentPaint.setTextSize(55);
		currentPaint.setTypeface(Typeface.SERIF);

		notCurrentPaint.setTextSize(textSize);
		notCurrentPaint.setTypeface(Typeface.DEFAULT);

		try {

			if (LrcProcess.isNOT_FOUND()) {
				if (drawCount > 1000) {
					Log.i(TAG, "drawCount is " + drawCount);
					setText("");
					setText("未检索到歌词");
				}
			} else {
				setText("");

				if (textWidth > getScreenWidth()) {
					String[] longStr = mLrcList.get(index).getLrcStr().split("\\(");
					canvas.drawText(longStr[0], width / 2, height / 2, currentPaint);
					canvas.drawText(longStr[1], width / 2, height / 2 + textHeight,
							currentPaint);
				} else {
					canvas.drawText(mLrcList.get(index).getLrcStr(), width / 2,
							height / 2, currentPaint);
				}

				float tempY = height / 2;
				// 画出本句之前的句子
				for (int i = index - 1; i >= 0; i--) {
					// 向上推移
					tempY = tempY - textHeight;
					canvas.drawText(mLrcList.get(i).getLrcStr(), width / 2, tempY,
							notCurrentPaint);
				}
				tempY = height / 2;
				// 画出本句之后的句子
				for (int i = index + 1; i < mLrcList.size(); i++) {
					// 往下推移
					tempY = tempY + textHeight;
					canvas.drawText(mLrcList.get(i).getLrcStr(), width / 2, tempY,
							notCurrentPaint);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			// canvas.drawText("未检索到歌词..", width / 2, height / 2, currentPaint);
			Log.i(TAG, "未检索到歌词..");
		}
		
		drawCount++;

	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		textWidth = getDefaultSize(0, widthMeasureSpec);
		// Log.i(TAG, textWidth + "");
	}

	/**
	 * 当view大小改变的时候调用的方法
	 */
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		this.width = w;
		this.height = h;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	/**
	 * 获取屏幕宽高
	 * 
	 * @return
	 */
	private int getScreenWidth() {

		WindowManager manager = (WindowManager) MyApplication.getContext()
				.getSystemService(Context.WINDOW_SERVICE);

		DisplayMetrics dm = new DisplayMetrics();
		manager.getDefaultDisplay().getMetrics(dm);
		int screenWidth = dm.widthPixels;
		int screenHeight = dm.heightPixels;
		Log.d("screenWidth", String.valueOf(screenWidth));
		Log.d("screenHeight", String.valueOf(screenHeight));

		return screenWidth;

	}

}
