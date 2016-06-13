package com.whut.adapter;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.whut.entiy.Song;
import com.whut.music.R;

/**
 * 歌曲列表Adapter
 * 
 * @author chenfu
 * 
 */
public class SongListAdapter extends BaseAdapter {

	private List<Song> songList;
	private LayoutInflater layoutInflater;
	private Context context;

	// 正在播放的歌曲的id，用以突出显示
	private long currentItem;
	
	public void setCurrentItem(int currentItem) {
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

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Song song = songList.get(position);
		ViewHolder viewHolder = null;
		if (convertView == null) {
			viewHolder = new ViewHolder();
			convertView = layoutInflater.inflate(R.layout.songlist_item, null);
			viewHolder.song_name = (TextView) convertView
					.findViewById(R.id.song_name);
			viewHolder.song_singer = (TextView) convertView
					.findViewById(R.id.song_singer);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}

		viewHolder.song_name.setText(song.getSongName());
		viewHolder.song_singer.setText(song.getSinger());

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

}
