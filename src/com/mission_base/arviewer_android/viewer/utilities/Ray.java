/*
 * Copyright (C) 2012 Gregory Beauchamp
 * 
 * Derived from 
 * http://android-raypick.blogspot.com/2012/04/first-i-want-to-state-this-is-my-first.html
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mission_base.arviewer_android.viewer.utilities;

import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLU;
import android.opengl.Matrix;

public class Ray
{
	public float[] P0;
	public float[] P1;

	public Ray(float[] modelView, float[] projection, int width, int height, float xTouch, float yTouch)
	{
		int[] viewport = { 0, 0, width, height };

		float[] nearCoOrds = new float[3];
		float[] farCoOrds = new float[3];
		float[] temp = new float[4];
		float[] temp2 = new float[4];

		// get the near and far cords for the click

		float winx = xTouch, winy = (float) viewport[3] - yTouch;

		// Log.d(TAG, "modelView is =" +
		// Arrays.toString(matrixGrabber.mModelView));
		// Log.d(TAG, "projection view is =" + Arrays.toString(
		// matrixGrabber.mProjection ));

		int result = GLU.gluUnProject(winx, winy, 1.0f, modelView, 0, projection, 0, viewport, 0, temp, 0);

		Matrix.multiplyMV(temp2, 0, modelView, 0, temp, 0);
		if (result == GL10.GL_TRUE)
		{
			nearCoOrds[0] = temp2[0] / temp2[3];
			nearCoOrds[1] = temp2[1] / temp2[3];
			nearCoOrds[2] = temp2[2] / temp2[3];
		}

		result = GLU.gluUnProject(winx, winy, 0, modelView, 0, projection, 0, viewport, 0, temp, 0);
		Matrix.multiplyMV(temp2, 0, modelView, 0, temp, 0);
		if (result == GL10.GL_TRUE)
		{
			farCoOrds[0] = temp2[0] / temp2[3];
			farCoOrds[1] = temp2[1] / temp2[3];
			farCoOrds[2] = temp2[2] / temp2[3];
		}
		this.P0 = farCoOrds;
		this.P1 = nearCoOrds;
	}
}
