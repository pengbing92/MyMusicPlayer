package com.whut.database.service;

import java.util.List;

import com.whut.database.entiy.Song;

/**
 * 1. 删除歌曲有权限限制
 * 2. 更改歌曲名无意义，因为是从媒体库中读取信息。即使更改的文件名，媒体库中的歌曲名依然不变。
 * 
 * @author chenfu
 *
 */
public interface SongService {

	public void addMusicList2DB(List<Song> songList); // 将本地扫描到的歌曲信息保存到数据库中
	public Song getFirstSong();
	public Song getNextSong(Song currentSong); // 点击按钮，切换下一曲
	public Song getPreSong(Song currentSong); // 点击按钮，切换上一曲
	public void updateCurrentSong(Song song); // 更新当前播放歌曲信息
	public Song getCurrentSong();
	public Song getSongById(long m_id); // 根据m_id找到歌曲
	public List<Song> getAllSong(); // 获取数据库中的所有歌曲
	public Song getSongByName(String songName); // 根据歌曲名查找歌曲
	public List<Song> getSongs(String str); // 根据关键字模糊查询
	public boolean isCurrentSongTbEmpty();  // 判断当前播放歌曲表是否为空
	public boolean isMusicListTbEmpty();  // 判断歌曲表是否为空
	public void insert2MusicList(List<Song> songList); // 添加记录到歌曲表
	public int getMusicCountInDB(); // 获取数据库中歌曲数目
	
}
