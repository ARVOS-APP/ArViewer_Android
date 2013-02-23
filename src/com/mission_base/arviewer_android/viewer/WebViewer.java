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
import android.os.*;
import android.view.*;
import android.webkit.*;
import com.mission_base.arviewer_android.*;

/**
 * The web viewer used to showing web pages liked by pois.
 * 
 * @author peter
 * 
 */
public class WebViewer extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		String url = getIntent().getStringExtra("url");

		setContentView(R.layout.web_viewer_main);

		WebView webView = (WebView) findViewById(R.id.WebView01);
		webView.setWebViewClient(new Callback());
		WebSettings webSettings = webView.getSettings();
		webSettings.setBuiltInZoomControls(true);

		// ---Part 1---
		webView.loadUrl(url);

		// ---Part 2---
		// final String mimeType = "text/html";
		// final String encoding = "UTF-8";
		// String html =
		// "<H1>A simple HTML page</H1><body>" +
		// "<p>The quick brown fox jumps over the lazy dog</p></body>";
		// webView.loadDataWithBaseURL("", html, mimeType, encoding, "");

		// ---Part 3---
		// webView.loadUrl("file:///android_asset/index.html");
	}

	private class Callback extends WebViewClient
	{
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url)
		{
			return (false);
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();
	}

	@Override
	protected void onPause()
	{
		super.onPause();
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
		// menuItem.setIcon(R.drawable.ic_action_refresh_white);
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
