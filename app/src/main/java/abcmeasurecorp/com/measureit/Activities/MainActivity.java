package abcmeasurecorp.com.measureit.activities;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.Random;

import abcmeasurecorp.com.measureit.R;
import abcmeasurecorp.com.measureit.view.RulerView;

/**
 * Created by Joel Anderson on 12/12/16.
 *
 * Main Activity class, whose layout contains the ruler view
 * and all additional interactive views.
 */

public class MainActivity extends AppCompatActivity {

    private AppCompatButton mUnitsButton;
    private AppCompatButton mTogglePointerButton;
    private AlertDialog mColorDialog;
    private RulerView mRulerView;
    private LinearLayout mRightContainer;
    private SharedPreferences mPrefs;
    private int mCurrentColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPrefs = getPreferences(MODE_PRIVATE);

        mRulerView = (RulerView) findViewById(R.id.ruler);
        mRightContainer = (LinearLayout) findViewById(R.id.right_container);
        mTogglePointerButton = (AppCompatButton) findViewById(R.id.toggle_pointer_button);
        AppCompatButton randomColorButton = (AppCompatButton) findViewById(R.id.random_color_button);
        mUnitsButton = (AppCompatButton) findViewById(R.id.toggle_metric_button);

        mTogglePointerButton.setOnClickListener(clickTogglePointer());
        randomColorButton.setOnClickListener(clickShowDialog());
        mUnitsButton.setOnClickListener(clickToggleUnits());

        initUserPreferences();
    }

    private void initUserPreferences() {
        boolean isMetric = mPrefs.getBoolean(getString(R.string.ruler_is_metric_pref_key), false);
        boolean showPointer = mPrefs.getBoolean(getString(R.string.ruler_show_pointer_pref_key), true);
        mCurrentColor = mPrefs.getInt(getString(R.string.ruler_color_pref_key),
                ContextCompat.getColor(MainActivity.this, R.color.colorAccent));

        mRulerView.setShowPointer(showPointer);
        mRulerView.setIsMetric(isMetric);
        mRulerView.setAccentColor(mCurrentColor);
        mRightContainer.setBackgroundColor(mCurrentColor);

        mUnitsButton.setText(isMetric ?
                getString(R.string.button_imperial) : getString(R.string.button_metric));
        mTogglePointerButton.setText(showPointer ?
                getString(R.string.button_hide_pointer) : getString(R.string.button_show_pointer));
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && Build.VERSION.SDK_INT >= 19) {
            setImmersiveMode();
        }
    }

    /**
     * Uses custom properties and events of RulerView to toggle the units
     * used in ruler view on click
     *
     * @return OnClickListener to assign to the toggle button
     */
    private View.OnClickListener clickToggleUnits() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleUnits();
            }
        };
    }

    /**
     * Toggles units of ruler, toggles text on units button
     */
    private void toggleUnits() {
        String label;
        SharedPreferences.Editor editor = mPrefs.edit();
        if (mRulerView.isMetric()) {
            label = getString(R.string.button_metric);
            editor.putBoolean(getString(R.string.ruler_is_metric_pref_key), false);
        } else {
            label = getString(R.string.button_imperial);
            editor.putBoolean(getString(R.string.ruler_is_metric_pref_key), true);
        }
        editor.apply();
        mUnitsButton.setText(label);
        mRulerView.toggleMetric();
    }

    /**
     * Uses custom properties and events of RulerView to toggle the visibility
     * of the ruler's pointer on button click
     *
     * @return OnClickListener to assign to the toggle button
     */
    private View.OnClickListener clickTogglePointer() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                togglePointer();
            }
        };
    }

    /**
     * Toggles visibility of pointer, toggles text on pointer visibility button
     */
    private void togglePointer() {
        String label;
        SharedPreferences.Editor editor = mPrefs.edit();
        if (mRulerView.isPointerShown()) {
            label = getString(R.string.button_show_pointer);
            editor.putBoolean(getString(R.string.ruler_show_pointer_pref_key), false);
        } else {
            label = getString(R.string.button_hide_pointer);
            editor.putBoolean(getString(R.string.ruler_show_pointer_pref_key), true);
        }
        editor.apply();
        mTogglePointerButton.setText(label);
        mRulerView.animateShowHidePointer();
    }

    /**
     * Uses custom properties and events of RulerView to change accent colors onscreen
     * to random color on button click.
     *
     * @return OnClickListener to assign to the toggle button
     */
    private View.OnClickListener clickShowDialog() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog();
            }
        };
    }

    private DialogInterface.OnClickListener clickRandomColor() {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                setRandomColor();
                saveColorSelection();
            }
        };
    }

    /**
     * Creates random color, sets right container background and ruler accent colors
     */
    private void setRandomColor() {
        Random rnd = new Random();
        mCurrentColor = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));

        animateBackgroundColor(mCurrentColor);
        mRulerView.animateAccentColor(mCurrentColor);
    }

    /**
     * Changes the background color of the right container with animation
     *
     * @param color desired new background color
     */
    private void animateBackgroundColor(int color) {
        ObjectAnimator colorAnim = ObjectAnimator.ofInt(mRightContainer, "backgroundColor",
                mRulerView.getAccentColor(), color);
        colorAnim.setEvaluator(new ArgbEvaluator());
        colorAnim.setDuration(RulerView.ANIMATION_DURATION);
        colorAnim.start();
    }

    /**
     * Sets fullscreen mode, hides system bars
     */
    private void setImmersiveMode() {
        if (Build.VERSION.SDK_INT >= 19) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private void showDialog() {
        mCurrentColor = ContextCompat.getColor(MainActivity.this, R.color.colorAccent);

        mColorDialog = new AlertDialog.Builder(this)
                .setView(R.layout.color_picker)
                .setTitle(R.string.color_picker_title)
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.random_color, clickRandomColor())
                .show();

        final ImageView button = (ImageView) mColorDialog.findViewById(R.id.choose_color);
        final ImageView colorSpectrum = (ImageView) mColorDialog.findViewById(R.id.color_spectrum);

        if (colorSpectrum != null && button != null) {
            final Bitmap bitmap = ((BitmapDrawable) colorSpectrum.getDrawable()).getBitmap();
            colorSpectrum.setOnTouchListener(onSpectrumTouched(colorSpectrum, button, bitmap));

            button.setOnClickListener(onColorSelected());
        }
    }

    private View.OnClickListener onColorSelected() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animateBackgroundColor(mCurrentColor);
                mRulerView.animateAccentColor(mCurrentColor);
                mColorDialog.cancel();
                saveColorSelection();
            }
        };
    }

    private View.OnTouchListener onSpectrumTouched(final ImageView colorSpectrum,
                                                   final ImageView button, final Bitmap bitmap) {
        return new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        int pixel = getSelectedPixel(event, colorSpectrum, bitmap);
                        if (pixel != 0) {
                            mCurrentColor = Color.argb(255,
                                    Color.red(pixel), Color.green(pixel), Color.blue(pixel));
                            button.getBackground()
                                    .setColorFilter(mCurrentColor, PorterDuff.Mode.SRC_ATOP);
                        }
                }
                return true;
            }
        };
    }

    private int getSelectedPixel(MotionEvent event,
                                 final ImageView colorSpectrum, final Bitmap bitmap) {
        Matrix inverse = new Matrix();
        colorSpectrum.getImageMatrix().invert(inverse);
        float[] touchPoint = new float[]{event.getX(), event.getY()};
        inverse.mapPoints(touchPoint);
        int currentX = (int) touchPoint[0];
        int currentY = (int) touchPoint[1];

        if (currentX < 0) {
            currentX = 0;
        }
        if (currentX > bitmap.getWidth() - 1) {
            currentX = bitmap.getWidth() - 1;
        }

        if (currentY < 0) {
            currentY = 0;
        }
        if (currentY > bitmap.getHeight() - 1) {
            currentY = bitmap.getHeight() - 1;
        }
        return bitmap.getPixel(currentX, currentY);
    }

    private void saveColorSelection() {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putInt(getString(R.string.ruler_color_pref_key), mCurrentColor);
        editor.apply();
    }
}
