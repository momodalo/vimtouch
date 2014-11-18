/* 
 * Copyright (C) 2012 Paul Burke
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

package com.ipaulpro.afilechooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ipaulpro.afilechooser.utils.FileInfo;

/**
 * List adapter for Files.
 * 
 * @version 2012-10-28
 * 
 * @author paulburke (ipaulpro)
 * 
 */
public class FileListAdapter extends BaseAdapter {

	private final static int ICON_FOLDER = R.drawable.ic_folder;
	private final static int ICON_FILE = R.drawable.ic_file;

	private List<FileInfo> mFiles = new ArrayList<FileInfo>();
	private LayoutInflater mInflater;

	public FileListAdapter(Context context) {
		mInflater = LayoutInflater.from(context);
	}

	public List<FileInfo> getListItems() {
		return mFiles;
	}

	public void setListItems(List<FileInfo> files) {
		this.mFiles = files;
		notifyDataSetChanged();
	}

	public int getCount() {
		return mFiles.size();
	}

	public void clear() {
		mFiles.clear();
	}

	public FileInfo getItem(int position) {
		return mFiles.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		ViewHolder holder = null;

		if (row == null) {
			row = mInflater.inflate(R.layout.file, parent, false);
			holder = new ViewHolder(row);
			row.setTag(holder);
		} else {
			// Reduce, reuse, recycle!
			holder = (ViewHolder) row.getTag();
		}

		// Get the file at the current position
		final FileInfo file = (FileInfo) getItem(position);

		// Set the TextView as the file name
		holder.nameView.setText(file.getTitle());

		// If the item is not a directory, use the file icon
		holder.iconView.setImageResource(file.getFile().isDirectory() ? ICON_FOLDER
				: ICON_FILE);

		return row;
	}

	static class ViewHolder {
		TextView nameView;
		ImageView iconView;

		ViewHolder(View row) {
			nameView = (TextView) row.findViewById(R.id.file_name);
			iconView = (ImageView) row.findViewById(R.id.file_icon);
		}
	}
}