package com.firrael.tracker.openCV;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;

import com.firrael.tracker.App;
import com.firrael.tracker.DriveUtils;
import com.firrael.tracker.R;
import com.firrael.tracker.SettingsFragment;
import com.firrael.tracker.Utils;
import com.firrael.tracker.tesseract.RecognizedRegion;
import com.firrael.tracker.tesseract.Tesseract;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.MSER;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Emitter;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

import static org.opencv.android.Utils.bitmapToMat;
import static org.opencv.android.Utils.matToBitmap;
import static org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_MEAN_C;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;

/**
 * Created by railag on 04.01.2018.
 */

public class OpenCVActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private final static String TAG = OpenCVActivity.class.getSimpleName();

    public final static String KEY_SCAN_RESULTS = "scanResults";

    private final static Scalar CONTOUR_COLOR = Scalar.all(100);
    private static final int KERNEL_POWER = 50; // increase to make regions larger, decrease to make regions smaller


    private FocusCameraView mOpenCVCameraView;
    private CropImageView mCropImageView;
    private ImageButton mCropAcceptButton;
    private ImageButton mCropBackButton;

    private Tesseract mTesseract;
    private MSER mDetector;
    private Mat mGrey, mRgba, mIntermediateMat;
    private DriveResourceClient mDriveResourceClient;
    private int mTesseractCounter;
    private Tesseract.Language mLanguage;
    private CompositeSubscription mSubscription;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    if (mDetector == null) {
                        initDetector();
                    }

                    mOpenCVCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_opencv);

        mSubscription = new CompositeSubscription();

        initializeLanguage();

        Window window = getWindow();

        //FULLSCREEN MODE
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //transparent toolbar & status bar
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().hide();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        initializeTesseract();

        mDriveResourceClient = App.getDrive();

        mCropImageView = findViewById(R.id.cropImageView);
        mCropImageView.setAutoZoomEnabled(true);
        mCropImageView.setGuidelines(CropImageView.Guidelines.ON);

        mCropAcceptButton = findViewById(R.id.cropImageAcceptButton);
        mCropBackButton = findViewById(R.id.cropImageBackButton);

        mOpenCVCameraView = findViewById(R.id.javaCameraView);
        mOpenCVCameraView.setOnLongClickListener(v1 -> {
            if (mTesseract.isAvailable()) {
                detectRegions();
            } else {
                Snackbar.make(mOpenCVCameraView, R.string.recognition_in_progress_error, Snackbar.LENGTH_SHORT).show();
            }

            return true;
        });

        mOpenCVCameraView.setVisibility(View.VISIBLE);
        mOpenCVCameraView.setCvCameraViewListener(this);
    }

    private void initializeLanguage() {
        String savedLanguage = Utils.prefs(this).getString(SettingsFragment.LANGUAGE_KEY, "");
        if (TextUtils.isEmpty(savedLanguage)) {
            mLanguage = Tesseract.Language.EN;
        } else {
            Tesseract.Language[] languages = Tesseract.Language.LANGUAGES;
            for (Tesseract.Language language : languages) {
                if (language.getLocaleTag().equalsIgnoreCase(savedLanguage)) {
                    mLanguage = language;
                    break;
                }
            }

            if (mLanguage == null) {
                mLanguage = Tesseract.Language.EN;
            }
        }
    }

    private void initializeTesseract() {
        mTesseract = new Tesseract(this, mLanguage);

        mTesseractCounter = 0;


        mSubscription.add(Observable
                .range(0, Tesseract.WORKER_POOL_SIZE)
                .subscribe(integer -> {
                    Observable<Integer> observable = getInitNewWorkerObservable(integer);
/*                    observable.doOnUnsubscribe(new Action0() {
                        @Override
                        public void call() {
                            // TODO handle stop
                        }
                    });*/
                    mSubscription.add(observable.subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(this::getCreatedObservable, OpenCVActivity.this::onError));
                }));
    }

    private Observable<Integer> getInitNewWorkerObservable(int integer) {
        return Observable.create(emitter -> {
            mTesseract.initNewWorker();
            emitter.onNext(integer);
        }, Emitter.BackpressureMode.LATEST);
    }

    private void getCreatedObservable(int i) {
        Log.i(TAG, "Worker# " + i + " initialized");
        mTesseractCounter++;
        if (mTesseractCounter == Tesseract.WORKER_POOL_SIZE) {
            Log.i(TAG, "Tesseract initialization finished");
        }
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mGrey = inputFrame.gray();
        mRgba = inputFrame.rgba();

        return mRgba;
    }

    private void initDetector() {
        // C++: static Ptr_MSER create(int _delta = 5, int _min_area = 60, int _max_area = 14400, double _max_variation = 0.25, double _min_diversity = .2, int _max_evolution = 200, double _area_threshold = 1.01, double _min_margin = 0.003, int _edge_blur_size = 5)

        mDetector = MSER.create();
        // TODO use all features    mDetector = MSER.create(5, 60, 14400, 0.25,
        //            0.2, 200, 1.01, 0.003, 5);
        //    mDetector.setMinArea(60);
        //    mDetector.setMaxArea(14400);
        //    mDetector.setDelta(5);
        // TODO ???    mDetector.setPass2Only();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCVCameraView != null)
            mOpenCVCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCVCameraView != null)
            mOpenCVCameraView.disableView();

        if (mTesseract != null) {
            if (mSubscription != null && !mSubscription.isUnsubscribed()) {
                mSubscription.unsubscribe();
            }
            mTesseract.onDestroy();
        }
    }

    public void onCameraViewStarted(int width, int height) {
        mOpenCVCameraView.initializeCamera();
        mOpenCVCameraView.setFocusMode(FocusCameraView.FOCUS_CONTINUOUS_PICTURE); // Continuous Picture Mode

        mIntermediateMat = new Mat();
        mGrey = new Mat(height, width, CvType.CV_8UC4);
        mRgba = new Mat(height, width, CvType.CV_8UC4);
    }

    public void processBitmapResults(BitmapResults bitmapResults) {
        List<Bitmap> bitmapRegions = bitmapResults.getRegions();

        if (bitmapRegions == null || bitmapRegions.size() == 0) {
            // no recognized regions
            mTesseract.setAvailable(true);
            Snackbar.make(mOpenCVCameraView,
                    R.string.no_text_recognized_error, Snackbar.LENGTH_SHORT).show();
            return;
        }

        List<BitmapRegion> regions = new ArrayList<>();
        for (int i = 0; i < bitmapRegions.size(); i++) {
            regions.add(new BitmapRegion(i, bitmapRegions.get(i)));
        }

        mTesseract.setAvailable(false);

        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setCancelable(false);
        dialog.setTitle(getString(R.string.loading));
        dialog.setMessage(getString(R.string.text_processing));
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setIndeterminate(false);
        dialog.setMax(regions.size()); // last image in 'regions' is full image, not region
        dialog.setProgress(0);
        dialog.show();

        Observable.create((Action1<Emitter<Void>>) emitter -> {
            uploadBitmapsToGoogleDrive(regions, bitmapResults.getSourceBitmaps());
            emitter.onCompleted();
        }, Emitter.BackpressureMode.LATEST)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aVoid -> {
                        }, this::onError,
                        () -> Log.i(TAG, "Bitmaps uploading completed."));

        List<RecognizedRegion> results = new ArrayList<>();

        Observable
                .from(regions)
                .flatMap(Observable::just)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bitmapRegion -> Observable.create((Action1<Emitter<RecognizedRegion>>) emitter -> {
                    RecognizedRegion region = mTesseract.processImage(bitmapRegion);
                    emitter.onNext(region);
                }, Emitter.BackpressureMode.LATEST)
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> {
                            Log.i(TAG, "Result " + result);
                            results.add(result);
                            dialog.setProgress(dialog.getProgress() + 1);
                            if (results.size() == regions.size()) {
                                mTesseract.setAvailable(true);
                                dialog.dismiss();
                                onSuccess(results);
                            }
                        }, this::onError));
    }

    private void onError(Throwable throwable) {
        throwable.printStackTrace();
    }

    private void onSuccess(List<RecognizedRegion> s) {
        Collections.sort(s);
        Log.i(TAG, "Results: " + s);
        Intent data = new Intent();
        ArrayList<RecognizedRegion> results = new ArrayList<>(s);
        data.putParcelableArrayListExtra(KEY_SCAN_RESULTS, results);
        setResult(RESULT_OK, data);
        finish();
    }

    private void detectRegions() {
        Bitmap source = textSkew(mGrey);

        // crop image
        mCropAcceptButton.setVisibility(View.VISIBLE);
        mCropAcceptButton.setOnClickListener(v -> {
            List<Bitmap> sourceImages = new ArrayList<>();
            sourceImages.add(source); // full image source

            Bitmap croppedBitmap = mCropImageView.getCroppedImage();
            Mat croppedMat = new Mat();
            bitmapToMat(croppedBitmap, croppedMat);
            Imgproc.cvtColor(croppedMat, croppedMat, Imgproc.COLOR_BGR2GRAY); // convert bitmap back to grayscale mat


            mIntermediateMat = croppedMat;

            MatOfKeyPoint keypoint = new MatOfKeyPoint();
            List<KeyPoint> listpoint;
            KeyPoint kpoint;
            Mat mask = Mat.zeros(croppedMat.size(), CvType.CV_8UC1);
            int rectanx1;
            int rectany1;
            int rectanx2;
            int rectany2;

            List<MatOfPoint> contours = new ArrayList<>();
            Mat kernel = new Mat(1, KERNEL_POWER, CvType.CV_8UC1, Scalar.all(255)); // KERNEL_POWER = region width
            Mat morbyte = new Mat();
            Mat hierarchy = new Mat();

            Rect rectan3;
            //

            if (mDetector == null) {
                initDetector();
            }

            mDetector.detect(croppedMat, keypoint);
            listpoint = keypoint.toList();

            for (int i = 0; i < listpoint.size(); i++) {
                kpoint = listpoint.get(i);
                rectanx1 = (int) (kpoint.pt.x - 0.5 * kpoint.size);
                rectany1 = (int) (kpoint.pt.y - 0.5 * kpoint.size);
                rectanx2 = (int) (kpoint.size);
                rectany2 = (int) (kpoint.size);
                if (rectanx1 <= 0)
                    rectanx1 = 1;
                if (rectany1 <= 0)
                    rectany1 = 1;
                if ((rectanx1 + rectanx2) > croppedMat.width())
                    rectanx2 = croppedMat.width() - rectanx1;
                if ((rectany1 + rectany2) > croppedMat.height())
                    rectany2 = croppedMat.height() - rectany1;
                Rect rectant = new Rect(rectanx1, rectany1, rectanx2, rectany2);
                try {
                    Mat roi = new Mat(mask, rectant);
                    roi.setTo(CONTOUR_COLOR);
                } catch (Exception ex) {
                    Log.d(TAG, "mat roi error " + ex.getMessage());
                }
            }

            Bitmap sourceMask = Bitmap.createBitmap(mask.width(), mask.height(), Bitmap.Config.ARGB_8888);
            matToBitmap(mask, sourceMask);
            sourceImages.add(sourceMask); // mask before dilation filter, but with detected contours

            Imgproc.morphologyEx(mask, morbyte, Imgproc.MORPH_DILATE, kernel); // dilate filter

            Bitmap sourceImage = Bitmap.createBitmap(morbyte.width(), morbyte.height(), Bitmap.Config.ARGB_8888);
            matToBitmap(morbyte, sourceImage);
            sourceImages.add(sourceImage); // mask after dilation filter

            Imgproc.findContours(morbyte, contours, hierarchy,
                    Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

            Collections.reverse(contours);

            List<Bitmap> bitmapsToRecognize = new ArrayList<>();

            for (int ind = 0; ind < contours.size(); ind++) {
                rectan3 = Imgproc.boundingRect(contours.get(ind));
                Imgproc.rectangle(croppedMat, rectan3.br(), rectan3.tl(),
                        CONTOUR_COLOR);
                Bitmap bmp = null;
                try {
                    Mat croppedPart;
                    croppedPart = mIntermediateMat.submat(rectan3);
                    bmp = Bitmap.createBitmap(croppedPart.width(), croppedPart.height(), Bitmap.Config.ARGB_8888);
                    matToBitmap(croppedPart, bmp);
                } catch (Exception e) {
                    Log.d(TAG, "cropped part data error " + e.getMessage());
                }
                if (bmp != null) {
                    bitmapsToRecognize.add(bmp);
                }
            }

            Bitmap sourceCropped = Bitmap.createBitmap(croppedMat.width(), croppedMat.height(), Bitmap.Config.ARGB_8888);
            matToBitmap(croppedMat, sourceImage);
            sourceImages.add(sourceCropped);

            BitmapResults results = new BitmapResults(bitmapsToRecognize, sourceImages);

            processBitmapResults(results);
        });

        mCropBackButton.setVisibility(View.VISIBLE);
        mCropBackButton.setOnClickListener(v -> {
            reset();
        });

        mCropImageView.setVisibility(View.VISIBLE);
        mCropImageView.setImageBitmap(source);

        if (mOpenCVCameraView != null) {
            mOpenCVCameraView.disableView();
        }
    }

    private Bitmap textSkew(Mat greyscale) {
        List<Bitmap> sourceImages = new ArrayList<>();

        Bitmap sourceImage = Bitmap.createBitmap(greyscale.width(), greyscale.height(), Bitmap.Config.ARGB_8888);
        matToBitmap(greyscale, sourceImage);
        sourceImages.add(sourceImage);

        Mat source = greyscale.clone();

        //Binarize it
        //Use adaptive threshold if necessary
        Imgproc.adaptiveThreshold(greyscale, greyscale, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 15, 40);
        //Imgproc.threshold(greyscale, greyscale, 200, 255, Imgproc.THRESH_BINARY);

        Bitmap sourceThreshold = Bitmap.createBitmap(greyscale.width(), greyscale.height(), Bitmap.Config.ARGB_8888);
        matToBitmap(greyscale, sourceThreshold);
        sourceImages.add(sourceThreshold);

        //Invert the colors (because objects are represented as white pixels, and the background is represented by black pixels)
        Core.bitwise_not(greyscale, greyscale);
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));

        Bitmap sourceElement = Bitmap.createBitmap(element.width(), element.height(), Bitmap.Config.ARGB_8888);
        matToBitmap(element, sourceElement);
        sourceImages.add(sourceElement);

        //We can now perform our erosion, we must declare our rectangle-shaped structuring element and call the erode function
        Imgproc.erode(greyscale, greyscale, element);

        Bitmap sourceErodedGreyscale = Bitmap.createBitmap(greyscale.width(), greyscale.height(), Bitmap.Config.ARGB_8888);
        matToBitmap(greyscale, sourceErodedGreyscale);
        sourceImages.add(sourceErodedGreyscale);

        //Find all white pixels
        Mat wLocMat = Mat.zeros(greyscale.size(), greyscale.type());
        Core.findNonZero(greyscale, wLocMat);

        Bitmap sourceLockMat = Bitmap.createBitmap(greyscale.width(), greyscale.height(), Bitmap.Config.ARGB_8888);
        matToBitmap(greyscale, sourceLockMat);
        sourceImages.add(sourceLockMat);

        //Create an empty Mat and pass it to the function
        MatOfPoint matOfPoint = new MatOfPoint(wLocMat);

        //Translate MatOfPoint to MatOfPoint2f in order to user at a next step
        MatOfPoint2f mat2f = new MatOfPoint2f();
        matOfPoint.convertTo(mat2f, CvType.CV_32FC2);

        //Get rotated rect of white pixels
        RotatedRect rotatedRect = Imgproc.minAreaRect(mat2f);

        Point[] vertices = new Point[4];
        rotatedRect.points(vertices);
        List<MatOfPoint> boxContours = new ArrayList<>();
        boxContours.add(new MatOfPoint(vertices));
        Imgproc.drawContours(greyscale, boxContours, 0, new Scalar(128, 128, 128), -1);

        Bitmap sourceGreyscale = Bitmap.createBitmap(greyscale.width(), greyscale.height(), Bitmap.Config.ARGB_8888);
        matToBitmap(greyscale, sourceGreyscale);
        sourceImages.add(sourceGreyscale);

        double resultAngle = rotatedRect.angle;
        resultAngle += 90.f; // for landscape mode
        if (rotatedRect.size.width > rotatedRect.size.height) {
            rotatedRect.angle += 90.f;
        }

        //Or
        //rotatedRect.angle = rotatedRect.angle < -45 ? rotatedRect.angle + 90.f : rotatedRect.angle;

        Mat result = deskew(source, resultAngle);

        Bitmap bitmap = Bitmap.createBitmap(result.width(), result.height(), Bitmap.Config.ARGB_8888);
        matToBitmap(result, bitmap);
        sourceImages.add(bitmap);

        uploadBitmapsToGoogleDrive(new ArrayList<>(), sourceImages);

        return bitmap;
    }

    public Mat deskew(Mat src, double angle) {
        Point center = new Point(src.width() / 2, src.height() / 2);
        Mat rotImage = Imgproc.getRotationMatrix2D(center, angle, 1.0);
        //1.0 means 100 % scale
        Size size = new Size(src.width(), src.height());
        Imgproc.warpAffine(src, src, rotImage, size, Imgproc.INTER_LINEAR + Imgproc.CV_WARP_FILL_OUTLIERS);
        return src;
    }

    private void reset() {
        mCropImageView.setVisibility(View.GONE);
        mCropAcceptButton.setVisibility(View.GONE);
        mCropBackButton.setVisibility(View.GONE);
        mOpenCVCameraView.enableView();
    }

    @Override
    public void onCameraViewStopped() {
        Log.i(TAG, "onCameraViewStopped called");
    }

    public void uploadBitmapsToGoogleDrive(List<BitmapRegion> regions, List<Bitmap> sourceBitmaps) {
        String timeStamp = Utils.getCurrentTimestamp();
        final String folderName = timeStamp + " openCV";

        Task<DriveFolder> folderTask = DriveUtils.createFolder(folderName, mDriveResourceClient);
        folderTask.continueWith(task -> {
            DriveFolder parentFolder = folderTask.getResult();

            for (int i = 0; i < regions.size(); i++) {
                BitmapRegion region = regions.get(i);
                addGoogleDriveImage(region.getBitmap(), "region #" + region.getRegionNumber(), folderName);
            }

            if (sourceBitmaps != null && sourceBitmaps.size() > 0) {
                for (int i = 0; i < sourceBitmaps.size(); i++) {
                    addGoogleDriveImage(sourceBitmaps.get(i), "source image " + i, folderName);
                }
            }

            return null;
        });
    }


    private void addGoogleDriveImage(Bitmap image, String name, String folderName) {
        final Task<DriveContents> createContentsTask = mDriveResourceClient.createContents();
        Task<MetadataBuffer> folders = DriveUtils.getMetadataForFolder(folderName, mDriveResourceClient);

        Tasks.whenAll(folders, createContentsTask).continueWithTask(task -> {
            MetadataBuffer metadata = folders.getResult();
            DriveFolder folder = DriveUtils.getDriveFolder(metadata, folderName);

            DriveContents contents = createContentsTask.getResult();

            metadata.release();

            return DriveUtils.createImage(contents, image, name, folder, mDriveResourceClient);
        })
                .addOnSuccessListener(this,
                        driveFile -> Log.i(TAG, "Upload finished " + name))
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Unable to create file", e);
                });
    }
}