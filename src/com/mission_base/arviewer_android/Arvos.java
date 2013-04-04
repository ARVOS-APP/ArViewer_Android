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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.hardware.SensorManager;
import android.view.OrientationEventListener;

import com.mission_base.arviewer_android.viewer.ArvosAugment;
import com.mission_base.arviewer_android.viewer.WebViewer;
import com.mission_base.arviewer_android.viewer.opengl.ArvosObject;

/**
 * Static Arvos instance, used to keep static values during the lifetime of the
 * application.
 * 
 * @author peter
 * 
 */
@SuppressLint("UseSparseArrays")
public class Arvos
{
	public boolean mSimulateWeb = false;

	public boolean mIsAuthor = false;
	public String mAuthorKey;
	public String mDeveloperKey;
	public String mSessionId;
	public float mLongitude = -1000f;
	public float mLatitude = -1000f;
	public int mVersion = 1;
	
	public float mAzimuth = 0;
	public float mCorrectedAzimuth = 0;
	public float mPitch = 0;
	public float mRoll = 0;
	
	public boolean mUseCache = true;
	
	public int mWidth;
	public int mHeight;
	public long mFPS = 0;
	
	public ArvosAugment mAugment;
	public String mAugmentsUrl = "http://www.mission-base.com/arvos/augments.json";

	private int mOrientation = 0;

	private static String mLock = "Lock";
	private static Arvos mInstance = null;

	private Activity mActivity;
	private OrientationEventListener mListener;

	public volatile boolean mHandleTouch = false;
	public volatile boolean mModelViewMatrixesRequested = false;

	/**
	 * The model view matrixes created by the renderer for the touch handler.
	 */
	public float[] mProjectionMatrix = null;

	/**
	 * The model view matrixes created by the renderer for the touch handler.
	 */
	public final HashMap<Integer, float[]> mModelViewMatrixes = new HashMap<Integer, float[]>();

	/**
	 * The objects drawn.
	 * <p>
	 * Created and used by the renderer. Stored in the static instance so the
	 * radar view can also use it.
	 */
	public final List<ArvosObject> mArvosObjects = Collections.synchronizedList(new LinkedList<ArvosObject>());

	private Arvos()
	{
	}

	/**
	 * Returns the static Arvos instance.
	 * 
	 * @return The instance.
	 */
	public static Arvos getInstance()
	{
		if (mInstance == null)
		{
			synchronized (mLock)
			{
				if (mInstance == null)
				{
					mInstance = new Arvos();
				}
			}
		}
		return mInstance;
	}

	/**
	 * Returns the static Arvos instance.
	 * 
	 * @param activity
	 *            The viewer activity to use.
	 * @return The instance.
	 */
	public static Arvos getInstance(Activity activity)
	{
		getInstance();
		if (activity != null)
		{
			mInstance.setActivity(activity);
		}
		return mInstance;
	}

	/**
	 * Returns the ArvosViewer activity.
	 * 
	 * @return The ArvosViewer activity.
	 */
	public Activity getActivity()
	{
		return mActivity;
	}

	private void setActivity(Activity activity)
	{
		mActivity = activity;
		if (mActivity != null && mListener == null)
		{
			mListener = new OrientationEventListener(mActivity, SensorManager.SENSOR_DELAY_UI)
			{
				public void onOrientationChanged(int orientation)
				{
					if (orientation == ORIENTATION_UNKNOWN)
					{
						mOrientation = 0;
					}
					else
					{
						mOrientation = orientation;
					}
				}
			};
		}
	}

	/**
	 * Gets the device orientation as reported by the SensorManager.
	 * 
	 * @return The device orientation as reported by the SensorManager.
	 */
	public int getOrientation()
	{
		return mOrientation;
	}

	/**
	 * Gets the device rotation derived from the SensorManager, 0, 90, 180 or
	 * 270.
	 * 
	 * @return The device rotation derived from the SensorManager, 0, 90, 180 or
	 *         270 degrees.
	 */
	public int getRotationDegrees()
	{
		int degrees = 0;
		if (mOrientation < 45)
		{
			degrees = 0;
		}
		else if (mOrientation < 135)
		{
			degrees = 270;
		}
		else if (mOrientation < 225)
		{
			degrees = 180;
		}
		else if (mOrientation < 315)
		{
			degrees = 90;
		}
		return degrees;
	}

	public void onResume()
	{
		mListener.enable();
	}

	public void onPause()
	{
		mListener.disable();
	}

	private static float toDegreesFactor = 180 / 3.141593f;

	/**
	 * Converts the radians given as parameter to degrees.
	 * 
	 * @param radians
	 *            The radians to be converted.
	 * 
	 * @return The degrees converted from the radians.
	 */
	public static float toDegrees(float radians)
	{
		return radians * toDegreesFactor;
	}

	private static String mMessage;

	/**
	 * Logs to the action bar subtitle.
	 * 
	 * @param tag
	 *            The tag word used for the log.
	 * 
	 * @param message
	 *            The message shown.
	 */
	public static void log(String tag, String message)
	{
		mMessage = tag + "" + message;
		getInstance().mActivity.runOnUiThread(new Runnable()
		{
			public void run()
			{
				getInstance().mActivity.getActionBar().setSubtitle(mMessage);
			}
		});
	}

	/**
	 * Starts the web viewer.
	 * 
	 * @param url
	 *            The url to display in the web viewer.
	 */
	public void startWebViewer(String url)
	{
		new ArvosRunnable(url).run();
	}

	private class ArvosRunnable extends Thread
	{
		private String mUrl;

		public ArvosRunnable(String url)
		{
			mUrl = url;
		}

		public void run()
		{
			Intent intent = new Intent(Arvos.getInstance().mActivity, WebViewer.class);
			intent.putExtra("url", mUrl);

			Arvos.getInstance().mActivity.startActivity(intent);
		}
	}
}
