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

import java.util.ArrayList;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.mission_base.arviewer_android.viewer.ArvosAugment;

/**
 * Adapter used for displaying the list of augments.
 * 
 * @author peter
 * 
 */
public class ArvosCustomArrayAdapter extends ArrayAdapter<ArvosAugment>
{
	private final Activity context;
	private final ArrayList<ArvosAugment> augments;
	private final int imageId;

	public ArvosCustomArrayAdapter(Activity context, ArrayList<ArvosAugment> augments, int imageId)
	{
		super(context, R.layout.lvrowlayout, augments);
		this.context = context;
		this.augments = augments;
		this.imageId = imageId;
	}

	static class ViewContainer
	{
		public ImageView imageView;
		public TextView txtTitle;
		public TextView txtLine;
		public TextView txtDescription;
	}

	@Override
	public View getView(int position, View view, ViewGroup parent)
	{
		ViewContainer viewContainer;
		View rowView = view;

		// ---print the index of the row to examine---
		// Log.d("CustomArrayAdapter",String.valueOf(position));

		// ---if the row is displayed for the first time---
		if (rowView == null)
		{
			// Log.d("CustomArrayAdapter", "New");
			LayoutInflater inflater = context.getLayoutInflater();
			rowView = inflater.inflate(R.layout.lvrowlayout, null, true);

			// ---create a view container object---
			viewContainer = new ViewContainer();

			// ---get the references to all the views in the row---
			viewContainer.txtTitle = (TextView) rowView.findViewById(R.id.txtAugmentName);
			viewContainer.txtLine = (TextView) rowView.findViewById(R.id.txtLine);
			viewContainer.txtDescription = (TextView) rowView.findViewById(R.id.txtDescription);
			viewContainer.imageView = (ImageView) rowView.findViewById(R.id.icon);

			// ---assign the view container to the rowView---
			rowView.setTag(viewContainer);
		}
		else
		{
			// ---view was previously created; can recycle---
			// Log.d("CustomArrayAdapter", "Recycling");
			// ---retrieve the previously assigned tag to get
			// a reference to all the views; bypass the findViewByID() process,
			// which is computationally expensive---
			viewContainer = (ViewContainer) rowView.getTag();
		}

		// ---customize the content of each row based on position---
		viewContainer.txtTitle.setText(augments.get(position).mName);
		viewContainer.txtLine.setText(augments.get(position).mAuthor);
		viewContainer.txtDescription.setText(augments.get(position).mDescription);
		viewContainer.imageView.setImageResource(imageId);
		return rowView;
	}
}
