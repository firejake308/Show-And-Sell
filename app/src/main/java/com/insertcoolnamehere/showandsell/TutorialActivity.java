package com.insertcoolnamehere.showandsell;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.stepstone.stepper.Step;
import com.stepstone.stepper.StepperLayout;
import com.stepstone.stepper.VerificationError;
import com.stepstone.stepper.adapter.AbstractFragmentStepAdapter;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class TutorialActivity extends AppCompatActivity implements StepperLayout.StepperListener {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;

    private static final String CURRENT_STEP_POSITION_KEY = "CURRENT_STEP_POSITION_KEY";
    private final Handler mHideHandler = new Handler();
    private StepperLayout mStepper;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mStepper.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tutorial);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mVisible = true;
        mStepper = (StepperLayout) findViewById(R.id.stepper_layout);
        mStepper.setAdapter(new StepperAdapter(getSupportFragmentManager(), this));
        mStepper.setListener(this);

        // Set up the user interaction to manually show or hide the system UI.
        ImageView mainContent = (ImageView) findViewById(R.id.tutorialImg);
        mainContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        //findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button.
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mStepper.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    public void onCompleted(View completeButton) {
        // go to main activity
        Intent goMainIntent = new Intent(this, MainActivity.class);
        startActivity(goMainIntent);
    }

    @Override
    public void onError(VerificationError error) {
        // ignore
    }

    @Override
    public void onStepSelected(int newPosition) {
        ImageView display = (ImageView) findViewById(R.id.tutorialImg);
        TextView tutorialText = (TextView) findViewById(R.id.tutorial_text);
        switch(newPosition) {
            case 0:
                display.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.tutorial_1));
                tutorialText.setText(R.string.tutorial_1);
                break;
            case 1:
                display.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.tutorial_2));
                tutorialText.setText(R.string.tutorial_2);
                break;
            case 2:
                display.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.tutorial_3));
                tutorialText.setText(R.string.tutorial_3);
                break;
            case 3:
                display.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.tutorial_4));
                tutorialText.setText(R.string.tutorial_4);
                break;
            case 4:
                display.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.tutorial_3));
                tutorialText.setText(R.string.tutorial_5);
                break;
            case 5:
                display.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.tutorial_6));
                tutorialText.setText(R.string.tutorial_6);
                break;
            case 6:
                display.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.tutorial_7));
                tutorialText.setText(R.string.tutorial_7);
                break;
        }
    }

    @Override
    public void onReturn() {

    }

    public static class StepFragment extends Fragment implements Step {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.layout_step, container, false);

            Log.d("TutorialActivity", "view width"+view.getMeasuredWidth());

            return view;
        }

        @Override
        public void onSelected() {
            // update UI when selected

        }

        @Override
        public VerificationError verifyStep() {
            // you literally can't screw this up
            return null;
        }

        @Override
        public void onError(VerificationError error) {
            // do nothing
        }
    }

    public static class StepperAdapter extends AbstractFragmentStepAdapter {

        StepperAdapter(FragmentManager manager, Context cxt) {
            super(manager, cxt);
        }

        @Override
        public Step createStep(int position) {
            StepFragment step = new StepFragment();
            Bundle bundle = new Bundle();
            bundle.putInt(CURRENT_STEP_POSITION_KEY, position);
            step.setArguments(bundle);
            return step;
        }

        @Override
        public int getCount() {
            return 7;
        }
    }
}
