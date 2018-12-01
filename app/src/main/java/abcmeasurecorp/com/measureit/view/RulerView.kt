package abcmeasurecorp.com.measureit.view

import abcmeasurecorp.com.measureit.R
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.annotation.Keep
import androidx.core.content.ContextCompat
import java.util.*

/**
 * Created by Joel Anderson on 12/12/16.
 *
 *
 * Custom view class that gets height and width of viewport and
 * displays accurate ruler markings every 1/16th inch.
 */

class RulerView : View {

    private val defaultStrokeWidth = resources.getDimension(R.dimen.stroke_width)
    private val labelTextSize = resources.getDimension(R.dimen.text_size_sub_header)
    private val marginOffset = resources.getDimension(R.dimen.text_size_sub_header)

    private var mWidthInches: Float = 0f
    private var mXDPI: Float = 0f

    private var mPointerLocation = 100f

    var isMetric = false

    private var mAccentColor = ContextCompat.getColor(context, R.color.colorAccent)
    private var mPointerAlpha = 255

    private val mPaint = Paint()
    private val mTextPaint = Paint()

    val isPointerShown: Boolean
        get() = mPointerAlpha > 0

    /**
     * Returns opposite of current alpha value for visibility animations
     *
     * @return desired new alpha value
     */
    private val targetAlpha: Int
        get() = if (mPointerAlpha > 0) 0 else 255

    /**
     * Sets the color of the pointer and inch markers
     */
    var accentColor: Int
        get() = mAccentColor
        @Keep
        set(accentColor) {
            mAccentColor = accentColor
            refreshView()
        }

    constructor(
            context: Context
    ) : super(context) {
        initPaints()
    }

    constructor(
            context: Context,
            attrs: AttributeSet
    ) : super(context, attrs) {
        initPaints()
        initAttributes(context, attrs)
    }

    constructor(
            context: Context,
            attrs: AttributeSet,
            defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        initPaints()
        initAttributes(context, attrs)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        initPaints()
        initAttributes(context, attrs)
    }

    /**
     * Load custom view attributes, if possible
     *
     * @param context from which to load the resources
     * @param attrs   attributes passed from view creation
     */
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.RulerView, 0, 0)

        try {
            mAccentColor = a.getColor(R.styleable.RulerView_accentColor,
                    ContextCompat.getColor(getContext(), R.color.colorAccent))
            isMetric = a.getBoolean(R.styleable.RulerView_metric, false)
            val showPointer = a.getBoolean(R.styleable.RulerView_showPointer, true)
            if (!showPointer) mPointerAlpha = 0
        } finally {
            a.recycle()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        measureViewport()

        if (!isMetric) drawStrokes(canvas)
        else drawMetricStrokes(canvas)

        if (mPointerAlpha > 0) {
            drawPointer(canvas)
            drawPointerLabel(canvas)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (mPointerAlpha > 0) {
            //update pointer location
            mPointerLocation = event.x
            //refresh view
            this.invalidate()
            true
        } else super.onTouchEvent(event)
    }

    /**
     * Initialize paint for ruler strokes
     */
    private fun initPaints() {
        //Initialize paint properties for ruler strokes
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeWidth = defaultStrokeWidth
        mPaint.isAntiAlias = false
        mPaint.color = ContextCompat.getColor(context, R.color.black)

        //Initialize paint properties for label text
        mTextPaint.style = Paint.Style.FILL
        mTextPaint.textSize = labelTextSize
        mTextPaint.isFakeBoldText = true
        mTextPaint.color = ContextCompat.getColor(context, R.color.black)
    }

    /**
     * Measures the viewport, calculates height and width in inches
     */
    private fun measureViewport() {
        val dm = DisplayMetrics()
        //Ensure viewport can be measured (rare case, but better to check before casting)
        if (context is Activity) {
            (context as Activity).windowManager.defaultDisplay.getMetrics(dm)
            mXDPI = dm.xdpi
            mWidthInches = width / mXDPI
        } else Log.d("Error", "View not in activity, skipping measurements")
    }

    /**
     * Iterates once over the height of the device, making ruler stroke marks
     * every 1/16th of an inch. Stroke widths are longer at quarter inch strokes,
     * and each inch stroke is labeled.
     *
     * @param canvas to draw ruler lines on
     */
    private fun drawStrokes(canvas: Canvas) {
        var i = 0f
        while (i < mWidthInches) {
            updatePaintColor(i)

            val lineWidth = getLineWidth(i)
            val strokeLocation = i * mXDPI
            canvas.drawLine(strokeLocation, height - lineWidth.toFloat(), strokeLocation, height.toFloat(), mPaint)

            drawLabel(canvas, i, strokeLocation + labelTextSize / 2, height.toFloat() - lineWidth)

            i += 0.0625f
        }
    }

    private fun drawMetricStrokes(canvas: Canvas) {
        var i = 0f
        val widthMetric = (mWidthInches * 2.54).toFloat() * 10
        while (i < widthMetric) {
            updatePaintColor(i / 10)

            val lineWidth = getLineWidth(i / 10)
            val strokeLocation = (i.toDouble() / 10.0 / 2.54).toFloat() * mXDPI
            canvas.drawLine(strokeLocation, height - lineWidth.toFloat(), strokeLocation, height.toFloat(), mPaint)

            drawLabel(canvas, i / 10, strokeLocation + labelTextSize / 2, height.toFloat() - lineWidth)

            i += 1f
        }
    }

    /**
     * Change stroke color for whole inch markers, keep it black for others
     *
     * @param i the current location onscreen in inches
     */
    private fun updatePaintColor(i: Float) {
        val floor = i.toInt()
        if (i == floor.toFloat()) {
            mPaint.color = mAccentColor
        } else {
            mPaint.color = ContextCompat.getColor(context, R.color.black)
        }
    }

    /**
     * Returns the strokes desired width
     *
     * @param inches the current location onscreen in inches
     * @return line width for stroke at current location
     */
    private fun getLineWidth(inches: Float): Int {
        val ceiling = Math.ceil(inches.toDouble())
        val floor = Math.floor(inches.toDouble())

        val maxWidth = height / 4

        return if (inches.toDouble() == floor) maxWidth
        else if (inches - 0.5 == floor) maxWidth / 2
        else if (inches - 0.25 == floor || inches + 0.25 == ceiling) maxWidth / 4
        else maxWidth / 8
    }

    /**
     * Draws the label for the current stroke if location is whole inch
     *
     * @param canvas to draw label on
     * @param inches the current location onscreen in inches
     * @param x      location in width to start text
     * @param y      location in height to start text
     */
    private fun drawLabel(canvas: Canvas, inches: Float, x: Float, y: Float) {
        val floor = inches.toInt()
        if (inches == floor.toFloat()) {
            val label = floor.toString()

            canvas.drawText(label, x, y, mTextPaint)
        }
    }

    /**
     * Draws line and circle that user can manipulate to measure objects.
     *
     * @param canvas to draw pointer on
     */
    private fun drawPointer(canvas: Canvas) {
        //Set new paint attributes
        mPaint.color = mAccentColor
        mPaint.style = Paint.Style.FILL
        mPaint.alpha = mPointerAlpha

        //Draw line and circle
        val circleRadius = height / 16
        val lineY = (height.toFloat() / 2) + circleRadius.toFloat() + marginOffset
        val circleY = (height.toFloat() / 2) + marginOffset
        canvas.drawLine(mPointerLocation, lineY, mPointerLocation, height.toFloat(), mPaint)
        canvas.drawCircle(mPointerLocation,  circleY, circleRadius.toFloat(), mPaint)

        //Revert paint attributes
        mPaint.style = Paint.Style.STROKE
        mPaint.color = ContextCompat.getColor(context, R.color.white)
        mPaint.alpha = 255
    }

    /**
     * Draws label on pointer, marking number of inches to one significant figure
     *
     * @param canvas to draw label on
     */
    private fun drawPointerLabel(canvas: Canvas) {
        //Set new paint attributes
        mTextPaint.color = ContextCompat.getColor(context, R.color.white)
        mTextPaint.alpha = mPointerAlpha

        val labelValue = if (!isMetric) mPointerLocation else mPointerLocation * 2.54f
        val pointerLabel = String.format(Locale.getDefault(), "%.2f", labelValue / mXDPI) //Round to tenth place

        //Draw Label in circle
        val circleRadius = height / 16
        val y = (height.toFloat() / 2)  + marginOffset + labelTextSize / 3
        val x = if (pointerLabel.length > 4) mPointerLocation - labelTextSize / 4 else mPointerLocation
        canvas.drawText(pointerLabel, x - labelTextSize, y, mTextPaint)//offset text to center in circle

        //Revert paint attributes
        mTextPaint.color = ContextCompat.getColor(context, R.color.black)
        mTextPaint.alpha = 255
    }

    fun toggleMetric() {
        isMetric = !isMetric
        this.invalidate()
    }

    fun setShowPointer(show: Boolean) = setPointerAlpha(if (show) 255 else 0)

    /**
     * Sets the transparency of the pointer
     *
     * @param pointerAlpha desired pointer transparency
     */
    @Keep
    fun setPointerAlpha(pointerAlpha: Int) {
        mPointerAlpha = pointerAlpha
        refreshView()
    }

    /**
     * Toggle pointer visibility with smooth animation
     */
    fun animateShowHidePointer() = ObjectAnimator
            .ofInt(this@RulerView, "pointerAlpha", mPointerAlpha, targetAlpha)
            .apply { duration = ANIMATION_DURATION }
            .start()

    /**
     * Sets accent color to new color with smooth animation
     *
     * @param accentColor desired new accent color
     */
    fun animateAccentColor(accentColor: Int) = ObjectAnimator
            .ofInt(this@RulerView, "accentColor", mAccentColor, accentColor)
            .apply {
                setEvaluator(ArgbEvaluator())
                duration = ANIMATION_DURATION
            }
            .start()

    /**
     * Redraws the view onscreen, used for setting of custom attributes at runtime
     */
    private fun refreshView() {
        invalidate()
        requestLayout()
    }

    companion object {
        const val ANIMATION_DURATION = 200L
    }
}
