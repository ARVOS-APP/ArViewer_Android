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

import java.util.Map.Entry;

import android.opengl.Matrix;

import com.mission_base.arviewer_android.Arvos;
import com.mission_base.arviewer_android.viewer.utilities.Ray;
import com.mission_base.arviewer_android.viewer.utilities.Triangle;
import com.mission_base.arviewer_android.viewer.utilities.Vector;

/**
 * Handles touches in the opengl view.
 * <p>
 * Finds out which object was clicked if any.
 * 
 * @author peter
 * 
 */
public class ArvosTouchHandler extends Thread
{
	private static String Lock = "Lock";

	private Arvos mInstance;
	private float mX;
	private float mY;

	public ArvosTouchHandler(float x, float y)
	{
		super("ArvosTouchHandler");

		mInstance = Arvos.getInstance();
		mX = x;
		mY = y;
	}

	public void run()
	{
		if (!mInstance.mHandleTouch)
		{
			return;
		}

		synchronized (Lock)
		{
			if (!mInstance.mHandleTouch)
			{
				return;
			}
			handleTouch(mX, mY);
			mInstance.mHandleTouch = false;
			mInstance.mModelViewMatrixesRequested = false;
		}
	}

	private void handleTouch(float x, float y)
	{
		if (!mInstance.mHandleTouch)
		{
			return;
		}

		// let the renderer provide the model view matrixes of all objects
		//
		mInstance.mModelViewMatrixesRequested = true;
		while (mInstance.mModelViewMatrixesRequested)
		{
			try
			{
				Thread.sleep(18);
			}
			catch (InterruptedException e)
			{
				return;
			}
			if (!mInstance.mHandleTouch)
			{
				return;
			}
		}
		if (!mInstance.mHandleTouch)
		{
			return;
		}

		Triangle triangle = new Triangle(new float[3], new float[3], new float[3]);

		float[] intersection = new float[3];
		float minLength = Float.MAX_VALUE;
		int id = 0;
		boolean found = false;

		float[] convertedSquare = new float[ArvosSquare.vertices.length];
		float[] resultVector = new float[4];
		float[] inputVector = new float[4];

		float[] projection = mInstance.mProjectionMatrix;
		if (projection == null)
		{
			return;
		}

		synchronized (mInstance.mModelViewMatrixes)
		{
			for (Entry<Integer, float[]> entry : mInstance.mModelViewMatrixes.entrySet())
			{
				if (!mInstance.mHandleTouch)
				{
					return;
				}

				float[] modelView = entry.getValue();

				Ray ray = new Ray(modelView, projection, mInstance.mWidth, mInstance.mHeight, x, y);

				for (int i = 0; i < ArvosSquare.vertices.length; i = i + 3)
				{
					inputVector[0] = ArvosSquare.vertices[i];
					inputVector[1] = ArvosSquare.vertices[i + 1];
					inputVector[2] = ArvosSquare.vertices[i + 2];
					inputVector[3] = 1;

					Matrix.multiplyMV(resultVector, 0, modelView, 0, inputVector, 0);
					convertedSquare[i] = resultVector[0] / resultVector[3];
					convertedSquare[i + 1] = resultVector[1] / resultVector[3];
					convertedSquare[i + 2] = resultVector[2] / resultVector[3];
				}

				triangle.V0[0] = convertedSquare[0];
				triangle.V0[1] = convertedSquare[1];
				triangle.V0[2] = convertedSquare[2];
				triangle.V1[0] = convertedSquare[3];
				triangle.V1[1] = convertedSquare[4];
				triangle.V1[2] = convertedSquare[5];
				triangle.V2[0] = convertedSquare[6];
				triangle.V2[1] = convertedSquare[7];
				triangle.V2[2] = convertedSquare[8];

				if (Triangle.intersectRayAndTriangle(ray, triangle, intersection) == 1)
				{
					found = true;
					float length = Vector.length(intersection);
					if (length < minLength)
					{
						minLength = length;
						id = entry.getKey();
					}
					continue;
				}

				triangle.V0[0] = convertedSquare[9];
				triangle.V0[1] = convertedSquare[10];
				triangle.V0[2] = convertedSquare[11];

				if (Triangle.intersectRayAndTriangle(ray, triangle, intersection) == 1)
				{
					found = true;
					float length = Vector.length(intersection);
					if (length < minLength)
					{
						minLength = length;
						id = entry.getKey();
					}
				}
			}
			mInstance.mModelViewMatrixes.clear();
		}

		if (found)
		{
			// Arvos.log("touch", " " + id);
			mInstance.mAugment.addClick(id);
		}
	}
}
