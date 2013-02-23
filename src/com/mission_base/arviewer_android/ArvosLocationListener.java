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

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;

/**
 * Listens for location updates from the LocationManager.
 * 
 * @author peter
 * 
 */
public class ArvosLocationListener implements LocationListener
{
	private IArvosLocationReceiver mReceiver;
	private LocationManager mLocationManager;
	private Arvos mInstance;

	/**
	 * Creates a new listener.
	 * 
	 * @param locationManager
	 *            The location manager to listen to.
	 * @param receiver
	 *            The receiver of the location updates.
	 */
	public ArvosLocationListener(LocationManager locationManager, IArvosLocationReceiver receiver)
	{
		mLocationManager = locationManager; // (LocationManager)
											// getSystemService(Context.LOCATION_SERVICE);
		mReceiver = receiver;
		mInstance = Arvos.getInstance();
	}

	public void onResume()
	{
		requestLocation();
	}

	public void onPause()
	{
		Log.d("removeUpdates", "this");
		mLocationManager.removeUpdates(this);
	}

	private Location mCurrentBestLocation = null;

	private void requestLocation()
	{
		Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (location != null)
		{
			onLocationChanged(false, location);
		}

		location = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if (location != null)
		{
			onLocationChanged(false, location);
		}

		Log.d("requestLocationUpdates", LocationManager.GPS_PROVIDER);
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 3, this);
		Log.d("requestLocationUpdates", LocationManager.NETWORK_PROVIDER);
		mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 3, this);
	}

	@Override
	public void onLocationChanged(Location location)
	{
		onLocationChanged(true, location);
	}

	private void onLocationChanged(boolean isNew, Location location)
	{
		// Log.d("onLocationChanged", "new " + isNew);
		// Log.d("onLocationChanged", location.getProvider());

		mInstance.mLatitude = (float) location.getLatitude();
		// Log.d("onLocationChanged", "lat " + mInstance.mLatitude);

		mInstance.mLongitude = (float) location.getLongitude();
		// Log.d("onLocationChanged", "lon " + mInstance.mLongitude);
		//
		// Log.d("onLocationChanged", "alt " + location.getAltitude());
		// Log.d("onLocationChanged", "acc " + location.getAccuracy());
		// Log.d("onLocationChanged", "tim " + location.getTime());

		if (mReceiver != null)
		{
			if (isBetterLocation(location, mCurrentBestLocation))
			{
				mCurrentBestLocation = location;
				mReceiver.onLocationChanged(true, location);
			}
		}
	}

	@Override
	public void onProviderDisabled(String provider)
	{
		Log.d("onProviderDisabled", provider);
	}

	@Override
	public void onProviderEnabled(String provider)
	{
		Log.d("onProviderEnabled", provider);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras)
	{
		if (status == LocationProvider.OUT_OF_SERVICE)
		{
			Log.d("LocationProvider.OUT_OF_SERVICE", provider);
		}
		else if (status == LocationProvider.TEMPORARILY_UNAVAILABLE)
		{
			Log.d("LocationProvider.TEMPORARILY_UNAVAILABLE", provider);
		}
		else
		{
			Log.d("LocationProvider.AVAILABLE", provider);
		}
	}

	private static final int TWO_MINUTES = 1000 * 60 * 2;

	/**
	 * Determines whether one Location reading is better than the current
	 * Location fix
	 * 
	 * @param location
	 *            The new Location that you want to evaluate
	 * @param currentBestLocation
	 *            The current Location fix, to which you want to compare the new
	 *            one
	 */
	protected boolean isBetterLocation(Location location, Location currentBestLocation)
	{
		if (currentBestLocation == null)
		{
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use
		// the new location
		// because the user has likely moved
		if (isSignificantlyNewer)
		{
			return true;
			// If the new location is more than two minutes older, it must be
			// worse
		}
		else if (isSignificantlyOlder)
		{
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(), currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and
		// accuracy
		if (isMoreAccurate)
		{
			return true;
		}
		else if (isNewer && !isLessAccurate)
		{
			return true;
		}
		else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider)
		{
			return true;
		}
		return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2)
	{
		if (provider1 == null)
		{
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}
}
