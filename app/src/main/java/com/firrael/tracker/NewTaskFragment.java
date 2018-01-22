package com.firrael.tracker;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;

import com.firrael.tracker.base.SimpleFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

/**
 * Created by railag on 15.01.2018.
 */

public class NewTaskFragment extends SimpleFragment {

    private final static String TAG = NewTaskFragment.class.getSimpleName();

    public static NewTaskFragment newInstance() {

        Bundle args = new Bundle();

        NewTaskFragment fragment = new NewTaskFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private EditText mTaskNameEdit;
    private ImageView mVoiceIcon;

    private SpeechRecognizer mRecognizer;

    @Override
    protected String getTitle() {
        return getString(R.string.new_task_title);
    }

    @Override
    protected int getViewId() {
        return R.layout.fragment_new_task;
    }

    @Override
    protected void initView(View v) {
        mTaskNameEdit = v.findViewById(R.id.task_name_edit);
        mVoiceIcon = v.findViewById(R.id.voice_icon);

        toggleFab(false);

        mTaskNameEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String name = mTaskNameEdit.getText().toString();
                boolean isValid = !TextUtils.isEmpty(name) && name.length() > 0;
                toggleFab(isValid);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        mTaskNameEdit.setOnEditorActionListener((v12, actionId, event) -> {
            boolean handled = false;
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                String name = mTaskNameEdit.getText().toString();
                boolean isValid = !TextUtils.isEmpty(name) && name.length() > 0;
                if (isValid) {
                    next();
                    handled = true;
                }
            }
            return handled;
        });

        mVoiceIcon.setOnClickListener(v1 -> startVoice());

        mRecognizer = SpeechRecognizer.createSpeechRecognizer(getActivity());
        mRecognizer.setRecognitionListener(new RecognitionListener() {
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
                stopVoice();
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

                stopVoice();
            }

            @Override
            public void onResults(Bundle results) {
                stopVoice();

                ArrayList<String> result = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                float[] scores = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);

                Log.i(TAG, "onResults " + result);
                Log.i(TAG, "onResults scores: " + Arrays.toString(scores));

                if (result != null && result.size() > 0) {
                    setResult(result.get(0)); // send best option
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
    }

    private void stopVoice() {
        if (mRecognizer != null) {
            mRecognizer.stopListening();
        }

        mVoiceIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_keyboard_voice_black_76dp));
        mVoiceIcon.setOnClickListener(v1 -> startVoice());
    }

    private void toggleFab(boolean isValid) {
        if (isValid) {
            getMainActivity().setupFab(view -> next(), MainActivity.FAB_NEXT);
        } else {
            getMainActivity().hideFab();
        }
    }

    private void next() {
        String taskName = mTaskNameEdit.getText().toString();
        getMainActivity().toAttach(taskName);
    }

    private void startVoice() {
        if (!Utils.checkVoicePermission(getActivity())) {
            Utils.verifySpeechPermission(getActivity());
            return;
        }

        mVoiceIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_settings_voice_black_76dp));
        mVoiceIcon.setOnClickListener(view -> stopVoice());

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        //intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru_RU");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        //    intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hello, How can I help you?");
        mRecognizer.startListening(intent);
    }

    private void setResult(String task) {
        mTaskNameEdit.setText(task);
    }
}