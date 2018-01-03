package com.firrael.tracker.base;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.firrael.tracker.App;
import com.firrael.tracker.MainActivity;

/**
 * Created by Railag on 19.03.2017.
 */

public abstract class SimpleFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        String title = getTitle();
        if (!TextUtils.isEmpty(title)) {
            getMainActivity().setTitle(title);
        }

        View view = inflater.inflate(getViewId(), container, false);

        initView(view);

        return view;
    }

    protected MainActivity getMainActivity() {
        return App.getMainActivity();
    }

    protected void startLoading() {
        if (getMainActivity() != null)
            getMainActivity().startLoading();
    }

    protected void stopLoading() {
        if (getMainActivity() != null)
            getMainActivity().stopLoading();
    }

    protected abstract String getTitle();

    protected abstract int getViewId();

    protected void initView(View v) {
        // initialization
    }

    protected void toast(String message) {
        if (getActivity() != null)
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    protected void toast(int resId) {
        toast(getString(resId));
    }
}