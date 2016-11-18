package com.whut.music;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.whut.adapter.SongListAdapter;
import com.whut.application.MusicManager;
import com.whut.database.entiy.Play_Model;
import com.whut.database.entiy.Song;
import com.whut.database.service.imp.ModelServiceDao;
import com.whut.database.service.imp.SongServiceDao;
import com.whut.service.MyMusicService;
import com.whut.util.Msg_Music;
import com.whut.util.ToastUtil;
import com.whut.view.LrcProcess;

/**
 * 歌曲列表界面
 * 
 * @author chenfu
 * 
 */
public class SongListActivity extends Activity implements OnClickListener,
		OnItemClickListener, OnItemLongClickListener {

	private List<Song> songList = new ArrayList<Song>();
	private ListView songListView;
	private RelativeLayout bottomLayout;
	private ImageView songImage;
	private static TextView songName;
	private static TextView singer;
	private ImageView playBtn;
	private ImageView nextBtn;

	// 后退按钮
	private ImageView back_Btn;

	private static Context context;

	private static String songName_str = ""; // 歌曲名
	private static String singer_str = ""; // 歌手名
	private int secondPause = -1; // 播放中断位置，默认值为-1

	private static SongListAdapter songListAdapter;

	// 播放状态标志
	private static boolean isPlaying;

	// 广播接收器
	private IsplayingReceiver ipRev;
	private SecondPauseReceiver spRev;
	private CurrentPositonReceiver cpRev;
	private SwitchSongReceiver ssRev;
	private HeadSetStatus hssRev; // 接收耳机插拔状态的广播
	private static int HEADSET_DISCONNECT = 0; // 未插耳机
	private static int HEADSET_CONNECT = 1; // 插入耳机

	// 当前正在播放的歌曲
	private static int currentPosition = 0; // 当前播放的位置
	private static long currentId = 0; // 当前播放歌曲的id
	private static int currentModel = Play_Model.CYCLEALL; // 当前播放模式,默认为列表循环
	private static Song currentSong;

	// 播放是否结束
	private boolean isEnd = false;

	// 发送指令
	private int msg = -1;

	// 播放下一曲
	private boolean playNext = false;

	// 手机存储根目录
	private String rootPath = Environment.getExternalStorageDirectory()
			.getPath();

	// 歌曲缩略图旋转动画
	private ObjectAnimator objectAnimatorPre; // 先从0旋转到180
	private ObjectAnimator objectAnimatorNext; // 再从180旋转到360，周而复始

	private static SongServiceDao songServiceDao;
	private static ModelServiceDao modelServiceDao;


	/**
	 * 分组的布局
	 */
	private LinearLayout alphabet_titleLayout;

	/**
	 * 弹出式分组的布局
	 */
	private RelativeLayout sectionToastLayout;

	/**
	 * 右侧可滑动字母表
	 */
	private Button alphabetButton;

	/**
	 * 分组上显示的字母
	 */
	private TextView alphabetTitle;

	/**
	 * 弹出式分组上的文字
	 */
	private TextView sectionToastText;

	/**
	 * 定义字母表的排序规则
	 */
	private String alphabet = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	/**
	 * 上次第一个可见元素，用于滚动时记录标识。
	 */
	private int lastFirstVisibleItem = -1;

	public static int getCurrentPosition() {
		return currentPosition;
	}

	@SuppressLint("HandlerLeak")
	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case 0:
				// 更新UI
				
				break;
			case 1:
				currentSong = songServiceDao.getCurrentSong();
				currentId = currentSong.getId();
				songName_str = currentSong.getSongName();
				singer_str = currentSong.getSinger();
				songName.setText(songName_str.split("\\(")[0].trim());
				singer.setText(singer_str.split(",")[0].trim());
				// 正在播放的歌曲字体变红
				songListAdapter.setCurrentItem(currentId);
				songListAdapter.notifyDataSetChanged();
				break;
			case 2:
				if (msg.arg1 == HEADSET_DISCONNECT) {
					ToastUtil.toastInfo(context, "未插入耳机");
				} else {
					ToastUtil.toastInfo(context, "已插入耳机");
				}
				break;
			}
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_song_list);

		context = this;

		currentSong = new Song();
		songServiceDao = new SongServiceDao(context);
		modelServiceDao = new ModelServiceDao(context);

		// 创建歌词文件夹
		createLrcFolder();

		initBroadcastReceiver();

		initData();
		initView();

	}

	// 创建保存歌词文件的文件夹
	public void createLrcFolder() {

		File lrcDir = new File(rootPath + "/MyMusicPlayer/PengBing_lrcs");
		if (!lrcDir.exists()) {
			lrcDir.mkdirs();
		}
	}

	/**
	 * 从LrcAty返回,执行onStart->onResume
	 */
	@Override
	protected void onStart() {
		super.onStart();
	}

	/**
	 * 执行流程onCreate->onStart->onResume, 从LrcAty进入则执行onStart->onResume
	 */
	@Override
	protected void onResume() {
		super.onResume();

		if (isPlaying) {
			playBtn.setBackgroundResource(R.drawable.paubtn_selector);
		} else {
			playBtn.setBackgroundResource(R.drawable.playbtn_selector);
		}
		// 若LrcAty中切换了歌曲，更新UI
		handler.sendEmptyMessage(1);

		// 开启旋转动画效果
		startImageAnimation();

	}

	// 注册广播接收器
	public void initBroadcastReceiver() {

		ipRev = new IsplayingReceiver();
		IntentFilter ipFilter = new IntentFilter();
		ipFilter.addAction("isplaying");
		registerReceiver(ipRev, ipFilter);

		spRev = new SecondPauseReceiver();
		IntentFilter spFilter = new IntentFilter();
		spFilter.addAction("recordSecondPause");
		registerReceiver(spRev, spFilter);

		cpRev = new CurrentPositonReceiver();
		IntentFilter cpFilter = new IntentFilter();
		cpFilter.addAction("GetPosition");
		registerReceiver(cpRev, cpFilter);

		ssRev = new SwitchSongReceiver();
		IntentFilter ssFilter = new IntentFilter();
		ssFilter.addAction("switchSong");
		registerReceiver(ssRev, ssFilter);

		hssRev = new HeadSetStatus();
		IntentFilter hssFilter = new IntentFilter();
		hssFilter.addAction("android.intent.action.HEADSET_PLUG");
		registerReceiver(hssRev, hssFilter);

	}

	// 初始化数据
	public void initData() {

		Intent intent = getIntent();
		secondPause = intent.getIntExtra("secondPause", -1);

		songList = songServiceDao.getAllSong();

		isPlaying = MusicManager.isPlaying();
		currentSong = songServiceDao.getCurrentSong();
		currentId = currentSong.getId();
		songName_str = currentSong.getSongName();
		singer_str = currentSong.getSinger();
		currentModel = modelServiceDao.getCurrentModel();

		songListAdapter = new SongListAdapter(songList, context);

	}

	public void initView() {

		songListView = (ListView) findViewById(R.id.songList);
		bottomLayout = (RelativeLayout) findViewById(R.id.bottomView);
		songImage = (ImageView) findViewById(R.id.songImg);
		songName = (TextView) findViewById(R.id.songName);
		singer = (TextView) findViewById(R.id.singer);
		playBtn = (ImageView) findViewById(R.id.play_btn);
		nextBtn = (ImageView) findViewById(R.id.next_btn);
		back_Btn = (ImageView) findViewById(R.id.back);

		alphabet_titleLayout = (LinearLayout) findViewById(R.id.title_alphabet);
		alphabetTitle = (TextView) findViewById(R.id.tv_alphabet);
		alphabetButton = (Button) findViewById(R.id.alphabetButton);
		sectionToastLayout = (RelativeLayout) findViewById(R.id.section_toast_layout);
		sectionToastText = (TextView) findViewById(R.id.section_toast_text);

		// 为ListView设置滚动监听事件
		setupContactsListView();
		// 设置字母表上的触摸事件
		setAlpabetListener();

		songImage.setBackgroundResource(R.drawable.app_music);

		if (isPlaying) {
			playBtn.setBackgroundResource(R.drawable.paubtn_selector);
		} else {
			playBtn.setBackgroundResource(R.drawable.playbtn_selector);
		}

		songName.setText(songName_str.split("\\(")[0].trim());
		singer.setText(singer_str.split(",")[0].trim());
		songListAdapter.setCurrentItem(currentId);
		songListAdapter.notifyDataSetChanged();

		// 点击事件监听
		bottomLayout.setOnClickListener(this);
		playBtn.setOnClickListener(this);
		nextBtn.setOnClickListener(this);
		back_Btn.setOnClickListener(this);

		// 歌曲图片，旋转动画效果
		startImageAnimation();

	}

	

	// 360度旋转动画
	public void startImageAnimation() {

		// 歌曲缩略图旋转动画效果
		objectAnimatorPre = ObjectAnimator.ofFloat(songImage, "rotation", 0f,
				180f);
		objectAnimatorPre.setDuration(3000);

		objectAnimatorNext = ObjectAnimator.ofFloat(songImage, "rotation",
				180f, 360f);
		objectAnimatorNext.setDuration(3000);

		if (isPlaying) {
			objectAnimatorPre.start();
		}

		// 动画状态监听
		objectAnimatorPre.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				super.onAnimationEnd(animation);
				if (isPlaying) {
					objectAnimatorNext.start();
				}

			}
		});
		objectAnimatorNext.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				super.onAnimationEnd(animation);
				if (isPlaying) {
					objectAnimatorPre.start();
				}

			}
		});

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.bottomView:
			// 从SongAty跳转到LrcAty
			startActivity(gotoLrcAty(context, isPlaying, false));
			break;
		case R.id.play_btn:
			startMusicService();
			break;
		case R.id.next_btn:
			playNextSong();
			// 切换歌曲的时候，重新设置 NOT_FOUND 的初始值为false
			LrcProcess.setNOT_FOUND(false);
			break;
		case R.id.back:
			// 结束当前Aty，返回上一个Aty
			finish();
			break;
		}
	}

	// 通过Btn启动service
	public void startMusicService() {
		Intent gotoService = new Intent(context, MyMusicService.class);
		if (playNext) {
			secondPause = -1;
			gotoService.putExtra("other_music", true);
			// 重置
			playNext = false;
			playBtn.setBackgroundResource(R.drawable.paubtn_selector);
		} else { // 没有切换歌曲
			if (isPlaying) {
				msg = Msg_Music.PAUSE;
				secondPause = -1;
				playBtn.setBackgroundResource(R.drawable.playbtn_selector);
			} else {
				msg = Msg_Music.PLAY;
				playBtn.setBackgroundResource(R.drawable.paubtn_selector);
			}
			gotoService.putExtra("other_music", false);
		}

		gotoService.putExtra("msg", msg);
		gotoService.putExtra("secondPause", secondPause);
		// 启动Service
		startService(gotoService);
		MusicManager.setServiceOpen(true);

	}

	@Override
	protected void onDestroy() {
		// 注销广播接收器
		unRegisterBroadcastRev();
		Log.i("songList_Aty", "onDestory");
		super.onDestroy();
	}

	/**
	 * 进入LrcAty的intent，包含相关信息
	 * 
	 * @return intent
	 */
	public static Intent gotoLrcAty(Context context, boolean isPlaying,
			boolean fromNotification) {

		Intent intent = new Intent(context, LrcActivity.class);
		intent.putExtra("isPlaying", isPlaying);
		intent.putExtra("fromNotification", fromNotification);
		if (fromNotification) {
			if (isPlaying) {
				MusicManager.setServiceOpen(true);
			} else {
				MusicManager.setServiceOpen(false);
			}
		}

		return intent;
	}

	// 注销广播接收器
	public void unRegisterBroadcastRev() {
		unregisterReceiver(ipRev);
		unregisterReceiver(spRev);
		unregisterReceiver(cpRev);
		unregisterReceiver(ssRev);
		unregisterReceiver(hssRev);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {

		if (currentId == songList.get(position).getId()) {
			// 点击的是同一首歌曲
			playNext = false;
		} else {
			//ToastUtil.toastInfo(context, songList.get(position).getSongName());
			// 点击的不是同一首歌曲
			MusicManager.setSeekPosition(-1);

			playNext = true;
			msg = Msg_Music.PLAY;

			// 更新当前播放歌曲id
			currentId = songList.get(position).getId();
			// 更新数据库
			songServiceDao.updateCurrentSong(songList.get(position));

			// 更新UI
			handler.sendEmptyMessage(1);

			// 切换歌曲的时候，重新设置 NOT_FOUND 的初始值为false
			LrcProcess.setNOT_FOUND(false);
		}

		startMusicService();

	}

	// 播放结束后的处理
	public void playEnd(boolean isEnd) {
		if (isEnd) {
			if (currentModel != Play_Model.CYCLEONE) {
				// 播放下一首歌曲
				playNextSong();
			} else {
				// 单曲循环
				cycleOne();
			}
		}
	}

	// 单曲循环
	public void cycleOne() {
		// 单曲循环需重新播放，设置playNext为true
		playNext = true;
		// 播放指令
		msg = Msg_Music.PLAY;
		startMusicService();
	}

	// 播放下一曲
	public void playNextSong() {
		playNext = true;
		msg = Msg_Music.NEXT;
		startMusicService();
	}

	/**
	 * 为ListView设置监听事件，根据当前的滑动状态来改变分组的显示位置，从而实现挤压动画的效果。
	 */
	private void setupContactsListView() {

		songListView.setAdapter(songListAdapter);
		songListView.setOnItemClickListener(this);
		songListView.setOnItemLongClickListener(this);

		songListView.setOnScrollListener(new OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				
				handler.sendEmptyMessage(0);

				int section = songListAdapter
						.getSectionForPosition(firstVisibleItem) - 'A' + 1;
				int nextSecPosition = songListAdapter
						.getPositionForSection(section + 1);
				if (firstVisibleItem != lastFirstVisibleItem) {
					MarginLayoutParams params = (MarginLayoutParams) alphabet_titleLayout
							.getLayoutParams();
					params.topMargin = 0;
					alphabet_titleLayout.setLayoutParams(params);
					if (section == -29) { // #号的情况
						section = 0;
					}
					alphabetTitle.setText(String.valueOf(alphabet
							.charAt(section)));
				}
				if (nextSecPosition == firstVisibleItem + 1) {
					View childView = view.getChildAt(0);
					if (childView != null) {
						int titleHeight = alphabet_titleLayout.getHeight();
						int bottom = childView.getBottom();
						MarginLayoutParams params = (MarginLayoutParams) alphabet_titleLayout
								.getLayoutParams();
						if (bottom < titleHeight) {
							float pushedDistance = bottom - titleHeight;
							params.topMargin = (int) pushedDistance;
							alphabet_titleLayout.setLayoutParams(params);
						} else {
							if (params.topMargin != 0) {
								params.topMargin = 0;
								alphabet_titleLayout.setLayoutParams(params);
							}
						}
					}
				}
				lastFirstVisibleItem = firstVisibleItem;

			}
		});

	}

	/**
	 * 设置字母表上的触摸事件，根据当前触摸的位置结合字母表的高度，计算出当前触摸在哪个字母上。
	 * 当手指按在字母表上时，展示弹出式分组。手指离开字母表时，将弹出式分组隐藏。
	 */
	private void setAlpabetListener() {
		alphabetButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				float alphabetHeight = alphabetButton.getHeight();
				float y = event.getY();
				int sectionPosition = (int) ((y / alphabetHeight) / (1f / 27f));
				if (sectionPosition < 0) {
					sectionPosition = 0;
				} else if (sectionPosition > 26) {
					sectionPosition = 26;
				}
				String sectionLetter = String.valueOf(alphabet
						.charAt(sectionPosition));
				int position = songListAdapter
						.getPositionForSection(sectionLetter.charAt(0));
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					alphabetButton.setBackgroundResource(R.drawable.a_z_click);
					sectionToastLayout.setVisibility(View.VISIBLE);
					sectionToastText.setText(sectionLetter);
					songListView.setSelection(position);
					break;
				case MotionEvent.ACTION_MOVE:
					sectionToastText.setText(sectionLetter);
					songListView.setSelection(position);
					break;
				default:
					alphabetButton.setBackgroundResource(R.drawable.a_z);
					sectionToastLayout.setVisibility(View.GONE);
				}
				return true;
			}
		});
	}

	/**
	 * 定义广播接收器
	 * 
	 * @author chenfu
	 * 
	 */
	// 获取当前播放位置
	public class CurrentPositonReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			isEnd = intent.getBooleanExtra("isEnd", false);
			// 播放结束后的处理
			playEnd(isEnd);
			if (isPlaying) {
				if (!isEnd) {
					currentPosition = intent.getIntExtra("position", 0);
				}
			}

		}

	}

	// 获取播放状态
	public class IsplayingReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			isPlaying = intent.getBooleanExtra("isplaying", false);
			MusicManager.setPlaying(isPlaying);
		}
	}

	// 获取暂停位置
	public class SecondPauseReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			secondPause = intent.getIntExtra("secondPause", -1);
		}
	}

	// 切换音乐
	public class SwitchSongReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			handler.sendEmptyMessage(1);

		}

	}

	/**
	 * 实时监测耳机插拔状态
	 * 
	 * @author chenfu
	 * 
	 */
	public class HeadSetStatus extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Message msg = handler.obtainMessage();
			msg.what = 2;
			if (intent.hasExtra("state")) {

				if (intent.getIntExtra("state", 0) == 0) {
					// 未插入耳机
					msg.arg1 = HEADSET_DISCONNECT;
				}

				if (intent.getIntExtra("state", 0) == 1) {
					// 插入耳机
					msg.arg1 = HEADSET_CONNECT;
				}
			}
			handler.sendMessage(msg);
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		// TODO 长按事件
		return false;
	}

}
