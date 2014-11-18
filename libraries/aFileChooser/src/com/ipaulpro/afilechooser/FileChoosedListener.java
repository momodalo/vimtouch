package com.ipaulpro.afilechooser;

import com.ipaulpro.afilechooser.utils.FileInfo;

import java.io.File;

public interface FileChoosedListener {
	public void onFileSelected(FileInfo file);
}
