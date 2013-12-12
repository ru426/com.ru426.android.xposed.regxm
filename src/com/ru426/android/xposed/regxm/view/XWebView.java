package com.ru426.android.xposed.regxm.view;

import java.io.File;
import java.lang.reflect.Field;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings.ZoomDensity;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.ru426.android.xposed.regxm.util.PluginsDatabaseHelper;

public class XWebView extends WebView {
	private XWebViewListener mXWebViewListener;
	public interface XWebViewListener{
		public void onDownloadComplete(long id);
		public void onPageStarted(WebView view, String url, Bitmap favicon);
		public void onPageFinished(WebView view, String url);
	}
	public void setXWebViewListener(XWebViewListener listener){
		mXWebViewListener = listener;
	}
	private Context mContext;
	public XWebView(Context context) {
		super(context);
		init(context);
	}
	public XWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	public XWebView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	@SuppressLint("SetJavaScriptEnabled")
	private void init(Context context){
		mContext = context;
		setWebViewClient(new XWebViewClient());
		setWebChromeClient(new XWebChromeClient());
		setVerticalScrollbarOverlay(true);
		setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
		setOverScrollMode(View.OVER_SCROLL_NEVER);
		setBackgroundColor(getResources().getColor(android.R.color.transparent));
		
		getSettings().setGeolocationEnabled(true);
		getSettings().setDefaultZoom(ZoomDensity.FAR);
		getSettings().setLoadWithOverviewMode(true);
		getSettings().setUseWideViewPort(true);
		getSettings().setSaveFormData(true);
		getSettings().setJavaScriptEnabled(true);
		getSettings().setAppCacheEnabled(true);
		getSettings().setLoadsImagesAutomatically(true);
	    getSettings().setAllowFileAccess(true);
	    getSettings().setDomStorageEnabled(true);
	    getSettings().setBuiltInZoomControls(true);
		getSettings().setSupportZoom(true);
		
		setDownloadListener(downloadListener);
		
		try{
		    Field nameField = getSettings().getClass().getDeclaredField("mBuiltInZoomControls");
		    nameField.setAccessible(true);
		    nameField.set(getSettings(), false);
		}catch(Exception e){
		    e.printStackTrace();
		    getSettings().setBuiltInZoomControls(false);
		}		
	}
	
	private final class XWebViewClient extends WebViewClient {
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			super.onPageStarted(view, url, favicon);
			if(mXWebViewListener != null) mXWebViewListener.onPageStarted(view, url, favicon);
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			if(mXWebViewListener != null) mXWebViewListener.onPageFinished(view, url);
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
	        return false;
	    }
	}

	private final class XWebChromeClient extends WebChromeClient {
		public void onProgressChanged(WebView view, int progress) {
			if (progress == 100) {
				setBackgroundColor(getResources().getColor(android.R.color.white));
			}
        }
	}
	
	private DownloadListener downloadListener = new DownloadListener() {
		@Override
		public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
			if (url.endsWith("apk")) {
				String[] urls = url.split("/");
				String host = "";
				for(String _url : urls){
					if(_url.endsWith("mediafire.com")){
						host = _url;
						break;
					}
				}
				String fileName = getFileName(url);
				if(fileName.length() > 0 && host.length() > 0) startDownload(url, fileName, host);
			}
		}
	};

	private void startDownload(String url, String fileName, String host){
		try{
			DownloadManager mDownloadManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
			Uri uri = Uri.parse(url);
			DownloadManager.Request mRequest = new DownloadManager.Request(uri);
			mRequest.setTitle(fileName);
			mRequest.setDescription(host);
			mRequest.setDestinationInExternalFilesDir(mContext, Environment.DIRECTORY_DOWNLOADS, fileName);
			File pathExternalDir = mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
			pathExternalDir.mkdirs();
			File[] files = pathExternalDir.listFiles();
			for(File file : files){
				if(file.exists() && file.getName().contains(fileName))
					file.delete();
			}
			PluginsDatabaseHelper.updateCanUpgradePlugins(mContext, 0, fileName);
			mRequest.setVisibleInDownloadsUi(true);
			mRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
			mDownloadManager.enqueue(mRequest);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private static String getFileName(String fullFilePath) {
		if (fullFilePath != null) {
			int fileSeparatorLastIndex = fullFilePath.lastIndexOf("/");
			if (fileSeparatorLastIndex != -1)
				return fullFilePath.substring(fileSeparatorLastIndex + 1);
		}
		return fullFilePath;
	}

	BroadcastReceiver downloadReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
				long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
				if (id != -1) {
					if(mXWebViewListener != null) mXWebViewListener.onDownloadComplete(id);
				}
			}
		}
	};
	
	@Override
	protected void onAttachedToWindow() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
		mContext.registerReceiver(downloadReceiver, filter);
		super.onAttachedToWindow();
	}
	
	@Override
	protected void onDetachedFromWindow() {
		mContext.unregisterReceiver(downloadReceiver);
		super.onDetachedFromWindow();
	}
}
