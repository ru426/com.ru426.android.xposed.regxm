package com.ru426.android.xposed.regxm;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ru426.android.xposed.library.util.XModUtil;
import com.ru426.android.xposed.regxm.mod.XGXModOnlyNavigationBarModule;
import com.ru426.android.xposed.regxm.mod.XNotificationToolsModule;
import com.ru426.android.xposed.regxm.util.GetPluginsData;
import com.ru426.android.xposed.regxm.util.GetPluginsData.GetPluginsDataListener;
import com.ru426.android.xposed.regxm.util.GetPluginsDataFromMediaFire;
import com.ru426.android.xposed.regxm.util.GetPluginsDataFromMediaFire.GetPluginsDataFromMediaFireListener;
import com.ru426.android.xposed.regxm.util.PluginsDatabaseHelper;
import com.ru426.android.xposed.regxm.view.ModPreferenceFragment;
import com.ru426.android.xposed.regxm.view.ModPreferenceLongClickable;
import com.ru426.android.xposed.regxm.view.ModPreferenceLongClickable.ModPreferenceClickableListener;
import com.ru426.android.xposed.regxm.view.XWebView;
import com.ru426.android.xposed.regxm.view.XWebView.XWebViewListener;

public class Settings extends PreferenceActivity {
	public static final String PACKAGE_NAME = Settings.class.getPackage().getName();
	public static final String IS_GXMOD = "is_GXMod";
	public static final String DELETED_PACKAGE_NAME_KEY = PACKAGE_NAME + ".intent.preference.DELETED_PACKAGE_NAME_KEY";
	private static Context mContext;
	private static SharedPreferences prefs;
	private static GetPluginsData mGetPluginsData;
	private static GetPluginsDataFromMediaFire mGetPluginsDataFromMediaFire;
	private static HashMap<String, HashMap<String, String>> pluginSettingActivityData;
	private static Cursor pluginList;
	private static HashMap<String, Integer> categoryCountList = new HashMap<String, Integer>();
	private static ListView listView;
	private static List<Header> headerList;
	private static int pluginListSize = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setProgressBarIndeterminateVisibility(Boolean.FALSE);
		mContext = this;
		prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		prefs.edit().putString(DELETED_PACKAGE_NAME_KEY, "").commit();
		prefs.edit().putBoolean(IS_GXMOD, XModUtil.isGXModSystemUI(mContext)).commit();
		prefs.registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
		if(prefs.getBoolean(getString(R.string.ru_use_light_theme_key), false)){
			setTheme(android.R.style.Theme_DeviceDefault_Light);
			onChangeAppTheme();
		}
		super.onCreate(savedInstanceState);
	}
	
	@Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch(item.getItemId()){
		case android.R.id.home:
			finish();
			break;
		}
        return super.onMenuItemSelected(featureId, item);
    }
	
	private static void showHomeButton(){
		if(mContext != null && ((Activity) mContext).getActionBar() != null){
			((Activity) mContext).getActionBar().setHomeButtonEnabled(true);
	        ((Activity) mContext).getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}
	
	@Override
	public void onBuildHeaders(List<Header> target) {		
	    loadHeadersFromResource(R.xml.settings_main, target);
	    headerList = target;
		listView = getListView();
		initialPlugins();
		if(PluginsDatabaseHelper.hasCanUpgradePlugins(mContext))
			Toast.makeText(mContext, getString(R.string.update_notification), Toast.LENGTH_SHORT).show();
	}
	
	static void showRestartToast(){
		Toast.makeText(mContext, R.string.ru_restart_message, Toast.LENGTH_SHORT).show();
	}
	
	static void onChangeAppTheme(){
		if(mContext != null){
			Intent intent = new Intent(mContext.getString(R.string.ru_action_plugin_theme_settings_change));
			intent.putExtra(mContext.getString(R.string.ru_extra_plugin_theme_settings), prefs.getBoolean(mContext.getString(R.string.ru_use_light_theme_key), false));
			mContext.sendBroadcast(intent);			
		}
	}

	@Override
	protected void onStop() {
		if(mGetPluginsData != null && (mGetPluginsData.getStatus().equals(AsyncTask.Status.PENDING) | mGetPluginsData.getStatus().equals(AsyncTask.Status.RUNNING))){
			mGetPluginsData.cancel(true);
		}
		if(mGetPluginsDataFromMediaFire != null && (mGetPluginsDataFromMediaFire.getStatus().equals(AsyncTask.Status.PENDING) | mGetPluginsDataFromMediaFire.getStatus().equals(AsyncTask.Status.RUNNING))){
			mGetPluginsDataFromMediaFire.cancel(true);
		}		
		initialPlugins();
		super.onStop();
	}
	
	/// Get info plugins SettingActivity data and Module data.
	private static void initialPlugins(){
		if(mGetPluginsData != null && (mGetPluginsData.getStatus().equals(AsyncTask.Status.PENDING) | mGetPluginsData.getStatus().equals(AsyncTask.Status.RUNNING))){
			mGetPluginsData.cancel(true);
		}
		mGetPluginsData = new GetPluginsData();
		mGetPluginsData.setGetPluginsDataListener(mGetPluginsDataListener);
		mGetPluginsData.execute(new Object[]{mContext, prefs});
	}
	
	private static GetPluginsDataListener mGetPluginsDataListener = new GetPluginsDataListener(){
		@Override
		public void onPreExecute() {}
		@Override
		public void onPostExecute(boolean result) {
			if(!result){
				if(listView != null && listView.getCount() > 0){
					ArrayList<Header> list = new ArrayList<Header>();
					for(int i = 0; i < listView.getCount(); i++){
						Header header = (Header) listView.getAdapter().getItem(i);
						String title = (String) header.getTitle(mContext.getResources());
						if(title.equals(mContext.getString(R.string.settings_lockscreen_title))
								| title.equals(mContext.getString(R.string.settings_power_menu_title))
								| title.equals(mContext.getString(R.string.settings_behavior_and_etc_title))){
							list.add(header);
						}
						if(!XModUtil.isGXModSystemUI(mContext) && title.equals(mContext.getString(R.string.settings_gxmod_only_title))){
							list.add(header);
						}
					}
					if(list.size() > 0){
						for(Header header : list){
							 headerList.remove(header);
						}
					}
					@SuppressWarnings("unchecked")
					ArrayAdapter<Header> adapter = (ArrayAdapter<Header>) listView.getAdapter();
					adapter.notifyDataSetChanged();
				}
			}else{
				if(pluginSettingActivityData != null) pluginSettingActivityData.clear();
				if(categoryCountList != null) categoryCountList.clear();
				pluginSettingActivityData = makePluginData(GetPluginsData.MOD_SETTINGS_HEADER);
				if(pluginSettingActivityData != null && pluginSettingActivityData.size() > 0){
					Set<String> keys = pluginSettingActivityData.keySet();
					for(String key : keys){
						categoryCountList.put(pluginSettingActivityData.get(key).get("category"), categoryCountList.get(categoryCountList) != null ? categoryCountList.get(categoryCountList) + 1 : 0 + 1);
					}
					
					if(listView != null && listView.getCount() > 0){
						ArrayList<Header> list = new ArrayList<Header>();
						for(int i = 0; i < listView.getCount(); i++){
							Header header = (Header) listView.getAdapter().getItem(i);
							String title = (String) header.getTitle(mContext.getResources());
							if(title.equals(mContext.getString(R.string.settings_lockscreen_title))){
								int count = !categoryCountList.isEmpty() && categoryCountList.get(mContext.getString(R.string.ru_category_lockscreen)) != null ? categoryCountList.get(mContext.getString(R.string.ru_category_lockscreen)) : 0;
								if(count < 1) list.add(header);
							}else if(title.equals(mContext.getString(R.string.settings_power_menu_title))){
								int count = !categoryCountList.isEmpty() && categoryCountList.get(mContext.getString(R.string.ru_category_power_button)) != null ? categoryCountList.get(mContext.getString(R.string.ru_category_power_button)) : 0;
								if(count < 1) list.add(header);
							}else if(title.equals(mContext.getString(R.string.settings_behavior_and_etc_title))){
								int count = !categoryCountList.isEmpty() && categoryCountList.get(mContext.getString(R.string.ru_category_behavior_and_etc)) != null ? categoryCountList.get(mContext.getString(R.string.ru_category_behavior_and_etc)) : 0;
								if(count < 1) list.add(header);
							}else if(!XModUtil.isGXModSystemUI(mContext) && title.equals(mContext.getString(R.string.settings_gxmod_only_title))){
								list.add(header);
							}
						}
						if(list.size() > 0){
							for(Header header : list){
								 headerList.remove(header);
							}
						}
						@SuppressWarnings("unchecked")
						ArrayAdapter<Header> adapter = (ArrayAdapter<Header>) listView.getAdapter();
						adapter.notifyDataSetChanged();
					}
				}
			}
		}
	};
	
	public static OnPreferenceChangeListener onPreferenceChangeListener = new OnPreferenceChangeListener(){
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			PreferenceManager manager = preference.getPreferenceManager();
			switch(preference.getTitleRes()){
			case R.string.ru_use_light_theme_title:
				prefs.edit().putBoolean(mContext.getString(R.string.ru_use_light_theme_key), false).commit();
				Toast.makeText(mContext, R.string.ru_restart_app_message, Toast.LENGTH_SHORT).show();
				onChangeAppTheme();
				break;
			case R.string.add_brightnessbar_title:
				manager.findPreference(mContext.getString(R.string.move_brightnessbar_key)).setEnabled((Boolean) newValue);
			case R.string.move_brightnessbar_title:
			case R.string.move_tools_title:
			case R.string.show_carrier_label_title:
				sendToolsStateChangeIntent(preference, (Boolean) newValue);
				break;
			case R.string.settings_gxmod_only_keybuttonview_home_recent_key:
			case R.string.settings_gxmod_only_keybuttonview_optional_key:
				sendGXModOnlyStateChangeIntent(preference, (Boolean) newValue);
				break;
			}
			return true;
		}		
	};
	
	private static void sendToolsStateChangeIntent(Preference preference, boolean newValue){
		Intent intent = new Intent(XNotificationToolsModule.STATE_CHANGE);
		intent.putExtra(XNotificationToolsModule.STATE_EXTRA_ADD_BRIGHTNESSBAR, preference.getTitleRes() == R.string.add_brightnessbar_title ? newValue : prefs.getBoolean(mContext.getString(R.string.add_brightnessbar_key), false));
		intent.putExtra(XNotificationToolsModule.STATE_EXTRA_MOVE_BRIGHTNESSBAR, preference.getTitleRes() == R.string.move_brightnessbar_title ? newValue : prefs.getBoolean(mContext.getString(R.string.move_brightnessbar_key), false));
		intent.putExtra(XNotificationToolsModule.STATE_EXTRA_MOVE_TOOLSBAR, preference.getTitleRes() == R.string.move_tools_title ? newValue : prefs.getBoolean(mContext.getString(R.string.move_tools_key), false));
		intent.putExtra(XNotificationToolsModule.STATE_EXTRA_SHOW_CARRIER_NAME, preference.getTitleRes() == R.string.show_carrier_label_title ? newValue : prefs.getBoolean(mContext.getString(R.string.show_carrier_label_key), true));
		mContext.sendBroadcast(intent);
	}
	
	private static void sendGXModOnlyStateChangeIntent(Preference preference, boolean newValue){
		Intent intent = new Intent(XGXModOnlyNavigationBarModule.STATE_CHANGE);
		intent.putExtra(XGXModOnlyNavigationBarModule.STATE_EXTRA_DISABLE_HOME_RECENT, preference.getTitleRes() == R.string.settings_gxmod_only_keybuttonview_home_recent_title ? newValue : prefs.getBoolean(mContext.getString(R.string.settings_gxmod_only_keybuttonview_home_recent_key), false));
		intent.putExtra(XGXModOnlyNavigationBarModule.STATE_EXTRA_ENABLE_OPTIONAL_LONG_CLICK, preference.getTitleRes() == R.string.settings_gxmod_only_keybuttonview_optional_title ? newValue : prefs.getBoolean(mContext.getString(R.string.settings_gxmod_only_keybuttonview_optional_key), false));
		mContext.sendBroadcast(intent);
	}
	
	public static class SystemUI extends ModPreferenceFragment {
	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        addPreferencesFromResource(R.xml.settings_systemui);
	        if(getPreferenceScreen().getPreferenceCount() > 0){					
				for(int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++){
					if(getPreferenceScreen().getPreference(i) instanceof PreferenceCategory){
						ArrayList<Preference> list = new ArrayList<Preference>();
						PreferenceCategory preferenceCategory = (PreferenceCategory) getPreferenceScreen().getPreference(i);
						for(int j = 0; j < preferenceCategory.getPreferenceCount(); j++){
							Preference preference = preferenceCategory.getPreference(j);
							String title = (String) preference.getTitle();
							if(title.equals(mContext.getString(R.string.settings_systemui_notification_title))){
								int count = !categoryCountList.isEmpty() && categoryCountList.get(mContext.getString(R.string.ru_category_systemui_notification)) != null ? categoryCountList.get(mContext.getString(R.string.ru_category_systemui_notification)) : 0;
								if(count < 1) list.add(preference);
							}else if(title.equals(mContext.getString(R.string.settings_systemui_navigation_title))){
								int count = !categoryCountList.isEmpty() && categoryCountList.get(mContext.getString(R.string.ru_category_systemui_navigation)) != null ? categoryCountList.get(mContext.getString(R.string.ru_category_systemui_navigation)) : 0;
								if(count < 1) list.add(preference);
							}	
						}
						if(list.size() > 0){
							for(Preference preference : list){
								preferenceCategory.removePreference(preference);
							}
						}
					}
				}
			}
	    }
	}
	
	public static class SystemUINavigation extends ModPreferenceFragment {
	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        addPreferencesFromResource(R.xml.settings_plane);
	        makePluginPreference(getPreferenceScreen(), getActivity().getString(R.string.ru_category_systemui_navigation));
	    }
		@Override
		public void onResume() {
			super.onResume();
			if(getPreferenceScreen().getPreferenceCount() < 1){
				makePluginPreference(getPreferenceScreen(), getActivity().getString(R.string.ru_category_systemui_navigation));
			}
		}
	}
	
	public static class SystemUINotification extends ModPreferenceFragment {
	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        addPreferencesFromResource(R.xml.settings_plane);
	        makePluginPreference(getPreferenceScreen(), getActivity().getString(R.string.ru_category_systemui_notification));
	    }
	    @Override
		public void onResume() {
			super.onResume();
			if(getPreferenceScreen().getPreferenceCount() < 1){
				makePluginPreference(getPreferenceScreen(), getActivity().getString(R.string.ru_category_systemui_notification));
			}
		}
	}
	
	public static class SystemUITools extends ModPreferenceFragment {		
	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        addPreferencesFromResource(R.xml.settings_systemui_notification_tools);
	        findPreference(getString(R.string.move_brightnessbar_key)).setEnabled(((CheckBoxPreference) findPreference(getString(R.string.add_brightnessbar_key))).isChecked());
	        ((CheckBoxPreference) findPreference(getString(R.string.show_carrier_label_key))).setChecked(prefs.getBoolean(getString(R.string.show_carrier_label_key), true));
	        makePluginPreference(getPreferenceScreen(), getActivity().getString(R.string.ru_category_systemui_tools));
	    }
	    @Override
		public void onResume() {
			super.onResume();
			if(getPreferenceScreen().getPreferenceCount() < 2){
				makePluginPreference(getPreferenceScreen(), getActivity().getString(R.string.ru_category_systemui_tools));
			}
		}
	}
	
	public static class LockScreen extends ModPreferenceFragment {
	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        addPreferencesFromResource(R.xml.settings_plane);
	        makePluginPreference(getPreferenceScreen(), getActivity().getString(R.string.ru_category_lockscreen));
	    }
	    @Override
		public void onResume() {
			super.onResume();
			if(getPreferenceScreen().getPreferenceCount() < 1){
				makePluginPreference(getPreferenceScreen(), getActivity().getString(R.string.ru_category_lockscreen));
			}
		}
	}
	
	public static class PowerButtonMenu extends ModPreferenceFragment {
	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        addPreferencesFromResource(R.xml.settings_plane);
	        makePluginPreference(getPreferenceScreen(), getActivity().getString(R.string.ru_category_power_button));
	    }
	    @Override
		public void onResume() {
			super.onResume();
			if(getPreferenceScreen().getPreferenceCount() < 1){
				makePluginPreference(getPreferenceScreen(), getActivity().getString(R.string.ru_category_power_button));
			}
		}
	}
	
	public static class BehaviorAndEtc extends ModPreferenceFragment {
	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        addPreferencesFromResource(R.xml.settings_plane);
	        makePluginPreference(getPreferenceScreen(), getActivity().getString(R.string.ru_category_behavior_and_etc));
	    }
	    @Override
		public void onResume() {
			super.onResume();
			if(getPreferenceScreen().getPreferenceCount() < 1){
				makePluginPreference(getPreferenceScreen(), getActivity().getString(R.string.ru_category_behavior_and_etc));
			}
		}
	}
	
	public static class GXModOnly extends ModPreferenceFragment {
		@Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.settings_gxmod);			
		}
	}
		
	public static class PluginsList extends ModPreferenceFragment {
		HashMap<String, String> fileNameTextAndValue = new HashMap<String, String>();
	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        addPreferencesFromResource(R.xml.settings_plugin_list);
	        setHasOptionsMenu(true);
	        if(pluginListSize == 0 || pluginList.getCount() != pluginListSize){
	        	if(mGetPluginsDataFromMediaFire != null) mGetPluginsDataFromMediaFire.cancel(true);
	        	int theme = R.style.RuDialogDark;
	        	if(prefs.getBoolean(getString(R.string.ru_use_light_theme_key), false)){
	        		theme = R.style.RuDialogLight;
	    		}
	        	final ProgressDialog dialog = new ProgressDialog(getActivity(), theme);
	        	dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
	        	mGetPluginsDataFromMediaFire = new GetPluginsDataFromMediaFire();
	    		mGetPluginsDataFromMediaFire.setGetPluginsDataFromMediaFireListener(new GetPluginsDataFromMediaFireListener() {					
					@Override
					public void onPreExecute() {
					    dialog.setTitle(mContext.getString(R.string.loading_dialog_title));
					    dialog.setMessage(mContext.getString(R.string.loading_dialog_message));
					    dialog.setIndeterminate(true);
					    dialog.setCancelable(true);
					    dialog.setCanceledOnTouchOutside(true);
						dialog.setButton(
								DialogInterface.BUTTON_NEGATIVE,
								getString(android.R.string.cancel),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										Toast.makeText(getActivity(), getString(R.string.loading_dialog_canceled_message), Toast.LENGTH_SHORT).show();
										dialog.cancel();
									}
								});
					    dialog.show();
					}					
					@Override
					public void onPostExecute(Boolean result) {
						if(!result){
							Toast.makeText(mContext, getString(R.string.fail_to_get_list_of_plugin_from_network), Toast.LENGTH_SHORT).show();
						}else{
							pluginList = PluginsDatabaseHelper.getPluginsData(mContext);
							if(pluginList != null){
								if((pluginListSize = pluginList.getCount()) > 0)
									setPluginListPreferences(getPreferenceScreen());								
							}
						}
						dialog.cancel();
					}
				});
	    		mGetPluginsDataFromMediaFire.execute(new Object[]{getActivity()});
	        }else{
	        	setPluginListPreferences(getPreferenceScreen());
	        }
	    }
	    
	    private void setPluginListPreferences(PreferenceScreen preferenceScreen){
			if(pluginList != null && pluginList.getCount() > 0 && pluginList.moveToFirst()){
				do{
					String quickkey = pluginList.getString(pluginList.getColumnIndex("quickkey"));
		        	String filename = pluginList.getString(pluginList.getColumnIndex("filename"));
		        	final String packageName = filename.replaceFirst("[.][^.]+$", "");
		        	String appName = "";
		        	String description = pluginList.getString(pluginList.getColumnIndex("description"));
		        	Context context = null;
		        	String versionName = "1.0.0";
	        		String installedStateText = "";
		        	try {
						context = mContext.createPackageContext(packageName, Context.CONTEXT_RESTRICTED);
						if(context != null){
			        		Resources res = context.getResources();
			        		res.getConfiguration().locale = Locale.getDefault();
							int resId = res.getIdentifier("app_desc", "string", packageName);
			        		description = resId > 0 ? res.getString(resId) : description;						
			        		resId = res.getIdentifier("app_name", "string", packageName);
			        		appName = resId > 0 ? res.getString(resId) : "";
			        		PackageManager packageManager = context.getPackageManager();
			        		PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);
			        		versionName = packageInfo.versionName;
			        	}
					} catch (NameNotFoundException e) {
						installedStateText = mContext.getString(R.string.no_installed);
						if(description.split(",") != null){
							description = Locale.getDefault().equals(Locale.JAPAN) ? pluginList.getString(pluginList.getColumnIndex("ja")) : pluginList.getString(pluginList.getColumnIndex("en"));
						}
					}
		        	
		        	String created = pluginList.getString(pluginList.getColumnIndex("created"));
		        	String update = pluginList.getString(pluginList.getColumnIndex("update_date"));
		        	if(update == null) update = "";
		        	if(update.length() > 0) update = "\nupdate:" + update;
		        	
		        	String version = pluginList.getString(pluginList.getColumnIndex("version"));		        	
		        	if(version == null) version = "";
		        	String[] versionTexts = version.split("\\.");
	        		int verInt = 0;
	        		String verStr = "";
	        		for(String str : versionTexts){
	        			verStr += str;
	        		}
	        		try{
		        		verInt = verStr.length() > 0 ? Integer.parseInt(verStr) : 0;	        			
	        		}catch(NumberFormatException e){
	        			verInt = 0;
	        		}
	        		
	        		versionTexts = versionName.split("\\.");
	        		int verInt2 = 0;
	        		verStr = "";
	        		for(String str : versionTexts){
	        			verStr += str;
	        		}
	        		try{
	        			verInt2 = verStr.length() > 0 ? Integer.parseInt(verStr) : 0;        			
	        		}catch(NumberFormatException e){
	        			verInt2 = 0;
	        		}
	        		
	        		if(verInt > verInt2){
	        			PluginsDatabaseHelper.updateCanUpgradePlugins(mContext, 1, filename);
	        			installedStateText = mContext.getString(R.string.can_update);
	        		}else{
	        			version = versionName;
	        		}
	        		version = "\nversion:" + version + installedStateText;
	        		
		        	final String downLoadUrl = "http://www.mediafire.com/download/" + quickkey + "/" + filename;

		        	ModPreferenceLongClickable pluginPreference = new ModPreferenceLongClickable(mContext);
		        	pluginPreference.setOnModPreferenceClickableListener(new ModPreferenceClickableListener() {				
						@Override
						public void onClick(View v) {
							GetPluginWebView.setUrl(downLoadUrl);
							Intent intent = new Intent(mContext, Settings.class);
							intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, GetPluginWebView.class.getName());
							intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
							mContext.startActivity(intent);
						}
						@Override
						public boolean onLongClick(View v) {
							PackageManager pm = mContext.getPackageManager();
							List<ApplicationInfo> list = pm.getInstalledApplications(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
							boolean installed = false;
							for (ApplicationInfo ai : list) {
								if (installed = ai.packageName.equals(packageName))
									break;
							}
							if(installed){
								Settings.prefs.edit().putString(DELETED_PACKAGE_NAME_KEY, packageName).commit();
								((Settings) mContext).startActivityForResult(new Intent("android.settings.APPLICATION_DETAILS_SETTINGS", Uri.parse("package:" + packageName)), 0);						
							}
							return true;
						}
					});
					pluginPreference.setTitle(appName.length() > 0 ? appName : filename);
					if(description != null && description.length() > 0){
						pluginPreference.setSummary(description + update + version);
						if(pluginList.getInt(pluginList.getColumnIndex("update_flag")) == 1){
							pluginPreference.setIcon(R.drawable.ic_menu_notifications);
						}
					}else{
						pluginPreference.setSummary(created);
					}
					if(quickkey.length() > 0) preferenceScreen.addPreference(pluginPreference);
				}while(pluginList.moveToNext());
			}
			if(pluginList != null) pluginList.close();
			
			if(preferenceScreen.getPreferenceCount() > 0){
				PackageManager mPackageManager = mContext.getPackageManager();
		        Intent intent = new Intent();
		        intent.setAction(mContext.getString(R.string.ru_action_plugin_settings));
		        List<ResolveInfo> appInfoList = mPackageManager.queryIntentActivities(intent, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
				if (appInfoList != null && appInfoList.size() > 0) {
					Collections.sort(appInfoList,new ResolveInfo.DisplayNameComparator(mPackageManager));
					for (ResolveInfo app : appInfoList) {
						String label = (String) app.loadLabel(mPackageManager);
						final String packageName = app.activityInfo.packageName;
						boolean preferenceAdded = false;
						for(int i = 0; i < preferenceScreen.getPreferenceCount(); i++){
							if(preferenceScreen.getPreference(i).getTitle().toString().contains(label)){
								preferenceAdded = true;
								break;
							}
						}
						if(preferenceAdded) continue;
						ModPreferenceLongClickable pluginPreference = new ModPreferenceLongClickable(mContext);
			        	pluginPreference.setOnModPreferenceClickableListener(new ModPreferenceClickableListener() {				
							@Override
							public void onClick(View v) {
								Settings.prefs.edit().putString(DELETED_PACKAGE_NAME_KEY, packageName).commit();
								((Settings) mContext).startActivityForResult(new Intent("android.settings.APPLICATION_DETAILS_SETTINGS", Uri.parse("package:" + packageName)), 0);
							}
							@Override
							public boolean onLongClick(View v) {
								return false;
							}
						});
						pluginPreference.setTitle(label);
						ActivityInfo actInfo = null;
						try {
							actInfo = mPackageManager.getActivityInfo(new ComponentName(packageName, app.activityInfo.name), PackageManager.GET_META_DATA );
							String description = "";
							if(actInfo != null) description = actInfo.metaData.getString(mContext.getString(R.string.ru_description));
							if(description.length() > 0){
								pluginPreference.setSummary(description);
							}
						} catch (NameNotFoundException e) {
							e.printStackTrace();
						}
						preferenceScreen.addPreference(pluginPreference);
					}
				}				
			}
		}
	    
	    @Override
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {			
			super.onCreateOptionsMenu(menu, inflater);
			inflater.inflate(R.menu.plugin_list, menu);
			File downloadedFileDir = mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        	downloadedFileDir.mkdirs();
			File[] files = downloadedFileDir.listFiles();
			fileNameTextAndValue.clear();
			if(files != null && files.length > 0){
				SubMenu submenu = menu.findItem(R.id.showDownloadFolder).getSubMenu();
				submenu.clear();
				
				int idx = 0;
				for(File file : files){
					if(file.exists() && getExtension(file.getName()).toLowerCase(Locale.US).equals("apk")){
						String[] fileNameSplit = file.getName().split("\\.");
						String fileNameValue = "";
						if(fileNameSplit != null && fileNameSplit.length > 1){
							fileNameValue = fileNameSplit[fileNameSplit.length-2] + "." + fileNameSplit[fileNameSplit.length-1];
						}
						if(fileNameValue.length() <= 0) fileNameValue = file.getName();
						fileNameTextAndValue.put(fileNameValue, file.getName());
						submenu.add(Menu.NONE, idx, Menu.NONE, fileNameValue);
						idx++;
					}
				}
			} else {
				menu.removeItem(R.id.showDownloadFolder);
			}			
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			Intent intent;
	        switch (item.getItemId()){
	        case R.id.searchGooglePlay:
	        	Uri uri = Uri.parse("market://search?q=com.ru426.android.xposed.parts OR \"REGXM PLUGIN\" OR \"com.ru426.android.xposed.regxm.plugin\"");
				intent = new Intent(Intent.ACTION_VIEW, uri);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
	            break;
	        case R.id.showDownloadFolder:
	        	break;
	        case android.R.id.home:
	        	onDestroy();
				break;
	        default:
	        	File downloadedFileDir = mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
	        	String fileName = downloadedFileDir + "/" + fileNameTextAndValue.get(item.getTitle());
				intent = new Intent(Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.fromFile(new File(fileName)), "application/vnd.android.package-archive");
				startActivity(intent);
	        	return super.onOptionsItemSelected(item);
	        }
	        return true;
		}
		
		private String getExtension(String fullFilePath) {
			int extensionLastIndex;
			if (fullFilePath != null)
				if ((extensionLastIndex = fullFilePath.lastIndexOf(".")) != -1)
					return fullFilePath.substring(extensionLastIndex + 1);
			return null;
		}

		@Override
		public void onDestroy() {
			if(mGetPluginsDataFromMediaFire != null) mGetPluginsDataFromMediaFire.cancel(true);
			if(pluginList != null) pluginList.close();
			pluginListSize = 0;
			super.onDestroy();
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == 0){
            if(resultCode == RESULT_OK){// app is uninstalled
            	String deleteTargetPackageName = Settings.prefs.getString(DELETED_PACKAGE_NAME_KEY, "");
            	if(deleteTargetPackageName.length() > 0){
            		for(String key : prefs.getAll().keySet()){
            			if(key.contains(deleteTargetPackageName)){
            				prefs.edit().remove(key).commit();
            			}
            		}
            		callRegxmStateChange();
                	Settings.prefs.edit().putString(DELETED_PACKAGE_NAME_KEY, "").commit();
				}
            }
		}
	}

	public static class AppSettings extends ModPreferenceFragment {
		@Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.settings_app_settings);			
			Preference clearAllData = (Preference) findPreference(getText(R.string.clear_all_key));
			clearAllData.setOnPreferenceClickListener(new OnPreferenceClickListener() {				
				@Override
				public boolean onPreferenceClick(Preference preference) {
					showClearAllDialog();
					return false;
				}
			});
		}
	}
	
	public static HashMap<String, HashMap<String, String>> makePluginData(String modHeader){
		Map<String, ?> allPref = prefs.getAll();
		Set<String> keys = allPref.keySet();
		HashMap<String, HashMap<String, String>> pluginData = new HashMap<String, HashMap<String, String>>();
		try{
			for(String key : keys){
				if(key.contains(modHeader)){
					@SuppressWarnings("unchecked")
					Set<String> moduleData = (Set<String>) allPref.get(key);
					HashMap<String, String> keyAndValues = new HashMap<String, String>();
					for(String val : moduleData){
						String[] vals = val.split(":");
						try{
							keyAndValues.put(vals[0], vals[1]);
						}catch(Exception e){
							e.printStackTrace();
						}
					}
					if(keyAndValues.size() > 0) pluginData.put(key, keyAndValues);
				}
			}			
		}catch(Exception e){
			e.printStackTrace();
		}
		return pluginData;
	}
	
	private static void makePluginPreference(PreferenceScreen parent, String category){
		if(pluginSettingActivityData != null && pluginSettingActivityData.size() > 0){
			Set<String> keys = pluginSettingActivityData.keySet();
			for(String key : keys){
				if(pluginSettingActivityData.get(key).get("category").equals(category)){
					PreferenceCategory pluginPreferenceCategory = new PreferenceCategory(mContext);
					pluginPreferenceCategory.setKey(pluginSettingActivityData.get(key).get("canonicalClassName"));
					pluginPreferenceCategory.setTitle(pluginSettingActivityData.get(key).get("label"));
					Preference pluginPreference = new Preference(mContext);
					pluginPreference.setTitle(pluginSettingActivityData.get(key).get("label"));
					pluginPreference.setSummary(pluginSettingActivityData.get(key).get("description"));
					List<ResolveInfo> resolveInfo = XModUtil.getIsInstalled(mContext, pluginSettingActivityData.get(key).get("packageName"), "." + pluginSettingActivityData.get(key).get("simpleClassName"));
					if(resolveInfo != null){
						Intent intent = new Intent();
						intent.setClassName(pluginSettingActivityData.get(key).get("packageName"), pluginSettingActivityData.get(key).get("canonicalClassName"));
						pluginPreference.setIntent(intent);
					}					
					parent.addPreference(pluginPreferenceCategory);
					pluginPreferenceCategory.addPreference(pluginPreference);
				}
			}			
		}
	}
	
	private static void showClearAllDialog() {
        FragmentManager manager = ((Activity) mContext).getFragmentManager();
        ClearAllDialog dialog = new ClearAllDialog();
        int theme = prefs.getBoolean(mContext.getString(R.string.ru_use_light_theme_key), false) ? android.R.style.Theme_DeviceDefault_Light_Dialog : android.R.style.Theme_DeviceDefault_Dialog;
        dialog.setStyle(DialogFragment.STYLE_NO_FRAME, theme);
        dialog.show(manager, "dialog");
    }
 
    public static class ClearAllDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
        	Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getActivity().getString(R.string.clear_all_title));
            builder.setMessage(getActivity().getString(R.string.clear_all_dialog_title));
            builder.setPositiveButton(getActivity().getString(android.R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					prefs.edit().clear().commit();
					Toast.makeText(getActivity(), getActivity().getString(R.string.ru_cleared_pref_message), Toast.LENGTH_SHORT).show();
					Toast.makeText(mContext, R.string.ru_restart_app_message, Toast.LENGTH_SHORT).show();
				}
			});
            builder.setNegativeButton(getActivity().getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
            return builder.create();
        }
    }

	public static class AboutApp extends Fragment {
		int count;
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View layout = inflater.inflate(R.layout.about_app, container, false);
			if(layout != null){
				String translator = getResources().getString(R.string.translator);
                if (translator.isEmpty()) {
                	layout.findViewById(R.id.about_translator_label).setVisibility(View.GONE);
                	layout.findViewById(R.id.about_translator).setVisibility(View.GONE);
                } else {
                        ((TextView) layout.findViewById(R.id.about_translator)).setMovementMethod(LinkMovementMethod.getInstance());
                }
                
                ((TextView) layout.findViewById(R.id.about_developers)).setMovementMethod(LinkMovementMethod.getInstance());
                
                String libraries = getResources().getString(R.string.about_libraries);
                if (libraries.isEmpty()) {
                	layout.findViewById(R.id.about_libraries_label).setVisibility(View.GONE);
                	layout.findViewById(R.id.about_libraries).setVisibility(View.GONE);
                } else {
                        ((TextView) layout.findViewById(R.id.about_libraries)).setMovementMethod(LinkMovementMethod.getInstance());
                }
                
				try {
					String packageName = getActivity().getPackageName();
					String version = getActivity().getPackageManager().getPackageInfo(packageName, 0).versionName;
					((TextView) layout.findViewById(R.id.version)).setText(version);
				} catch (NameNotFoundException e) {
				}
				TextView appDesc = (TextView) layout.findViewById(R.id.appDesc);
				Resources res = getResources();
				InputStream inputStream = res.openRawResource(R.raw.app_desc);
				BufferedReader reader = null;
			    String str = "";
			    String result = "";
				try {
					reader = new BufferedReader(new InputStreamReader(inputStream, "utf-8"));				 
				    while((str = reader.readLine()) != null){
				    	result += str + "\n";
				    }
				} catch (IOException e) {
					e.printStackTrace();
				}finally {
				    if (reader != null) {
				    	try {
							reader.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
				    }
				}
				appDesc.setText(result);
				appDesc.setOnClickListener(new OnClickListener() {					
					@Override
					public void onClick(View view) {
						if(count > 5){
							prefs.edit().putBoolean(DEBUG_OPTION, !prefs.getBoolean(DEBUG_OPTION, false)).commit();
							String isDegug = prefs.getBoolean(DEBUG_OPTION, false) ? "ON":"OFF";
							Toast.makeText(mContext, getString(R.string.debug_option_changed, isDegug), Toast.LENGTH_SHORT).show();
							showRestartToast();
							count = 0;
							return;
						}
						count++;
					}
				});
			}
			return layout;
		}
		@Override
		public void onResume() {
			showHomeButton();
			super.onResume();
		}
		@Override
		public void onDestroy() {
			count = 0;
			super.onDestroy();
		}
	}
	public static final String DEBUG_OPTION = PACKAGE_NAME + ".IS_DEBUG";
	
	public static class GetPluginWebView extends Fragment {
		private static String url = "";
		public static String getUrl() {
			return url;
		}
		public static void setUrl(String url) {
			GetPluginWebView.url = url;
		}
		@Override
		public void onAttach(Activity activity) {
			activity.getActionBar().setTitle(R.string.settings_plugins_summary);
			super.onAttach(activity);
		}
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			container.setBackgroundColor(Color.WHITE);
			View layout = inflater.inflate(R.layout.get_plugin_webview, container, false);
			if(layout != null){
				XWebView webView = (XWebView) layout.findViewById(R.id.webView);
				if(getUrl().length() > 0){
					webView.setXWebViewListener(mXWebViewListener);
					webView.loadUrl(getUrl());
				}
			}
			return layout;
		}
		@Override
		public void onResume() {
			showHomeButton();
			super.onResume();
		}
	}
	
	static XWebViewListener mXWebViewListener = new XWebViewListener(){
		@Override
		public void onDownloadComplete(long id) {
			Toast.makeText(mContext, mContext.getString(R.string.please_install_message), Toast.LENGTH_LONG).show();
			((Settings)mContext).finish();
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			((Settings)mContext).setProgressBarIndeterminateVisibility(Boolean.TRUE);
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			((Settings)mContext).setProgressBarIndeterminateVisibility(Boolean.FALSE);
		}
	};
	
	public static void callRegxmStateChange(){
		initialPlugins();
		onChangeAppTheme();
	}
	
	OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {			
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			if(key.equals(DELETED_PACKAGE_NAME_KEY) && sharedPreferences.getString(key, "").equals(PACKAGE_NAME)){
				Settings.prefs.edit().putString(DELETED_PACKAGE_NAME_KEY, "").commit();
				callRegxmStateChange();
				Toast.makeText(mContext, R.string.ru_restart_app_message, Toast.LENGTH_SHORT).show();
			}
		}
	};
}
