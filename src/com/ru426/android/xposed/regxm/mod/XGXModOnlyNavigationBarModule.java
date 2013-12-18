package com.ru426.android.xposed.regxm.mod;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.ImageView;

import com.ru426.android.xposed.library.ModuleBase;
import com.ru426.android.xposed.library.util.XModUtil;
import com.ru426.android.xposed.regxm.R;
import com.ru426.android.xposed.regxm.Settings;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class XGXModOnlyNavigationBarModule extends ModuleBase {
	private static String TAG = XGXModOnlyNavigationBarModule.class.getSimpleName();
	public static final String STATE_CHANGE = XGXModOnlyNavigationBarModule.class.getName() + ".intent.action.STATE_CHANGE";
	public static final String STATE_EXTRA_DISABLE_HOME_RECENT = XGXModOnlyNavigationBarModule.class.getName() + ".intent.extra.DISABLE_HOME_RECENT";
	public static final String STATE_EXTRA_ENABLE_OPTIONAL_LONG_CLICK = XGXModOnlyNavigationBarModule.class.getName() + ".intent.extra.ENABLE_OPTIONAL_LONG_CLICK";

	public static boolean isGXMod = false;
	public static boolean isDisableRecentHome = false;
	public static boolean isEnableOptionalLongClick = false;	
	public static boolean isPerformLongClick = false;

	@Override
	public void init(XSharedPreferences prefs, ClassLoader classLoader, boolean isDebug) {
		super.init(prefs, classLoader, isDebug);
		
		isGXMod = prefs.getBoolean(Settings.IS_GXMOD, false);
		isDisableRecentHome = prefs.getBoolean(xGetString(R.string.settings_gxmod_only_keybuttonview_home_recent_key), false);
		isEnableOptionalLongClick = prefs.getBoolean(xGetString(R.string.settings_gxmod_only_keybuttonview_optional_key), false);
		
		Class<?> xKeyButtonView = XposedHelpers.findClass("com.android.systemui.statusbar.policy.KeyButtonView", classLoader);
		XposedBridge.hookAllConstructors(xKeyButtonView, new XC_MethodHook(){
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				super.afterHookedMethod(param);
				try{					
					if(isGXMod){
						xLog(TAG + " : " + "afterHookedMethod hookAllConstructors");
						ImageView mKeyButtonView = (ImageView) param.thisObject;
						mContext = mKeyButtonView.getContext();
						IntentFilter intentFilter = new IntentFilter();
						intentFilter.addAction(STATE_CHANGE);
						xRegisterReceiver(mContext, intentFilter);						
					}
				} catch (Throwable throwable) {
					XposedBridge.log(throwable);
				}
			}			
		});
		
		Object callback[] = new Object[4];
		callback[0] = int.class;
		callback[1] = int.class;
		callback[2] = long.class;
		callback[3] = new XC_MethodReplacement() {
			@Override
			protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
				try {
					xLog(TAG + " : " + "replaceHookedMethod sendEvent");
					if(!isDisableRecentHome){
						XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
						return null;
					}
					int flags = (Integer) param.args[1];
					int mCode = (Integer) XposedHelpers.getObjectField(param.thisObject, "mCode");
					String mCodeName = getCodeName(mCode);
					if(flags == KeyEvent.FLAG_LONG_PRESS){
						xLog(TAG + " : " + "KeyButtonView Hock flags is : FLAG_LONG_PRESS " + "[" + flags +"]");
						xLog(TAG + " : " + "KeyButtonView Hock codeName is : " + mCodeName);
						if(mCode == 3){
							boolean mSupportsLongpresss = (Boolean) XposedHelpers.getObjectField(param.thisObject, "mSupportsLongpress");
							mSupportsLongpresss = !isDisableRecentHome;
							param.args[1] = mSupportsLongpresss ? -1 : 1;
						}
					}
				} catch (Throwable throwable) {
					XposedBridge.log(throwable);
				}
				XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
				return null;
			}
		};
		try{
			if(isGXMod)
				XposedHelpers.findAndHookMethod(xKeyButtonView, "sendEvent", callback);
		}catch(NoSuchMethodError e){
			XposedBridge.log(e);
		}	
		
		Object callback2[] = new Object[2];
		callback2[0] = MotionEvent.class;
		callback2[1] = new XC_MethodReplacement() {
			@Override
			protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
				try {
					xLog(TAG + " : " + "replaceHookedMethod onTouchEvent");
					ImageView mKeyButtonView = (ImageView) param.thisObject;
					Context context = mKeyButtonView.getContext();
					final int action = ((MotionEvent) param.args[0]).getAction();
					switch (action) {
		            case MotionEvent.ACTION_UP:
						if(context != null && context.getResources() != null && context.getResources().getIdentifier("optional", "id",  context.getPackageName()) > 0 && mKeyButtonView.getId() == context.getResources().getIdentifier("optional", "id",  context.getPackageName())){
			            	XposedHelpers.setObjectField(param.thisObject, "mSupportsLongpress", true);
			            	XposedHelpers.callMethod(param.thisObject, "setPressed", false);
			            	if(!isPerformLongClick){
				            	Intent intent = new Intent("com.android.systemui.statusbar.OPTIONAL_BUTTON_CLICKED");
				            	context.sendBroadcast(intent);
			            	}
							isPerformLongClick = false;
							return true;							
						}
						isPerformLongClick = false;
					}
					XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
				} catch (Throwable throwable) {
					XposedBridge.log(throwable);
				}
				return true;
			}
		};
		try{
			if(isGXMod)
				XposedHelpers.findAndHookMethod(xKeyButtonView, "onTouchEvent", callback2);
		}catch(NoSuchMethodError e){
			XposedBridge.log(e);
		}
		
		Class<?> NavigationBarView = XposedHelpers.findClass("com.android.systemui.statusbar.phone.NavigationBarView", classLoader);
		Object callback3[] = new Object[2];
		callback3[0] = View.class;
		callback3[1] = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				super.afterHookedMethod(param);
				try{
					xLog(TAG + " : " + "afterHookedMethod setUpNavigationBarKeys");
					View mCurrentView = (View) XposedHelpers.getObjectField(param.thisObject, "mCurrentView");
					Context context = mCurrentView.getContext();
					if(context != null && XModUtil.isGXModSystemUI(context) && context.getResources() != null && context.getResources().getIdentifier("optional", "id",  context.getPackageName()) > 0){
						if(mCurrentView.findViewById(context.getResources().getIdentifier("optional", "id",  context.getPackageName())) != null){
							ImageView optional = (ImageView) mCurrentView.findViewById(context.getResources().getIdentifier("optional", "id",  context.getPackageName()));
							optional.setClickable(true);
							optional.setOnLongClickListener(null);
							optional.setOnLongClickListener(optionalOnLongClickListener);
						}
					}					
				} catch (Throwable throwable) {
					XposedBridge.log(throwable);
				}
			}			
		};
		try{
			if(isGXMod){
				XposedHelpers.findAndHookMethod(NavigationBarView, "setUpNavigationBarKeys", callback3);
				XposedHelpers.findAndHookMethod(NavigationBarView, "setUpLandNavigationBarKeys", callback3);				
			}
		}catch(NoSuchMethodError e){
		}
	}
		
	@Override
	protected void xOnReceive(Context context, Intent intent) {
		super.xOnReceive(context, intent);
		xLog(TAG + " : " + intent.getAction());
		if (intent.getAction().equals(STATE_CHANGE)) {
			isDisableRecentHome = intent.getBooleanExtra(STATE_EXTRA_DISABLE_HOME_RECENT, false);
			isEnableOptionalLongClick = intent.getBooleanExtra(STATE_EXTRA_ENABLE_OPTIONAL_LONG_CLICK, false);
		}
	}
	
	private static OnLongClickListener optionalOnLongClickListener =  new OnLongClickListener() {							
		@Override
		public boolean onLongClick(View v) {
			if(isEnableOptionalLongClick){
				Intent intent = new Intent("com.android.systemui.statusbar.OPTIONAL_BUTTON_LONG_CLICKED");
				v.getContext().sendBroadcast(intent);
				isPerformLongClick = true;				
			}
			return true;
		}
	};
	
	public static String getCodeName(int code){
		String result = "";
		switch(code){
		case 0:
			result = "UNKNOWN";
			break;
		case 3:
			result = "HOME";
			break;
		case 4:
			result = "BACK";
			break;
		case 26:
			result = "POWER";
			break;
		case 82:
			result = "MENU";
			break;
		case 84:
			result = "SEARCH";
			break;
		}
		return result;
	}
}
