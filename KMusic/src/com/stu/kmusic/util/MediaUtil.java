package com.stu.kmusic.util;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import com.stu.kmusic.ConstantValue;
import com.stu.kmusic.bean.Music;

public class MediaUtil {

	private List<Music> songList = new ArrayList<Music>(); 
	
	public static int CURRENTPOS = 0;
	public static int PLAYSTATE = ConstantValue.OPTION_PAUSE;
	
	private static MediaUtil instance = new MediaUtil();
	
	private MediaUtil(){		
	}
	
	public static MediaUtil getInstance(){
		return instance;
	}
	
	public List<Music> getSongList(){
		return songList;
	}
	
	public void initMusics(Context context){
		songList.clear();
		Cursor cur = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
				, new String[] {MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DURATION
								, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA}
				, null, null, null);
		try {
			if (cur != null){
				while (cur.moveToNext()){
					Music music = new Music();
					music.setTitle(cur.getString(0));
					music.setDuration(cur.getString(1));
					music.setArtist(cur.getString(2));
					music.setId(cur.getString(3));
					music.setPath(cur.getString(4));
					songList.add(music);
				}
			}
		} catch (Exception e) {
		} finally{
			if (cur != null)
				cur.close();
		}
	}
	
	public Music getCuMusic(){
		return songList.get(CURRENTPOS);
	}
}






































