package com.firrael.tracker.tesseract;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import rx.Emitter;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by railag on 27.12.2017.
 */

public class Tesseract {
    private final static String TAG = Tesseract.class.getSimpleName();

    private String datapath;
    private TessBaseAPI mTess;
    Context context;

    private boolean available = true;

    public Tesseract(Context context) {
        this.context = context;
        datapath = Environment.getExternalStorageDirectory() + "/ocrctz/";
        File dir = new File(datapath + "/tessdata/");
        File file = new File(datapath + "/tessdata/" + "eng.traineddata");
        if (!file.exists()) {
            Log.d(TAG, "in file doesn't exist");
            dir.mkdirs();
            copyFile(context);
        }

        mTess = new TessBaseAPI();
        String language = "eng";
        mTess.init(datapath, language);//Auto only        mTess.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO_ONLY);
    }

    public void stopRecognition() {
        mTess.stop();
    }

    public Observable<List<String>> getOCRResult(List<Bitmap> bitmaps) {
        available = false;

        return Observable.create(objectEmitter -> {
            List<String> results = new ArrayList<>();
            for (Bitmap b : bitmaps) {
                mTess.setImage(b);
                String result = mTess.getUTF8Text();
                results.add(result);
            }
            objectEmitter.onNext(results);
            objectEmitter.onCompleted();
            available = true;
        }, Emitter.BackpressureMode.LATEST);
    }

    public void onDestroy() {
        if (mTess != null)
            mTess.end();
    }

    private void copyFile(Context context) {
        AssetManager assetManager = context.getAssets();
        try {
            InputStream in = assetManager.open("eng.traineddata");
            OutputStream out = new FileOutputStream(datapath + "/tessdata/" + "eng.traineddata");
            byte[] buffer = new byte[1024];
            int read = in.read(buffer);
            while (read != -1) {
                out.write(buffer, 0, read);
                read = in.read(buffer);
            }
        } catch (Exception e) {
            Log.d(TAG, "couldn't copy with the following error : " + e.toString());
        }
    }

    public boolean isAvailable() {
        return available;
    }

}
