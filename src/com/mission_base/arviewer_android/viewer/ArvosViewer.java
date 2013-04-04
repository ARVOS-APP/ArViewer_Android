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

package com.mission_base.arviewer_android.viewer;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.location.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.mission_base.arviewer_android.*;
import com.mission_base.arviewer_android.viewer.opengl.*;

/**
 * The augment viewer application.
 * <p>
 * Shows the camera view, overlays it with the augment being shown an opengl view
 * and overlays both with the radar view.
 * @author peter
 *
 */
public class ArvosViewer extends Activity implements IArvosLocationReceiver, IArvosHttpReceiver
{
	public ArvosCameraView mCameraView = null;
	public ArvosGLView mGLSurfaceView = null;
	public ArvosTextView mTextView = null;
	public ArvosRadarView mRadarView = null;

	private Arvos mInstance;
	private ArvosLocationListener mLocationListener;
	private ArvosHttpRequest mArvosHttpRequest;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mInstance = Arvos.getInstance(this);
		mLocationListener = new ArvosLocationListener((LocationManager) getSystemService(Context.LOCATION_SERVICE), this);

		String augmentText = getIntent().getStringExtra("augmentText");
		String augmentName = getIntent().getStringExtra("augmentName");

		ArvosAugment augment = new ArvosAugment();
		augment.parse(augmentText);
		if (augment.mName == null)
		{
			augment.mName = augmentName;
		}
		mInstance.mAugment = augment;
		requestTextures();
	}
	
	private void requestTextures()
	{
		ActionBar actionBar = getActionBar();
		actionBar.setTitle("Retrieving textures");
		actionBar.setSubtitle("Please wait ...");

		ArvosAugment augment = mInstance.mAugment;
		synchronized (augment)
		{
			mArvosHttpRequest = null;
			for (ArvosPoi poi : augment.mPois)
			{
				for (ArvosPoiObject poiObject : poi.mPoiObjects)
				{
					if (poiObject.mTextureUrl != null && poiObject.mImage == null)
					{
						mArvosHttpRequest = new ArvosHttpRequest(this, this);
						mArvosHttpRequest.getImage(poiObject.mTextureUrl);
						break;
					}
				}
				break;
			}
			if (mArvosHttpRequest == null)
			{
				onTexturesLoaded();
			}
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		mLocationListener.onResume();
		mInstance.onResume();

		if (mGLSurfaceView != null)
		{
			mGLSurfaceView.onResume();
		}
		if (mRadarView != null)
		{
			mRadarView.onResume();
		}
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		mLocationListener.onPause();
		Arvos.getInstance().onPause();
		if (mGLSurfaceView != null)
		{
			mGLSurfaceView.onPause();
		}
		if (mRadarView != null)
		{
			mRadarView.onPause();
		}
	}

	@Override
	public void onHttpResponse(String url, String error, String text, Bitmap bitmap)
	{
		if (error.startsWith("ER"))
		{
			ActionBar actionBar = getActionBar();
			actionBar.setSubtitle("Error: " + text);
			mArvosHttpRequest = null;
			return;
		}

		String nextTexture = null;
		ArvosAugment augment = Arvos.getInstance().mAugment;
		synchronized (augment)
		{
			for (ArvosPoi poi : augment.mPois)
			{
				for (ArvosPoiObject poiObject : poi.mPoiObjects)
				{
					if (url.equals(poiObject.mTextureUrl))
					{
						poiObject.mImage = bitmap;
					}
					else if (poiObject.mTextureUrl != null && poiObject.mImage == null)
					{
						nextTexture = poiObject.mTextureUrl;
					}
				}
			}

			if (nextTexture != null)
			{
				mArvosHttpRequest = new ArvosHttpRequest(this, this);
				mArvosHttpRequest.getImage(nextTexture);
				return;
			}
		}
		onTexturesLoaded();
	}

	private void onTexturesLoaded()
	{
		mArvosHttpRequest = null;

		ActionBar actionBar = getActionBar();
		actionBar.setTitle(mInstance.mAugment.mName);
		actionBar.setSubtitle(String.format("Lon %.6f, Lat %.6f", mInstance.mLongitude, mInstance.mLatitude));

		FrameLayout frame = new FrameLayout(this);

		mCameraView = new ArvosCameraView(this);
		frame.addView(mCameraView);

		mGLSurfaceView = new ArvosGLView(this);
		frame.addView(mGLSurfaceView);

		mRadarView = new ArvosRadarView(this);
		frame.addView(mRadarView);

		mTextView = new ArvosTextView(this);
		frame.addView(mTextView);

		setContentView(frame);

		if (mTextView != null)
		{
			mTextView.updateWithNewLocation();
		}
		if (mGLSurfaceView != null)
		{
			mGLSurfaceView.onResume();
		}
		if (mRadarView != null)
		{
			mRadarView.onResume();
		}
	}

	@Override
	public void onLocationChanged(boolean isNew, Location location)
	{
		ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(String.format("Lon %.6f, Lat %.6f", mInstance.mLongitude, mInstance.mLatitude));
	}
	
	static final private int MENU_ITEM_CLOSE = Menu.FIRST;

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);

		int groupId = 0;
		int menuItemId = MENU_ITEM_CLOSE;
		int menuItemOrder = Menu.NONE;
		int menuItemText = R.string.menu_close;

		// Create the Menu Item and keep a reference to it
		MenuItem menuItem = menu.add(groupId, menuItemId, menuItemOrder, menuItemText);
		menuItem = menu.add(groupId, menuItemId++, menuItemOrder, menuItemText);
		//	menuItem.setIcon(R.drawable.ic_action_refresh_white);
		menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item)
	{
		super.onOptionsItemSelected(item);

		switch (item.getItemId())
		{
			case (MENU_ITEM_CLOSE):
				finish();
				return true;
		}
		return false;
	}	
}
