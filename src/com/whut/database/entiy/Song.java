package com.whut.database.entiy;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 将歌曲实体类做序列化处理， 
 * 以便于将歌曲列表从SongAty传递到MainAty
 * 
 * @author chenfu
 * 
 */
public class Song implements Parcelable {

	private long id; // 音乐id
	private int duration; // 歌曲时长
	private String singer; // 歌手
	private String songName; // 音乐标题
	private String mp3Path; // 存储路径
	private long size; // 文件大小
	private String album; // 专辑图片
	private int albumId;  // 专辑图片Id

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public String getSinger() {
		return singer;
	}

	public void setSinger(String singer) {
		this.singer = singer;
	}

	public String getSongName() {
		return songName;
	}

	public void setSongName(String songName) {
		this.songName = songName;
	}

	public String getMp3Path() {
		return mp3Path;
	}

	public void setMp3Path(String mp3Path) {
		this.mp3Path = mp3Path;
	}
	
	

	public String getAlbum() {
		return album;
	}

	public void setAlbum(String album) {
		this.album = album;
	}

	public int getAlbumId() {
		return albumId;
	}

	public void setAlbumId(int albumId) {
		this.albumId = albumId;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(id);
		dest.writeInt(duration);
		dest.writeString(singer);
		dest.writeString(songName);
		dest.writeString(mp3Path);
		dest.writeLong(size);
		dest.writeString(album);
		dest.writeInt(albumId);
	}

	public static final Parcelable.Creator<Song> CREATOR = new Parcelable.Creator<Song>() {

		@Override
		public Song createFromParcel(Parcel source) {
			Song song = new Song();
			song.id = source.readLong();
			song.duration = source.readInt();
			song.singer = source.readString();
			song.songName = source.readString();
			song.mp3Path = source.readString();
			song.size = source.readLong();
			song.album = source.readString();
			song.albumId = source.readInt();
			return song;
		}

		@Override
		public Song[] newArray(int size) {
			return new Song[size];
		}
	};

}
