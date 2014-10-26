package org.opencv.samples.facedetect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eyetracker.fragment.ReadFragment;
import org.eyetracker.fragment.StartFragment;
import org.eyetracker.fragment.StartFragment.OnArticleSelectedListener;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ScrollView;
import android.widget.TextView;

public class FdActivity extends Activity implements CvCameraViewListener2,
		OnArticleSelectedListener {

	private static final String TAG = "OCVSample::Activity";
	// private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
	public static final int JAVA_DETECTOR = 0;
	private static final int TM_SQDIFF = 0;
	private static final int TM_SQDIFF_NORMED = 1;
	private static final int TM_CCOEFF = 2;
	private static final int TM_CCOEFF_NORMED = 3;
	private static final int TM_CCORR = 4;
	private static final int TM_CCORR_NORMED = 5;

	private int learn_frames = 0;
	private Mat teplateR; // 右眼区域（矩阵）
	private Mat teplateL; // 左眼区域（矩阵）
	private int method = 0;

	private MenuItem mItemFace50;
	private MenuItem mItemFace40;
	private MenuItem mItemFace30;
	private MenuItem mItemFace20;
	private MenuItem mItemType;

	private Mat mRgba; // rgb图像
	private Mat mGray; // 灰度图
	// // matrix for zooming
	// private Mat mZoomWindow;
	// private Mat mZoomWindow2;

	private File mCascadeFile;
	private CascadeClassifier mJavaDetector; // 人脸筛分机
	private CascadeClassifier mJavaDetectorEye; // 人眼筛分机

	private int mDetectorType = JAVA_DETECTOR;
	private String[] mDetectorName;

	private float mRelativeFaceSize = 0.2f;
	private int mAbsoluteFaceSize = 0;

	private CameraBridgeViewBase mOpenCvCameraView;

	// private SeekBar mMethodSeekbar;
	// private TextView mValue;

	private Point eye; // 存储眼睛坐标
	private Point left_eye; // 左眼坐标
	private Point right_eye; // 右眼坐标
	private Point center;
	private boolean isCheck = false;
	private Handler mHandler = null;

	private TextView mCoordinate;
	private File mChooseFile;
	private ScrollView mSvReader;

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {// 加载
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");
				try {
					// 加载脸部识别文件“lbpcascade_frontalface.xml”
					InputStream is = getResources().openRawResource(
							R.raw.lbpcascade_frontalface);
					// getDir在应用程序的数据文件下获取或创建name对应的子目录
					File cascadeDir = getDir("cascade", Context.MODE_PRIVATE/* 该文件只能被当前的应用程序所读写 */);
					mCascadeFile = new File(cascadeDir,
							"lbpcascade_frontalface.xml");
					FileOutputStream os = new FileOutputStream(mCascadeFile);

					byte[] buffer = new byte[4096];
					int bytesRead;
					while ((bytesRead = is.read(buffer)) != -1) {
						os.write(buffer, 0, bytesRead);
					}
					is.close();
					os.close();
					// --------------------------------- load left eye
					// classificator--筛分机 ---------------------------
					InputStream iser = getResources().openRawResource(
							R.raw.haarcascade_lefteye_2splits);
					File cascadeDirER = getDir("cascadeER",
							Context.MODE_PRIVATE);
					File cascadeFileER = new File(cascadeDirER,
							"haarcascade_eye_right.xml");
					FileOutputStream oser = new FileOutputStream(cascadeFileER);

					byte[] bufferER = new byte[4096];
					int bytesReadER;
					while ((bytesReadER = iser.read(bufferER)) != -1) {
						oser.write(bufferER, 0, bytesReadER);
					}
					iser.close();
					oser.close();

					mJavaDetector = new CascadeClassifier(
							mCascadeFile.getAbsolutePath());
					if (mJavaDetector.empty()) {
						Log.e(TAG, "Failed to load cascade classifier");
						mJavaDetector = null;
					} else
						Log.i(TAG, "Loaded cascade classifier from "
								+ mCascadeFile.getAbsolutePath());

					mJavaDetectorEye = new CascadeClassifier(
							cascadeFileER.getAbsolutePath());
					if (mJavaDetectorEye.empty()) {
						Log.e(TAG, "Failed to load cascade classifier");
						mJavaDetectorEye = null;
					} else
						Log.i(TAG, "Loaded cascade classifier from "
								+ mCascadeFile.getAbsolutePath());

					cascadeDir.delete();

				} catch (IOException e) {
					e.printStackTrace();
					Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
				}
				mOpenCvCameraView.setMaxFrameSize(480, 480);
				mOpenCvCameraView.setCameraIndex(1);// Sets the camera index
				// 选择前置摄像头
				mOpenCvCameraView.enableFpsMeter(); // This method enables label
				// with fps value on the
				// screen
				// 估计是允许帧数显示在屏幕，感觉不是关键问题
				mOpenCvCameraView.enableView(); /*
												 * This method is provided for
												 * clients, so they can enable
												 * the camera connection
												 */
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	public FdActivity() {
		mDetectorName = new String[2];
		mDetectorName[JAVA_DETECTOR] = "Java";

		Log.i(TAG, "Instantiated new " + this.getClass());
	}

	/** Called when the activity is first created. */
	@SuppressLint("HandlerLeak") @Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.face_detect_surface_view);
		// 实例化layout
		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
		mOpenCvCameraView.setCvCameraViewListener(this);

		mCoordinate = (TextView) findViewById(R.id.coordinate);
		mCoordinate.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mSvReader = (ScrollView) findViewById(R.id.sv_reader);
			}
		});

		FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		ft.add(R.id.linear_main, new StartFragment(), "00001");
		ft.commit();
		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				if (msg.what == 1) {
					String h = (msg.arg1 == 0) ? "left" : "right";
					String v = (msg.arg2 == 0) ? "up" : "down";
					if (mSvReader != null) {
						if ((msg.arg2 == 0)) {
							mSvReader.scrollBy(0, -10);
						} else {
							mSvReader.scrollBy(0, 10);
						}
					} else {
						mSvReader = (ScrollView) findViewById(R.id.sv_reader);
					}
					mCoordinate.setText(h + " " + v);
				} else if (msg.what == 2) {
					ReadFragment readFragment = new ReadFragment();
					FragmentManager fm = getFragmentManager();
					FragmentTransaction ft = fm.beginTransaction();
					ft.replace(R.id.linear_main, readFragment, "00001");
					ft.addToBackStack(null);
					ft.commit();
					mSvReader = (ScrollView) findViewById(R.id.sv_reader);
				}
			}
		};

	}

	@Override
	public void onPause() {
		super.onPause();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this,
				mLoaderCallback);
	}

	public void onDestroy() {
		super.onDestroy();
		mOpenCvCameraView.disableView();
	}

	public void onCameraViewStarted(int width, int height) {
		mGray = new Mat();
		mRgba = new Mat();
	}

	public void onCameraViewStopped() {
		mGray.release();
		mRgba.release();
		// mZoomWindow.release();
		// mZoomWindow2.release();
	}

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

		mRgba = inputFrame.rgba();
		mGray = inputFrame.gray();

		mRgba = rotateMat(mRgba, 0);
		mGray = rotateMat(mGray, 0);

		Core.flip(mRgba, mRgba, 1); // 绕Y轴旋转，镜像对称
		Core.flip(mGray, mGray, 1); // 绕Y轴旋转，镜像对称

		if (mAbsoluteFaceSize == 0) {
			int height = mGray.cols();
			if (Math.round(height * mRelativeFaceSize) > 0) {
				mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
			}
		}

		// if (mZoomWindow == null || mZoomWindow2 == null)
		// CreateAuxiliaryMats();

		MatOfRect faces = new MatOfRect();
		// mJavaDetector--------------------人脸检测
		if (mJavaDetector != null)
			mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, // TODO:
					// objdetect.CV_HAAR_SCALE_IMAGE
					new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());

		Rect[] facesArray = faces.toArray();
		for (int i = 0; i < facesArray.length; i++) {
			// Core.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(),
			// FACE_RECT_COLOR, 3); //脸部位置
			// xCenter = (facesArray[i].x + facesArray[i].width +
			// facesArray[i].x) / 2;
			// yCenter = (facesArray[i].y + facesArray[i].y +
			// facesArray[i].height) / 2;
			// Point center = new Point(xCenter, yCenter);
			// Core.circle(mRgba, center, 10, new Scalar(255, 0, 0, 255), 3);
			// Core.putText(mRgba, "[" + center.x + "," + center.y + "]",
			// new Point(center.x + 20, center.y + 20),
			// Core.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(255, 255, 255,
			// 255));
			Rect r = facesArray[i];
			// -----------计算人眼区域大小-------------------
			// Rect eyearea = new Rect(r.x + r.width / 8,
			// (int) (r.y + (r.height / 4.5)), r.width - 2 * r.width / 8,
			// (int) (r.height / 3.0));
			// 左眼---------------右眼
			Rect eyearea_right = new Rect(r.x + r.width / 16,
					(int) (r.y + (r.height / 4.5)),
					(r.width - 2 * r.width / 16) / 2, (int) (r.height / 3.0));
			Rect eyearea_left = new Rect(r.x + r.width / 16
					+ (r.width - 2 * r.width / 16) / 2,
					(int) (r.y + (r.height / 4.5)),
					(r.width - 2 * r.width / 16) / 2, (int) (r.height / 3.0));
			// Core.rectangle(mRgba, eyearea_left.tl(), eyearea_left.br(),
			// new Scalar(255, 0, 0, 255), 2);
			// Core.rectangle(mRgba, eyearea_right.tl(), eyearea_right.br(),
			// new Scalar(255, 0, 0, 255), 2);
			// -----------------------------------------------------------------//
			if (learn_frames < 5) {
				// 得到模板
				teplateR = get_template(mJavaDetectorEye, eyearea_right, 24);
				right_eye = eye; // 左眼坐标
				teplateL = get_template(mJavaDetectorEye, eyearea_left, 24);
				left_eye = eye; // 右眼坐标
				if (left_eye != null && right_eye != null) {
					Point nowPoint = new Point((right_eye.x + left_eye.x) / 2,
							(right_eye.y + left_eye.y) / 2);
					if (isCheck) {
						Message message = new Message();
						message.what = 1;
						message.arg1 = (center.x > nowPoint.x) ? 0 : 1;
						message.arg2 = (center.y > nowPoint.y) ? 0 : 1;
						mHandler.sendMessage(message);
					} else {
						isCheck = true;
						center = nowPoint;
					}
				}

				learn_frames++;
			} else {
				// Learning finished, use the new templates for template
				// matching
				match_eye(eyearea_right, teplateR, method);
				match_eye(eyearea_left, teplateL, method);
				learn_frames = 0;
			}

			// Imgproc.resize(mRgba.submat(eyearea_left), mZoomWindow2,
			// mZoomWindow2.size()); //矩阵块复制
			// Imgproc.resize(mRgba.submat(eyearea_right), mZoomWindow,
			// mZoomWindow.size());
		}

		return mRgba;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i(TAG, "called onCreateOptionsMenu");
		mItemFace50 = menu.add("Face size 50%");
		mItemFace40 = menu.add("Face size 40%");
		mItemFace30 = menu.add("Face size 30%");
		mItemFace20 = menu.add("Face size 20%");
		mItemType = menu.add(mDetectorName[mDetectorType]);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
		if (item == mItemFace50)
			setMinFaceSize(0.5f);
		else if (item == mItemFace40)
			setMinFaceSize(0.4f);
		else if (item == mItemFace30)
			setMinFaceSize(0.3f);
		else if (item == mItemFace20)
			setMinFaceSize(0.2f);
		else if (item == mItemType) {
			int tmpDetectorType = (mDetectorType + 1) % mDetectorName.length;
			item.setTitle(mDetectorName[tmpDetectorType]);
		}
		return true;
	}

	private Mat rotateMat(Mat mat, int radianInt) {
		Mat mat_frame = null;
		Mat mat_frame_submat = null;
		Mat mat_rot = null;
		Mat mat_res = null;
		try {
			double radians = Math.toRadians(radianInt);
			double sin = Math.abs(Math.sin(radians));
			double cos = Math.abs(Math.cos(radians));
			int width = mat.width();
			int height = mat.height();
			int newWidth = (int) (width * cos + height * sin);
			int newHeight = (int) (width * sin + height * cos);
			// 能把原图像和旋转后图像同时放入的外框
			int frameWidth = Math.max(width, newWidth);
			int frameHeight = Math.max(height, newHeight);
			Size frameSize = new Size(frameWidth, frameHeight);
			mat_frame = new Mat(frameSize, mat.type());
			// 将原图像copy进外框
			int offsetX = (frameWidth - width) / 2;
			int offsetY = (frameHeight - height) / 2;
			mat_frame_submat = mat_frame.submat(offsetY, offsetY + height,
					offsetX, offsetX + width);
			mat.copyTo(mat_frame_submat);
			// 旋转外框
			Point center = new Point(frameWidth / 2, frameHeight / 2);
			mat_rot = Imgproc.getRotationMatrix2D(center, 90, 1.0);
			mat_res = new Mat(); // result
			Imgproc.warpAffine(mat_frame, mat_res, mat_rot, frameSize,
					Imgproc.INTER_LINEAR, Imgproc.BORDER_CONSTANT,
					Scalar.all(0));
			// 从旋转后的外框获取新图像
			offsetX = (frameWidth - newWidth) / 2;
			offsetY = (frameHeight - newHeight) / 2;
			return mat_res.submat(offsetY, offsetY + newHeight, offsetX,
					offsetX + newWidth);
		} finally {
			mat.release();
			mat_frame.release();
			mat_frame_submat.release();
			mat_rot.release();
			mat_res.release();
		}
	}

	private void setMinFaceSize(float faceSize) {
		mRelativeFaceSize = faceSize;
		mAbsoluteFaceSize = 0;
	}

	// private void CreateAuxiliaryMats() {
	// if (mGray.empty())
	// return;
	//
	// int rows = mGray.rows();
	// int cols = mGray.cols();
	//
	// if (mZoomWindow == null) {
	// mZoomWindow = mRgba.submat(rows / 2 + rows / 10, rows, cols / 2
	// + cols / 10, cols);
	// mZoomWindow2 = mRgba.submat(0, rows / 2 - rows / 10, cols / 2
	// + cols / 10, cols);
	// }
	//
	// }

	private void match_eye(Rect area, Mat mTemplate, int type) {
		Point matchLoc;
		Mat mROI = mGray.submat(area);// 区域（矩阵块）复制
		int result_cols = mROI.cols() - mTemplate.cols() + 1;
		int result_rows = mROI.rows() - mTemplate.rows() + 1;
		// Check for bad template size
		if (mTemplate.cols() == 0 || mTemplate.rows() == 0) {
			return;
		}
		Mat mResult = new Mat(result_cols, result_rows, CvType.CV_8U);

		switch (type) {
		case TM_SQDIFF:
			Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_SQDIFF);
			break;
		case TM_SQDIFF_NORMED:
			Imgproc.matchTemplate(mROI, mTemplate, mResult,
					Imgproc.TM_SQDIFF_NORMED);
			break;
		case TM_CCOEFF:
			Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCOEFF);
			break;
		case TM_CCOEFF_NORMED:
			Imgproc.matchTemplate(mROI, mTemplate, mResult,
					Imgproc.TM_CCOEFF_NORMED);
			break;
		case TM_CCORR:
			Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCORR);
			break;
		case TM_CCORR_NORMED:
			Imgproc.matchTemplate(mROI, mTemplate, mResult,
					Imgproc.TM_CCORR_NORMED);
			break;
		}

		Core.MinMaxLocResult mmres = Core.minMaxLoc(mResult);
		// there is difference in matching methods - best match is max/min value
		if (type == TM_SQDIFF || type == TM_SQDIFF_NORMED) {
			matchLoc = mmres.minLoc;
		} else {
			matchLoc = mmres.maxLoc;
		}

		Point matchLoc_tx = new Point(matchLoc.x + area.x, matchLoc.y + area.y);
		Point matchLoc_ty = new Point(matchLoc.x + mTemplate.cols() + area.x,
				matchLoc.y + mTemplate.rows() + area.y);

		Core.rectangle(mRgba, matchLoc_tx, matchLoc_ty, new Scalar(255, 255, 0,
				255));
		// Rect rec = new Rect(matchLoc_tx, matchLoc_ty);
	}

	private Mat get_template(CascadeClassifier clasificator, Rect area, int size) {
		Mat template = new Mat();
		Mat mROI = mGray.submat(area);
		MatOfRect eyes = new MatOfRect();
		Point iris = new Point();
		Rect eye_template = new Rect();
		clasificator.detectMultiScale(mROI, eyes, 1.15, 2,
				Objdetect.CASCADE_FIND_BIGGEST_OBJECT
						| Objdetect.CASCADE_SCALE_IMAGE, new Size(30, 30),
				new Size());
		Rect[] eyesArray = eyes.toArray();
		for (int i = 0; i < eyesArray.length;) {
			Rect e = eyesArray[i];
			e.x = area.x + e.x;
			e.y = area.y + e.y;
			int x = (int) (e.tl().y + e.height * 0.4);
			int y = (int) (e.tl().x);

			Rect eye_only_rectangle = new Rect(y, x, (int) e.width,
					(int) (e.height * 0.6));

			mROI = mGray.submat(eye_only_rectangle);
			Mat vyrez = mRgba.submat(eye_only_rectangle);

			Core.MinMaxLocResult mmG = Core.minMaxLoc(mROI);
			Core.circle(vyrez, mmG.minLoc, 2, new Scalar(255, 255, 255, 255), 2);

			iris.x = mmG.minLoc.x + eye_only_rectangle.x;
			iris.y = mmG.minLoc.y + eye_only_rectangle.y;
			eye = new Point(iris.x, iris.y); // -------------人眼位置存储
			// eye_template = new Rect((int) iris.x - size / 2, (int) iris.y
			// - size / 2, size, size);
			// Core.rectangle(mRgba, eye_template.tl(), eye_template.br(),
			// new Scalar(255, 0, 0, 255), 2);

			template = (mGray.submat(eye_template)).clone();
			return template;
		}
		return template;
	}

	@Override
	public void onArticleSelected(File file) {
		// TODO Auto-generated method stub
		this.mChooseFile = file;
		Message message = new Message();
		message.what = 2;
		mHandler.sendMessage(message);
	}

	public File getFile() {
		return mChooseFile;
	}
}
