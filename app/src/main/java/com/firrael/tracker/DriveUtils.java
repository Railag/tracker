package com.firrael.tracker;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * Created by railag on 18.01.2018.
 */

public class DriveUtils {
    public static DriveFolder getDriveFolder(MetadataBuffer metadataBuffer, String folderName) {
        Metadata folderMetadata = null;
        for (int i = 0; i < metadataBuffer.getCount(); i++) {
            Metadata metadata = metadataBuffer.get(i);
            if (metadata.getTitle().equalsIgnoreCase(folderName)) {
                folderMetadata = metadata;
                break;
            }
        }

        DriveFolder folder = folderMetadata.getDriveId().asDriveFolder();
        return folder;
    }

    public static Task<MetadataBuffer> getMetadataForFolder(String folderName, DriveResourceClient driveClient) {
        Query query = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.TITLE, folderName))
                .build();
        Task<MetadataBuffer> folders = driveClient.query(query);
        return folders;
    }

    public static Task<DriveFile> createImage(DriveContents contents, Bitmap image, String name, DriveFolder driveFolder, DriveResourceClient driveClient) {
        OutputStream outputStream = contents.getOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, outputStream);

        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle(name)
                .setMimeType("image/jpeg")
                .build();

        return driveClient.createFile(driveFolder, changeSet, contents);
    }

    public static Task<DriveFile> createText(DriveContents contents, String text, String name, DriveFolder driveFolder, DriveResourceClient driveClient) {
        OutputStream outputStream = contents.getOutputStream();
        try {
            outputStream.write(text.getBytes(Charset.forName("UTF-8")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle(name)
                .setMimeType("text/plain")
                .build();

        return driveClient.createFile(driveFolder, changeSet, contents);
    }

    public static Task<DriveFolder> createFolder(String name, DriveResourceClient driveClient) {
        final Task<DriveFolder> rootFolderTask = driveClient.getRootFolder();
        rootFolderTask.continueWithTask(task -> {
            DriveFolder parentFolder = task.getResult();
            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                    .setTitle(name)
                    .setMimeType(DriveFolder.MIME_TYPE)
                    .setStarred(true)
                    .build();
            return driveClient.createFolder(parentFolder, changeSet);
        });
        return rootFolderTask;
    }

    public static Task<DriveFolder> createSubFolder(String name, DriveFolder driveFolder, DriveResourceClient driveClient) {
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle(name)
                .setMimeType(DriveFolder.MIME_TYPE)
                .setStarred(true)
                .build();
        return driveClient.createFolder(driveFolder, changeSet);
    }
}
