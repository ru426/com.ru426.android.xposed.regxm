package com.ru426.android.xposed.regxm.mod;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
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
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

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
		callback[1] = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {				
				super.afterHookedMethod(param);
				try{
					xLog(TAG + " : " + "afterHookedMethod makeExpandedVisible");
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
								toolsExpandedHeader.addView(brightnessBar, isMovedToolsBar ? 0 : 3);
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
		try{
			XposedHelpers.findAndHookMethod(xPhoneStatusBar, "makeExpandedVisible", callback);
		}catch(NoSuchMethodError e){
			XposedBridge.log(e);
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
			XposedHelpers.findAndHookMethod(xPhoneStatusBar, "updateNotificationPanelHeaderHeight", callback4);
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
					try {
						mStatusBarWindow = (FrameLayout) XposedHelpers.getObjectField(param.thisObject, "mStatusBarWindow");											
					} catch (Exception e) {
						XposedBridge.log(e);
					}
					try {
						mNotificationPanel = (FrameLayout) XposedHelpers.getObjectField(param.thisObject, "mNotificationPanel");
					} catch (Exception e) {
						XposedBridge.log(e);
					}
					try {
						mScrollView = (ScrollView) XposedHelpers.getObjectField(param.thisObject, "mScrollView");
					} catch (Exception e) {
						XposedBridge.log(e);
					}
					try {
						mCloseViewHeight = (Integer) XposedHelpers.getObjectField(param.thisObject, "mCloseViewHeight");
					} catch (Exception e) {
						XposedBridge.log(e);
					}
					try {
						mCarrierLabelHeight = (Integer) XposedHelpers.getObjectField(param.thisObject, "mCarrierLabelHeight");						
					} catch (Exception e) {
						XposedBridge.log(e);
					}
					
					if(mStatusBarWindow == null){
						XposedBridge.log("Couldn't initialize Notifications ExpandedHeader");
						return;
					}					
					mContext = mStatusBarWindow.getContext();					
					if(mContext == null){
						XposedBridge.log("mContext is null");
						return;
					}
					
					int toolsContainerId = mContext.getResources().getIdentifier("tools_expanded", "id", mContext.getPackageName());//GXMod
					if(toolsContainerId <= 0) toolsContainerId = mContext.getResources().getIdentifier("expand_header", "id", mContext.getPackageName());//GX
					if(toolsContainerId <= 0) toolsContainerId = mContext.getResources().getIdentifier("header", "id", mContext.getPackageName());//Z
					
					toolsExpandedHeader = (LinearLayout) mStatusBarWindow.findViewById(toolsContainerId);
					
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
										//clockContainer
										clockContainer = view;
									}else{
										//GXMod brightnessBar
										brightnessBar = view;
										removeFromParent(brightnessBar);
									}
								}
							} else if (toolsExpandedHeader.getChildAt(i) instanceof View){
								statusBarHr = (View) toolsExpandedHeader.getChildAt(i);
							}
						}
						IntentFilter intentFilter = new IntentFilter();
						intentFilter.addAction(STATE_CHANGE);
						xRegisterReceiver(mContext, intentFilter);
					}
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
					if(parent != null) parent.addView(brightnessBar);
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
	
	private static void makeBrightnessBar(boolean force){
		if(!force && brightnessBar != null) return;
		brightnessBar = (LinearLayout) makeToolbarBrightnessController(mContext);
	}

	private static View makeToolbarBrightnessController(Context context){
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
			LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1);
			mScrollView.setLayoutParams(layoutParams);
			LinearLayout parent = null;
			if(mScrollView.getParent() instanceof LinearLayout){
				parent = (LinearLayout) mScrollView.getParent();
			}else{//Z
				if(mScrollView.getParent().getParent() instanceof LinearLayout){
					parent = (LinearLayout) mScrollView.getParent().getParent();
				}
			}
			layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			int targetHeight = 0;
			targetHeight += mCloseViewHeight;
			if(showCarrierName) targetHeight += mCarrierLabelHeight;
			targetHeight += mClearButtonLowerHeight;
			layoutParams.bottomMargin = targetHeight;
			removeFromParent(movedToolsContainer);
			if(movedToolsContainer!= null && parent != null){
				movedToolsContainer.setLayoutParams(layoutParams);
				parent.addView(movedToolsContainer);
			}
		}
	}
	
	private static void updateModNotificationPanelHeaderHeight(){
		if(toolsExpandedHeader != null && mContext != null && toolsRow0 != null){
			LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			for(int i = 0; i < toolsExpandedHeader.getChildCount(); i++){
				if(brightnessBar == toolsExpandedHeader.getChildAt(i)){
					layoutParams.height += mBrightnessBarHeight;
				}else{
					if(toolsExpandedHeader.getChildAt(i).getVisibility() == View.VISIBLE){
						layoutParams.height += toolsExpandedHeader.getChildAt(i).getLayoutParams().height;
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
	
	@Override
	protected void xOnReceive(Context context, Intent intent) {		
		super.xOnReceive(context, intent);
		xLog(TAG + " : " + intent.getAction());
		if (intent.getAction().equals(STATE_CHANGE)) {
			useBrightnessbar = intent.getBooleanExtra(STATE_EXTRA_ADD_BRIGHTNESSBAR, false);
			moveToolsBar = intent.getBooleanExtra(STATE_EXTRA_MOVE_TOOLSBAR, false);
			moveBrightnessBar = intent.getBooleanExtra(STATE_EXTRA_MOVE_BRIGHTNESSBAR, false);
			showCarrierName = intent.getBooleanExtra(STATE_EXTRA_SHOW_CARRIER_NAME, true);
			xLog(TAG + " : " + "useBrightnessbar is " + useBrightnessbar);
			xLog(TAG + " : " + "moveToolsBar is " + moveToolsBar);
			xLog(TAG + " : " + "moveBrightnessBar is " + moveBrightnessBar);
			xLog(TAG + " : " + "showCarrierName is " + showCarrierName);
		}
	}
}