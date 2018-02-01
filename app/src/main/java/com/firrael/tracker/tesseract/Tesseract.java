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

import rx.Observable;

/**
 * Created by railag on 27.12.2017.
 */

public class Tesseract {
    private final static String TAG = Tesseract.class.getSimpleName();

    public final static int WORKER_POOL_SIZE = 50;

    private String datapath;
    private List<TessBaseAPI> mWorkers;
    Context context;

    private static int sCurrentWorker = 0;

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

        mWorkers = new ArrayList<>();
    }

    public TessBaseAPI initNewWorker() {
        TessBaseAPI worker = new TessBaseAPI();
        String language = "eng";
        worker.init(datapath, language);//Auto only        mWorkers.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO_ONLY);
        mWorkers.add(worker);
        return worker;
    }


    private TessBaseAPI takeWorker() {
        if (mWorkers != null && mWorkers.size() > sCurrentWorker) {
            TessBaseAPI worker = mWorkers.get(sCurrentWorker);
            sCurrentWorker++;
            return worker;
        } else {
            return initNewWorker();
        }
    }

    public void stopRecognition() {
        for (TessBaseAPI worker : mWorkers) {
            worker.stop();
        }
    }

    public String processImage(Bitmap bitmap) {
        TessBaseAPI worker = takeWorker();
        worker.setImage(bitmap);
        String result = worker.getUTF8Text();
        Log.i(TAG, "worker just finished");
        return result;
    }

    public void onDestroy() {
        if (mWorkers != null) {
            for (TessBaseAPI worker : mWorkers) {
                worker.end();
            }
        }
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

    public List<TessBaseAPI> getWorkers() {
        return mWorkers;
    }

    public String getDatapath() {
        return datapath;
    }

}
