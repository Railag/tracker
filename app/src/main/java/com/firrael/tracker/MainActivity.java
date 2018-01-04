package com.firrael.tracker;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.firrael.tracker.openCV.OpenCVFRagment;
import com.firrael.tracker.realm.RealmDB;
import com.firrael.tracker.realm.TaskModel;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filter;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.wang.avi.AVLoadingIndicatorView;

import org.opencv.android.OpenCVLoader;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import io.realm.Realm;

import static com.google.android.gms.drive.Drive.getDriveClient;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String TAG_MAIN = "mainTag";

    private static final int REQUEST_GOOGLE_SERVICES_AVAILABILITY = 100;
    private static final int REQUEST_CODE_CREATE_FILE = 101;
    private static final int REQUEST_GOOGLE_SIGN_IN = 102;

    private Toolbar toolbar;
    private AVLoadingIndicatorView loading;
    private TextView toolbarTitle;

    private GoogleSignInClient mGoogleSignInClient;
    private DriveClient mDriveClient;
    private DriveResourceClient mDriveResourceClient;

    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar);

        loading = findViewById(R.id.loading);

        toolbarTitle = findViewById(R.id.toolbarTitle);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        App.setMainActivity(this);

        toolbar.setNavigationOnClickListener(v -> onBackPressed());


        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> newVoiceTask());

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        hideToolbar();

        initOpenCV();

        toSplash();

        mGoogleSignInClient = buildGoogleSignInClient();

        try {
            handleSignInResult(mGoogleSignInClient.silentSignIn());
        } catch (Exception e) {
            e.printStackTrace();
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, REQUEST_GOOGLE_SIGN_IN);
        }
    }

    private void newVoiceTask() {
        SpeechRecognizer recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.i(TAG, "onReadyForSpeech");
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.i(TAG, "onBeginningOfSpeech");
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                //            Log.i(TAG, "onRmsChanged " + rmsdB);
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                Log.i(TAG, "onBufferReceived " + new String(buffer));
            }

            @Override
            public void onEndOfSpeech() {
                Log.i(TAG, "onEndOfSpeech");
            }

            @Override
            public void onError(int error) {
                switch (error) {
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        Log.i(TAG, "ERROR_SPEECH_TIMEOUT");
                        break;
                    case SpeechRecognizer.ERROR_NO_MATCH:
                        Log.i(TAG, "ERROR_NO_MATCH");
                        break;
                    default:
                        Log.i(TAG, "Error code " + error);
                }
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> result = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                float[] scores = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);

                Log.i(TAG, "onResults " + result);
                Log.i(TAG, "onResults scores: " + Arrays.toString(scores));

                if (result != null && result.size() > 0) {
                    saveNewTask(result.get(0)); // send best option
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                Log.i(TAG, "onPartialResults " + partialResults);
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                Log.i(TAG, "onEvent " + eventType + params);
            }
        });
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        //intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru_RU");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        //    intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hello, How can I help you?");
        recognizer.startListening(intent);
    }

    private void saveNewTask(String task) {
        Realm realm = RealmDB.get();
        TaskModel taskModel = new TaskModel(task);
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(@NonNull Realm realm) {
                // This will create a new object in Realm or throw an exception if the
                // object already exists (same primary key)
                // realm.copyToRealm(obj);

                // This will update an existing object with the same primary key
                // or create a new object if an object with no primary key = 42
                realm.copyToRealmOrUpdate(taskModel);
            }
        });
    }

    private GoogleSignInClient buildGoogleSignInClient() {
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestScopes(Drive.SCOPE_FILE)
                        .build();
        return GoogleSignIn.getClient(this, signInOptions);
    }

    private void initOpenCV() {
        boolean initialized = OpenCVLoader.initDebug();
        if (initialized) {
            Log.i(TAG, "OpenCV initialized successfully.");
        } else {
            Log.i(TAG, "Error during OpenCV initialization.");
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        if (toolbarTitle != null) {
            toolbarTitle.setText(title);
        }
    }

    public void startLoading() {
        loading.setVisibility(View.VISIBLE);
    }

    public void stopLoading() {
        loading.setVisibility(View.GONE);
    }

    private <T extends Fragment> void setFragment(final T fragment) {
        runOnUiThread(() -> {
            currentFragment = fragment;

            final FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();

            // TODO custom transaction animations
            fragmentTransaction.addToBackStack(fragment.getClass().getSimpleName());
            fragmentTransaction.replace(R.id.mainFragment, fragment, TAG_MAIN);
            fragmentTransaction.commitAllowingStateLoss();

        });
    }

    public void setCurrentFragment(Fragment fragment) {
        this.currentFragment = fragment;
    }

    public void showToolbar() {
        toolbar.setVisibility(View.VISIBLE);
    }

    public void hideToolbar() {
        toolbar.setVisibility(View.GONE);
    }

    public void transparentStatusBar() {
        Window window = getWindow();

        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }

    public void toSplash() {
        setFragment(SplashFragment.newInstance());
    }

    public void toStart() {
        setFragment(OpenCVFRagment.newInstance());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkGooglePlayServices();
    }

    private void checkGooglePlayServices() {
        GoogleApiAvailability availability = GoogleApiAvailability.getInstance();
        int result = availability.isGooglePlayServicesAvailable(this);
        switch (result) {
            case ConnectionResult.SUCCESS:
                break;
            default:
                availability.getErrorDialog(this, result, REQUEST_GOOGLE_SERVICES_AVAILABILITY).show();
        }
    }

   /* private boolean isFolderExists(String folderName) {

        DriveId folderId = DriveId.decodeFromString(folderName);
        DriveFolder folder = Drive.DriveApi.getFolder(mGoogleApiClient, folderId);
        folder.getMetadata(mGoogleApiClient).setResultCallback(metadataRetrievedCallback);

    }

    final private ResultCallback<DriveResource.MetadataResult> metadataRetrievedCallback = new
            ResultCallback<DriveResource.MetadataResult>() {
                @Override
                public void onResult(DriveResource.MetadataResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Log.v(TAG, "Problem while trying to fetch metadata.");
                        return;
                    }

                    Metadata metadata = result.getMetadata();
                    if(metadata.isTrashed()){
                        Log.v(TAG, "Folder is trashed");
                    }else{
                        Log.v(TAG, "Folder is not trashed");
                    }

                }
            };
*/
    private void addGoogleDriveImage(Bitmap image, String name, String folderName) {
        final Task<DriveContents> createContentsTask = mDriveResourceClient.createContents();
        Query query = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.TITLE, folderName))
                .build();
        Task<MetadataBuffer> folders = mDriveResourceClient.query(query);
                Tasks.whenAll(folders, createContentsTask).continueWithTask(task -> {
                    Metadata folderMetadata = null;
                    MetadataBuffer metadataBuffer = folders.getResult();
                    for (int i = 0; i < metadataBuffer.getCount(); i++) {
                        Metadata metadata = metadataBuffer.get(i);
                        if (metadata.getTitle().equalsIgnoreCase(folderName)) {
                            folderMetadata = metadata;
                            break;
                        }
                    }

                    DriveFolder folder = folderMetadata.getDriveId().asDriveFolder();

                    DriveContents contents = createContentsTask.getResult();
                    OutputStream outputStream = contents.getOutputStream();
                    image.compress(Bitmap.CompressFormat.PNG, 100, outputStream);

                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                            .setTitle(name)
                            .setMimeType("image/jpeg")
                            .build();

                    return mDriveResourceClient.createFile(folder, changeSet, contents);
                })
                .addOnSuccessListener(this,
                        driveFile -> Log.i(TAG, "Upload finished " + name))
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Unable to create file", e);
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_GOOGLE_SIGN_IN:
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                handleSignInResult(task);
                break;
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            // Signed in successfully, show authenticated UI.
            Log.i(TAG, "Sign in success");
            // Build a drive client.
            mDriveClient = Drive.getDriveClient(getApplicationContext(), account);
            // Build a drive resource client.
            mDriveResourceClient =
                    Drive.getDriveResourceClient(getApplicationContext(), account);

        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
        }
    }


    public void uploadBitmapsToGoogleDrive(List<Bitmap> regions) {
        final String folderName = "openCV";
        createGoogleDriveFolder(folderName);

        for (int i = 0; i < regions.size(); i++) {
            addGoogleDriveImage(regions.get(i), "region #" + i, folderName);
        }
    }

    private void createGoogleDriveFolder(String name) {
        final Task<DriveFolder> rootFolderTask = mDriveResourceClient.getRootFolder();
        rootFolderTask.continueWithTask(task -> {
            DriveFolder parentFolder = task.getResult();
            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                    .setTitle(name)
                    .setMimeType(DriveFolder.MIME_TYPE)
                    .setStarred(true)
                    .build();
            return mDriveResourceClient.createFolder(parentFolder, changeSet);
        });
    }


}
