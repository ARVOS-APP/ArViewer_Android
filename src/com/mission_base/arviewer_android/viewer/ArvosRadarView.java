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
   
   The mOrientation code is derived from
    
     package com.epichorns.compass3D;
   
   found on stackoverflow, see answer to
   
     http://stackoverflow.com/questions/10192057/android-getorientation-method-returns-bad-results
     
   by user epichorns, see
   
     http://stackoverflow.com/users/1350375/epichorns
 */

package com.mission_base.arviewer_android.viewer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.View;

import com.mission_base.arviewer_android.Arvos;
import com.mission_base.arviewer_android.viewer.opengl.ArvosObject;

/**
 * The view showing the radar heads up display.
 * <p>
 * Uses the orientation sensors of the device and updates the orientation values
 * of the instance.
 * 
 * @author peter
 * 
 */
public class ArvosRadarView extends View
{
	private Arvos mInstance;
	private String mTag = "ArvosRadarView";
	private Paint mPaint = new Paint();

	private float mAngleFilteredAzimuth = 0;
	private float mAngleFilteredPitch = 0;
	private float mAngleFilteredRoll = 0;

	private SensorManager mSensorManager;

	// sensor calculation values
	private float[] mGravity = null;
	private float[] mGeomagnetic = null;
	private float Rmat[] = new float[9];
	private float Imat[] = new float[9];
	private float mOrientation[] = new float[3];

	/**
	 * Creates the view.
	 * 
	 * @param context
	 *            The application context.
	 */
	public ArvosRadarView(Context context)
	{
		super(context);
		Log.d(mTag, "ArvosRadarView()");

		mInstance = Arvos.getInstance();
		mPaint.setColor(0xff0000ff);
		mPaint.setStyle(Style.STROKE);
		mPaint.setAntiAlias(true);

		mSensorManager = (SensorManager) mInstance.getActivity().getSystemService(Context.SENSOR_SERVICE);
		mSensorManager.registerListener(mAccelerometerListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
		mSensorManager.registerListener(mMagnetometerListener, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_UI);
		update();
	}

	SensorEventListener mAccelerometerListener = new SensorEventListener()
	{
		public void onAccuracyChanged(Sensor sensor, int accuracy)
		{
		}

		public void onSensorChanged(SensorEvent event)
		{
			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
			{
				mGravity = event.values.clone();
				processSensorData();
				update();
			}
		}
	};
	SensorEventListener mMagnetometerListener = new SensorEventListener()
	{
		public void onAccuracyChanged(Sensor sensor, int accuracy)
		{
		}

		public void onSensorChanged(SensorEvent event)
		{
			if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
			{
				mGeomagnetic = event.values.clone();
				processSensorData();
				update();
			}
		}
	};

	private float restrictAngle(float angle)
	{
		while (angle >= 180)
		{
			angle -= 360;
		}
		while (angle < -180)
		{
			angle += 360;
		}
		return angle;
	}

	// x is a raw angle value from getOrientation(...)
	// y is the current filtered angle value
	private float calculateFilteredAngle(float x, float y)
	{
		final float alpha = 0.3f;
		float diff = x - y;

		// here, we ensure that abs(diff)<=180
		diff = restrictAngle(diff);

		// ensure that y stays within [-180, 180[ bounds
		y = restrictAngle(y + alpha * diff);

		return y;
	}

	private void processSensorData()
	{
		if (mGravity != null && mGeomagnetic != null)
		{
			boolean success = SensorManager.getRotationMatrix(Rmat, Imat, mGravity, mGeomagnetic);
			if (success)
			{
				SensorManager.getOrientation(Rmat, mOrientation);
				mAngleFilteredAzimuth = calculateFilteredAngle(Arvos.toDegrees(mOrientation[0]), mAngleFilteredAzimuth);
				mAngleFilteredPitch = calculateFilteredAngle(Arvos.toDegrees(mOrientation[1]), mAngleFilteredPitch);
				mAngleFilteredRoll = calculateFilteredAngle(Arvos.toDegrees(mOrientation[2]), mAngleFilteredRoll);
			}
			mGravity = null; // full new refresh
			mGeomagnetic = null;
		}
	}

	private void update()
	{
		mInstance.mAzimuth = mAngleFilteredAzimuth;
		mInstance.mPitch = mAngleFilteredPitch;
		mInstance.mRoll = mAngleFilteredRoll;

		this.invalidate();
		ArvosTextView textView = ((ArvosViewer) (mInstance.getActivity())).mTextView;
		if (textView != null)
		{
			textView.updateWithNewLocation();
		}
	}

	/**
	 * Draws the radar heads up display
	 */
	protected void onDraw(Canvas canvas)
	{
		int width = getWidth();
		int centerX = width - 100;
		int centerY = 100;

		float degrees = mInstance.mCorrectedAzimuth;

		mPaint.setColor(0xffff0000);
		mPaint.setStrokeWidth(2);
		synchronized (mInstance.mArvosObjects)
		{
			for (ArvosObject arvosObject : mInstance.mArvosObjects)
			{
				int x = Math.round(arvosObject.mPosition[0]);
				int y = Math.round(arvosObject.mPosition[2]);

				double distance = Math.sqrt(x * x + y * y);
				if (distance < 99)
				{
					x += centerX;
					y += centerY;
					canvas.drawLine(x - 3, y - 3, x + 3, y + 3, mPaint);
					canvas.drawLine(x + 3, y - 3, x - 3, y + 3, mPaint);
				}
			}
		}
		mPaint.setColor(0xff0000ff);

		canvas.rotate(degrees, centerX, centerY);

		mPaint.setStrokeWidth(4);
		canvas.drawLine(centerX, centerY, centerX, 0, mPaint);
		mPaint.setStrokeWidth(8);
		canvas.drawCircle(centerX, centerY, 50, mPaint);
	}

	public void onResume()
	{
		mSensorManager.registerListener(mAccelerometerListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
		mSensorManager.registerListener(mMagnetometerListener, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_UI);

		mAngleFilteredAzimuth = 0;
		mAngleFilteredPitch = 0;
		mAngleFilteredRoll = 0;

		update();
	}

	public void onPause()
	{
		mSensorManager.unregisterListener(mAccelerometerListener);
		mSensorManager.unregisterListener(mMagnetometerListener);
	}
}
