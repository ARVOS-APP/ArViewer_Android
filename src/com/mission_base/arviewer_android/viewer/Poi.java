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

import android.location.*;
import com.mission_base.arviewer_android.*;
import com.mission_base.arviewer_android.viewer.opengl.*;
import java.util.*;
import org.json.*;

public class Poi
{
	public long mAnimationDuration;

	public Float mLongitude = null;
	public Float mLatitude = null;
	public String mDeveloperKey;

	public Augment mParent;
	public List<PoiObject> mPoiObjects;

	private List<PoiObject> mObjectsToDeactivate = new LinkedList<PoiObject>();
	private List<PoiObject> mObjectsToStart = new LinkedList<PoiObject>();
	private List<PoiObject> mObjectsClicked = new LinkedList<PoiObject>();

	private Arvos mInstance;

	public Poi(Augment augment)
	{
		mParent = augment;
		mPoiObjects = new LinkedList<PoiObject>();
		mInstance = Arvos.getInstance();
	}

	public void parse(JSONObject jsonPoi) throws JSONException
	{
		mAnimationDuration = jsonPoi.has("animationDuration") ? jsonPoi.getInt("animationDuration") : 0;

		if (jsonPoi.has("lat"))
		{
			mLatitude = (float) jsonPoi.getDouble("lat");
		}
		if (jsonPoi.has("lon"))
		{
			mLongitude = (float) jsonPoi.getDouble("lon");
		}
		mDeveloperKey = jsonPoi.has("developerKey") ? jsonPoi.getString("developerKey") : null;

		JSONArray jsonPoiObjects = new JSONArray(jsonPoi.getString("poiObjects"));
		if (jsonPoiObjects == null || jsonPoiObjects.length() == 0)
		{
			throw new JSONException("No poiObjects found in poi.");
		}

		for (int i = 0; i < jsonPoiObjects.length(); i++)
		{
			JSONObject jsonPoiObject = jsonPoiObjects.getJSONObject(i);
			if (jsonPoiObject != null)
			{
				PoiObject poiObject = new PoiObject(this);
				poiObject.parse(jsonPoiObject);
				mPoiObjects.add(poiObject);
			}
		}
		return;
	}

	public void getObjects(long time, List<ArvosObject> result, List<ArvosObject> arvosObjects)
	{
		float deviceLatitude = mInstance.mLatitude;
		float deviceLongitude = mInstance.mLongitude;
		float poiLatitude = 0f;
		float poiLongitude = 0f;

		float offsetX = 0f;
		float offsetZ = 0f;

		if (mLatitude != null)
		{
			poiLatitude = mLatitude;
			Location currentLoction = new Location(LocationManager.GPS_PROVIDER);
			currentLoction.setLatitude(deviceLatitude);

			Location poiLocation = new Location(LocationManager.GPS_PROVIDER);
			poiLocation.setLatitude(poiLatitude);

			offsetZ = currentLoction.distanceTo(poiLocation);
			if (deviceLatitude > poiLatitude)
			{
				if (offsetZ < 0)
				{
					offsetZ = -offsetZ;
				}
			}
			else
			{
				if (offsetZ > 0)
				{
					offsetZ = -offsetZ;
				}
			}

		}
		if (mLongitude != null)
		{
			poiLongitude = mLongitude;
			Location currentLoction = new Location(LocationManager.GPS_PROVIDER);
			currentLoction.setLatitude(deviceLongitude);

			Location poiLocation = new Location(LocationManager.GPS_PROVIDER);
			poiLocation.setLatitude(poiLongitude);

			offsetX = currentLoction.distanceTo(poiLocation);
			if (deviceLongitude > poiLongitude)
			{
				if (offsetX > 0)
				{
					offsetX = -offsetX;
				}
			}
			else
			{
				if (offsetX < 0)
				{
					offsetX = -offsetX;
				}
			}
		}

		HashSet<String> objectsToDraw = new HashSet<String>();
		for (PoiObject poiObject : mPoiObjects)
		{
			ArvosObject arvosObject = poiObject.getObject(time, arvosObjects);
			if (arvosObject != null)
			{
				arvosObject.mPosition[0] += offsetX;
				arvosObject.mPosition[2] += offsetZ;

				objectsToDraw.add(arvosObject.mName);
				result.add(arvosObject);
			}
		}
		
		synchronized (mObjectsClicked)
		{
			for (PoiObject poiObject : mObjectsClicked)
			{
				poiObject.onClick();
			}
			mObjectsClicked.clear();
		}

		for (PoiObject poiObject : mObjectsToDeactivate)
		{
			poiObject.stop();
		}

		for (PoiObject poiObject : mObjectsToStart)
		{
			poiObject.start(time);

			if (!objectsToDraw.contains(poiObject.mName))
			{
				ArvosObject arvosObject = poiObject.getObject(time, arvosObjects);
				if (arvosObject != null)
				{
					objectsToDraw.add(arvosObject.mName);
					result.add(arvosObject);
				}
			}
		}

		mObjectsToDeactivate.clear();
		mObjectsToStart.clear();
	}

	public PoiObject findPoiObject(String name)
	{
		for (PoiObject poiObject : mPoiObjects)
		{
			if (name.equals(poiObject.mName))
			{
				return poiObject;
			}
		}
		for (Poi poi : mParent.mPois)
		{
			for (PoiObject poiObject : poi.mPoiObjects)
			{
				if (name.equals(poiObject.mName))
				{
					return poiObject;
				}
			}
		}
		return null;
	}
	public PoiObject findPoiObject(int id)
	{
		for (Poi poi : mParent.mPois)
		{
			for (PoiObject poiObject : poi.mPoiObjects)
			{
				if (id == poiObject.mId)
				{
					return poiObject;
				}
			}
		}
		return null;
	}

	public void requestActivate(PoiObject poiObject)
	{
		poiObject.mIsActive = true;
		requestStart(poiObject);
	}

	public void requestStart(PoiObject poiObject)
	{
		mObjectsToStart.add(poiObject);
	}

	public void requestStop(PoiObject poiObject)
	{
		mObjectsToDeactivate.add(poiObject);
	}

	public void requestDeactivate(PoiObject poiObject)
	{
		poiObject.mIsActive = false;
		requestStop(poiObject);
	}

	public void addClick(PoiObject poiObject)
	{
		synchronized (mObjectsClicked)
		{
			mObjectsClicked.add(poiObject);
		}
	}
}
