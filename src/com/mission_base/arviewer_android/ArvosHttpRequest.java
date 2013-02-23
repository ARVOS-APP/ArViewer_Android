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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Pair;

/**
 * Handling http downloads asynchronously.
 * 
 * @author peter
 * 
 */
public class ArvosHttpRequest
{
	private Arvos mInstance;
	private IArvosHttpReceiver mReceiver;
	private Context mContext;

	/**
	 * Creates a new asynchronous http request.
	 * 
	 * @param receiver
	 *            The receiver of the downloaded file.
	 * @param context
	 *            The application context.
	 */
	public ArvosHttpRequest(IArvosHttpReceiver receiver, Context context)
	{
		mInstance = Arvos.getInstance();
		mReceiver = receiver;
		mContext = context;
	}

	/**
	 * Downloads a text file from the web.
	 * 
	 * @param url
	 *            The url of the file to download.
	 */
	public void getText(String url)
	{
		new DownloadTextTask().execute(url);
	}

	/**
	 * Downloads an image file from the web.
	 * 
	 * @param url
	 *            The url of the file to download.
	 */
	public void getImage(String url)
	{
		new DownloadImageTask().execute(url);
	}

	private String downloadText(String url)
	{
		float latitude = mInstance.mLatitude;
		float longitude = mInstance.mLongitude;
		float azimuth = mInstance.mCorrectedAzimuth;

		InputStream inputStream = null;
		if (mInstance.mSimulateWeb)
		{
			try
			{
				if (url.contains("augments.json"))
				{
					inputStream = mContext.getResources().openRawResource(R.raw.augments);
				}
				else if (url.contains("augment1.json"))
				{
					inputStream = mContext.getResources().openRawResource(R.raw.augment1);
				}
				else if (url.contains("augment2.json"))
				{
					inputStream = mContext.getResources().openRawResource(R.raw.augment2);
				}
				else if (url.contains("augment3.json"))
				{
					inputStream = mContext.getResources().openRawResource(R.raw.augment3);
				}
				else if (url.contains("augment4.json"))
				{
					inputStream = mContext.getResources().openRawResource(R.raw.augment4);
				}

				if (inputStream != null)
				{
					StringBuilder stringBuilder = new StringBuilder();
					stringBuilder.append("OK");

					BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
					String line;
					while ((line = reader.readLine()) != null)
					{
						line = line.trim();
						if (!line.startsWith("#"))
						{
							stringBuilder.append(line);
						}
					}
					return stringBuilder.toString();
				}
			}
			catch (Exception e)
			{
				return "ERException. " + e.getLocalizedMessage();
			}
			finally
			{
				if (inputStream != null)
				{
					try
					{
						inputStream.close();
					}
					catch (IOException e)
					{
					}
				}
			}
		}

		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("id=");
		stringBuilder.append(mInstance.mSessionId == null ? "" : mInstance.mSessionId);
		stringBuilder.append("&lat=");
		stringBuilder.append(latitude);
		stringBuilder.append("&lon=");
		stringBuilder.append(longitude);
		stringBuilder.append("&azi=");
		stringBuilder.append(azimuth);

		boolean isAuthor = mInstance.mIsAuthor;
		stringBuilder.append("&aut=");
		stringBuilder.append(isAuthor);
		stringBuilder.append("&ver=");
		stringBuilder.append(mInstance.mVersion);

		if (mInstance.mAugmentsUrl.equals(url))
		{
			String key = mInstance.mAuthorKey;
			if (isAuthor && key != null && key.length() >= 20)
			{
				stringBuilder.append("&akey=");
				stringBuilder.append(urlEncode(key));
			}

			key = mInstance.mDeveloperKey;
			if (key != null && key.length() > 0)
			{
				stringBuilder.append("&dkey=");
				stringBuilder.append(urlEncode(key));
			}
		}

		if (url.contains("?"))
		{
			url = url.replaceFirst("?", "?" + stringBuilder.toString() + "&");
		}
		else if (url.contains("#"))
		{
			url = url.replaceFirst("#", "#" + stringBuilder.toString() + "&");
		}
		else
		{
			url = url + "#" + stringBuilder.toString();
		}

		stringBuilder = new StringBuilder();
		stringBuilder.append("OK");

		try
		{
			HttpClient httpClient = new DefaultHttpClient();
			HttpResponse response = httpClient.execute(new HttpGet(url));
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == 200)
			{
				HttpEntity entity = response.getEntity();
				inputStream = entity.getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
				String line;
				while ((line = reader.readLine()) != null)
				{
					line = line.trim();
					if (!line.startsWith("#"))
					{
						stringBuilder.append(line);
					}
				}
			}
			else
			{
				return "ERHTTP error status " + statusCode;
			}
		}
		catch (Exception e)
		{
			return "ERNetwork error. " + e.getLocalizedMessage();
		}
		finally
		{
			if (inputStream != null)
			{
				try
				{
					inputStream.close();
				}
				catch (IOException e)
				{
				}
			}
		}
		return stringBuilder.toString();
	}

	/**
	 * Encodes an url.
	 * 
	 * @param in
	 *            The url to encode.
	 * @return The endoced url.
	 */
	public static String urlEncode(String in)
	{
		try
		{
			return URLEncoder.encode(in, "UTF-8");
		}
		catch (UnsupportedEncodingException e)
		{
			return "";
		}
	}

	private static InputStream openHttpGETConnection(String url) throws Exception
	{
		InputStream inputStream = null;

		HttpClient httpclient = new DefaultHttpClient();
		HttpResponse httpResponse = httpclient.execute(new HttpGet(url));
		inputStream = httpResponse.getEntity().getContent();

		return inputStream;
	}

	private Pair<String, Bitmap> downloadImage(String url)
	{
		Bitmap bitmap;
		try
		{
			bitmap = ArvosCache.getBitmap(url);
			if (bitmap != null)
			{
				return new Pair<String, Bitmap>("OK", bitmap);
			}
		}
		catch (Exception e)
		{
			return new Pair<String, Bitmap>("ERCache read error. " + e.getLocalizedMessage(), null);
		}

		InputStream inputStream = null;
		if (mInstance.mSimulateWeb)
		{
			try
			{
				if (url.contains("one.png"))
				{
					inputStream = mContext.getResources().openRawResource(R.raw.one);
				}
				else if (url.contains("two.png"))
				{
					inputStream = mContext.getResources().openRawResource(R.raw.two);
				}
				else if (url.contains("three.png"))
				{
					inputStream = mContext.getResources().openRawResource(R.raw.three);
				}

				if (inputStream != null)
				{
					bitmap = BitmapFactory.decodeStream(inputStream);
					try
					{
						ArvosCache.add(url, bitmap);
					}
					catch (Exception e)
					{
						return new Pair<String, Bitmap>("ERCache write error. " + e.getLocalizedMessage(), null);
					}

					return new Pair<String, Bitmap>("OK", bitmap);
				}
			}
			catch (Exception e)
			{
				return new Pair<String, Bitmap>("ERException. " + e.getLocalizedMessage(), null);
			}
			finally
			{
				if (inputStream != null)
				{
					try
					{
						inputStream.close();
					}
					catch (IOException e)
					{
					}
				}
			}
		}

		try
		{
			inputStream = openHttpGETConnection(url);
			bitmap = BitmapFactory.decodeStream(inputStream);
			try
			{
				ArvosCache.add(url, bitmap);
			}
			catch (Exception e)
			{
				return new Pair<String, Bitmap>("ERCache write error. " + e.getLocalizedMessage(), null);
			}

			return new Pair<String, Bitmap>("OK", bitmap);
		}
		catch (Exception e)
		{
			return new Pair<String, Bitmap>("ERNetwork error. " + e.getLocalizedMessage(), null);
		}
		finally
		{
			if (inputStream != null)
			{
				try
				{
					inputStream.close();
				}
				catch (IOException e)
				{
				}
			}
		}
	}

	private class DownloadTextTask extends AsyncTask<String, Void, String>
	{
		private String url;

		protected String doInBackground(String... urls)
		{
			url = urls[0];
			return downloadText(urls[0]);
		}

		@Override
		protected void onPostExecute(String result)
		{
			mReceiver.onHttpResponse(url, result.substring(0, 2), result.substring(2), null);
		}
	}

	private class DownloadImageTask extends AsyncTask<String, Void, Pair<String, Bitmap>>
	{
		private String url;

		protected Pair<String, Bitmap> doInBackground(String... urls)
		{
			url = urls[0];
			return downloadImage(urls[0]);
		}

		protected void onPostExecute(Pair<String, Bitmap> result)
		{
			mReceiver.onHttpResponse(url, result.first.substring(0, 2), result.first.substring(2), result.second);
		}
	}
}
