package abcmeasurecorp.com.measureit.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import abcmeasurecorp.com.measureit.R;

/**
 * Created by Joel Anderson on 12/12/16.
 *
 * Custom view class that gets height and width of
 * viewport and displays accurate ruler markings in imperial units.
 */

public class RulerView extends View {

    private static final int LABEL_TEXT_SIZE = 56;

    Paint mPaint = new Paint();
    Paint mTextPaint = new Paint();

    private float mHeightInches;
    private float mYDPI;

    public RulerView(Context context) {
        super(context);
    }

    public RulerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RulerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public RulerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        initPaints();
        measureViewport();
        drawStrokes(canvas);
    }

    /**
     * Initialize paint for ruler strokes
     */
    private void initPaints() {
        //Initialize paint properties for ruler strokes
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(10);
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
        //Ensure viewport can be measured
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

            drawLabel(canvas, i, lineWidth - 50, strokeLocation - 25);

            i += 0.0625;
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
            mPaint.setColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
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
        if (inches == floor && inches > 0) {
            String label = String.valueOf(floor);
            canvas.drawText(label, x, y, mTextPaint);
        }
    }
}
