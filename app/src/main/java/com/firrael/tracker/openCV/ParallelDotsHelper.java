package com.firrael.tracker.openCV;

import android.app.Activity;
import android.util.Log;

import com.firrael.tracker.DriveUtils;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by railag on 07.05.2018.
 */
public class ParallelDotsHelper {
    private static final String TAG = ParallelDotsHelper.class.getSimpleName();


    private static class SavingTrustManager implements X509TrustManager {

        private final X509TrustManager tm;
        private X509Certificate[] chain;

        SavingTrustManager(X509TrustManager tm) {
            this.tm = tm;
        }

        public X509Certificate[] getAcceptedIssuers() {

            return new X509Certificate[0];
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            throw new UnsupportedOperationException();
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            this.chain = chain;
            tm.checkServerTrusted(chain, authType);
        }
    }

    private static void setUpCert(String hostname) throws Exception {
        SSLSocketFactory factory = HttpsURLConnection.getDefaultSSLSocketFactory();

        SSLSocket socket = (SSLSocket) factory.createSocket(hostname, 443);
        try {
            socket.startHandshake();
            socket.close();
            //System.out.println("No errors, certificate is already trusted");
            return;
        } catch (SSLException e) {
            //System.out.println("cert likely not found in keystore, will pull cert...");
        }


        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = "changeit".toCharArray();
        ks.load(null, password);

        SSLContext context = SSLContext.getInstance("TLS");
        TrustManagerFactory tmf =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        X509TrustManager defaultTrustManager = (X509TrustManager) tmf.getTrustManagers()[0];
        SavingTrustManager tm = new SavingTrustManager(defaultTrustManager);
        context.init(null, new TrustManager[]{tm}, null);
        factory = context.getSocketFactory();

        socket = (SSLSocket) factory.createSocket(hostname, 443);
        try {
            socket.startHandshake();
        } catch (SSLException e) {
            //we should get to here
        }
        X509Certificate[] chain = tm.chain;
        if (chain == null) {
            System.out.println("Could not obtain server certificate chain");
            return;
        }

        X509Certificate cert = chain[0];
        String alias = hostname;
        ks.setCertificateEntry(alias, cert);

        //System.out.println("copy this file to your jre/lib/security folder");
        FileOutputStream fos = new FileOutputStream("paralleldotscacerts");
        ks.store(fos, password);
        fos.close();
    }

    public static void findDiff(Activity activity, String text1, String text2, String fileName, String folderName, DriveResourceClient driveResourceClient) {
        try {
            setUpCert("apis.paralleldots.com");

            final String api_key = "K4m7Q6nEPuKGhpv8nN0X2X0cUScGNO1tpxDY3UoFjWw";
            final String host = "https://apis.paralleldots.com/v3/findDiff";
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
            MediaType mediatype = MediaType.parse("multipart/form-data");
            RequestBody body = RequestBody.create(mediatype, "");
            Request request = (new Request.Builder()).url(host + "?api_key=" + api_key + "&text_1=" + text1 + "&text_2=" + text2).post(body).build();
            Response response = client.newCall(request).execute();
            final String similarity = response.body().string();
            System.out.println(similarity);
            Log.i("SIMILARITY", similarity);

            final Task<DriveContents> createContentsTask = driveResourceClient.createContents();
            Task<MetadataBuffer> folders = DriveUtils.getMetadataForFolder(folderName, driveResourceClient);

            Tasks.whenAll(folders, createContentsTask).continueWithTask(task -> {
                MetadataBuffer metadata = folders.getResult();
                DriveFolder folder = DriveUtils.getDriveFolder(metadata, folderName);

                DriveContents contents = createContentsTask.getResult();

                metadata.release();

                return DriveUtils.createText(contents, similarity + "\n\n" + text1 + "\n\n\n" + text2, fileName, folder, driveResourceClient);
            })
                    .addOnFailureListener(activity, e -> {
                        Log.e(TAG, "Unable to create file", e);
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
