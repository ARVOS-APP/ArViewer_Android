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

import com.mission_base.arviewer_android.Arvos;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.widget.TextView;

/**
 * The view showing current state of the application.
 * 
 * @author peter
 * 
 */
public class ArvosTextView extends TextView
{
	private LocationManager mLocationManager;

	public ArvosTextView(Context context)
	{
		super(context);
		String svcName = Context.LOCATION_SERVICE;
		mLocationManager = (LocationManager) Arvos.getInstance().getActivity().getSystemService(svcName);
		setTextColor(Color.RED);
	}

	/**
	 * Handles location updates displays some state information.
	 */
	public void updateWithNewLocation()
	{
		String provider = LocationManager.GPS_PROVIDER;
		Location location = mLocationManager.getLastKnownLocation(provider);

		String latLongString = "No location found";
		if (location != null)
		{
			latLongString = //
			"Lat:" + location.getLatitude() //
					+ "\nLon:" + location.getLongitude() //
			;
		}
		latLongString += "\nFPS:" + Arvos.getInstance().mFPS //
				+ "\nAzi:" + Arvos.getInstance().mAzimuth //
				+ "\nPit:" + Arvos.getInstance().mPitch //
				+ "\nRol:" + Arvos.getInstance().mRoll //
				+ "\nOri:" + Arvos.getInstance().getRotationDegrees() //
				+ "\nDeg:" + Arvos.getInstance().mCorrectedAzimuth //
		;

		setText(latLongString);
	}
}
