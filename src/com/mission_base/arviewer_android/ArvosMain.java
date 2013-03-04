/*
 Copyright (C) 2013, Peter Graf

   This file is part of Arvos - AR Viewer Open Source for Android.
   Arvos is free software.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
    
   For more information on the AR Viewer Open Source or Peter Graf,
   please see: http://www.mission-base.com/.
 */

package com.mission_base.arviewer_android;

import java.util.ArrayList;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.mission_base.arviewer_android.viewer.ArvosViewer;
import com.mission_base.arviewer_android.viewer.ArvosAugment;

/**
 * The main activity of the Arvos app.
 * <P>
 * Downloads the list of augments and displays them in a list. When an augment
 * is selected it downloads the augments and calls the ArvosViewer app for
 * displaying the augment.
 * 
 * @author peter
 * 
 */
public class ArvosMain extends ListActivity implements IArvosLocationReceiver, IArvosHttpReceiver
{
	private ArrayList<ArvosAugment> mAugments = new ArrayList<ArvosAugment>();

	private ArvosLocationListener mLocationListener;
	private Arvos mInstance;
	private int mMenuPreferencesCount = 0;

	private static long mMaxAge = 1000L * 60 * 60 * 24 * 30;
	private static long mMaxSize = 64 * 1024 * 1024;
	private static int mMaxFiles = 256;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mInstance = Arvos.getInstance();
		mLocationListener = new ArvosLocationListener((LocationManager) getSystemService(Context.LOCATION_SERVICE), this);

		ArvosCustomArrayAdapter adapter = new ArvosCustomArrayAdapter(this, mAugments, R.drawable.arvos_logo_black);
		setListAdapter(adapter);

		updateFromPreferences();
		ArvosCache.initialize(this, mMaxAge, mMaxFiles, mMaxSize);
	}

	/**
	 * Handles clicks on the augment list items.
	 */
	public void onListItemClick(ListView parent, View v, int position, long id)
	{
		mMenuPreferencesCount = 0;

		ArvosAugment augment = mAugments.get(position);
		String name = augment.mName;
		String url = augment.mUrl;

		if (name != null && name.trim().length() > 0 && url != null && url.trim().length() > 0)
		{
			Toast.makeText(this, name, Toast.LENGTH_SHORT).show();
			requestAugment(augment);
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		onResumeLocal();
	}

	private void onResumeLocal()
	{
		ActionBar actionBar = getActionBar();
		if (!hasInitialAugmentsRequest)
		{
			actionBar.setTitle("Retrieving location");
			actionBar.setSubtitle("Please wait ...");
		}
		else
		{
			actionBar.setTitle("Augments");
			actionBar.setSubtitle(String.format("Lon %.6f, Lat %.6f", mInstance.mLongitude, mInstance.mLatitude));
		}
		mLocationListener.onResume();
	}

	private ArvosHttpRequest mArvosHttpRequest = null;

	private void requestAugments()
	{
		ActionBar actionBar = getActionBar();
		actionBar.setTitle("Retrieving augments");
		actionBar.setSubtitle("Please wait ...");

		mArvosHttpRequest = new ArvosHttpRequest(this, this);
		mArvosHttpRequest.getText(mInstance.mAugmentsUrl);
	}

	private void requestAugment(ArvosAugment augment)
	{
		ActionBar actionBar = getActionBar();

		actionBar.setTitle("Retrieving augment " + augment.mName);
		actionBar.setSubtitle("Please wait ...");

		mArvosHttpRequest = new ArvosHttpRequest(this, this);
		mArvosHttpRequest.getText(augment.mUrl);
	}

	@Override
	protected void onPause()
	{
		super.onPause();

		mLocationListener.onPause();

		mArvosHttpRequest = null;
	}

	/**
	 * Handles http responses from http requests.
	 */
	public void onHttpResponse(String url, String error, String text, Bitmap bitmap)
	{
		if (error.startsWith("ER"))
		{
			ActionBar actionBar = getActionBar();
			actionBar.setSubtitle("Error: " + text);
			mArvosHttpRequest = null;
			return;
		}

		if (mInstance.mAugmentsUrl.equals(url))
		{
			mAugments.clear();
			error = ArvosAugment.parse(text, mAugments);
			if (error.startsWith("RD"))
			{
				mInstance.mAugmentsUrl = error.substring(2);
				requestAugments();
				return;
			}
			if (error.startsWith("ER"))
			{
				ActionBar actionBar = getActionBar();
				actionBar.setSubtitle("Error: " + error.substring(2));
				mArvosHttpRequest = null;
				return;
			}

			((ArvosCustomArrayAdapter) (this.getListAdapter())).notifyDataSetChanged();

			ActionBar actionBar = getActionBar();

			actionBar.setTitle("Augments");
			actionBar.setSubtitle(String.format("Lon %.6f, Lat %.6f", mInstance.mLongitude, mInstance.mLatitude));
			return;
		}

		for (int i = 0; i < mAugments.size(); i++)
		{
			ArvosAugment augment = mAugments.get(i);
			if (url.equals(augment.mUrl))
			{
				String name = augment.mName;

				ArvosAugment parsedAugment = new ArvosAugment();
				error = parsedAugment.parse(text);
				if (error.startsWith("ER"))
				{
					ActionBar actionBar = getActionBar();
					actionBar.setSubtitle("Error: " + error.substring(2));
					return;
				}

				Intent intent = new Intent(this, ArvosViewer.class);
				intent.putExtra("augmentText", text);
				intent.putExtra("augmentName", name.trim());

				startActivity(intent);
			}
		}
	}

	private boolean hasInitialAugmentsRequest = false;

	/**
	 * Handles location updates sent from the location listener.
	 */
	public void onLocationChanged(boolean isNew, Location location)
	{
		if (isNew && !hasInitialAugmentsRequest)
		{
			hasInitialAugmentsRequest = true;
			requestAugments();
		}
		else
		{
			ActionBar actionBar = getActionBar();
			actionBar.setSubtitle(String.format("Lon %.6f, Lat %.6f", mInstance.mLongitude, mInstance.mLatitude));
		}
	}

	static final private int MENU_ITEM_REFRESH = Menu.FIRST;
	static final private int MENU_PREFERENCES = Menu.FIRST + 1;
	private static final int SHOW_PREFERENCES = 1;

	/**
	 * Creates the menu.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);

		int groupId = 0;
		int menuItemId = MENU_ITEM_REFRESH;
		int menuItemOrder = Menu.NONE;
		int menuItemText = R.string.menu_refresh;

		menu.add(groupId, MENU_PREFERENCES, menuItemOrder, R.string.menu_preferences);

		MenuItem menuItem = menu.add(groupId, menuItemId, menuItemOrder, menuItemText);
		menuItem = menu.add(groupId, menuItemId++, menuItemOrder, menuItemText);
		menuItem.setIcon(R.drawable.ic_action_refresh_white);
		menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

		return true;
	}

	/**
	 * Handles menu item selections.
	 */
	public boolean onOptionsItemSelected(MenuItem item)
	{
		super.onOptionsItemSelected(item);

		switch (item.getItemId())
		{
		case (MENU_ITEM_REFRESH):
			mMenuPreferencesCount = 0;
			if (!hasInitialAugmentsRequest)
			{
				onResumeLocal();
			}
			else
			{
				requestAugments();
			}
			return true;

		case (MENU_PREFERENCES):
			showPreferences();
			return true;
		}
		return false;
	}

	private void showPreferences()
	{
		Intent i = new Intent(this, ArvosPreferences.class);
		startActivityForResult(i, SHOW_PREFERENCES);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == SHOW_PREFERENCES)
		{
			mMenuPreferencesCount++;
			if (mMenuPreferencesCount >= 5)
			{
				mMenuPreferencesCount = 0;
				mInstance.mIsAuthor = !mInstance.mIsAuthor;
				if (mInstance.mIsAuthor)
				{
					Toast.makeText(this, "Entering author mode", Toast.LENGTH_SHORT).show();
				}
				else
				{
					Toast.makeText(this, "Leaving author mode", Toast.LENGTH_SHORT).show();
				}
				Context context = getApplicationContext();
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
				SharedPreferences.Editor ed = prefs.edit();
				ed.putBoolean(ArvosPreferences.PREF_IS_AUTHOR, mInstance.mIsAuthor);
				ed.commit();
			}
			else
			{
				Toast.makeText(this, "Preferences updated", Toast.LENGTH_SHORT).show();
			}
			checkAuthorKey();
			updateFromPreferences();
		}
	}

	private void checkAuthorKey()
	{
		Context context = getApplicationContext();
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		boolean isAuthor = prefs.getBoolean(ArvosPreferences.PREF_IS_AUTHOR, false);
		String key = prefs.getString(ArvosPreferences.PREF_AUTHOR_KEY, "").trim();
		if (isAuthor && key.length() > 0 && (!key.equals(ArvosHttpRequest.urlEncode(key)) || key.length() < 20))
		{
			mMenuPreferencesCount = 0;
			AlertDialog.Builder ad = new AlertDialog.Builder(this);
			ad.setTitle("Error");
			ad.setMessage("The author key must have at least 20 alphanumeric characters");
			ad.setPositiveButton("Update", new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int arg1)
				{
					showPreferences();
				}
			});
			ad.setNegativeButton("Erase", new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int arg1)
				{
					SharedPreferences.Editor ed = prefs.edit();
					ed.putString(ArvosPreferences.PREF_AUTHOR_KEY, "");
					ed.commit();
				}
			});
			ad.setCancelable(false);
			ad.show();
		}
	}

	private void updateFromPreferences()
	{
		Context context = getApplicationContext();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		mInstance.mUseCache = prefs.getBoolean(ArvosPreferences.PREF_USE_CACHE, true);
		mInstance.mIsAuthor = prefs.getBoolean(ArvosPreferences.PREF_IS_AUTHOR, false);
		mInstance.mAuthorKey = prefs.getString(ArvosPreferences.PREF_AUTHOR_KEY, "").trim();
		mInstance.mDeveloperKey = prefs.getString(ArvosPreferences.PREF_DEVELOPER_KEY, "").trim();
	}
}
