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
    private Language language;
    Context context;

    private static int sCurrentWorker = 0;

    private boolean available = true;

    private boolean isClosing = false;

    public enum Language {
        EN("eng"),
        RU("rus");

        public final static Language[] LANGUAGES = new Language[]{EN, RU};

        private String localeTag;

        Language(String localeTag) {
            this.localeTag = localeTag;
        }

        public String getLocaleTag() {
            return localeTag;
        }
    }

    public Tesseract(Context context, Language language) {
        this.context = context;
        datapath = Environment.getExternalStorageDirectory() + "/ocrctz/";
        this.language = language;
        File dir = new File(datapath + "/tessdata/");
        //    File file = new File(datapath + "/tessdata/" + "eng.traineddata");
        File file = new File(datapath + "/tessdata/" + language.getLocaleTag() + ".traineddata");
        if (!file.exists()) {
            Log.d(TAG, "in file doesn't exist");
            dir.mkdirs();
            copyFile(context);
        }

        mWorkers = new ArrayList<>();
    }

    public TessBaseAPI initNewWorker() {
        if (isClosing) {
            return null;
        }

        TessBaseAPI worker = new TessBaseAPI();
        String language = this.language.getLocaleTag();
        //    worker.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT);
        worker.init(datapath, language); //Auto only        mWorkers.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO_ONLY);
        if (!isClosing) {
            mWorkers.add(worker);
        }
        return worker;
    }


    private TessBaseAPI takeWorker() {
        if (isClosing) {
            return null;
        }

        if (mWorkers != null && mWorkers.size() > sCurrentWorker) {
            TessBaseAPI worker = mWorkers.get(sCurrentWorker);
            sCurrentWorker++;
            return worker;
        } else {
            return initNewWorker();
        }
    }

/*    public void stopRecognition() {
        for (TessBaseAPI worker : mWorkers) {
            worker.stop();
        }
    }*/

    public String processImage(Bitmap bitmap) {
        TessBaseAPI worker = takeWorker();
        worker.setImage(bitmap);
        String result = worker.getUTF8Text();
        Log.i(TAG, "worker just finished");
        return result;
    }

    public void onDestroy() {
        isClosing = true;
        if (mWorkers != null) {
            for (TessBaseAPI worker : mWorkers) {
                worker.stop();
                worker.end();
            }
        }
    }

    private void copyFile(Context context) {
        AssetManager assetManager = context.getAssets();
        try {
            InputStream in = assetManager.open(language.getLocaleTag() + ".traineddata");
            OutputStream out = new FileOutputStream(datapath + "/tessdata/" + language.getLocaleTag() + ".traineddata");
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

    public void setAvailable(boolean isAvailable) {
        this.available = isAvailable;
    }
}
