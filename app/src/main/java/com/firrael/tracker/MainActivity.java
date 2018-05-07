package com.firrael.tracker;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firrael.tracker.openCV.OpenCVActivity;
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
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.tasks.Task;
import com.wang.avi.AVLoadingIndicatorView;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String TAG_MAIN = "mainTag";

    private static final int REQUEST_GOOGLE_SERVICES_AVAILABILITY = 100;
    private static final int REQUEST_GOOGLE_SIGN_IN = 101;

    private Toolbar toolbar;
    private AVLoadingIndicatorView loading;
    private TextView toolbarTitle;

    private ImageView profileImage;
    private TextView profileName;
    private TextView profileEmail;

    private GoogleSignInClient mGoogleSignInClient;
    private DriveClient mDriveClient;
    private DriveResourceClient mDriveResourceClient;

    private FloatingActionButton mFab;

    private Fragment mCurrentFragment;
    private BackupFragment.SelectFolderListener selectFolderListener;

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

        mFab = findViewById(R.id.fab);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View headerView = navigationView.getHeaderView(0);
        profileImage = headerView.findViewById(R.id.profile_image);
        profileName = headerView.findViewById(R.id.profile_name);
        profileEmail = headerView.findViewById(R.id.profile_email);

        toSplash();

        signIn();
    }

    private void signIn() {
        mGoogleSignInClient = buildGoogleSignInClient();

        try {
            Task<GoogleSignInAccount> task = mGoogleSignInClient.silentSignIn();
            if (task.isSuccessful()) {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                handleSignInResult(account);
                Log.i(TAG, "Sign in success");
            } else {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, REQUEST_GOOGLE_SIGN_IN);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, REQUEST_GOOGLE_SIGN_IN);
        }
    }

    private GoogleSignInClient buildGoogleSignInClient() {
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestScopes(Drive.SCOPE_FILE)
                        .requestEmail()
                        .build();
        return GoogleSignIn.getClient(this, signInOptions);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (mCurrentFragment instanceof LandingTaskFragment) {
                finish();
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        if (toolbarTitle != null) {
            toolbarTitle.setText(title);
        }
    }

    public void startLoading() {
        if (loading != null) {
            loading.setVisibility(View.VISIBLE);
        }
    }

    public void stopLoading() {
        if (loading != null) {
            loading.setVisibility(View.GONE);
        }
    }

    private <T extends Fragment> void setFragment(final T fragment) {
        runOnUiThread(() -> {
            mCurrentFragment = fragment;

            final FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();

            fragmentTransaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
            fragmentTransaction.addToBackStack(fragment.getClass().getSimpleName());
            fragmentTransaction.replace(R.id.mainFragment, fragment, TAG_MAIN);
            fragmentTransaction.commitAllowingStateLoss();
        });
    }

    public void setCurrentFragment(Fragment fragment) {
        this.mCurrentFragment = fragment;
    }

    public void showToolbar() {
        if (toolbar != null) {
            toolbar.setVisibility(View.VISIBLE);
        }
    }

    public void hideToolbar() {
        if (toolbar != null) {
            toolbar.setVisibility(View.GONE);
        }
    }

    public void transparentStatusBar() {
        Window window = getWindow();
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
    }

    public void toSplash() {
        setFragment(SplashFragment.newInstance());
    }

    public void toNewTask() {
        setFragment(NewTaskFragment.newInstance());
    }

    public void toAttach(String taskName) {
        setFragment(AttachFragment.newInstance(taskName));
    }

    public void toEditTask(TaskModel task) {
        setFragment(EditTaskFragment.newInstance(task));
    }

    public void toBackup() {
        setFragment(BackupFragment.newInstance());
    }

    public void toSettings() {
        setFragment(SettingsFragment.newInstance());
    }

    public void toLanding() {
        setFragment(LandingTaskFragment.newInstance());
    }

    public void toOpenCVTest(Context context) {
        Intent intent = new Intent(context, OpenCVActivity.class);
        intent.putExtra(OpenCVActivity.KEY_TEST, true);
        startActivity(intent);
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
        int id = item.getItemId();

        if (id == R.id.nav_view_tasks) {
            toLanding();
        } else if (id == R.id.nav_add_task) {
            toNewTask();
        } else if (id == R.id.nav_settings) {
            toSettings();
        } else if (id == R.id.nav_share) {
            shareApp();
        } else if (id == R.id.nav_backup) {
            toBackup();
        } else if (id == R.id.nav_test) {
            toOpenCVTest(this);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void shareApp() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text));
        intent.setType("text/plain");
        PackageManager packageManager = getPackageManager();
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(Intent.createChooser(intent, getString(R.string.sharing_message)));
        } else {
            Snackbar.make(toolbar, R.string.share_error, Snackbar.LENGTH_SHORT).show();
        }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_GOOGLE_SIGN_IN:
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                task.continueWith(task1 -> {
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        handleSignInResult(account);
                        Log.i(TAG, "Sign in success");
                    } catch (ApiException e) {
                        Log.e(TAG, "signInResult:failed code=" + e.getStatusCode());
                        Snackbar.make(toolbar, R.string.google_drive_login_message, Snackbar.LENGTH_SHORT).show();
                        signIn();
                    }

                    return null;
                });
                break;
            case BackupFragment.REQUEST_FIND_FOLDER:
                if (selectFolderListener != null) {
                    if (data != null) {
                        Bundle extras = data.getExtras();
                        selectFolderListener.selectedFolder(extras);
                    } else {
                        stopLoading();
                    }
                }
                break;
        }
    }

    private void handleSignInResult(GoogleSignInAccount account) {
        mDriveClient = Drive.getDriveClient(getApplicationContext(), account);
        mDriveResourceClient = Drive.getDriveResourceClient(getApplicationContext(), account);

        App.setDriveClient(mDriveClient);
        App.setDrive(mDriveResourceClient);

        updateProfileUI(account);
    }

    private void updateProfileUI(GoogleSignInAccount account) {
        if (profileImage != null) {
            Uri profileUri = account.getPhotoUrl();
            if (profileUri != null) {
                Glide.with(this).load(profileUri).into(profileImage);
            }
        }

        if (profileName != null) {
            String name = account.getDisplayName();
            profileName.setText(name);
        }

        if (profileEmail != null) {
            String email = account.getEmail();
            if (!TextUtils.isEmpty(email)) {
                profileEmail.setText(email);
            }
        }
    }

    public final static int FAB_NEW = 0;
    public final static int FAB_NEXT = 1;
    public final static int FAB_DONE = 2;

    public void setupFab(View.OnClickListener listener, int fabState) {
        if (mFab != null) {
            showFab();
            mFab.setOnClickListener(listener);


            int fabDrawableId = R.drawable.ic_menu_send;
            switch (fabState) {
                case FAB_NEW:
                    fabDrawableId = R.drawable.ic_add_black_24dp;
                    break;
                case FAB_NEXT:
                    fabDrawableId = R.drawable.ic_forward_black_24dp;
                    break;
                case FAB_DONE:
                    fabDrawableId = R.drawable.ic_done_black_24dp;
                    break;
            }
            mFab.setImageDrawable(ContextCompat.getDrawable(this, fabDrawableId));
        }
    }

    private void showFab() {
        if (mFab != null) {
            mFab.setVisibility(View.VISIBLE);
        }
    }

    public void hideFab() {
        if (mFab != null) {
            mFab.setVisibility(View.GONE);
        }
    }

    public void setSelectFolderListener(BackupFragment.SelectFolderListener
                                                selectFolderListener) {
        this.selectFolderListener = selectFolderListener;
    }
}