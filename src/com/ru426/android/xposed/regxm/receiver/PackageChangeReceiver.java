package com.ru426.android.xposed.regxm.receiver;

import com.ru426.android.xposed.regxm.Settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PackageChangeReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		if(intent != null && intent.getData() != null && intent.getData().getSchemeSpecificPart() != null){
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			if(intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED) | intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED)){
				prefs.edit().putString(Settings.DELETED_PACKAGE_NAME_KEY, intent.getData().getSchemeSpecificPart()).commit();
			}
			if(intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)){
				String deletedPackageName = intent.getData().getSchemeSpecificPart();
	        	if(deletedPackageName.length() > 0){
	        		for(String key : prefs.getAll().keySet()){
	        			if(key.contains(deletedPackageName)){
	        				prefs.edit().remove(key).commit();
	        			}
	        		}
				}
			}			
		}
	}
}
