package com.example.myapo;

import android.app.ActionBar;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class mViewToolbar {

	public static void MFnmodsToolbar(Activity ct) {
		ActionBar actionBar = ct.getActionBar();
		if (actionBar == null) return;
		
		View inflate = ct.getLayoutInflater().inflate(R.layout.custom_theme, (ViewGroup) null);
		LayoutParams layoutParams = new LayoutParams(-2, -1, 17);
		
		TextView titleTv = (TextView) inflate.findViewById(R.id.Fnmods_Toolbar);
		if (titleTv != null) {
			titleTv.setText(R.string.app_name);
		}
		
		actionBar.setCustomView(inflate, layoutParams);
		actionBar.setDisplayShowCustomEnabled(true);
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setElevation(7);
		actionBar.setHomeAsUpIndicator(R.drawable.ic_theme);
	}
}