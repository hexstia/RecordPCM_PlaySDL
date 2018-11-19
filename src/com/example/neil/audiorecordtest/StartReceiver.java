package com.example.neil.audiorecordtest;


import android.util.Log;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StartReceiver extends BroadcastReceiver {

	static final String ACTION = "android.intent.action.BOOT_COMPLETED";

	@Override
	public void onReceive(Context context, Intent intent) {

        Log.i("TTTT","START RECEIVER");
		if (intent.getAction().equals(ACTION)) {

            Intent mainActivityIntent = new Intent(context, StartService.class); // 要启动的Activity
			mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mainActivityIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
			
		    context.startService(mainActivityIntent);
		}
	}

}
