package com.ru426.android.xposed.regxm.util;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;

import com.ru426.android.xposed.regxm.Settings;

public class PluginsDatabaseHelper {
	private static final String DATABASE_NAME = "plugins.db";
    private static final int DATABASE_VERSION = 1;
    public static final String PLUGIN_TABLE = "plugins_data";
    public static final String PLUGIN_TABLE_EXTRA = "plugins_data_extra";
    private static DatabaseHelper instance;
	public static DatabaseHelper getDatabaseHelperInstance(Context context){
		if(instance == null) instance = new DatabaseHelper(context);
		return instance;
	}
	
	public static void savePluginsData(Context context, String packageName, HashMap<String, String> list){
		try{
			//initialize For upgrade this app version is from v01 to v02
			File[] pluginsDataFiles = new File(Environment.getDataDirectory(), "data/" + Settings.PACKAGE_NAME + "/shared_prefs").listFiles();
			if(pluginsDataFiles != null){
				for(File file : pluginsDataFiles){
					if(file.exists()){							
						String _fileName = file.getName().replaceFirst("[.][^.]+$", "");
						if(!_fileName.contains("_preferences"))
							file.delete();
					}
				}
			}
			DatabaseHelper helper = getDatabaseHelperInstance(context);
			SQLiteDatabase db = helper.getWritableDatabase();
			ContentValues values = new ContentValues();
			String description = "";
			for(String _key : list.keySet()){
				values.put(_key, list.get(_key));
				if(_key.equals("description")) description = list.get(_key); 
			}
			if(db != null && db.isOpen()){
				if(db.update(PLUGIN_TABLE, values, "quickkey = ?", new String[]{(String) values.get("quickkey")}) < 1){
					db.insert(PLUGIN_TABLE, null, values);
				}
				Cursor cursor = db.query(true, PLUGIN_TABLE, new String[]{"id"}, "quickkey = ?", new String[]{(String) values.get("quickkey")}, null, null, null, "1");
				int id = -1;
				if(cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()){
					id = cursor.getInt(cursor.getColumnIndex("id"));
					cursor.close();
				}
				if(id >= 0){
					ContentValues valuesExtra = new ContentValues();
					valuesExtra.put("id", id);
					if(description.length() > 0 && description.startsWith("ver:")){
						String[] descData = description.split(",");						
						for(String vals : descData){
							if (vals.startsWith("date:")) {								
								Cursor update_date = db.query(true, PLUGIN_TABLE_EXTRA, new String[]{"update_date"}, "id = ?", new String[]{String.valueOf(id)}, null, null, null, "1");
								if(update_date != null && update_date.getCount() > 0 && update_date.moveToFirst()){
									if(getIsUpgrade(update_date.getString(update_date.getColumnIndex("update_date")), vals.replace("date:", ""))){
										valuesExtra.put("update_flag", 1);
									}
								}
								update_date.close();
								valuesExtra.put("update_date", vals.replace("date:", ""));
							} else {
								String[] val = vals.split(":");
								if(val != null && val.length == 2){
									String key = val[0];
									String value = val[1];
									if(key.equals("ver")) key = "version";
									valuesExtra.put(key, value);
								}
							}
						}
					}
					if(db.update(PLUGIN_TABLE_EXTRA, valuesExtra, "id = ?", new String[]{String.valueOf(id)}) < 1){
						valuesExtra.put("update_flag", 0);
						db.insert(PLUGIN_TABLE_EXTRA, null, valuesExtra);
					}
				}
			}			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	@SuppressLint("SimpleDateFormat")
	private static boolean getIsUpgrade(String source , String target){
		final String DATE_PATTERN ="yyyy-MM-dd HH:mm:ss";
		Date _source = null;
		Date _target = null;
		if(source == null || target == null) return false;
		try {
			_source = (new SimpleDateFormat(DATE_PATTERN)).parse(source);
			_target = (new SimpleDateFormat(DATE_PATTERN)).parse(target);
		} catch (ParseException e) {
			e.printStackTrace();
		}		
		return _source.compareTo(_target) != 0;
	}
	
	public static Cursor getPluginsData(Context context){
		try{
			DatabaseHelper helper = getDatabaseHelperInstance(context);
			SQLiteDatabase db = helper.getWritableDatabase();
			if(db != null && db.isOpen()){
				Cursor cursor = db.rawQuery("SELECT * FROM " + PLUGIN_TABLE + " AS T1 INNER JOIN " + PLUGIN_TABLE_EXTRA + " AS T2 ON T1.id = T2.id", null);
				if(cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()){
					return cursor;
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public static boolean hasCanUpgradePlugins(Context context){
		boolean result = false;
		DatabaseHelper helper = PluginsDatabaseHelper.getDatabaseHelperInstance(context);
		SQLiteDatabase db = helper.getWritableDatabase();
		Cursor cursor = db.rawQuery("SELECT * FROM " + PluginsDatabaseHelper.PLUGIN_TABLE + " AS T1 INNER JOIN " + PluginsDatabaseHelper.PLUGIN_TABLE_EXTRA + " AS T2 ON T1.id = T2.id "
				+ "WHERE T2.update_flag = ?", new String[]{String.valueOf(1)});
		result = cursor != null && cursor.getCount() > 0 && cursor.moveToFirst();
		cursor.close();
		db.close();
		helper.close();
		return result;
	}
	
	public static void updateCanUpgradePlugins(Context context, int value, String selection){
		DatabaseHelper helper = PluginsDatabaseHelper.getDatabaseHelperInstance(context);
		SQLiteDatabase db = helper.getWritableDatabase();
		Cursor cursor = db.rawQuery("SELECT * FROM " + PluginsDatabaseHelper.PLUGIN_TABLE + " AS T1 INNER JOIN " + PluginsDatabaseHelper.PLUGIN_TABLE_EXTRA + " AS T2 ON T1.id = T2.id "
				+ "WHERE T1.filename = ?", new String[]{selection});
		if(cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()){
			if(cursor.getInt(cursor.getColumnIndex("update_flag")) == 1){
				int id = cursor.getInt(cursor.getColumnIndex("id"));
				ContentValues values = new ContentValues();
				values.put("update_flag", value);
				db.update(PluginsDatabaseHelper.PLUGIN_TABLE_EXTRA, values, "id = ?", new String[]{String.valueOf(id)});
			}
		}
		cursor.close();
		db.close();
		helper.close();
	}
	
	public static class DatabaseHelper extends SQLiteOpenHelper {		
	    public DatabaseHelper(Context context) {
	        super(context, DATABASE_NAME, null, DATABASE_VERSION);
	    }

	    @Override
	    public void onCreate(SQLiteDatabase db) {
	    	db.execSQL("CREATE TABLE IF NOT EXISTS " + PLUGIN_TABLE + " (id INTEGER PRIMARY KEY AUTOINCREMENT,"
	    			+ " quickkey text, hash text,"
	    			+ " filename text, description text,"
	    			+ " size text, privacy text,"
	    			+ " created text, password_protected text,"
	    			+ " mimetype text, filetype text,"
	    			+ " view text, edit text,"
	    			+ " revision text, flag text);");
	    	db.execSQL("CREATE TABLE IF NOT EXISTS " + PLUGIN_TABLE_EXTRA + " (id INTEGER,"
	    			+ " version text, en text,"
	    			+ " ja text, update_date text, update_flag integer);");
	    }

	    @Override
	    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	    }
	}
}
