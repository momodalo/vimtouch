package com.ipaulpro.afilechooser.utils;

import java.io.File;

/**
 * Created by vorobyev on 11/18/14.
 */
public class FileInfo {

    private File file = null;
    private String title = null;

    public FileInfo(File file, String title) {
        this.file = file;
        this.title = title;
    }

    public File getFile() {
        return file;
    }

    public String getTitle() {
        return title;
    }
}
