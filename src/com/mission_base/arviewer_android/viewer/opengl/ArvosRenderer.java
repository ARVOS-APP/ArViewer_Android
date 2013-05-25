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

package com.mission_base.arviewer_android.viewer.opengl;

import java.util.LinkedList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLU;

import com.mission_base.arviewer_android.Arvos;
import com.mission_base.arviewer_android.viewer.utilities.MatrixGrabber;
import com.mission_base.arviewer_android.viewer.utilities.MatrixTrackingGL;

/**
 * Opengl renderer.
 * 
 * @author peter
 * 
 */
public class ArvosRenderer implements Renderer
{
	private Arvos mInstance;

	public ArvosRenderer()
	{
		mInstance = Arvos.getInstance();
	}

	private long mSecond = System.currentTimeMillis() / 1000;
	private long mFPS = 0;

	private int counter = 0;

	@Override
	public void onDrawFrame(GL10 gl)
	{
		long second = System.currentTimeMillis() / 1000;
		if (mSecond != second)
		{
			mInstance.mFPS = mFPS;
			mFPS = 1L;
			mSecond = second;
		}
		else
		{
			mFPS++;
		}

		long now = System.currentTimeMillis();

		List<ArvosObject> arvosObjects;
		synchronized (mInstance.mArvosObjects)
		{
			arvosObjects = new LinkedList<ArvosObject>(mInstance.mArvosObjects);
		}

		List<ArvosObject> newObjects = mInstance.mAugment.getObjects(now, arvosObjects);

		synchronized (mInstance.mArvosObjects)
		{
			mInstance.mArvosObjects.clear();
			mInstance.mArvosObjects.addAll(newObjects);

			MatrixTrackingGL mgl = null;

			if (mInstance.mModelViewMatrixesRequested)
			{
				gl = mgl = new MatrixTrackingGL(gl);
			}

			gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

			synchronized (mInstance.mModelViewMatrixes)
			{
				for (ArvosObject arvosObject : mInstance.mArvosObjects)
				{
					gl.glLoadIdentity();
					arvosObject.draw(gl);

					if (mgl != null)
					{
						MatrixGrabber matrixGrabber = new MatrixGrabber();
						matrixGrabber.getCurrentState(mgl);

						if (mInstance.mModelViewMatrixesRequested)
						{
							mInstance.mModelViewMatrixes.put(Integer.valueOf(arvosObject.mId), matrixGrabber.mModelView);
						}
					}
				}
			}
			mInstance.mModelViewMatrixesRequested = false;
		}

		if (++counter % 10 == 0)
		{
			getCorrectedAzimuth(new MatrixTrackingGL(gl));
			counter = 0;
		}
	}

	private void getCorrectedAzimuth(MatrixTrackingGL gl)
	{
		gl.glLoadIdentity();

		// Take the device orientation into account
		//
		gl.glRotatef(mInstance.getRotationDegrees(), 0f, 0f, 1f);

		// The device coordinates are flat on the table with X east, Y north and
		// Z up.
		// The world coordinates are X east, Y up and Z north
		//
		gl.glRotatef(90, 1f, 0f, 0f);

		// Apply azimut, pitch and roll of the device
		//
		gl.glRotatef(mInstance.mRoll, 0f, 0f, 1f);
		gl.glRotatef(mInstance.mPitch, 1f, 0f, 0f);
		gl.glRotatef(mInstance.mAzimuth, 0f, 1f, 0f);

		MatrixGrabber matrixGrabber = new MatrixGrabber();
		matrixGrabber.getCurrentState(gl);

		float[] modelViewMatrix = matrixGrabber.mModelView;

		float f = ArvosObject.l3dBillboardCylindricalDegrees(0, 0, 0, modelViewMatrix[2], 0, -modelViewMatrix[10], null);
		if (!Float.isNaN(f))
		{
			int sign = 1;
			if (modelViewMatrix[2] > 0)
			{
				sign = -1;
			}
			mInstance.mCorrectedAzimuth = sign * f;
		}
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height)
	{
		mInstance.mHandleTouch = false;
		mInstance.mModelViewMatrixesRequested = false;

		MatrixTrackingGL mgl = new MatrixTrackingGL(gl);
		if (height == 0)
		{
			height = 1;
		}

		mInstance.mHeight = height;
		mInstance.mWidth = width;

		synchronized (mInstance.mArvosObjects)
		{
			for (ArvosObject arvosObject : mInstance.mArvosObjects)
			{
				arvosObject.mTextureLoaded = false;
			}
		}

		mgl.glViewport(0, 0, width, height); // Reset The Current Viewport
		mgl.glMatrixMode(GL10.GL_PROJECTION); // Select The Projection Matrix
		mgl.glLoadIdentity(); // Reset The Projection Matrix

		// Calculate The Aspect Ratio Of The Window
		GLU.gluPerspective(mgl, 45.0f, (float) width / (float) height, 0.1f, 200.0f);

		MatrixGrabber matrixGrabber = new MatrixGrabber();
		matrixGrabber.getCurrentState(mgl);
		mInstance.mProjectionMatrix = matrixGrabber.mProjection;

		mgl.glMatrixMode(GL10.GL_MODELVIEW); // Select The Modelview Matrix
		mgl.glLoadIdentity(); // Reset The Modelview Matrix
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config)
	{
		gl.glEnable(GL10.GL_TEXTURE_2D); // Enable mTexture Mapping ( NEW )
		gl.glShadeModel(GL10.GL_SMOOTH); // Enable Smooth Shading

		gl.glClearDepthf(1.0f); // Depth Buffer Setup
		gl.glEnable(GL10.GL_DEPTH_TEST); // Enables Depth Testing
		gl.glDepthFunc(GL10.GL_LEQUAL); // The Type Of Depth Testing To Do

		// Really Nice Perspective Calculations
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);

		gl.glClearColor(0, 0, 0, 0);

		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnable(GL10.GL_BLEND);
	}
}
