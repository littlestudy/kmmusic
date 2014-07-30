package com.stu.kmusic.service;

import com.stu.kmusic.ConstantValue;
import com.stu.kmusic.util.HandlerManager;
import com.stu.kmusic.util.MediaUtil;
import com.stu.kmusic.util.PromptManager;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

public class MediaService extends Service 
	implements OnCompletionListener, OnSeekCompleteListener, OnErrorListener {
	
	private static String TAG = "MediaService";
	private static MediaPlayer player;	
	private static ProgressTask task;
	private String file;
	private int position = 0;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		if (player == null){
			player = new MediaPlayer();
			player.setOnSeekCompleteListener(this);
			player.setOnCompletionListener(this);
			player.setOnErrorListener(this);
		}
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		int option = intent.getIntExtra("option", -1);
		int progress = intent.getIntExtra("progress", -1);
		
		if (progress != -1){
			this.position = progress;
		}
		
		switch (option) {
		case ConstantValue.OPTION_PLAY:
			file = intent.getStringExtra("file");
			play(file);
			MediaUtil.PLAYSTATE = option;
			break;
		case ConstantValue.OPTION_PAUSE:
			position = player.getCurrentPosition();
			pause();
			MediaUtil.PLAYSTATE = option;
			break;
		case ConstantValue.OPTION_CONTINUE:
			playerToPosition(position);
			if (file == "" || file == null){
				file = intent.getStringExtra("file");
				play(file);
			} else {
				player.start();
			}			
			MediaUtil.PLAYSTATE = option;
			break;
		case ConstantValue.OPTION_UPDATE_PROGESS:
			playerToPosition(position);
			break;
		}
	}
	
	@Override
	public void onDestroy() {
		stop();
		super.onDestroy();
	}
	
	/**************************************************/
	/*                  播放器使用时的方法            */
	private void play(String path){
		if (player == null){
			player = new MediaPlayer();
		} 
		
		try {
			player.reset();
			player.setDataSource(file);
			player.prepare();
			player.start();
			
			if (task == null){
				task = new ProgressTask();
				task.execute();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void pause(){
		if (player != null && player.isPlaying())
			player.pause();		
	}
	
	private void stop(){
		if (player != null){
			player.stop();
			player.release();
		}
	}
	
	private void playerToPosition(int position){
		if (position > 0 && position < player.getDuration()){
			player.seekTo(position);
		}
	}	
	
	/********************************************************/
	/*					刷新播放界面用到的方法				*/
	private class ProgressTask extends AsyncTask<Void, Void, Void>{

		@Override
		protected Void doInBackground(Void... params) {
			while (true){
				Log.i(TAG, "isPlaying:" + player.isPlaying());
				SystemClock.sleep(1000);
				publishProgress();
			}
		}
		
		@Override
		protected void onProgressUpdate(Void... values) {
			if (player.isPlaying()){
				Message msg = Message.obtain();
				msg.what = ConstantValue.SEEKBAR_CHANGE;
				msg.arg1 = player.getCurrentPosition() + 1000;
				msg.arg2 = player.getDuration();
				// HandlerManager.getHandler().sendMessage(msg);
			}
			super.onProgressUpdate(values);
		}		
	}

	/************************************************/
	/*				接口实现的方法					*/
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		PromptManager.showToast(getApplicationContext(), "亲，音乐文件加载出错了");
		return false;
	}

	@Override
	public void onSeekComplete(MediaPlayer mp) {
		if (player.isPlaying())
			player.start();
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		HandlerManager.getHandler().sendEmptyMessage(ConstantValue.PLAY_END);
	}
}





























