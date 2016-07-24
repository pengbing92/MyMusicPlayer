package com.whut.database.service;

import java.util.List;

import com.whut.database.entiy.Song;

public interface SongService {

	public void addMusicList2DB(List<Song> songList); // 将本地扫描到的歌曲信息保存到数据库中
	public Song getFirstSong();
	public Song getNextSong(Song currentSong); // 点击按钮，切换下一曲
	public Song getPreSong(Song currentSong); // 点击按钮，切换上一曲
	public void updateCurrentSong(Song song); // 更新当前播放歌曲信息
	public Song getCurrentSong();
	public Song getSongById(long m_id); // 根据m_id找到歌曲
	public List<Song> getAllSong(); // 获取数据库中的所有歌曲
	public void deleteSongById(long m_id); // 通过m_id删除指定的歌曲
	
}
