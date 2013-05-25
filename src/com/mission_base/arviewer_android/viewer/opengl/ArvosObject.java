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

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;

import com.mission_base.arviewer_android.Arvos;
import com.mission_base.arviewer_android.viewer.utilities.MatrixUtils;

/**
 * An object to be shown in the opengl view.
 * 
 * @author peter
 * 
 */
public class ArvosObject extends ArvosSquare
{
	public static final String BillboardHandlingNone = "none";
	public static final String BillboardHandlingCylinder = "cylinder";
	public static final String BillboardHandlingSphere = "sphere";

	public int mId;
	public String mName;
	public String mTextureUrl;
	public float[] mPosition;
	public float[] mScale;
	public float[] mRotation;
	public String mBillboardHandling;

	public Bitmap mImage;

	public boolean mTextureLoaded = false;

	private Arvos mInstance;

	public ArvosObject(int id)
	{
		mId = id;
		mInstance = Arvos.getInstance();
	}

	/**
	 * Draws the object in the opengl view.
	 */
	public void draw(GL10 gl)
	{
		if (!mTextureLoaded)
		{
			if (mImage != null)
			{
				loadGLTexture(gl, mImage);
			}

			mTextureLoaded = true;
		}

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

		float x = 0f;
		float y = 0f;
		float z = 0f;

		if (mPosition != null && mPosition.length == 3)
		{
			x = mPosition[0];
			y = mPosition[1];
			z = mPosition[2];
		}

		// Move the object
		//
		gl.glTranslatef(x, y, z);

		// Make it face the camera
		//
		if (BillboardHandlingCylinder.equals(mBillboardHandling))
		{
			l3dBillboardCylindricalBegin(gl, 0f, 0f, 0f, x, y, z);
		}
		else if (BillboardHandlingSphere.equals(mBillboardHandling))
		{
			l3dBillboardSphericalBegin(gl, 0f, 0f, 0f, x, y, z);
		}

		if (mRotation != null && mRotation.length == 4)
		{
			gl.glRotatef(mRotation[3], mRotation[0], mRotation[1], mRotation[2]);
		}

		if (mScale != null && mScale.length == 3)
		{
			gl.glScalef(mScale[0], mScale[1], mScale[2]);
		}

		super.draw(gl);
	}

	/**
	 * True billboarding. With the spherical version the object will always face
	 * the camera. It requires more computational effort than the cylindrical
	 * billboard though. The parameters camX,camY, and camZ, are the target,
	 * i.e. a 3D point to which the object will point.
	 * 
	 * @param gl
	 * @param camX
	 * @param camY
	 * @param camZ
	 * @param posX
	 * @param posY
	 * @param posZ
	 */
	protected void l3dBillboardSphericalBegin(GL10 gl, float camX, float camY, float camZ, float posX, float posY, float posZ)
	{
		float[] lookAt = new float[] { 0, 0, 1 };
		float[] objToCamProj = new float[3];
		float[] objToCam = new float[3];
		float[] upAux = new float[3];
		float angleCosine;

		// objToCamProj is the vector in world coordinates from the local origin
		// to the camera
		// projected in the XZ plane
		objToCamProj[0] = camX - posX;
		objToCamProj[1] = 0;
		objToCamProj[2] = camZ - posZ;

		// normalize both vectors to get the cosine directly afterwards
		MatrixUtils.normalize(objToCamProj);

		// easy fix to determine whether the angle is negative or positive
		// for positive angles upAux will be a vector pointing in the
		// positive y direction, otherwise upAux will point downwards
		// effectively reversing the rotation.

		MatrixUtils.cross(lookAt, objToCamProj, upAux);

		// compute the angle
		angleCosine = MatrixUtils.dot(lookAt, objToCamProj);

		// perform the rotation. The if statement is used for stability reasons
		// if the lookAt and v vectors are too close together then |aux| could
		// be bigger than 1 due to lack of precision
		if ((angleCosine < 0.99990) && (angleCosine > -0.9999))
		{
			float f = Arvos.toDegrees((float) (Math.acos(angleCosine)));
			MatrixUtils.normalize(upAux);
			gl.glRotatef(f, upAux[0], upAux[1], upAux[2]);
		}

		// objToCam is the vector in world coordinates from the local origin to
		// the camera
		objToCam[0] = camX - posX;
		objToCam[1] = camY - posY;
		objToCam[2] = camZ - posZ;

		// Normalize to get the cosine afterwards
		MatrixUtils.normalize(objToCam);

		// Compute the angle between v and v2, i.e. compute the
		// required angle for the lookup vector
		angleCosine = MatrixUtils.dot(objToCamProj, objToCam);

		// Tilt the object. The test is done to prevent instability when
		// objToCam and objToCamProj have a very small
		// angle between them
		if ((angleCosine < 0.99990) && (angleCosine > -0.9999))
		{
			if (objToCam[1] < 0)
			{
				float f = Arvos.toDegrees((float) (Math.acos(angleCosine)));
				gl.glRotatef(f, 1, 0, 0);
			}
			else
			{
				float f = Arvos.toDegrees((float) (Math.acos(angleCosine)));
				gl.glRotatef(f, -1, 0, 0);
			}
		}
	}

	/**
	 * The objects motion is restricted to a rotation on a predefined axis The
	 * function bellow does cylindrical billboarding on the Y axis, i.e. the
	 * object will be able to rotate on the Y axis only.
	 * 
	 * @param camX
	 * @param camY
	 * @param camZ
	 * @param posX
	 * @param posY
	 * @param posZ
	 * @param pUpAux
	 * @return
	 */
	public static float l3dBillboardCylindricalDegrees(float camX, float camY, float camZ, float posX, float posY, float posZ, float[] pUpAux)
	{
		float[] lookAt = new float[] { 0, 0, 1 };
		float[] objToCamProj = new float[3];
		float[] upAux = pUpAux;
		if (upAux == null)
		{
			upAux = new float[3];
		}
		float angleCosine;

		// objToCamProj is the vector in world coordinates from the local origin
		// to the camera
		// projected in the XZ plane
		objToCamProj[0] = camX - posX;
		objToCamProj[1] = 0;
		objToCamProj[2] = camZ - posZ;

		// normalize both vectors to get the cosine directly afterwards
		MatrixUtils.normalize(objToCamProj);

		// easy fix to determine whether the angle is negative or positive
		// for positive angles upAux will be a vector pointing in the
		// positive y direction, otherwise upAux will point downwards
		// effectively reversing the rotation.

		MatrixUtils.cross(lookAt, objToCamProj, upAux);

		// compute the angle
		angleCosine = MatrixUtils.dot(lookAt, objToCamProj);

		// perform the rotation. The if statement is used for stability reasons
		// if the lookAt and v vectors are too close together then |aux| could
		// be bigger than 1 due to lack of precision
		if ((angleCosine < 0.99990) && (angleCosine > -0.9999))
		{
			return Arvos.toDegrees((float) (Math.acos(angleCosine)));
		}
		return Float.NaN;
	}

	/**
	 * Cylindrical billboarding.
	 * 
	 * @param gl
	 * @param camX
	 * @param camY
	 * @param camZ
	 * @param posX
	 * @param posY
	 * @param posZ
	 */
	public static void l3dBillboardCylindricalBegin(GL10 gl, float camX, float camY, float camZ, float posX, float posY, float posZ)
	{
		float[] upAux = new float[3];
		float f = l3dBillboardCylindricalDegrees(camX, camY, camZ, posX, posY, posZ, upAux);
		if (!Float.isNaN(f))
		{
			gl.glRotatef(f, upAux[0], upAux[1], upAux[2]);
		}
	}
}
