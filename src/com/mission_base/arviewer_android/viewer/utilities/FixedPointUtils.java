/*
 * Taken from android-gl, Making OpenGL Programming in Android Easier
 * URL: https://code.google.com/p/android-gl/source/browse/trunk/AndroidGL/src/edu/union/
 * 
 * License: GNU Lesser General Public License
 * 
 *   This library is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU Lesser General Public
 *   License as published by the Free Software Foundation; either
 *   version 2.1 of the License, or (at your option) any later version.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *   Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 *   License along with this library; if not, write to the Free Software
 *   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.mission_base.arviewer_android.viewer.utilities;

//Much of this is adapted from the beartronics FP lib
public class FixedPointUtils
{
	public static final int ONE = 0x10000;

	/**
	 * Convert a float to 16.16 fixed-point representation
	 * 
	 * @param val
	 *            The value to convert
	 * @return The resulting fixed-point representation
	 */
	public static int toFixed(float val)
	{
		return (int) (val * 65536F);
	}

	/**
	 * Convert an array of floats to 16.16 fixed-point
	 * 
	 * @param arr
	 *            The array
	 * @return A newly allocated array of fixed-point values.
	 */
	public static int[] toFixed(float[] arr)
	{
		int[] res = new int[arr.length];
		toFixed(arr, res);
		return res;
	}

	/**
	 * Convert an array of floats to 16.16 fixed-point
	 * 
	 * @param arr
	 *            The array of floats
	 * @param storage
	 *            The location to store the fixed-point values.
	 */
	public static void toFixed(float[] arr, int[] storage)
	{
		for (int i = 0; i < storage.length; i++)
		{
			storage[i] = toFixed(arr[i]);
		}
	}

	/**
	 * Convert a 16.16 fixed-point value to floating point
	 * 
	 * @param val
	 *            The fixed-point value
	 * @return The equivalent floating-point value.
	 */
	public static float toFloat(int val)
	{
		return ((float) val) / 65536.0f;
	}

	/**
	 * Convert an array of 16.16 fixed-point values to floating point
	 * 
	 * @param arr
	 *            The array to convert
	 * @return A newly allocated array of floats.
	 */
	public static float[] toFloat(int[] arr)
	{
		float[] res = new float[arr.length];
		toFloat(arr, res);
		return res;
	}

	/**
	 * Convert an array of 16.16 fixed-point values to floating point
	 * 
	 * @param arr
	 *            The array to convert
	 * @param storage
	 *            Pre-allocated storage for the result.
	 */
	public static void toFloat(int[] arr, float[] storage)
	{
		for (int i = 0; i < storage.length; i++)
		{
			storage[i] = toFloat(arr[i]);
		}
	}

	/**
	 * Multiply two fixed-point values.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public static int multiply(int x, int y)
	{
		long z = (long) x * (long) y;
		return ((int) (z >> 16));
	}

	/**
	 * Divide two fixed-point values.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public static int divide(int x, int y)
	{
		long z = (((long) x) << 32);
		return (int) ((z / y) >> 16);
	}

	/**
	 * Find the sqrt of a fixed-point value.
	 * 
	 * @param n
	 * @return
	 */
	public static int sqrt(int n)
	{
		int s = (n + 65536) >> 1;
		for (int i = 0; i < 8; i++)
		{
			// converge six times
			s = (s + divide(n, s)) >> 1;
		}
		return s;
	}
}
