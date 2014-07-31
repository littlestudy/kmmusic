package com.stu.kmusic;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.stu.kmusic.adapter.MySongListAdapter;
import com.stu.kmusic.bean.Music;
import com.stu.kmusic.receiver.ScanSdFilesReceiver;
import com.stu.kmusic.service.MediaService;
import com.stu.kmusic.util.HandlerManager;
import com.stu.kmusic.util.MediaUtil;
import com.stu.kmusic.util.PromptManager;

public class MainActivity extends Activity {

	private ListView songListView;
	private MySongListAdapter songAdapter;
	
	private ScanSdFilesReceiver scanSdFilesReceiver;
	private ImageView reflashSongListImageView;
	
	private Handler handler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case ConstantValue.STARTED: // 开始刷新播放列表界面
				PromptManager.showProgressDialog(MainActivity.this);
				break; 
			case ConstantValue.FINISHED: // 结束刷新播放列表界面
				MediaUtil.getInstance().initMusics(MainActivity.this);
				PromptManager.closeProgressDialog();
				songAdapter.notifyDataSetChanged();
				unregisterReceiver(scanSdFilesReceiver);
				break;
			case ConstantValue.PLAY_END:
				// 播放完成
				// 播放模式：单曲循环、顺序播放、循环播放、随机播放
				// 单曲循环:记录当前播放位置
				// 顺序播放:当前播放位置上＋1
				// 循环播放:判断如果，增加的结果大于songList的大小，修改播放位置为零
				// 随机播放:Random.nextInt() songList.size();
				
				// 顺序播放的实现
				MediaUtil.CURRENTPOS++;
				if (MediaUtil.CURRENTPOS < MediaUtil.getInstance().getSongList().size()){
					Music music = MediaUtil.getInstance().getSongList().get(MediaUtil.CURRENTPOS);
					startPlayService(music, ConstantValue.OPTION_PLAY);
					changeNotice(Color.GREEN);
				}				
				break;
			}
		};
	};

	/************* 音乐控制 ****************/
	private ImageView playPause;// 播放暂停
	private ImageView playNext;// 播放下一首
	private ImageView playPrev;// 播放上一首
	private ImageView playMode;// 修改播放模式
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		HandlerManager.putHandler(handler);
		init();
		setListener();
	}
	
	private void init(){
		loadSongList();
		mediaController();
		reflashSongList();
	}
	
	private void setListener(){
		playPause.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				switch (MediaUtil.PLAYSTATE) {
				case ConstantValue.OPTION_PLAY:
				case ConstantValue.OPTION_CONTINUE:
					startPlayService(null, ConstantValue.OPTION_PAUSE);
					playPause.setImageResource(R.drawable.appwidget_pause);
					break;
				case ConstantValue.OPTION_PAUSE:
					if (MediaUtil.CURRENTPOS >= 0
							&& MediaUtil.CURRENTPOS < MediaUtil.getInstance().getSongList().size()){
						startPlayService(MediaUtil.getInstance().getSongList().get(MediaUtil.CURRENTPOS)
								, ConstantValue.OPTION_CONTINUE);
						playPause.setImageResource(R.drawable.img_playback_bt_play);
					}
					break;
				}
			}
		});
		
		playNext.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				if (MediaUtil.getInstance().getSongList().size() > MediaUtil.CURRENTPOS + 1){
					changeNotice(Color.WHITE);
					MediaUtil.CURRENTPOS++;
					startPlayService(MediaUtil.getInstance().getSongList().get(MediaUtil.CURRENTPOS)
							, ConstantValue.OPTION_PLAY);
					playPause.setImageResource(R.drawable.img_playback_bt_play);
					MediaUtil.PLAYSTATE = ConstantValue.OPTION_PLAY;
					changeNotice(Color.GREEN);
				}
			}
		});
		
		playPrev.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				if (MediaUtil.CURRENTPOS > 0){
					changeNotice(Color.WHITE);
					MediaUtil.CURRENTPOS--;
					startPlayService(MediaUtil.getInstance().getSongList().get(MediaUtil.CURRENTPOS)
							, ConstantValue.OPTION_PLAY);
					playPause.setImageResource(R.drawable.img_playback_bt_play);
					MediaUtil.PLAYSTATE = ConstantValue.OPTION_PLAY;
					changeNotice(Color.GREEN);
				}
			}
		});
		
		songListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				changeNotice(Color.WHITE);
				MediaUtil.CURRENTPOS = position;
				Music music = MediaUtil.getInstance().getSongList().get(MediaUtil.CURRENTPOS);
				startPlayService(music, ConstantValue.OPTION_PLAY);
				playPause.setImageResource(R.drawable.img_playback_bt_play);
				changeNotice(Color.GREEN);
			}
		});
	}
	
	private void loadSongList(){
		songAdapter = new MySongListAdapter(getApplicationContext());
		songListView = (ListView)findViewById(R.id.play_list);
		songListView.setAdapter(songAdapter);
		
		// new InitDataTask().execute();//线程池，如操作的线程过多，等待情况
		new InitDataTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR); // 不用等待
	}
	
	private void mediaController(){
		playPause = (ImageView)findViewById(R.id.imgPlay);
		playPrev  = (ImageView)findViewById(R.id.imgPrev);
		playNext  = (ImageView)findViewById(R.id.imgNext);
		
		if (MediaUtil.PLAYSTATE == ConstantValue.OPTION_PAUSE){
			playPause.setImageResource(R.drawable.appwidget_pause);
		}
	}
	
	private void reflashSongList(){ // 刷新按钮的处理
		reflashSongListImageView = (ImageView)findViewById(R.id.title_right);
		reflashSongListImageView.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				reflash();
			}
		});
	}
	
	private void startPlayService(Music music, int option){
		Intent intent = new Intent(getApplicationContext(), MediaService.class);
		if (music != null){
			intent.putExtra("file", music.getPath());
		}
		intent.putExtra("option", option);
		startService(intent);		
	}
	
	private void changeNotice(int color){
		/* 在ListView中，根据tag找包含这个tag的View。tag的设置在
		 * public class MySongListAdapter extends BaseAdapter {
		 * 		public View getView(int position, View convertView, ViewGroup parent) {
		 * 		...............
		 * 		holder.tx1.setTag(position);
		 * 		...............
		 * 		}
		 * }
		 */
		TextView tx = (TextView)songListView.findViewWithTag(MediaUtil.CURRENTPOS);
		if (tx != null){
			tx.setTextColor(color);
		}
	}
	
	public void reflash(){
		/* 使用模拟sd卡插拔来使系统刷新sd卡数据
		 * 第一种模拟方法
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_MEDIA_MOUNTED);
		intent.setData(Uri.fromFile(Environment.getExternalStorageDirectory()));
		sendBroadcast(intent);
		*/
		
		IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_SCANNER_STARTED);
		intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
		intentFilter.addDataScheme("file");
		scanSdFilesReceiver = new ScanSdFilesReceiver();
		registerReceiver(scanSdFilesReceiver, intentFilter);
		sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED
				, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
	}
	
	/**
	 * 音乐资源过多
	 */	
	class InitDataTask extends AsyncTask<Void, Void, Void>{
		@Override
		protected void onPreExecute() {
			PromptManager.showProgressDialog(MainActivity.this);
		}
		@Override
		protected Void doInBackground(Void... params) {
			MediaUtil.getInstance().initMusics(MainActivity.this); // 加载多媒体信息
			SystemClock.sleep(1000);
			return null;
		}
		@Override
		protected void onPostExecute(Void result) {
			PromptManager.closeProgressDialog();
			songAdapter.notifyDataSetChanged();
		}
	}
	
}



























