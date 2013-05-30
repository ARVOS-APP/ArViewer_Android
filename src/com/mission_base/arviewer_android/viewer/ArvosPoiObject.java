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

import android.graphics.*;
import com.mission_base.arviewer_android.*;
import com.mission_base.arviewer_android.viewer.opengl.*;
import java.util.*;
import org.json.*;

/**
 * A poi object as shown in the opengl view.
 * 
 * @author peter
 */
public class ArvosPoiObject
{
	public int mId;
	public String mName;
	public String mTextureUrl;
	public String mBillboardHandling;

	public long mStartTime;
	public long mAnimationDuration;

	public boolean mLoop;
	public boolean mIsActive;

	public List<String> mOnClickUrls;
	public List<String> mOnClickActivates;
	public List<String> mOnClickDeactivates;

	public List<String> mOnDurationEndUrls;
	public List<String> mOnDurationEndActivates;
	public List<String> mOnDurationEndDeactivates;

	private float[] mStartPosition;
	private float[] mEndPosition;

	private float[] mStartScale;
	private float[] mEndScale;

	private float[] mStartRotation;
	private float[] mEndRotation;

	public long mTimeStarted;
	public ArvosPoi mParent;

	public Bitmap mImage;

	/**
	 * Create a poi object.
	 * 
	 * @param parent
	 *            The poi the poi object belongs to.
	 */
	public ArvosPoiObject(ArvosPoi parent)
	{
		mId = getNextId();
		mParent = parent;
		mAnimationDuration = parent.mAnimationDuration;
		mIsActive = true;
	}

	private static int mNextId = 0;
	private static int getNextId()
	{
		return ++mNextId;
	}
	
	/**
	 * Parses one poi object.
	 * 
	 * @param jsonPoiObject
	 *            The JSON input to parse.
	 * @throws JSONException
	 *             JSON parse exception.
	 */
	public void parse(JSONObject jsonPoiObject) throws JSONException
	{
		if (jsonPoiObject != null)
		{
			mTextureUrl = jsonPoiObject.getString("texture");

			mName = jsonPoiObject.has("name") ? jsonPoiObject.getString("name") : ("\" " + mId);
			mBillboardHandling = jsonPoiObject.has("billboardHandling") ? jsonPoiObject.getString("billboardHandling") : null;
			if (mBillboardHandling != null //
					&& !ArvosObject.BillboardHandlingNone.equals(mBillboardHandling) //
					&& !ArvosObject.BillboardHandlingCylinder.equals(mBillboardHandling) //
					&& !ArvosObject.BillboardHandlingSphere.equals(mBillboardHandling))
			{
				throw new JSONException("Illegal value for billboardHandling: " + mBillboardHandling);
			}

			mStartTime = jsonPoiObject.has("startTime") ? jsonPoiObject.getInt("startTime") : 0;
			mAnimationDuration = jsonPoiObject.has("duration") ? jsonPoiObject.getInt("duration") : 0;
			mLoop = jsonPoiObject.has("loop") ? jsonPoiObject.getBoolean("loop") : true;
			mIsActive = jsonPoiObject.has("isActive") ? jsonPoiObject.getBoolean("isActive") : true;

			mStartPosition = parseVec3f(jsonPoiObject, "startPosition");
			if (mStartPosition == null)
			{
				mStartPosition = new float[] { 0f, 0f, 0f };
			}
			mEndPosition = parseVec3f(jsonPoiObject, "endPosition");

			mStartScale = parseVec3f(jsonPoiObject, "startScale");
			mEndScale = parseVec3f(jsonPoiObject, "endScale");

			mStartRotation = parseVec4f(jsonPoiObject, "startRotation");
			mEndRotation = parseVec4f(jsonPoiObject, "endRotation");

			if (jsonPoiObject.has("onClick"))
			{
				JSONArray jsonArray = new JSONArray(jsonPoiObject.getString("onClick"));
				if (jsonArray != null && jsonArray.length() > 0)
				{
					for (int i = 0; i < jsonArray.length(); i++)
					{
						JSONObject jsonOnClick = jsonArray.getJSONObject(i);

						if (jsonOnClick.has("url"))
						{
							if (mOnClickUrls == null)
							{
								mOnClickUrls = new LinkedList<String>();
							}
							mOnClickUrls.add(jsonOnClick.getString("url"));
						}
						if (jsonOnClick.has("activate"))
						{
							if (mOnClickActivates == null)
							{
								mOnClickActivates = new LinkedList<String>();
							}
							mOnClickActivates.add(jsonOnClick.getString("activate"));
						}
						if (jsonOnClick.has("deactivate"))
						{
							if (mOnClickDeactivates == null)
							{
								mOnClickDeactivates = new LinkedList<String>();
							}
							mOnClickDeactivates.add(jsonOnClick.getString("deactivate"));
						}
					}
				}
			}
			if (jsonPoiObject.has("onDurationEnd"))
			{
				JSONArray jsonArray = new JSONArray(jsonPoiObject.getString("onDurationEnd"));
				if (jsonArray != null && jsonArray.length() > 0)
				{
					for (int i = 0; i < jsonArray.length(); i++)
					{
						JSONObject jsonOnDurationEnd = jsonArray.getJSONObject(i);
						if (jsonOnDurationEnd.has("url"))
						{
							if (mOnDurationEndUrls == null)
							{
								mOnDurationEndUrls = new LinkedList<String>();
							}
							mOnDurationEndUrls.add(jsonOnDurationEnd.getString("url"));
						}
						if (jsonOnDurationEnd.has("activate"))
						{
							if (mOnDurationEndActivates == null)
							{
								mOnDurationEndActivates = new LinkedList<String>();
							}
							mOnDurationEndActivates.add(jsonOnDurationEnd.getString("activate"));
						}
						if (jsonOnDurationEnd.has("deactivate"))
						{
							if (mOnDurationEndDeactivates == null)
							{
								mOnDurationEndDeactivates = new LinkedList<String>();
							}
							mOnDurationEndDeactivates.add(jsonOnDurationEnd.getString("deactivate"));
						}
					}
				}
			}
		}
	}

	/**
	 * Parses 3 float values, x, y, and z.
	 * 
	 * @param jsonObject
	 * @param name
	 * @return
	 * @throws JSONException
	 */
	public static float[] parseVec3f(JSONObject jsonObject, String name) throws JSONException
	{
		float[] result = null;
		if (jsonObject.has(name))
		{
			result = new float[3];

			JSONArray jsonArray = new JSONArray(jsonObject.getString(name));
			if (jsonArray != null && jsonArray.length() > 0)
			{
				JSONObject jsonVec3f = jsonArray.getJSONObject(0);
				result[0] = jsonVec3f.has("x") ? (float) jsonVec3f.getDouble("x") : 0f;
				result[1] = jsonVec3f.has("y") ? (float) jsonVec3f.getDouble("y") : 0f;
				result[2] = jsonVec3f.has("z") ? (float) jsonVec3f.getDouble("z") : 0f;
			}
		}
		return result;
	}

	/**
	 * Parses 4 float values, x, y, z and a.
	 * 
	 * @param jsonObject
	 * @param name
	 * @return
	 * @throws JSONException
	 */
	public static float[] parseVec4f(JSONObject jsonObject, String name) throws JSONException
	{
		float[] result = null;
		if (jsonObject.has(name))
		{
			result = new float[4];

			JSONArray jsonArray = new JSONArray(jsonObject.getString(name));
			if (jsonArray != null && jsonArray.length() > 0)
			{
				JSONObject jsonVec4f = jsonArray.getJSONObject(0);
				result[0] = jsonVec4f.has("x") ? (float) jsonVec4f.getDouble("x") : 0f;
				result[1] = jsonVec4f.has("y") ? (float) jsonVec4f.getDouble("y") : 0f;
				result[2] = jsonVec4f.has("z") ? (float) jsonVec4f.getDouble("z") : 0f;
				result[3] = jsonVec4f.has("a") ? (float) jsonVec4f.getDouble("a") : 0f;
			}
		}
		return result;
	}

	private ArvosObject findArvosObject(List<ArvosObject> arvosObjects)
	{
		for (ArvosObject arvosObject : arvosObjects)
		{
			if (arvosObject.mId == mId)
			{
				return arvosObject;
			}
		}
		return new ArvosObject(mId);
	}

	private long mWorldStartTime = -1;
	private long mWorldIteration = -1;

	/**
	 * Returns the arvos object to be drawn in the opengl view.
	 * 
	 * @param time
	 *            The current time.
	 * @param arvosObjects
	 *            List of previous objects.
	 * @return The object.
	 */
	public ArvosObject getObject(long time, List<ArvosObject> arvosObjects)
	{
		ArvosObject result = null;

		if (mWorldStartTime < 0)
		{
			mWorldStartTime = time;
			mWorldIteration = 0;
		}

		if (!mIsActive)
		{
			return result;
		}

		long duration = (mAnimationDuration > 0) ? mAnimationDuration : mParent.mAnimationDuration;
		if (mParent.mAnimationDuration <= 0 || duration <= 0)
		{
			// No animation, use start values
			//
			result = findArvosObject(arvosObjects);
			result.mName = mName;
			result.mTextureUrl = mTextureUrl;
			result.mBillboardHandling = mBillboardHandling;
			if (result.mPosition == null)
			{
				result.mPosition = new float[3];
			}
			result.mPosition[0] = mStartPosition[0];
			result.mPosition[1] = mStartPosition[1];
			result.mPosition[2] = mStartPosition[2];
			result.mScale = mStartScale;
			result.mRotation = mStartRotation;
			result.mImage = mImage;

			return result;
		}

		long worldTime = time - mWorldStartTime;
		long iteration = worldTime / mParent.mAnimationDuration;
		if (iteration > mWorldIteration)
		{
			mWorldIteration = iteration;
			mParent.requestStop(this);
			return null;
		}

		if (mTextureUrl == null)
		{
			return null;
		}

		long loopTime = worldTime % mParent.mAnimationDuration;
		if (loopTime < mStartTime || loopTime >= mStartTime + duration)
		{
			return null;
		}

		float factor = loopTime - mStartTime;
		factor /= duration;

		result = findArvosObject(arvosObjects);
		result.mName = mName;
		result.mTextureUrl = mTextureUrl;
		result.mBillboardHandling = mBillboardHandling;
		result.mPosition = mStartPosition;
		result.mScale = mStartScale;
		result.mRotation = mStartRotation;
		result.mImage = mImage;

		if (mStartPosition != null && mEndPosition != null && factor > 0)
		{
			result.mPosition = new float[3];
			result.mPosition[0] = mStartPosition[0] + factor * (mEndPosition[0] - mStartPosition[0]);
			result.mPosition[1] = mStartPosition[1] + factor * (mEndPosition[1] - mStartPosition[1]);
			result.mPosition[2] = mStartPosition[2] + factor * (mEndPosition[2] - mStartPosition[2]);
		}

		if (mStartScale != null && mEndScale != null && factor > 0)
		{
			result.mScale = new float[3];
			result.mScale[0] = mStartScale[0] + factor * (mEndScale[0] - mStartScale[0]);
			result.mScale[1] = mStartScale[1] + factor * (mEndScale[1] - mStartScale[1]);
			result.mScale[2] = mStartScale[2] + factor * (mEndScale[2] - mStartScale[2]);
		}

		if (mStartRotation != null && mEndRotation != null && factor > 0)
		{
			result.mRotation = new float[4];
			result.mRotation[0] = mStartRotation[0] + factor * (mEndRotation[0] - mStartRotation[0]);
			result.mRotation[1] = mStartRotation[1] + factor * (mEndRotation[1] - mStartRotation[1]);
			result.mRotation[2] = mStartRotation[2] + factor * (mEndRotation[2] - mStartRotation[2]);
			result.mRotation[3] = mStartRotation[3] + factor * (mEndRotation[3] - mStartRotation[3]);
		}
		return result;
	}

	/**
	 * Called when the animation of the object stops.
	 */
	public void stop()
	{
		onDurationEnd();

		if (mLoop)
		{
			mParent.requestStart(this);
		}
		else
		{
			mIsActive = false;
		}
	}

	/**
	 * Called when the animation of the object starts.
	 * 
	 * @param time
	 *            The current time.
	 */
	public void start(long time)
	{
		mTimeStarted = time;
	}

	/**
	 * Handles a click on the object.
	 */
	public void onClick()
	{
		handleAction(mOnClickActivates, mOnClickDeactivates, mOnClickUrls);
	}

	private void onDurationEnd()
	{
		handleAction(mOnDurationEndActivates, mOnDurationEndDeactivates, mOnDurationEndUrls);
	}

	private void handleAction(List<String> activates, List<String> deactivates, List<String> urls)
	{
		if (activates != null)
		{
			for (String otherObjectName : activates)
			{
				ArvosPoiObject poiObject = mParent.findPoiObject(otherObjectName);
				if (poiObject != null)
				{
					poiObject.mIsActive = true;
					mParent.requestActivate(poiObject);
				}
			}
		}
		if (deactivates != null)
		{
			for (String otherObjectName : deactivates)
			{
				ArvosPoiObject poiObject = mParent.findPoiObject(otherObjectName);
				if (poiObject != null)
				{
					poiObject.mIsActive = false;
					mParent.requestDeactivate(poiObject);
				}
			}
		}
		if (urls != null)
		{
			for (String url : urls)
			{
				Arvos.getInstance().startWebViewer(url);
			}
		}
	}
}
