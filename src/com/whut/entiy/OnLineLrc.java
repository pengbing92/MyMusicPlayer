package com.whut.entiy;


/**
 * 网络下载歌词,JsonObject
 * @author chenfu
 *
 */
public class OnLineLrc {
	
	private int aid; 
	private int artist_id;
	private int sid; // 下载地址中的id
	private String songName; // 歌曲名
	private String downloadUrl; // 下载地址
	
	public int getAid() {
		return aid;
	}
	public void setAid(int aid) {
		this.aid = aid;
	}
	public int getArtist_id() {
		return artist_id;
	}
	public void setArtist_id(int artist_id) {
		this.artist_id = artist_id;
	}
	public int getSid() {
		return sid;
	}
	public void setSid(int sid) {
		this.sid = sid;
	}
	public String getSongName() {
		return songName;
	}
	public void setSongName(String songName) {
		this.songName = songName;
	}
	public String getDownloadUrl() {
		return downloadUrl;
	}
	public void setDownloadUrl(String downloadUrl) {
		this.downloadUrl = downloadUrl;
	}
	
	

}
