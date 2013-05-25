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

/**
 * A poi - point of interest.
 * <p>
 * Contains a list of poiObjects.
 * @author peter
 * 
 */
public class ArvosPoi
{
	public long mAnimationDuration;

	public Float mLongitude = null;
	public Float mLatitude = null;
	public String mDeveloperKey;

	public ArvosAugment mParent;
	public List<ArvosPoiObject> mPoiObjects;

	private List<ArvosPoiObject> mObjectsToDeactivate = new LinkedList<ArvosPoiObject>();
	private List<ArvosPoiObject> mObjectsToStart = new LinkedList<ArvosPoiObject>();
	private List<ArvosPoiObject> mObjectsClicked = new LinkedList<ArvosPoiObject>();

	private Arvos mInstance;

	/**
	 * Constructor.
	 * 
	 * @param augment
	 *            The augment the poi belongs to.
	 */
	public ArvosPoi(ArvosAugment augment)
	{
		mParent = augment;
		mPoiObjects = new LinkedList<ArvosPoiObject>();
		mInstance = Arvos.getInstance();
	}

	/**
	 * Parses the description of a poi in JSON format.
	 * 
	 * @param jsonPoi
	 *            The JSON object to parse.
	 * @throws JSONException
	 *             JSON parse exceptions.
	 */
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
			JSONObject newPoiObject = jsonPoiObjects.getJSONObject(i);
			if (newPoiObject != null)
			{
				ArvosPoiObject poiObject = new ArvosPoiObject(this);
				poiObject.parse(newPoiObject);
				mPoiObjects.add(poiObject);
			}
		}
		return;
	}

	/**
	 * Returns the list of all objects to be drawn for the augment in the opengl
	 * view.
	 * 
	 * @param time
	 *            Returns the list of all objects to be drawn for the augment in
	 *            the opengl view.
	 * @param result
	 *            The list to add the resulting objects to.
	 * @param arvosObjects
	 *            The previous list of objects.
	 */
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
			Location currentLocation = new Location(LocationManager.GPS_PROVIDER);
			currentLocation.setLatitude(deviceLatitude);

			Location poiLocation = new Location(LocationManager.GPS_PROVIDER);
			poiLocation.setLatitude(poiLatitude);

			offsetZ = currentLocation.distanceTo(poiLocation);
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
			Location currentLocation = new Location(LocationManager.GPS_PROVIDER);
			currentLocation.setLongitude(deviceLongitude);

			Location poiLocation = new Location(LocationManager.GPS_PROVIDER);
			poiLocation.setLongitude(poiLongitude);

			offsetX = currentLocation.distanceTo(poiLocation);
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
		for (ArvosPoiObject poiObject : mPoiObjects)
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
			for (ArvosPoiObject poiObject : mObjectsClicked)
			{
				poiObject.onClick();
			}
			mObjectsClicked.clear();
		}

		for (ArvosPoiObject poiObject : mObjectsToDeactivate)
		{
			poiObject.stop();
		}
		mObjectsToDeactivate.clear();

		for (ArvosPoiObject poiObject : mObjectsToStart)
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
		mObjectsToStart.clear();
	}

	/**
	 * Searches a poi object by name.
	 * 
	 * @param name
	 * @return
	 */
	public ArvosPoiObject findPoiObject(String name)
	{
		for (ArvosPoiObject poiObject : mPoiObjects)
		{
			if (name.equals(poiObject.mName))
			{
				return poiObject;
			}
		}
		for (ArvosPoi poi : mParent.mPois)
		{
			for (ArvosPoiObject poiObject : poi.mPoiObjects)
			{
				if (name.equals(poiObject.mName))
				{
					return poiObject;
				}
			}
		}
		return null;
	}

	/**
	 * Searches a poi object by id.
	 * 
	 * @param id
	 * @return
	 */
	public ArvosPoiObject findPoiObject(int id)
	{
		for (ArvosPoi poi : mParent.mPois)
		{
			for (ArvosPoiObject poiObject : poi.mPoiObjects)
			{
				if (id == poiObject.mId)
				{
					return poiObject;
				}
			}
		}
		return null;
	}

	/**
	 * Handles activation of a poi object.
	 * 
	 * @param poiObject
	 */
	public void requestActivate(ArvosPoiObject poiObject)
	{
		poiObject.mIsActive = true;
		requestStart(poiObject);
	}

	/**
	 * Handles the start of a poi object animation.
	 * 
	 * @param poiObject
	 */
	public void requestStart(ArvosPoiObject poiObject)
	{
		mObjectsToStart.add(poiObject);
	}

	/**
	 * Handles the stop of a poi object animation.
	 * 
	 * @param poiObject
	 */
	public void requestStop(ArvosPoiObject poiObject)
	{
		mObjectsToDeactivate.add(poiObject);
	}

	/**
	 * Handles the deactivation of a poi object.
	 * 
	 * @param poiObject
	 */
	public void requestDeactivate(ArvosPoiObject poiObject)
	{
		poiObject.mIsActive = false;
		requestStop(poiObject);
	}

	/**
	 * Handles a click on a poi object.
	 * 
	 * @param poiObject
	 */
	public void addClick(ArvosPoiObject poiObject)
	{
		synchronized (mObjectsClicked)
		{
			mObjectsClicked.add(poiObject);
		}
	}
}
