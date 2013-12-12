package com.ru426.android.xposed.regxm.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Xml;

public class GetPluginsDataFromMediaFire extends AsyncTask<Object, Void, Boolean> {
	//ReGXm_Plugins Folder https://www.mediafire.com/#tq7d5l76ty8ec
	private static final String MEDIAFIRE = "http://www.mediafire.com/api/folder/get_content.php?folder_key=tq7d5l76ty8ec&content_type=files";
	private GetPluginsDataFromMediaFireListener mGetPluginsDataFromMediaFireListener;
	public interface GetPluginsDataFromMediaFireListener{
		public void onPreExecute();
		public void onPostExecute(Boolean result);
	}
	public void setGetPluginsDataFromMediaFireListener(GetPluginsDataFromMediaFireListener listener){
		mGetPluginsDataFromMediaFireListener = listener;
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		if(mGetPluginsDataFromMediaFireListener != null) mGetPluginsDataFromMediaFireListener.onPreExecute();
	}

	/// ??? not work postExcute when call client.execute(httpGet)
	@Override
	protected synchronized Boolean doInBackground(Object... params) {
		Context mContext = (Context) params[0];
		HttpGet httpGet = new HttpGet(MEDIAFIRE);
		DefaultHttpClient client = new DefaultHttpClient();
		HttpResponse response = null;
		HttpEntity entries = null;
		InputStream stream = null;
		try {
			response = client.execute(httpGet);
			int status = response.getStatusLine().getStatusCode();
			if (status == HttpStatus.SC_OK) {
				entries = response.getEntity();
				stream = entries.getContent();
				XmlPullParser xmlPullParser = Xml.newPullParser();
				try {
					xmlPullParser.setInput(stream, "UTF-8");
					for (int event = xmlPullParser.getEventType(); event != XmlPullParser.END_DOCUMENT; event = xmlPullParser.next()) {
						if(event == XmlPullParser.START_TAG && xmlPullParser.getName().equals("file")){
							HashMap<String, String> list = new HashMap<String, String>();
							boolean fileNameFlag = false;
							boolean returnFlag = false;
							String fileName = "";
							String key = "";
							for (int event2 = xmlPullParser.getEventType(); event2 != XmlPullParser.END_DOCUMENT; event2 = xmlPullParser.next()) {								
								switch(event2) {
				                case XmlPullParser.START_TAG:
				                	if(xmlPullParser.getName() != null) key = xmlPullParser.getName();
				                	fileNameFlag = xmlPullParser.getName() != null && xmlPullParser.getName().equals("filename");
				                	returnFlag = xmlPullParser.getName() != null && xmlPullParser.getName().equals("result");
				                    break;
				                case XmlPullParser.TEXT:
				                	if(xmlPullParser.getText() != null){
				                		list.put(key, xmlPullParser.getText());
				                		if(fileNameFlag) fileName = xmlPullParser.getText();
										if(returnFlag){
											if(xmlPullParser.getText().equals("Success")){
												if(mGetPluginsDataFromMediaFireListener != null) mGetPluginsDataFromMediaFireListener.onPostExecute(true);
												return true;
											}
										}
				                	}
				                    break;
				                case XmlPullParser.END_TAG:
				                	if(xmlPullParser.getName() != null && xmlPullParser.getName().equals("file")){
				                		PluginsDatabaseHelper.savePluginsData(mContext, fileName, list);
				                	}
				                	break;
				                }
							}
						}
					}
				} catch (XmlPullParserException e) {
					e.printStackTrace();
				}
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(entries != null){
				try {
					stream.close();
					entries.consumeContent();
					client.getConnectionManager().shutdown();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		if(mGetPluginsDataFromMediaFireListener != null) mGetPluginsDataFromMediaFireListener.onPostExecute(false);
		return false;
	}
}
