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

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Activity for handling the Arvos preferences.
 * 
 * @author peter
 * 
 */
public class ArvosPreferences extends PreferenceActivity
{
	public static final String PREF_USE_CACHE = "PREF_USE_CACHE";

	public static final String PREF_IS_AUTHOR = "PREF_IS_AUTHOR";
	public static final String PREF_AUTHOR_KEY = "PREF_AUTHOR_KEY";
	public static final String PREF_DEVELOPER_KEY = "PREF_DEVELOPER_KEY";

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if (Arvos.getInstance().mIsAuthor)
		{
			addPreferencesFromResource(R.xml.authorpreferences);
		}
		else
		{
			addPreferencesFromResource(R.xml.userpreferences);
		}
	}
}
