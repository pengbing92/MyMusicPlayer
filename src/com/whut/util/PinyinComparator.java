package com.whut.util;

import java.util.Comparator;

import com.whut.database.entiy.Song;

public class PinyinComparator implements Comparator<Song> {

	@Override
	public int compare(Song lhs, Song rhs) {
		//这里主要是用来对ListView里面的数据根据ABCDEFG...来排序
		if (rhs.getFirstLetter().equals("#")) {
			return -1;
		} else if (lhs.getFirstLetter().equals("#")) {
			return 1;
		} else {
			return lhs.getFirstLetter().compareTo(rhs.getFirstLetter());
		}
	}

}
