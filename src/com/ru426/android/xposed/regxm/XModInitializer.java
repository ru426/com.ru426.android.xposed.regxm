package com.ru426.android.xposed.regxm;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.content.res.XModuleResources;

import com.ru426.android.xposed.regxm.mod.XGXModOnlyNavigationBarModule;
import com.ru426.android.xposed.regxm.mod.XNotificationToolsModule;
import com.ru426.android.xposed.regxm.util.GetPluginsData;

import dalvik.system.PathClassLoader;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XModInitializer implements IXposedHookLoadPackage, IXposedHookZygoteInit, IXposedHookInitPackageResources {	
	public static final String PACKAGE_NAME = XModInitializer.class.getPackage().getName();
	public static final String TAG = PACKAGE_NAME + " : ";
	private static XSharedPreferences sharedPreferences = new XSharedPreferences(PACKAGE_NAME);
	private static final boolean DEBUG = sharedPreferences.getBoolean(Settings.DEBUG_OPTION, false);
	private XModuleResources xModuleResources;
	private static String MODULE_PATH = null;
	private static HashMap<String, HashMap<String, String>> pluginData;
	@Override
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
		if(pluginData != null && pluginData.size() > 0){
			Set<String> keys = pluginData.keySet();
			for(String key : keys){
				if(lpparam.packageName.equals(pluginData.get(key).get("targetPackageName"))){					
					try{
						ClassLoader loader = new PathClassLoader(pluginData.get(key).get("sourceDir") + ":", ClassLoader.getSystemClassLoader());
					    Class<?> module = Class.forName(pluginData.get(key).get("canonicalClassName"), true, loader);
//					    Method initZygote = module.getMethod("initZygote", new Class[]{XSharedPreferences.class, boolean.class});
					    Method initResources = module.getMethod("initResources", new Class[]{XModuleResources.class});
					    Method init = module.getMethod("init", new Class[]{XSharedPreferences.class, ClassLoader.class, boolean.class});
					    Object instance = module.newInstance();
					    initResources.invoke(instance, new Object[]{XModuleResources.createInstance(pluginData.get(key).get("sourceDir"), null)});
					    init.invoke(instance, new Object[]{sharedPreferences, lpparam.classLoader, DEBUG});
					}catch(Throwable throwable){
						XposedBridge.log(throwable);
					}					
				}
			}			
		}
		//com.android.systemui
		if (lpparam.packageName.equals("com.android.systemui")){
			try{
				XNotificationToolsModule xNotificationTools = new XNotificationToolsModule();
				xNotificationTools.initResources(xModuleResources);
				xNotificationTools.init(sharedPreferences, lpparam.classLoader, DEBUG);
			}catch(Throwable throwable){
				XposedBridge.log(throwable);
			}
			try{
				XGXModOnlyNavigationBarModule xGXModOnlyNavigationBarModule = new XGXModOnlyNavigationBarModule();
				xGXModOnlyNavigationBarModule.initResources(xModuleResources);
				xGXModOnlyNavigationBarModule.init(sharedPreferences, lpparam.classLoader, DEBUG);
			}catch(Throwable throwable){
				XposedBridge.log(throwable);
			}
		}
	}
	
	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {		
        MODULE_PATH = startupParam.modulePath;
        xModuleResources = XModuleResources.createInstance(MODULE_PATH, null);
        pluginData = makePluginData(GetPluginsData.MOD_MODULES_HEADER);
        
        if(pluginData != null && pluginData.size() > 0){
			Set<String> keys = pluginData.keySet();
			for(String key : keys){
				try{
					ClassLoader loader = new PathClassLoader(pluginData.get(key).get("sourceDir") + ":", ClassLoader.getSystemClassLoader());
				    Class<?> module = Class.forName(pluginData.get(key).get("canonicalClassName"), true, loader);
				    Method initZygote = module.getMethod("initZygote", new Class[]{XSharedPreferences.class, boolean.class});
				    Method initResources = module.getMethod("initResources", new Class[]{XModuleResources.class});
//				    Method init = module.getMethod("init", new Class[]{XSharedPreferences.class, ClassLoader.class, boolean.class});
				    Object instance = module.newInstance();
				    initResources.invoke(instance, new Object[]{XModuleResources.createInstance(pluginData.get(key).get("sourceDir"), null)});
				    initZygote.invoke(instance, new Object[]{sharedPreferences, DEBUG});
				}catch(Throwable throwable){
					XposedBridge.log(throwable);
				}
			}			
		}
	}

	@Override
	public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
	}
	
	public static HashMap<String, HashMap<String, String>> makePluginData(String modHeader){
		Map<String, ?> allPref = sharedPreferences.getAll();
		Set<String> keys = allPref.keySet();
		HashMap<String, HashMap<String, String>> pluginData = new HashMap<String, HashMap<String, String>>();
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
						XposedBridge.log(e);
					}
				}
				if(keyAndValues.size() > 0) pluginData.put(key, keyAndValues);
			}
		}
		return pluginData;
	}
}
