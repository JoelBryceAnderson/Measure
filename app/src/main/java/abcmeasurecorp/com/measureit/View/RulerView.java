package abcmeasurecorp.com.measureit.view;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.Locale;

import abcmeasurecorp.com.measureit.R;

import static android.R.attr.x;

/**
 * Created by Joel Anderson on 12/12/16.
 *
 * Custom view class that gets height and width of viewport and
 * displays accurate ruler markings every 1/16th inch.
 */

public class RulerView extends View {

    private static final int DEFAULT_STROKE_WIDTH = 10;
    private static final int LABEL_TEXT_SIZE = 56;
    private static final int MARGIN_OFFSET = 25;
    public static final int ANIMATION_DURATION = 200;

    private float mHeightInches;
    private float mYDPI;

    private float mPointerLocation = 100;

    private boolean mIsMetric = false;

    private int mAccentColor = ContextCompat.getColor(getContext(), R.color.colorAccent);
    private int mPointerAlpha = 255;

    Paint mPaint = new Paint();
    Paint mTextPaint = new Paint();

    public RulerView(Context context) {
        super(context);
        initPaints();
    }

    public RulerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaints();
        initAttributes(context, attrs);
    }

    public RulerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaints();
        initAttributes(context, attrs);
    }

    public RulerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initPaints();
        initAttributes(context, attrs);
    }

    /**
     * Load custom view attributes, if possible
     *
     * @param context from which to load the resources
     * @param attrs attributes passed from view creation
     */
    private void initAttributes(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.RulerView,
                0, 0);

        try {
            mAccentColor = a.getColor(R.styleable.RulerView_accentColor,
                    ContextCompat.getColor(getContext(), R.color.colorAccent));
            mIsMetric = a.getBoolean(R.styleable.RulerView_metric, false);
            boolean showPointer = a.getBoolean(R.styleable.RulerView_showPointer, true);
            if (!showPointer) {
                mPointerAlpha = 0;
            }
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        measureViewport();

        if (!mIsMetric) {
            drawStrokes(canvas);
        } else {
            drawMetricStrokes(canvas);
        }

        if (mPointerAlpha > 0) {
            drawPointer(canvas);
            drawPointerLabel(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mPointerAlpha > 0) {
            //update pointer location
            mPointerLocation = event.getY();
            //refresh view
            this.invalidate();
            return true;
        } else {
            return super.onTouchEvent(event);
        }
    }

    /**
     * Initialize paint for ruler strokes
     */
    private void initPaints() {
        //Initialize paint properties for ruler strokes
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(DEFAULT_STROKE_WIDTH);
        mPaint.setAntiAlias(false);
        mPaint.setColor(ContextCompat.getColor(getContext(), R.color.black));

        //Initialize paint properties for label text
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setTextSize(LABEL_TEXT_SIZE);
        mTextPaint.setFakeBoldText(true);
        mTextPaint.setColor(ContextCompat.getColor(getContext(), R.color.black));
    }

    /**
     * Measures the viewport, calculates height and width in inches
     */
    private void measureViewport() {
        DisplayMetrics dm = new DisplayMetrics();
        //Ensure viewport can be measured (rare case, but better to check before casting)
        if (getContext() instanceof Activity) {
            ((Activity) getContext()).getWindowManager().getDefaultDisplay().getMetrics(dm);
            mYDPI = dm.ydpi;
            mHeightInches = getHeight() / mYDPI;
        } else {
            Log.d("Error", "View not in activity, skipping measurements");
        }
    }

    /**
     * Iterates once over the height of the device, making ruler stroke marks
     * every 1/16th of an inch. Stroke widths are longer at quarter inch strokes,
     * and each inch stroke is labeled.
     *
     * @param canvas to draw ruler lines on
     */
    private void drawStrokes(Canvas canvas) {
        float i = 0;
        while (i < mHeightInches) {
            updatePaintColor(i);

            int lineWidth = getLineWidth(i);
            float strokeLocation = (i * mYDPI) + 5;//offset all lines by 5 to ensure whole first line is visible
            canvas.drawLine(0, strokeLocation, lineWidth, strokeLocation, mPaint);

            drawLabel(canvas, i, lineWidth - 50, strokeLocation + 50);

            i += 0.0625;
        }
    }

    private void drawMetricStrokes(Canvas canvas) {
        float i = 0;
        float heightMetric = (float) (mHeightInches * 2.54) * 10;
        while (i < heightMetric) {
            updatePaintColor(i/10);

            int lineWidth = getLineWidth(i/10);
            float strokeLocation = ((float) (i/10 / 2.54) * mYDPI) + 5;//offset all lines by 5 to ensure whole first line is visible
            canvas.drawLine(0, strokeLocation, lineWidth, strokeLocation, mPaint);

            drawLabel(canvas, i/10, lineWidth - 50, strokeLocation + 50);

            i += 1;
        }
    }

    /**
     * Change stroke color for whole inch markers, keep it black for others
     *
     * @param i the current location onscreen in inches
     */
    private void updatePaintColor(float i) {
        int floor = (int) i;
        if (i == floor) {
            mPaint.setColor(mAccentColor);
        } else {
            mPaint.setColor(ContextCompat.getColor(getContext(), R.color.black));
        }
    }

    /**
     * Returns the strokes desired width
     *
     * @param inches the current location onscreen in inches
     * @return line width for stroke at current location
     */
    private int getLineWidth(float inches) {
        double ceiling = Math.ceil(inches);
        double floor = Math.floor(inches);

        int maxWidth = getWidth() / 2;

        if (inches == floor) {
            return maxWidth;
        } else if (inches - 0.5 == floor) {
            return maxWidth / 2;
        } else if ( (inches - 0.25 == floor) || (inches + 0.25 == ceiling) ) {
            return maxWidth / 4;
        }
        return maxWidth / 8;
    }

    /**
     * Draws the label for the current stroke if location is whole inch
     *
     * @param canvas to draw label on
     * @param inches the current location onscreen in inches
     * @param x location in width to start text
     * @param y location in height to start text
     */
    private void drawLabel(Canvas canvas, float inches, float x, float y) {
        int floor = (int) inches;
        if (inches == floor) {
            String label = String.valueOf(floor);

            canvas.save();
            canvas.rotate(90,x,y);
            canvas.drawText(label, x, y, mTextPaint);
            canvas.restore();
        }
    }

    /**
     * Draws line and circle that user can manipulate to measure objects.
     *
     * @param canvas to draw pointer on
     */
    private void drawPointer(Canvas canvas) {
        //Set new paint attributes
        mPaint.setColor(mAccentColor);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAlpha(mPointerAlpha);

        //Draw line and circle
        int circleRadius = getWidth() / 8;
        float lineX = getWidth() - (circleRadius * 2) - MARGIN_OFFSET;
        float circleX = getWidth() - circleRadius - MARGIN_OFFSET;
        canvas.drawLine(0, mPointerLocation, lineX, mPointerLocation, mPaint);
        canvas.drawCircle(circleX, mPointerLocation, circleRadius, mPaint);

        //Revert paint attributes
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(ContextCompat.getColor(getContext(), R.color.white));
        mPaint.setAlpha(255);
    }

    /**
     * Draws label on pointer, marking number of inches to one significant figure
     *
     * @param canvas to draw label on
     */
    private void drawPointerLabel(Canvas canvas) {
        //Set new paint attributes
        mTextPaint.setColor(ContextCompat.getColor(getContext(), R.color.white));
        mTextPaint.setAlpha(mPointerAlpha);

        String pointerLabel;

        if (!mIsMetric) {
            //Round to tenth place
            pointerLabel = String.format(Locale.getDefault(), "%.1f", mPointerLocation/mYDPI);
        } else {
            //Round to tenth place
            pointerLabel = String.format(Locale.getDefault(), "%.1f", (mPointerLocation*2.54)/mYDPI);
        }

        //Draw Label in circle
        int circleRadius = getWidth() / 8;
        float x = getWidth() - circleRadius - MARGIN_OFFSET;
        float y;
        if (pointerLabel.length() > 3) {
            y = mPointerLocation - 20;
        } else {
            y = mPointerLocation;
        }
        canvas.save();
        canvas.rotate(90, x, y);
        canvas.drawText(pointerLabel, x - 40, y + 20, mTextPaint);//offset text to center in circle
        canvas.restore();

        //Revert paint attributes
        mTextPaint.setColor(ContextCompat.getColor(getContext(), R.color.black));
        mTextPaint.setAlpha(255);
    }

    public boolean isPointerShown() {
        return mPointerAlpha > 0;
    }

    public boolean isMetric() {
        return mIsMetric;
    }

    public void toggleMetric() {
        mIsMetric = !mIsMetric;
        this.invalidate();
    }

    public void setIsMetric(boolean isMetric) {
        this.mIsMetric = isMetric;
    }

    /**
     * Sets the transparency of the pointer
     *
     * @param pointerAlpha desired pointer transparency
     */
    public void setPointerAlpha(int pointerAlpha) {
        mPointerAlpha = pointerAlpha;
        refreshView();
    }

    /**
     * Toggle pointer visibility with smooth animation
     */
    public void animateShowHidePointer() {
        ObjectAnimator visAnim = ObjectAnimator
                .ofInt(RulerView.this, "pointerAlpha", mPointerAlpha, getTargetAlpha());
        visAnim.setDuration(ANIMATION_DURATION);
        visAnim.start();
    }

    /**
     * Returns opposite of current alpha value for visibility animations
     *
     * @return desired new alpha value
     */
    private int getTargetAlpha() {
        if (mPointerAlpha > 0) {
            return 0;
        } else {
            return 255;
        }
    }

    /**
     * Sets the color of the pointer and inch markers
     *
     * @param accentColor new accent color
     */
    public void setAccentColor(int accentColor) {
        mAccentColor = accentColor;
        refreshView();
    }

    /**
     * Sets accent color to new color with smooth animation
     *
     * @param accentColor desired new accent color
     */
    public void animateAccentColor(int accentColor) {
        ObjectAnimator colorAnim = ObjectAnimator.ofInt(
                RulerView.this, "accentColor", mAccentColor, accentColor);
        colorAnim.setEvaluator(new ArgbEvaluator());
        colorAnim.setDuration(ANIMATION_DURATION);
        colorAnim.start();
    }

    public int getAccentColor() {
        return mAccentColor;
    }

    /**
     * Redraws the view onscreen, used for setting of custom attributes at runtime
     */
    private void refreshView() {
        invalidate();
        requestLayout();
    }
}
