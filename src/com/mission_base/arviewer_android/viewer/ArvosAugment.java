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

import com.mission_base.arviewer_android.*;
import com.mission_base.arviewer_android.viewer.opengl.*;
import java.util.*;
import org.json.*;

/**
 * An augment as shown in the augment viewer.
 * <p>
 * Contains a list of pois.
 * 
 * @author peter
 */
public class ArvosAugment
{
	public String mName;
	public String mUrl;
	public String mAuthor;
	public String mDescription;
	public Float mLongitude;
	public Float mLatitude;
	public String mDeveloperKey;

	public LinkedList<ArvosPoi> mPois;

	public ArvosAugment()
	{
		mPois = new LinkedList<ArvosPoi>();
	}

	/**
	 * Parses the JSON format augment list downloaded from the web.
	 * 
	 * @param input
	 *            The input in JSON format.
	 * @param result
	 *            The list of augments to parse to.
	 * @return "OK" or "ER" followed by the error message.
	 */
	public static String parse(String input, List<ArvosAugment> result)
	{
		try
		{
			JSONObject jsonAugmentsList = new JSONObject(input);
			if (jsonAugmentsList.has("redirect"))
			{
				String redirectUrl = jsonAugmentsList.getString("redirect");
				if (redirectUrl != null)
				{
					redirectUrl = redirectUrl.trim();
					if (redirectUrl.length() > 0)
					{
						return "RD" + redirectUrl;
					}
				}
			}

			if (jsonAugmentsList.has("sessionId"))
			{
				String sessionId = jsonAugmentsList.getString("sessionId");
				if (sessionId != null)
				{
					sessionId = sessionId.trim();
					if (sessionId.length() > 0)
					{
						Arvos.getInstance().mSessionId = sessionId;
					}
				}
			}

			JSONArray jsonAugments = new JSONArray(jsonAugmentsList.getString("augments"));

			if (jsonAugments == null || jsonAugments.length() == 0)
			{
				return "ERNo augments found at your location. Retry ...";
			}

			for (int i = 0; i < jsonAugments.length(); i++)
			{
				JSONObject jsonAugment = jsonAugments.getJSONObject(i);
				if (jsonAugment != null)
				{
					ArvosAugment augment = new ArvosAugment();
					result.add(augment);

					augment.mName = jsonAugment.getString("name");
					augment.mUrl = jsonAugment.getString("url");

					augment.mAuthor = jsonAugment.has("author") ? jsonAugment.getString("author") : "";
					augment.mLatitude = (float) (jsonAugment.has("lat") ? jsonAugment.getDouble("lat") : 0f);
					augment.mLongitude = (float) (jsonAugment.has("lon") ? jsonAugment.getDouble("lon") : 0f);
					augment.mDescription = jsonAugment.has("description") ? jsonAugment.getString("description") : "";
					augment.mDeveloperKey = jsonAugment.has("developerKey") ? jsonAugment.getString("developerKey") : "";
				}
			}
		}
		catch (Exception e)
		{
			return "ERJSON parse error. " + e.getLocalizedMessage();
		}

		return "OK";
	}

	/**
	 * Fills the properties of one augment by parsing a description in JSON
	 * format downloaded from the web.
	 * 
	 * @param input
	 *            The augment description in JSON format.
	 * @return "OK" or "ER" followed by the error message.
	 */
	public String parse(String input)
	{
		try
		{
			JSONObject jsonAugment = new JSONObject(input);
			mName = jsonAugment.getString("name");
			mAuthor = jsonAugment.has("author") ? jsonAugment.getString("author") : "";
			mDescription = jsonAugment.has("description") ? jsonAugment.getString("description") : "";

			JSONArray jsonPois = new JSONArray(jsonAugment.getString("pois"));

			if (jsonPois == null || jsonPois.length() == 0)
			{
				return "ERNo pois found in augment " + mName;
			}

			for (int i = 0; i < jsonPois.length(); i++)
			{
				JSONObject newPoi = jsonPois.getJSONObject(i);
				if (newPoi != null)
				{
					ArvosPoi poi = new ArvosPoi(this);
					poi.parse(newPoi);
					mPois.add(poi);
				}
			}
		}
		catch (Exception e)
		{
			return "ERJSON parse error. " + e.getLocalizedMessage();
		}

		return "OK";
	}

	/**
	 * Returns the list of all objects to be drawn for the augment in the opengl view.
	 * 
	 * @param time
	 *            The current time.
	 * @param arvosObjects
	 *            The previous list of objects.
	 * @return Returns the list of all objects.
	 */
	public List<ArvosObject> getObjects(long time, List<ArvosObject> arvosObjects)
	{
		List<ArvosObject> result = new LinkedList<ArvosObject>();

		synchronized (mPois)
		{
			for (ArvosPoi poi : mPois)
			{
				poi.getObjects(time, result, arvosObjects);
			}
		}
		return result;
	}

	/**
	 * Handles a click on an object in the opengl view.
	 * 
	 * @param id
	 *            The id of the object clicked.
	 */
	public void addClick(int id)
	{
		synchronized (this)
		{
			if (!mPois.isEmpty())
			{
				ArvosPoi poi = mPois.getFirst();
				ArvosPoiObject poiObject = poi.findPoiObject(id);
				if (poiObject != null)
				{
					poiObject.mParent.addClick(poiObject);
				}
			}
		}
	}
}
