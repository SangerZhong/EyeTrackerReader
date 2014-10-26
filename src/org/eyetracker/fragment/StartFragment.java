package org.eyetracker.fragment;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eyetracker.tool.FileTool;
import org.opencv.samples.facedetect.R;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class StartFragment extends Fragment {

    private List<Map<String, Object>> fileList;
    private TextView tvDirectory;
    private OnArticleSelectedListener mListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
	    Bundle savedInstanceState) {
	return inflater.inflate(R.layout.fragment_start, container, false);
    }

    @Override
    public void onStart() {
	super.onStart();
	tvDirectory = (TextView) getActivity().findViewById(R.id.tv_directory);
	if (Environment.getExternalStorageState().equals(
		Environment.MEDIA_MOUNTED)) {
	    File root = new File(FileTool.getExtSDCardPaths().get(0));
	    tvDirectory.setText(root.getPath());
	    fileList = getMapData(root);
	    final SimpleAdapter adapter = new SimpleAdapter(getActivity(),
		    fileList, R.layout.fragment_start_item, new String[] {
			    "FileName", "Feature", "Attribute" }, new int[] {
			    R.id.tv_item_filename, R.id.tv_item_feature,
			    R.id.tv_item_attribute });
	    ListView mainList = (ListView) getActivity().findViewById(
		    R.id.lv_file_choose);
	    mainList.setAdapter(adapter);
	    mainList.setOnItemClickListener(new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view,
			int position, long id) {
		    Map<String, Object> map = fileList.get(position);
		    File file = new File((position == 0) ? map.get("Feature")
			    .toString() : tvDirectory.getText() + "/"
			    + map.get("FileName").toString());
		    if (file.isDirectory()) {
			tvDirectory.setText(file.getPath());
			fileList.clear();
			fileList.addAll(getMapData(file));
			adapter.notifyDataSetChanged();
		    } else {
			mListener.onArticleSelected(file);
		    }
		}
	    });
	} else {
	    Toast.makeText(getActivity(), "no sdcard!", Toast.LENGTH_SHORT)
		    .show();
	}
    }

    private List<Map<String, Object>> getMapData(File root) {
	File[] files = root.listFiles();
	ArrayList<Map<String, Object>> dir = new ArrayList<Map<String, Object>>();
	ArrayList<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
	Map<String, Object> firstItem = new HashMap<String, Object>();
	firstItem.put("FileName", "上一级");
	firstItem.put("Feature", root.getParentFile().getPath());
	firstItem.put("Attribute", "Dir");
	dir.add(firstItem);
	for (File f : files) {
	    Map<String, Object> item = new HashMap<String, Object>();
	    StringBuilder sb = new StringBuilder();
	    sb.append(FileTool.getFileLastModifiedTime(f) + " ");
	    String path = f.toString();
	    item.put("FileName",
		    path.substring(path.lastIndexOf("/") + 1, path.length()));
	    if (f.isDirectory()) {
		sb.append("文件夹");
		item.put("Attribute", "Dir");
		dir.add(item);
	    } else {
		sb.append(f.length() / 1024 + "kb");
		item.put("Attribute", "File");
		data.add(item);
	    }
	    item.put("Feature", sb.toString());
	}
	dir.addAll(data);
	return dir;
    }

    @Override
    public void onAttach(Activity activity) {
	super.onAttach(activity);
	try {
	    mListener = (OnArticleSelectedListener) activity;
	} catch (ClassCastException e) {
	    throw new ClassCastException(activity.toString()
		    + "must implement OnArticleSelectedListener");
	}
    }

    public interface OnArticleSelectedListener {
	public void onArticleSelected(File file);
    }
}
