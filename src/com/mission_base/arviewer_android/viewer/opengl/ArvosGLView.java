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

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

import com.mission_base.arviewer_android.Arvos;

/**
 * The opengl view showing the pois.
 * 
 * @author peter
 * 
 */
public class ArvosGLView extends GLSurfaceView
{
	private Arvos mInstance;

	public ArvosGLView(Context context)
	{
		super(context);

		mInstance = Arvos.getInstance();
		setZOrderMediaOverlay(true);
		setEGLConfigChooser(8, 8, 8, 8, 16, 0);
		setRenderer(new ArvosRenderer());
		getHolder().setFormat(PixelFormat.TRANSLUCENT);
	}

	@Override
	public boolean onTouchEvent(MotionEvent e)
	{
		float x = e.getX();
		float y = e.getY();

		int action = e.getAction();
		if (action != MotionEvent.ACTION_DOWN)
		{
			return true;
		}

		mInstance.mHandleTouch = true;
		ArvosTouchHandler arvosTouchHandler = new ArvosTouchHandler(x, y);
		arvosTouchHandler.start();

		return true;
	}

}
