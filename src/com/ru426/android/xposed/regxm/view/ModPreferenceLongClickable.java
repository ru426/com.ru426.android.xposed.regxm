package com.ru426.android.xposed.regxm.view;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;

public class ModPreferenceLongClickable extends Preference implements OnClickListener, OnLongClickListener{
	private ModPreferenceClickableListener mModPreferenceClickableListener;
	public interface ModPreferenceClickableListener{
		public void onClick(View v);
		public boolean onLongClick(View v);
	}
	public void setOnModPreferenceClickableListener(ModPreferenceClickableListener listener){
		mModPreferenceClickableListener = listener;
	}
	public ModPreferenceLongClickable(Context context) {
		super(context);
	}
	public ModPreferenceLongClickable(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	public ModPreferenceLongClickable(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}	
	@Override
	public View getView(View convertView, ViewGroup parent) {
		View view = super.getView(convertView, parent);
		if(view != null){
			view.setOnClickListener(this);
			view.setOnLongClickListener(this);
		}
		return view;
	}
	@Override
	public void onClick(View v) {
		if(mModPreferenceClickableListener != null) mModPreferenceClickableListener.onClick(v);
	}
	@Override
	public boolean onLongClick(View v) {
		if(mModPreferenceClickableListener != null) mModPreferenceClickableListener.onLongClick(v);
		return false;
	}
}
