package com.stu.kmusic.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.widget.Toast;

public class PromptManager {
	private static ProgressDialog dialog;
	
	public static void showToast(Context context, String msg){
		Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
	}
}
