package com.stu.kmusic.receiver;

import com.stu.kmusic.ConstantValue;
import com.stu.kmusic.util.HandlerManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ScanSdFilesReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (Intent.ACTION_MEDIA_SCANNER_STARTED.equals(action)){
			HandlerManager.getHandler().sendEmptyMessage(ConstantValue.STARTED);
		} 
		if (Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(action)){
			HandlerManager.getHandler().sendEmptyMessage(ConstantValue.FINISHED);
		}
	}

}
