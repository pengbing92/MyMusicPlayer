package com.whut.adapter;

import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.whut.application.MusicManager;
import com.whut.database.entiy.Song;
import com.whut.music.R;

/**
 * 歌曲列表Adapter
 * 
 * @author chenfu
 * 
 */
public class SongListAdapter extends BaseAdapter implements SectionIndexer {

	private List<Song> songList;
	private LayoutInflater layoutInflater;
	private Context context;
	
	private String albumPath = null; // 专辑封面图存储路径

	// 正在播放的歌曲的id，用以突出显示
	private long currentItem;
	
	public void setCurrentItem(long currentItem) {
		this.currentItem = currentItem;
	}

	public SongListAdapter(List<Song> songList, Context context) {
		super();
		this.songList = songList;
		this.context = context;
		layoutInflater = LayoutInflater.from(context);

	}

	public static class ViewHolder {
		TextView song_name;
		TextView song_singer;
		ImageView albumImage; // 专辑图片
		Button del_Btn; // 删除按钮
		LinearLayout sortKeyLayout;
		TextView sortKey;
		
	}

	@Override
	public int getCount() {
		return songList.size();
	}

	@Override
	public Object getItem(int position) {
		return songList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@SuppressLint("InflateParams")
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Song song = (Song) getItem(position);
		ViewHolder viewHolder = null;
		if (convertView == null) {
			viewHolder = new ViewHolder();
			convertView = layoutInflater.inflate(R.layout.songlist_item, null);
			viewHolder.song_name = (TextView) convertView
					.findViewById(R.id.song_name);
			viewHolder.song_singer = (TextView) convertView
					.findViewById(R.id.song_singer);
			viewHolder.albumImage = (ImageView) convertView.findViewById(R.id.albumImage);
			viewHolder.del_Btn = (Button) convertView.findViewById(R.id.del_Btn);
			viewHolder.sortKeyLayout = (LinearLayout) convertView.findViewById(R.id.sort_key_layout);
			viewHolder.sortKey = (TextView) convertView.findViewById(R.id.sort_key);
			convertView.setTag(viewHolder);		
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		
		//根据position获取分类的首字母的char ascii值
		int section = getSectionForPosition(position);
		//如果当前位置等于该分类首字母的Char的位置 ，则认为是第一次出现
		if (position == getPositionForSection(section)) {
			viewHolder.sortKey.setText(song.getFirstLetter());
			viewHolder.sortKeyLayout.setVisibility(View.VISIBLE);
		} else {
			viewHolder.sortKeyLayout.setVisibility(View.GONE);
		}

		viewHolder.song_name.setText(song.getSongName());
		viewHolder.song_singer.setText(song.getSinger());
		
		// 设置专辑封面图
		albumPath = getAlbumImagePath(song.getMp3Path());
		Bitmap bitmap = MusicManager.getArtwork(albumPath);
		if (bitmap != null) {
			viewHolder.albumImage.setImageBitmap(bitmap);
		} else {
			// 默认图片
			Bitmap defaultAlbum = MusicManager.getDefaultArtwork(context);
			viewHolder.albumImage.setImageBitmap(defaultAlbum);
		}
		
		//viewHolder.del_Btn.setOnTouchListener(this);
		

		// 根据传入的下标，将相应的item的字体显示为红色
		if (currentItem == song.getId()) {
			viewHolder.song_name.setTextColor(context.getResources().getColor(
					R.color.red));
		}else {
			// item的view是回收复用的,所以要加else
			viewHolder.song_name.setTextColor(context.getResources().getColor(
					R.color.black));
		}

		return convertView;
	}

	/**
	 * 获取专辑封面图片存储路径
	 * 
	 * @param songPath
	 * @return 专辑封面图存储路径
	 */
	public String getAlbumImagePath(String songPath) {
		
		Log.i("歌曲存储路径", songPath);
		
		// 魅蓝note2，音乐专辑图片存储路径，不同手机可能情况不同
		albumPath = songPath.substring(0, songPath.lastIndexOf("/"));
		albumPath = albumPath.replace("Download", "Cover")+".jpg";
		
		Log.i("专辑封面图路径", albumPath);
		
		return albumPath;
	}

	/**
	 * 根据分类的首字母的Char ascii值获取其第一次出现该首字母的位置
	 */
	@Override
	public int getPositionForSection(int sectionIndex) {
		for (int i=0;i<getCount();i++) {
			String sortStr = songList.get(i).getFirstLetter();
			char firstChar = sortStr.charAt(0);
			if (firstChar == sectionIndex) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * 根据ListView的当前位置获取分类的首字母的char ascii值
	 */
	@Override
	public int getSectionForPosition(int position) {
		// TODO Auto-generated method stub
		return songList.get(position).getFirstLetter().charAt(0);
	}

	@Override
	public Object[] getSections() {
		// TODO Auto-generated method stub
		return null;
	}
}
