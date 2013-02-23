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

import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.mission_base.arviewer_android.Arvos;

/**
 * The view showing the camera preview.
 * 
 * @author peter
 * 
 */
public class ArvosCameraView extends SurfaceView implements SurfaceHolder.Callback
{
	private static String mTag = "ArvosCameraView";
	private SurfaceHolder mHolder;
	private Camera mCamera;

	/**
	 * Create the camera view.
	 * 
	 * @param context
	 *            The application context.
	 */
	@SuppressWarnings("deprecation")
	ArvosCameraView(Context context)
	{
		super(context);
		Log.d(mTag, "ArvosCameraView()");

		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	public void surfaceCreated(SurfaceHolder holder)
	{
		Log.d(mTag, "surfaceCreated");
		try
		{
			if (mCamera != null)
			{
				mCamera.release();
			}

			// The Surface has been created, acquire the camera and tell it
			// where to draw.
			mCamera = Camera.open();
			mCamera.setPreviewDisplay(holder);
		}
		catch (Exception e)
		{
			Log.d(mTag, e.getMessage());
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder)
	{
		Log.d(mTag, "surfaceDestroyed");
		try
		{
			// Surface will be destroyed when we return, so stop the preview.
			// Because the CameraDevice object is not a shared resource, it's
			// very important to release it when the activity is paused.
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}
		catch (Exception e)
		{
			Log.d(mTag, e.getMessage());
		}
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h)
	{
		Log.d(mTag, "surfaceChanged");
		try
		{
			// Now that the size is known, set up the camera parameters and
			// begin the preview.
			Camera.Parameters parameters = mCamera.getParameters();

			Size previewSize = getOptimalPreviewSize(parameters.getSupportedPreviewSizes(), w, h);

			parameters.setPreviewSize(previewSize.width, previewSize.height);
			mCamera.setParameters(parameters);
			setCameraDisplayOrientation(0, mCamera);
		}
		catch (Exception e)
		{
			Log.d(mTag, e.getMessage());
		}
		Log.d(mTag, "startPreview");
		try
		{
			mCamera.startPreview();
		}
		catch (Exception e)
		{
			Log.d(mTag, e.getMessage());
		}
	}

	private static void setCameraDisplayOrientation(int cameraId, Camera camera)
	{
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(cameraId, info);

		int degrees = Arvos.getInstance().getRotationDegrees();
		// Log.d(mTag, "RotationDegrees = " + degrees);

		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
		{
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360; // compensate the mirror
		}
		else
		{ // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}
		camera.setDisplayOrientation(result);
	}

	private static Size getOptimalPreviewSize(List<Size> sizes, int w, int h)
	{
		final double ASPECT_TOLERANCE = 0.1;
		double targetRatio = (double) w / h;
		if (sizes == null)
			return null;

		Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;

		int targetHeight = h;

		// Try to find an size match aspect ratio and size
		for (Size size : sizes)
		{
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
				continue;
			if (Math.abs(size.height - targetHeight) < minDiff)
			{
				optimalSize = size;
				minDiff = Math.abs(size.height - targetHeight);
			}
		}

		// Cannot find the one match the aspect ratio, ignore the requirement
		if (optimalSize == null)
		{
			minDiff = Double.MAX_VALUE;
			for (Size size : sizes)
			{
				if (Math.abs(size.height - targetHeight) < minDiff)
				{
					optimalSize = size;
					minDiff = Math.abs(size.height - targetHeight);
				}
			}
		}
		return optimalSize;
	}
}
