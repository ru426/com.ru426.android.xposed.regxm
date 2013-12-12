package com.ru426.android.xposed.regxm.view;

import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import com.ru426.android.xposed.regxm.Settings;

public class ModPreferenceFragment extends PreferenceFragment {
	@Override
	public void onResume() {
		showHomeButton();
		for(int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++){
			if(getPreferenceScreen().getPreference(i) instanceof PreferenceCategory){
				PreferenceCategory mPreferenceCategory = (PreferenceCategory) getPreferenceScreen().getPreference(i);
				if(mPreferenceCategory.getPreferenceCount() < 1){
					getPreferenceScreen().removePreference(mPreferenceCategory);
				}
			}
		}
		setPreferenceChangeListener(getPreferenceScreen(), Settings.onPreferenceChangeListener);
		super.onResume();
	}
	@Override
	public void onDestroy() {
		removePreferenceChangeListener(getPreferenceScreen());
		super.onDestroy();
	}
	
	private void showHomeButton(){		
		if(getActivity() != null && getActivity().getActionBar() != null){
			getActivity().getActionBar().setHomeButtonEnabled(true);
	        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
		}		
	}
	
	private void setPreferenceChangeListener(PreferenceScreen preferenceScreen, OnPreferenceChangeListener listener){
		for(int i = 0; i < preferenceScreen.getPreferenceCount(); i++){
			if(preferenceScreen.getPreference(i) instanceof PreferenceCategory){
				for(int j = 0; j < ((PreferenceCategory)preferenceScreen.getPreference(i)).getPreferenceCount(); j++){
					((PreferenceCategory)preferenceScreen.getPreference(i)).getPreference(j).setOnPreferenceChangeListener(listener);
				}
			}else{
				preferenceScreen.getPreference(i).setOnPreferenceChangeListener(listener);				
			}
		}
	}
	
	private void removePreferenceChangeListener(PreferenceScreen preferenceScreen){
		for(int i = 0; i < preferenceScreen.getPreferenceCount(); i++){
			if(preferenceScreen.getPreference(i) instanceof PreferenceCategory){
				for(int j = 0; j < ((PreferenceCategory)preferenceScreen.getPreference(i)).getPreferenceCount(); j++){
					((PreferenceCategory)preferenceScreen.getPreference(i)).getPreference(j).setOnPreferenceChangeListener(null);
				}
			}else{
				preferenceScreen.getPreference(i).setOnPreferenceChangeListener(null);				
			}
		}
	}
}
