package com.whut.util;

import java.io.UnsupportedEncodingException;
import java.util.Comparator;

import com.whut.database.entiy.Song;

/**
 * 对歌曲名比较大小
 * 
 * @author chenfu
 * 
 */
public class PinyinComparator implements Comparator<Song> {

	@Override
	public int compare(Song lhs, Song rhs) {

		String lFirstLetter = lhs.getFirstLetter();
		String rFirstLetter = rhs.getFirstLetter();
		
		String lFirstWord = lhs.getSongName().substring(0, 1);
		String rFirstWord = rhs.getSongName().substring(0, 1);

		String lName = lhs.getSongName().replace("-", "").replace("(", "")
				.replace(")", "").replace(" ", "");
		String rName = rhs.getSongName().replace("-", "").replace("(", "")
				.replace(")", "").replace(" ", "");
		
		String lPinYin = "";
		String rPinYin = "";
		try {
			if (lFirstWord.getBytes("gbk").length == 1) { // 英文
				lPinYin = lName;		
			} else { // 汉字
				for (int i=0; i<lName.length(); i++) {
					lPinYin += HanZi2PinYin.getPinYin(lName.substring(i, i+1));
				}
			}
			if (rFirstWord.getBytes("gbk").length == 1) { // 英文
				rPinYin = rName;
			} else { // 汉字
				for (int i=0; i<rName.length(); i++) {
					rPinYin += HanZi2PinYin.getPinYin(rName.substring(i, i+1));
				}
			}
			
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//System.out.println(lPinYin + "\n" + rPinYin);

		if (lFirstLetter.equals("#")) {
			return -1;
		} else if (rFirstLetter.equals("#")) {
			return 1;
		} else {
			return lPinYin.toLowerCase().compareTo(rPinYin.toLowerCase());
		}
	}
}
