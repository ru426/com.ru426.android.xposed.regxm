package com.ru426.android.xposed.regxm.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;

import com.ru426.android.xposed.library.R;

public class GetPluginsData extends AsyncTask<Object, Void, Boolean> {
	public static final String MOD_SETTINGS_HEADER = "ModSettings_";
	public static final String MOD_MODULES_HEADER = "ModModules_";
	public static final String MOD_PREFERENCES_HEADER = "ModPreferences_";
	public static final String LABEL = "label:";
	public static final String PACKAGE_NAME = "packageName:";
	public static final String SIMPLE_CLASS_NAME = "simpleClassName:";
	public static final String CANONICAL_CLASS_NAME = "canonicalClassName:";
	public static final String DESCRIPTION = "description:";
	public static final String CATEGORY = "category:";
	public static final String SOURCE_DIR = "sourceDir:";
	public static final String TARGET_PACKAGE_NAME = "targetPackageName:";
	
	private GetPluginsDataListener mGetPluginsDataListener;
	public interface GetPluginsDataListener{
		public void onPreExecute();
		public void onPostExecute(boolean result);
	}
	public void setGetPluginsDataListener(GetPluginsDataListener listener){
		mGetPluginsDataListener = listener;
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		if(mGetPluginsDataListener != null) mGetPluginsDataListener.onPreExecute();
	}

	@Override
	protected synchronized Boolean doInBackground(Object... params) {
		try {
			Context mContext = (Context) params[0];
			SharedPreferences prefs = (SharedPreferences) params[1];
			PackageManager mPackageManager = mContext.getPackageManager();
	        Intent intent = new Intent();
	        intent.setAction(mContext.getString(R.string.ru_action_plugin_settings));
	        List<ResolveInfo> appInfoList = mPackageManager.queryIntentActivities(intent, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
	        
			if (appInfoList != null && appInfoList.size() > 0) {
				Collections.sort(appInfoList,new ResolveInfo.DisplayNameComparator(mPackageManager));
				for (ResolveInfo app : appInfoList) {
					Set<String> data = new HashSet<String>();
					String label = (String) app.loadLabel(mPackageManager);
					String packageName = app.activityInfo.packageName;
					String canonicalClassName = app.activityInfo.name;
					ActivityInfo actInfo = mPackageManager.getActivityInfo(new ComponentName(packageName, canonicalClassName), PackageManager.GET_META_DATA );
					String description = actInfo.metaData.getString(mContext.getString(R.string.ru_description));
					String category = actInfo.metaData.getString(mContext.getString(R.string.ru_category));
					data.add(LABEL + label);
					data.add(PACKAGE_NAME + packageName);
					data.add(CANONICAL_CLASS_NAME + canonicalClassName);
					data.add(SIMPLE_CLASS_NAME + getSimpleClassName(canonicalClassName));
					data.add(DESCRIPTION+ description);
					data.add(CATEGORY+ category);
					prefs.edit().putStringSet(MOD_SETTINGS_HEADER + canonicalClassName, data).commit();
				}
			}
			appInfoList.clear();
			intent = new Intent();
	        intent.setAction(mContext.getString(R.string.ru_action_plugin_modules));
	        appInfoList = mPackageManager.queryIntentActivities(intent, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
	        
			if (appInfoList != null && appInfoList.size() > 0) {
				Collections.sort(appInfoList, new ResolveInfo.DisplayNameComparator(mPackageManager));
				String prevPackName = "";
				for (ResolveInfo app : appInfoList) {
					Set<String> data = new HashSet<String>();
					String packageName = app.activityInfo.packageName;
					String canonicalClassName = app.activityInfo.name;
					ApplicationInfo appInfo = mPackageManager.getApplicationInfo(packageName, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
					String sourceDir = appInfo.sourceDir;					
					ActivityInfo actInfo = mPackageManager.getActivityInfo(new ComponentName(packageName, canonicalClassName), PackageManager.GET_META_DATA );
					String targetPackageName = actInfo.metaData.getString(mContext.getString(R.string.ru_targetpackagename));
					data.add(PACKAGE_NAME + packageName);
					data.add(CANONICAL_CLASS_NAME + canonicalClassName);
					data.add(SIMPLE_CLASS_NAME + getSimpleClassName(canonicalClassName));
					data.add(SOURCE_DIR + sourceDir);
					data.add(TARGET_PACKAGE_NAME + targetPackageName);
					prefs.edit().putStringSet(MOD_MODULES_HEADER + canonicalClassName, data).commit();

					if(!prevPackName.equals(packageName)){
						Context context = mContext.createPackageContext(packageName, Context.CONTEXT_RESTRICTED);
						SharedPreferences source = context.getSharedPreferences(packageName, Activity.MODE_PRIVATE | Activity.MODE_MULTI_PROCESS);
						if(source.getAll().isEmpty()){
							source = context.getSharedPreferences(packageName + "_preferences", Activity.MODE_PRIVATE | Activity.MODE_MULTI_PROCESS);							
						}
						copyPreferences(packageName, source, prefs);
					}
					prevPackName = packageName;
				}
				return true;
			}			
		} catch (Exception e) {
		    e.printStackTrace();
		}
		return false;
	}

	@Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);
		if(mGetPluginsDataListener != null) mGetPluginsDataListener.onPostExecute(result);
	}
	
	private static String getSimpleClassName(String canonicalName) {
		if (canonicalName != null) {
			int dotIndex = canonicalName.lastIndexOf(".");
			if (dotIndex != -1)
				return canonicalName.substring(dotIndex + 1);
		}
		return canonicalName;
	}
	
	private static void copyPreferences(String packageName, SharedPreferences source, SharedPreferences target){
		Map<String, ?> pluginPrefAll = source.getAll();
		//initialize
		for(String key : pluginPrefAll.keySet()){
			String _key = MOD_PREFERENCES_HEADER + packageName + "." + key;
			target.edit().remove(_key);
		}
		for(String key : pluginPrefAll.keySet()){
			Object obj = pluginPrefAll.get(key);
			String _key = MOD_PREFERENCES_HEADER + packageName + "." + key;
			try{
				boolean value = (Boolean) obj;
				target.edit().putBoolean(_key, value).commit();
			}catch(ClassCastException e){
				try{
					int value = (Integer) obj;
					target.edit().putInt(_key, value).commit();
				}catch(ClassCastException e1){
					try{
						long value = (Long) obj;
						target.edit().putLong(_key, value).commit();
					}catch(ClassCastException e2){
						try{
							float value = (Float) obj;
							target.edit().putFloat(_key, value).commit();
						}catch(ClassCastException e3){
							try{
								String value = (String) obj;
								target.edit().putString(_key, value).commit();
							}catch(ClassCastException e4){
								try{
									@SuppressWarnings("unchecked")
									Set<String> value = (Set<String>) obj;
									target.edit().putStringSet(_key, value).commit();
								}catch(ClassCastException e5){
								}
							}
						}
					}
				}
			}
		}
	}
}
