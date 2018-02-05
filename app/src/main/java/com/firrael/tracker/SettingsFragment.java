package com.firrael.tracker;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.firrael.tracker.base.SimpleFragment;
import com.firrael.tracker.tesseract.Tesseract;

/**
 * Created by railag on 05.02.2018.
 */

public class SettingsFragment extends SimpleFragment {
    private final static String TAG = SettingsFragment.class.getSimpleName();

    public final static String LANGUAGE_KEY = "languageKey";

    public static SettingsFragment newInstance() {

        Bundle args = new Bundle();

        SettingsFragment fragment = new SettingsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private Spinner mSpinner;
    private boolean initial = true;

    @Override
    protected String getTitle() {
        return getString(R.string.settings);
    }

    @Override
    protected int getViewId() {
        return R.layout.fragment_settings;
    }

    @Override
    protected void initView(View v) {
        mSpinner = v.findViewById(R.id.spinner);

        getMainActivity().hideFab();

        Tesseract.Language[] languages = Tesseract.Language.LANGUAGES;
        String[] data = new String[languages.length];
        for (int i = 0; i < languages.length; i++) {
            data[i] = languages[i].getLocaleTag();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_item, data);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mSpinner.setAdapter(adapter);

        // initialize
        String langKey = Utils.prefs(getActivity()).getString(SettingsFragment.LANGUAGE_KEY, "");
        if (!TextUtils.isEmpty(langKey)) {
            for (int i = 0; i < data.length; i++) {
                if (langKey.equalsIgnoreCase(data[i])) {
                    mSpinner.setSelection(i);
                }
            }
        }

        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                Tesseract.Language language = languages[position];
                Utils.prefs(getActivity())
                        .edit()
                        .putString(LANGUAGE_KEY, language.getLocaleTag())
                        .apply();
                if (!initial) {
                    Snackbar.make(getView(), R.string.language_changed, Snackbar.LENGTH_SHORT).show();
                } else {
                    initial = false;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

}