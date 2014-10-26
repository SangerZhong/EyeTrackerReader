package org.eyetracker.fragment;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.opencv.samples.facedetect.FdActivity;
import org.opencv.samples.facedetect.R;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ReadFragment extends Fragment {

    private TextView mTvReader;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
	    Bundle savedInstanceState) {
	return inflater.inflate(R.layout.fragment_read, container, false);
    }

    @Override
    public void onStart() {
	super.onStart();
	File file = ((FdActivity) getActivity()).getFile();
	mTvReader = (TextView) getActivity().findViewById(R.id.tv_reader);
	String textString = getStringFromFile(file);
	if (textString != null) {
	    mTvReader.setText(textString);
	} else {
	    mTvReader.setText("file error!!");
	}
    }

    private String getStringFromFile(File file) {
	StringBuffer sb = new StringBuffer();
	try {
	    FileInputStream fis = new FileInputStream(file);
	    BufferedInputStream bin = new BufferedInputStream(fis);
	    BufferedReader br = new BufferedReader(new InputStreamReader(bin,
		    "GBK"));
	    while (br.ready()) {
		sb.append(br.readLine()).append("\n");
	    }
	    br.close();
	    return sb.toString();
	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	} catch (UnsupportedEncodingException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}
	return null;
    }
}
