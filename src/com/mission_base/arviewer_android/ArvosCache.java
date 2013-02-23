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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

/**
 * Caches web files in a folder below the external cache directory or, if it is
 * not available, below the cache directory.
 * <p>
 * Implements an LRU cache where the maximum age of files, maximum number of
 * files and maximum number of bytes used can be specified during
 * initialization.
 * <p>
 * Each cached item is stored in a file, the last access time stamp of the item
 * is used as file name. Each file's first line contains the url of the cached
 * item, the cached item itself follows after that line. The static cache
 * instance keeps the list of cached urls and file names in memory for fast
 * access.
 * 
 * @author peter
 * 
 */
public class ArvosCache
{
	private static String mLock = "lock";
	private static ArvosCache instance;

	/**
	 * File name extension used for files in the cache.
	 */
	public static final String mExtension = ".arvos";

	private TreeSet<ArvosCacheEntry> mEntrySet = new TreeSet<ArvosCacheEntry>();
	private HashMap<String, ArvosCacheEntry> mEntryMap = new HashMap<String, ArvosCacheEntry>();
	private long mSize = 0;
	private File mCacheDir = null;

	private long mMaxAge;
	private long mMaxSize;
	private long mMaxFiles;

	private ArvosCache(long maxAge, long maxFiles, long maxSize)
	{
		mMaxAge = maxAge;
		mMaxFiles = maxFiles;
		mMaxSize = maxSize;
	}

	private Activity mActivity;

	/**
	 * Initializes the static cache instance.
	 * 
	 * @param activity
	 *            The activity using the cache.
	 * @param maxAge
	 *            Maximum age of cached files in milliseconds.
	 * @param maxFiles
	 *            Maximum number of files in cache.
	 * @param maxSize
	 *            Maximum total size of cache in bytes.
	 */
	public static void initialize(Activity activity, long maxAge, long maxFiles, long maxSize)
	{
		if (instance == null)
		{
			synchronized (mLock)
			{
				if (instance == null)
				{
					instance = new ArvosCache(maxAge, maxFiles, maxSize);
					instance.mActivity = activity;
				}
			}
		}
	}

	private static ArvosCache getInstance()
	{
		if (instance.mCacheDir == null)
		{
			synchronized (mLock)
			{
				if (instance.mCacheDir == null)
				{
					instance.init();
				}
			}
		}
		return instance;
	}

	private void init()
	{
		if (instance.IsExternalStorageAvailableAndWriteable())
		{
			mCacheDir = mActivity.getExternalCacheDir();
		}
		if (mCacheDir == null)
		{
			mCacheDir = mActivity.getCacheDir();
		}
		mCacheDir = new File(mCacheDir, "webcachedir");
		if (!mCacheDir.exists())
		{
			mCacheDir.mkdirs();
		}

		File[] mFiles = mCacheDir.listFiles();
		if (mFiles == null)
		{
			return;
		}

		char[] inputBuffer = new char[1];
		StringBuilder sb = new StringBuilder();

		for (File file : mFiles)
		{
			Long lastAccessTime = getLastAccessTime(file);
			if (lastAccessTime == null)
			{
				continue;
			}

			try
			{
				FileInputStream fIn = new FileInputStream(file);
				InputStreamReader isr = new InputStreamReader(fIn);

				try
				{
					sb.setLength(0);
					int length = 0;
					while (isr.read(inputBuffer) == 1)
					{
						length++;

						if ('\n' == inputBuffer[0])
						{
							ArvosCacheEntry entry = new ArvosCacheEntry();
							entry.url = sb.toString();
							entry.urlLength = length;
							entry.lastAccessTime = lastAccessTime;
							entry.fileLength = file.length();
							mEntrySet.add(entry);
							break;
						}

						sb.append(inputBuffer[0]);
					}
				}
				finally
				{
					isr.close();
				}
			}
			catch (Exception e)
			{
				continue;
			}
		}
		for (Iterator<ArvosCacheEntry> iterator = mEntrySet.iterator(); iterator.hasNext();)
		{
			ArvosCacheEntry entry = iterator.next();
			if (mEntryMap.containsKey(entry.url))
			{
				iterator.remove();
				delete(entry);
			}

			mEntryMap.put(entry.url, entry);
			mSize += entry.fileLength;
		}
		cleanup();
	}

	private Long getLastAccessTime(File file)
	{
		if (!file.isFile())
		{
			return null;
		}
		String name = file.getName();
		if (!name.endsWith(mExtension))
		{
			return null;
		}
		try
		{
			return Long.valueOf(name.replace(mExtension, ""));
		}
		catch (Exception e)
		{
		}
		return null;
	}

	private Long getLastAccessTime()
	{
		long now = System.currentTimeMillis();
		if (!mEntrySet.isEmpty())
		{
			if (mEntrySet.last().lastAccessTime.compareTo(Long.valueOf(now)) >= 0)
			{
				return Long.valueOf(mEntrySet.last().lastAccessTime + 1);
			}
		}
		return Long.valueOf(now);
	}

	private void cleanup()
	{
		long now = System.currentTimeMillis();

		while (mEntrySet.size() > 1 && (mMaxAge > 0L && (now - mEntrySet.first().lastAccessTime > mMaxAge)) || (mMaxSize > 0L && mSize > mMaxSize)
				|| (mMaxFiles > 0L && mEntrySet.size() > mMaxFiles))
		{
			delete(mEntrySet.first());
		}
	}

	/**
	 * Clears the cache, deletes all cached items.
	 */
	public static void clear()
	{
		getInstance().clearCache();
	}

	private void clearCache()
	{
		synchronized (mLock)
		{
			while (!mEntrySet.isEmpty())
			{
				delete(mEntrySet.first());
			}
		}
	}

	private void delete(ArvosCacheEntry entry)
	{
		File file = new File(mCacheDir, entry.getFileName());
		if (file.exists())
		{
			file.delete();
		}
		mEntryMap.remove(entry.url);
		if (mEntrySet.contains(entry))
		{
			mEntrySet.remove(entry);
		}
		mSize -= entry.fileLength;
	}

	/**
	 * Returns a cached bitmap or null if the bitmap is not in the cache.
	 * 
	 * @param url
	 *            The url to of the bitmap to search in the cache.
	 * @return The bitmap or null.
	 */
	public static Bitmap getBitmap(String url)
	{
		if (!Arvos.getInstance().mUseCache)
		{
			return null;
		}
		return getInstance().getCachedBitmap(ArvosHttpRequest.urlEncode(url));
	}

	private Bitmap getCachedBitmap(String url)
	{
		synchronized (mLock)
		{
			ArvosCacheEntry entry = mEntryMap.get(url);
			if (entry == null)
			{
				return null;
			}
			File file = new File(mCacheDir, entry.getFileName());
			try
			{
				if (file.exists())
				{
					Bitmap bitmap = null;
					FileInputStream inputStream = new FileInputStream(file);
					try
					{
						if (entry.urlLength == inputStream.skip(entry.urlLength))
						{
							bitmap = BitmapFactory.decodeStream(inputStream);
						}
					}
					finally
					{
						inputStream.close();
					}
					if (bitmap != null)
					{
						mEntrySet.remove(entry);
						entry.lastAccessTime = getLastAccessTime();
						mEntrySet.add(entry);
						file.renameTo(new File(mCacheDir, entry.getFileName()));
						return bitmap;
					}
				}
				delete(entry);
			}
			catch (Exception e)
			{
			}
			return null;
		}
	}

	/**
	 * Adds a bitmap to the cache.
	 * 
	 * @param url
	 *            The url of the bitmap to add.
	 * @param bitmap
	 *            The bitmap to add.
	 */
	public static void add(String url, Bitmap bitmap)
	{
		if (!Arvos.getInstance().mUseCache)
		{
			return;
		}
		getInstance().addBitmap(ArvosHttpRequest.urlEncode(url), bitmap);
	}

	private void addBitmap(String url, Bitmap bitmap)
	{
		synchronized (mLock)
		{
			ArvosCacheEntry other = mEntryMap.get(url);
			if (other != null)
			{
				delete(other);
			}

			ArvosCacheEntry entry = new ArvosCacheEntry();
			entry.url = url;
			entry.lastAccessTime = getLastAccessTime();
			File file = new File(mCacheDir, entry.getFileName());

			try
			{
				FileOutputStream fOut = new FileOutputStream(file);
				OutputStreamWriter osw = new OutputStreamWriter(fOut);

				try
				{
					osw.write(url + "\n");
					osw.flush();
					entry.urlLength = (int) file.length();

					bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
				}
				finally
				{
					osw.close();
					entry.fileLength = file.length();
				}
			}
			catch (Exception e)
			{
				file.delete();
				entry = null;
			}
			if (entry != null)
			{
				mSize += entry.fileLength;
				mEntrySet.add(entry);
				mEntryMap.put(entry.url, entry);
			}
			cleanup();
		}
	}

	private boolean IsExternalStorageAvailableAndWriteable()
	{
		boolean externalStorageAvailable = false;
		boolean externalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state))
		{
			externalStorageAvailable = externalStorageWriteable = true;
		}
		else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
		{
			externalStorageAvailable = true;
			externalStorageWriteable = false;
		}
		else
		{
			externalStorageAvailable = externalStorageWriteable = false;
		}
		return externalStorageAvailable && externalStorageWriteable;
	}
}
