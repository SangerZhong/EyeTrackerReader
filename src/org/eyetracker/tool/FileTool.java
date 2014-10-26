package org.eyetracker.tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.os.Environment;

public class FileTool {

    public static String getFileLastModifiedTime(File f) {
	Calendar cal = Calendar.getInstance();
	long time = f.lastModified();
	SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	cal.setTimeInMillis(time);
	return formatter.format(cal.getTime());
    }

    public static List<String> getExtSDCardPaths() {
	List<String> paths = new ArrayList<String>();
	String extFileStatus = Environment.getExternalStorageState();
	File extFile = Environment.getExternalStorageDirectory();
	if (extFileStatus.endsWith(Environment.MEDIA_UNMOUNTED)
		&& extFile.exists() && extFile.isDirectory()
		&& extFile.canWrite()) {
	    paths.add(extFile.getAbsolutePath());
	}
	try {
	    // obtain executed result of command line code of 'mount', to judge
	    // whether tfCard exists by the result
	    Runtime runtime = Runtime.getRuntime();
	    Process process = runtime.exec("mount");
	    InputStream is = process.getInputStream();
	    InputStreamReader isr = new InputStreamReader(is);
	    BufferedReader br = new BufferedReader(isr);
	    String line = null;
	    int mountPathIndex = 1;
	    while ((line = br.readLine()) != null) {
		// format of sdcard file system: vfat/fuse
		if ((!line.contains("fat") && !line.contains("fuse") && !line
			.contains("storage"))
			|| line.contains("secure")
			|| line.contains("asec")
			|| line.contains("firmware")
			|| line.contains("shell")
			|| line.contains("obb")
			|| line.contains("legacy") || line.contains("data")) {
		    continue;
		}
		String[] parts = line.split(" ");
		int length = parts.length;
		if (mountPathIndex >= length) {
		    continue;
		}
		String mountPath = parts[mountPathIndex];
		if (!mountPath.contains("/") || mountPath.contains("data")
			|| mountPath.contains("Data")) {
		    continue;
		}
		File mountRoot = new File(mountPath);
		if (!mountRoot.exists() || !mountRoot.isDirectory()
			|| !mountRoot.canWrite()) {
		    continue;
		}
		boolean equalsToPrimarySD = mountPath.equals(extFile
			.getAbsolutePath());
		if (equalsToPrimarySD) {
		    continue;
		}
		paths.add(mountPath);
	    }
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	return paths;
    }

}
