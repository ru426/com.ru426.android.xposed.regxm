package com.ru426.android.xposed.regxm.mod;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ru426.android.xposed.library.ModuleBase;
import com.ru426.android.xposed.regxm.R;
import com.ru426.android.xposed.regxm.Settings;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

@SuppressLint("DefaultLocale")
public class XNotificationToolsModule extends ModuleBase {
	private static String TAG = XNotificationToolsModule.class.getSimpleName();
	public static final String STATE_CHANGE = XNotificationToolsModule.class.getName() + ".intent.action.STATE_CHANGE";
	public static final String STATE_EXTRA_ADD_BRIGHTNESSBAR = XNotificationToolsModule.class.getName() + ".intent.extra.ADD_BRIGHTNESSBAR";
	public static final String STATE_EXTRA_MOVE_TOOLSBAR = XNotificationToolsModule.class.getName() + ".intent.extra.MOVE_TOOLSBAR";
	public static final String STATE_EXTRA_MOVE_BRIGHTNESSBAR = XNotificationToolsModule.class.getName() + ".intent.extra.MOVE_BRIGHTNESSBAR";
	public static final String STATE_EXTRA_SHOW_CARRIER_NAME = XNotificationToolsModule.class.getName() + ".intent.extra.SHOW_CARRIER_NAME";

	static boolean useBrightnessbar = false;
	static boolean moveToolsBar = false;
	static boolean moveBrightnessBar = false;
	static boolean showCarrierName = true;
	
	static boolean isSetBrightnessbar = false;
	static boolean isMovedToolsBar = false;
	static boolean isMovedBrightnessBar = false;
	
	static View brightnessBar;
	static LinearLayout toolsRow0;
	static LinearLayout toolsRow1;
	static View statusBarHr;
	
	static FrameLayout mStatusBarWindow;
	static FrameLayout mNotificationPanel;
	static LinearLayout toolsExpandedHeader;
	static LinearLayout clockContainer;
	static RelativeLayout datetimeContainer;
	static LinearLayout toolsExpandedHeaderContainer;
	static ScrollView mScrollView;
	
	static RelativeLayout movedToolsContainer;
	static LinearLayout movedToolsContainerInner;
	
	static int mCloseViewHeight = 36;
	static int mCarrierLabelHeight = 24;
	static int mClearButtonLowerHeight = 0;
	static int mBrightnessBarHeight = 128;

	@Override
	public void init(XSharedPreferences prefs, ClassLoader classLoader, boolean isDebug) {
		super.init(prefs, classLoader, isDebug);
		
		useBrightnessbar = prefs.getBoolean(xGetString(R.string.add_brightnessbar_key), false);
		moveToolsBar = prefs.getBoolean(xGetString(R.string.move_tools_key), false);
		moveBrightnessBar = prefs.getBoolean(xGetString(R.string.move_brightnessbar_key), false);
		showCarrierName = prefs.getBoolean(xGetString(R.string.show_carrier_label_key), true);
		
		Class<?> xPhoneStatusBar = XposedHelpers.findClass("com.android.systemui.statusbar.phone.PhoneStatusBar", classLoader);
		Object callback[] = new Object[2];
		callback[0] = boolean.class;
		XC_MethodHook mHookMakeExpandedVisible = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {				
				super.afterHookedMethod(param);
				try{
					xLog(TAG + " : " + "afterHookedMethod makeExpandedVisible");
					if(!initialViews(param)) return;
					
					boolean mExpandedVisible = (Boolean) param.args[0];
					if(!mExpandedVisible){
						xLog(TAG + " : " + "afterHookedMethod makeExpandedVisible : mExpandedVisible is false");
						return;
					}
					
					if (moveBrightnessBar | moveToolsBar) {
						makeMoveToolsContainer();
					} else {
						removeFromParent(movedToolsContainer);
					}
					
					if(moveToolsBar){
						isMovedToolsBar = true;
						removeFromParent(toolsRow0);
						movedToolsContainerInner.addView(toolsRow0, 0);
						movedToolsContainerInner.addView(toolsRow1, 1);
						movedToolsContainerInner.addView(statusBarHr, 2);
					}else{
						if(isMovedToolsBar){
							isMovedToolsBar = false;
							removeFromParent(toolsRow0);
							toolsExpandedHeader.addView(toolsRow0, 0);
							toolsExpandedHeader.addView(toolsRow1, 1);
							toolsExpandedHeader.addView(statusBarHr, 2);
						}
					}

					if(useBrightnessbar){
						isSetBrightnessbar = true;
						makeBrightnessBar(false);
						if(brightnessBar != null ){
							removeFromParent(brightnessBar);
							if(moveBrightnessBar){
								movedToolsContainerInner.addView(brightnessBar);
							} else {
								if(datetimeContainer == null){
									toolsExpandedHeader.addView(brightnessBar, isMovedToolsBar ? 0 : 3);
								}else{
									toolsExpandedHeader.addView(brightnessBar, 0);
								}
							}							
						}
					} else {
						if(isSetBrightnessbar){
							isSetBrightnessbar = false;
							removeFromParent(brightnessBar);
						}
					}
					updateModNotificationPanelHeaderHeight();
					setMovedToolsContainerBottomMargin();
				} catch (Throwable throwable) {
					XposedBridge.log(throwable);
				}
			}
		};
		callback[1] = mHookMakeExpandedVisible;
		
		Object callback_afterMR1[] = new Object[1];
		callback_afterMR1[0] = mHookMakeExpandedVisible;
		try{
			// JB
			XposedHelpers.findAndHookMethod(xPhoneStatusBar, "makeExpandedVisible", callback);
		}catch(NoSuchMethodError e){
			try{
				// later JB MR1 and KitKat 
				XposedHelpers.findAndHookMethod(xPhoneStatusBar, "makeExpandedVisible", callback_afterMR1);
			}catch(NoSuchMethodError e1){
				XposedBridge.log(e1);
			}
		}
		
		Object callback2[] = new Object[2];
		callback2[0] = boolean.class;
		callback2[1] = new XC_MethodReplacement() {
			@Override
			protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
				try{
					if (showCarrierName) {
						XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
					} else {
						xLog(TAG + " : " + "replaceHookedMethod updateCarrierLabelVisibility");
						if(XposedHelpers.getObjectField(param.thisObject, "mCarrierLabel") != null){
							TextView carrierLabel = (TextView) XposedHelpers.getObjectField(param.thisObject, "mCarrierLabel");
							if(carrierLabel.getText().toString().length() > 0) carrierLabel.setVisibility(View.GONE);
						}
					}
				} catch (Throwable throwable) {
					XposedBridge.log(throwable);
				}
				return null;
			}
		};
		try{
			XposedHelpers.findAndHookMethod(xPhoneStatusBar, "updateCarrierLabelVisibility", callback2);
		}catch(NoSuchMethodError e){
			XposedBridge.log(e);
		}
		
		Object callback3[] = new Object[1];
		callback3[0] = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				super.afterHookedMethod(param);
				try{
					xLog(TAG + " : " + "afterHookedMethod setAreThereNotifications");			
					if(mContext != null){
						int resId = mContext.getResources().getIdentifier("clear_all_button_lower", "id",  mContext.getPackageName());
						if(resId > 0 && XposedHelpers.getObjectField(param.thisObject, "mClearButtonLower") != null){
							View mClearButtonLower = (View) XposedHelpers.getObjectField(param.thisObject, "mClearButtonLower");
							if(mClearButtonLower.getVisibility() == View.VISIBLE){
								mClearButtonLowerHeight = (int) xModuleResources.getDimension(R.dimen.notification_panel_header_base_height);
							}else{
								mClearButtonLowerHeight = 0;
							}
						}						
					}
				} catch (Throwable throwable) {
					XposedBridge.log(throwable);
				} 
			}			
		};
		try{
			XposedHelpers.findAndHookMethod(xPhoneStatusBar, "setAreThereNotifications", callback3);
		}catch(NoSuchMethodError e){
			XposedBridge.log(e);
		}
		
		Object callback4[] = new Object[1];
		callback4[0] = new XC_MethodReplacement() {
			@Override
			protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
				try{
					xLog(TAG + " : " + "replaceHookedMethod updateNotificationPanelHeaderHeight");
					if((useBrightnessbar && !moveBrightnessBar) | moveToolsBar){
						updateModNotificationPanelHeaderHeight();
					}else{
						XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
					}
				}  catch (Throwable throwable) {
					XposedBridge.log(throwable);
				}
				return null;
			}
		};
		try{
			if(isXperiaDevice()){
				XposedHelpers.findAndHookMethod(xPhoneStatusBar, "updateNotificationPanelHeaderHeight", callback4);
			}
		}catch(NoSuchMethodError e){
			XposedBridge.log(e);
		}
		
		Object callback5[] = new Object[1];
		callback5[0] = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				super.afterHookedMethod(param);
				try{
					xLog(TAG + " : " + "afterHookedMethod makeStatusBarView");
					initialViews(param);
				} catch (Throwable throwable) {
					XposedBridge.log(throwable);
				}
			}			
		};
		try{
			XposedHelpers.findAndHookMethod(xPhoneStatusBar, "makeStatusBarView", callback5);
		}catch(NoSuchMethodError e){
			XposedBridge.log(e);
		}
		
		Object callback6[] = new Object[2];
		callback6[0] = Configuration.class;
		callback6[1] = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				super.afterHookedMethod(param);
				try{
					xLog(TAG + " : " + "afterHookedMethod onConfigurationChanged");
					ViewGroup parent = null;
					if(brightnessBar != null && brightnessBar.getParent() != null){
						parent = (ViewGroup) brightnessBar.getParent();					
					}
					removeFromParent(brightnessBar);
					makeBrightnessBar(true);
					if(parent != null){
						if(datetimeContainer == null){
							parent.addView(brightnessBar, isMovedToolsBar ? 0 : 3);
						}else{
							parent.addView(brightnessBar, 0);
						}
					}
					updateModNotificationPanelHeaderHeight();					
					setMovedToolsContainerBottomMargin();
				} catch (Throwable throwable) {
					XposedBridge.log(throwable);
				}
			}			
		};
		try{
			XposedHelpers.findAndHookMethod(xPhoneStatusBar, "onConfigurationChanged", callback6);
		}catch(NoSuchMethodError e){
			XposedBridge.log(e);
		}
	}
	
	private static boolean initialViews(MethodHookParam param){
		if(XposedHelpers.getObjectField(param.thisObject, "mStatusBarWindow") == null){
			XposedBridge.log("Couldn't initialize Notifications ExpandedHeader");
			return false;
		}
		FrameLayout mStatusBarWindow = (FrameLayout) XposedHelpers.getObjectField(param.thisObject, "mStatusBarWindow");
		if(mContext == null) {
			mContext = mStatusBarWindow.getContext();
			IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction(STATE_CHANGE);
			mContext.registerReceiver(xModuleReceiver, intentFilter);
		}
		
		if(toolsExpandedHeader == null){
			try {
				mNotificationPanel = (FrameLayout) XposedHelpers.getObjectField(param.thisObject, "mNotificationPanel");
				mScrollView = (ScrollView) XposedHelpers.getObjectField(param.thisObject, "mScrollView");
			} catch (Exception e) {
				XposedBridge.log(e);
			}
			if (Build.VERSION_CODES.JELLY_BEAN_MR1 <= Build.VERSION.SDK_INT) {
				// mCloseViewHeight is nothing
			} else if (Build.VERSION_CODES.ICE_CREAM_SANDWICH <= Build.VERSION.SDK_INT
					&& Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
				try {
					mCloseViewHeight = (Integer) XposedHelpers.getObjectField(param.thisObject, "mCloseViewHeight");
				} catch (NoSuchFieldError e) {
					mCloseViewHeight = 36;
				}
			}
			
			try {
				int id = mContext.getResources().getIdentifier("notification_panel_tools_row_height", "dimen", mContext.getPackageName());
				if(id > 0)
					mBrightnessBarHeight = mContext.getResources().getDimensionPixelSize(id);
			} catch (Exception e) { }
			try {
				mCarrierLabelHeight = (Integer) XposedHelpers.getObjectField(param.thisObject, "mCarrierLabelHeight");
			} catch (Exception e) { }
			
			int toolsContainerId = mContext.getResources().getIdentifier("tools_expanded", "id", mContext.getPackageName());//GXMod
			if(toolsContainerId <= 0) toolsContainerId = mContext.getResources().getIdentifier("expand_header", "id", mContext.getPackageName());//GX
			if(toolsContainerId <= 0) toolsContainerId = mContext.getResources().getIdentifier("header", "id", mContext.getPackageName());//Z
			
			if(toolsContainerId > 0 && mStatusBarWindow.findViewById(toolsContainerId) != null){
				toolsExpandedHeader = (LinearLayout) mStatusBarWindow.findViewById(toolsContainerId);
			}			
			
			if(toolsExpandedHeader != null){
				for(int i = 0; i < toolsExpandedHeader.getChildCount(); i++){
					if(toolsExpandedHeader.getChildAt(i) instanceof LinearLayout){
						LinearLayout view = (LinearLayout) toolsExpandedHeader.getChildAt(i);
						if(view.getId() == mContext.getResources().getIdentifier("tools_row_0", "id",  mContext.getPackageName())){
							toolsRow0 = view;									
						}else if(view.getId() == mContext.getResources().getIdentifier("tools_row_1", "id",  mContext.getPackageName())){
							toolsRow1 = view;
						}else{	
							if(view.getChildCount() > 2){
								clockContainer = view;
							}else{//GXMod brightnessBar
								brightnessBar = view;
								removeFromParent(brightnessBar);
							}
						}
					} else if (toolsExpandedHeader.getChildAt(i) instanceof View){
						statusBarHr = (View) toolsExpandedHeader.getChildAt(i);
					} else if (toolsExpandedHeader.getChildAt(i) instanceof RelativeLayout){// For Not SONY Xperia
						if(toolsExpandedHeader.getChildAt(i).getId() == mContext.getResources().getIdentifier("datetime", "id",  mContext.getPackageName())){
							datetimeContainer = (RelativeLayout) toolsExpandedHeader.getChildAt(i);
							break;
						}
					}
				}
			}
		}
		return true;
	}
	
	private static void makeBrightnessBar(boolean force){
		if(!force && brightnessBar != null) return;
		brightnessBar = (LinearLayout) makeToolbarBrightnessController(mContext);
	}

	private static View makeToolbarBrightnessController(Context context){
		if(context == null){
			XposedBridge.log(TAG + " : " + "makeToolbarBrightnessController mContext is null");
			return null;
		}
		Context regxmContext = null;
		try {
			regxmContext = context.createPackageContext(Settings.PACKAGE_NAME, 3);
		} catch (NameNotFoundException e) {
			XposedBridge.log(e);
		}
		if(regxmContext != null){
			return View.inflate(regxmContext, R.layout.toolbar_brightnessbar_main, null);
		}
		return null;
	}
	
	private static void makeMoveToolsContainer(){
		if(mContext == null){
			XposedBridge.log(TAG + " : " + "makeMoveToolsContainer mContext is null");
			return;
		}
		if(movedToolsContainer == null){
			LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			movedToolsContainer = new RelativeLayout(mContext);
			movedToolsContainer.setLayoutParams(layoutParams);		
			movedToolsContainerInner = new LinearLayout(mContext);
			layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			layoutParams.gravity = Gravity.BOTTOM;
			movedToolsContainerInner.setOrientation(LinearLayout.VERTICAL);
			movedToolsContainerInner.setLayoutParams(layoutParams);
			movedToolsContainer.addView(movedToolsContainerInner);			
		}
		setMovedToolsContainerBottomMargin();		
	}
	
	private static void setMovedToolsContainerBottomMargin(){
		if(mScrollView != null){		
			LinearLayout container = null;
			if(mScrollView.getParent() instanceof LinearLayout){
				container = (LinearLayout) mScrollView.getParent();
			}else{// after JELLY_BEAN_MR1
				if(mScrollView.getParent().getParent() instanceof LinearLayout){
					container = (LinearLayout) mScrollView.getParent().getParent();
				}
			}
			if(mScrollView.getParent() instanceof FrameLayout){// after JELLY_BEAN_MR1
				FrameLayout.LayoutParams frameLayoutParams = new FrameLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
				mScrollView.setLayoutParams(frameLayoutParams);
			}else if(mScrollView.getParent() instanceof LinearLayout){
				LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1);
				mScrollView.setLayoutParams(layoutParams);
			}
			
			LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			int targetHeight = 0;
			targetHeight += mCloseViewHeight;
			if(showCarrierName) targetHeight += mCarrierLabelHeight;
			targetHeight += mClearButtonLowerHeight;
			layoutParams.bottomMargin = targetHeight;
			removeFromParent(movedToolsContainer);
			if(movedToolsContainer!= null && container != null){
				movedToolsContainer.setLayoutParams(layoutParams);
				container.addView(movedToolsContainer);
			}
		}
	}
	
	private static void updateModNotificationPanelHeaderHeight(){
		if(toolsExpandedHeader != null && mContext != null){
			LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			for(int i = 0; i < toolsExpandedHeader.getChildCount(); i++){
				if(brightnessBar == toolsExpandedHeader.getChildAt(i)){
					layoutParams.height += mBrightnessBarHeight;
				}else{
					if(toolsExpandedHeader.getChildAt(i).getVisibility() == View.VISIBLE){
						if(toolsExpandedHeader.getChildAt(i).getLayoutParams() != null)
							layoutParams.height += toolsExpandedHeader.getChildAt(i).getLayoutParams().height;
						if(toolsExpandedHeader.getChildAt(i).getId() == mContext.getResources().getIdentifier("tools_row_0", "id", mContext.getPackageName())
								| toolsExpandedHeader.getChildAt(i).getId() == mContext.getResources().getIdentifier("tools_row_1", "id",  mContext.getPackageName())){
							layoutParams.height += 4; // adjust paddin value
						}
					}
				}
			}
			toolsExpandedHeader.setLayoutParams(layoutParams);			
		}
	}
	
	private static void removeFromParent(View view){
		if(view == toolsRow0 | view == toolsRow1 | view == statusBarHr){
			if(toolsRow0 != null && toolsRow0.getParent() != null){
				ViewGroup parent = (ViewGroup) toolsRow0.getParent();
				parent.removeView(toolsRow0);
			}
			if(toolsRow1 != null && toolsRow1.getParent() != null){
				ViewGroup parent = (ViewGroup) toolsRow1.getParent();
				parent.removeView(toolsRow1);
			}
			if(statusBarHr != null && statusBarHr.getParent() != null){
				ViewGroup parent = (ViewGroup) statusBarHr.getParent();
				parent.removeView(statusBarHr);
			}
		} else {
			if(view != null && view.getParent() != null){
				ViewGroup parent = (ViewGroup) view.getParent();
				parent.removeView(view);							
			}
		}
	}
	
	private static BroadcastReceiver xModuleReceiver = new BroadcastReceiver() {		
		@Override
		public void onReceive(Context context, Intent intent) {
			XposedBridge.log(TAG + " : " + intent.getAction());
			if (intent.getAction().equals(STATE_CHANGE)) {
				useBrightnessbar = intent.getBooleanExtra(STATE_EXTRA_ADD_BRIGHTNESSBAR, false);
				moveToolsBar = intent.getBooleanExtra(STATE_EXTRA_MOVE_TOOLSBAR, false);
				moveBrightnessBar = intent.getBooleanExtra(STATE_EXTRA_MOVE_BRIGHTNESSBAR, false);
				showCarrierName = intent.getBooleanExtra(STATE_EXTRA_SHOW_CARRIER_NAME, true);
			}
		}
	};
	
	public static boolean isXperiaDevice() {
        if (mIsXperiaDevice != null) return mIsXperiaDevice;

        mIsXperiaDevice = Build.MANUFACTURER.equalsIgnoreCase("sony")  && !isMtkDevice();
        return mIsXperiaDevice;
    }
	
	@SuppressLint("DefaultLocale")
	public static boolean isMtkDevice() {
        if (mIsMtkDevice != null) return mIsMtkDevice;

        mIsMtkDevice = MTK_DEVICES.contains(Build.HARDWARE.toLowerCase());
        return mIsMtkDevice;
    }
	
	private static Boolean mIsXperiaDevice = null;
	private static Boolean mIsMtkDevice = null;
	// Supported MTK devices
    private static final Set<String> MTK_DEVICES = new HashSet<String>(Arrays.asList(
        new String[] {"mt6572","mt6575","mt6577","mt8377","mt6582","mt6589","mt8389"}
    ));
}