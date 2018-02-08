package com.firrael.tracker;

import android.os.Bundle;
import android.os.Handler;

import com.firrael.tracker.base.SimpleFragment;

public class SplashFragment extends SimpleFragment {

    public static SplashFragment newInstance() {

        Bundle args = new Bundle();

        SplashFragment fragment = new SplashFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startLoading();
        if (getMainActivity() == null) {
            App.setMainActivity((MainActivity) getActivity());
        }

        getMainActivity().transparentStatusBar();
        getMainActivity().hideToolbar();
        getMainActivity().hideFab();

        if (savedInstanceState == null) {
            startLoading();

            Handler handler = new Handler();
            handler.postDelayed(() -> {
                stopLoading();
                getMainActivity().toLanding();
            }, 3500);
        }
    }

    @Override
    protected String getTitle() {
        return getString(R.string.app_name);
    }

    @Override
    protected int getViewId() {
        return R.layout.fragment_splash;
    }
}