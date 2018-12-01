package abcmeasurecorp.com.measureit.view

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View

import java.util.Locale

import abcmeasurecorp.com.measureit.R
import androidx.annotation.Keep
import androidx.core.content.ContextCompat

/**
 * Created by Joel Anderson on 12/12/16.
 *
 *
 * Custom view class that gets height and width of viewport and
 * displays accurate ruler markings every 1/16th inch.
 */

class RulerView : View {

    private val DEFAULT_STROKE_WIDTH = resources.getDimension(R.dimen.stroke_width)
    private val LABEL_TEXT_SIZE = resources.getDimension(R.dimen.text_size_sub_header)
    private val MARGIN_OFFSET = resources.getDimension(R.dimen.text_size_sub_header)

    private var mHeightInches: Float = 0.toFloat()
    private var mYDPI: Float = 0.toFloat()

    private var mPointerLocation = 100f

    var isMetric = false

    private var mAccentColor = ContextCompat.getColor(context, R.color.colorAccent)
    private var mPointerAlpha = 255

    internal var mPaint = Paint()
    internal var mTextPaint = Paint()

    val isPointerShown: Boolean
        get() = mPointerAlpha > 0

    /**
     * Returns opposite of current alpha value for visibility animations
     *
     * @return desired new alpha value
     */
    private val targetAlpha: Int
        get() = if (mPointerAlpha > 0) {
            0
        } else {
            255
        }

    /**
     * Sets the color of the pointer and inch markers
     *
     * @param accentColor new accent color
     */
    var accentColor: Int
        get() = mAccentColor
        @Keep
        set(accentColor) {
            mAccentColor = accentColor
            refreshView()
        }

    constructor(context: Context) : super(context) {
        initPaints()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initPaints()
        initAttributes(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
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
        val a = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.RulerView,
                0, 0)

        try {
            mAccentColor = a.getColor(R.styleable.RulerView_accentColor,
                    ContextCompat.getColor(getContext(), R.color.colorAccent))
            isMetric = a.getBoolean(R.styleable.RulerView_metric, false)
            val showPointer = a.getBoolean(R.styleable.RulerView_showPointer, true)
            if (!showPointer) {
                mPointerAlpha = 0
            }
        } finally {
            a.recycle()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        measureViewport()

        if (!isMetric) {
            drawStrokes(canvas)
        } else {
            drawMetricStrokes(canvas)
        }

        if (mPointerAlpha > 0) {
            drawPointer(canvas)
            drawPointerLabel(canvas)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (mPointerAlpha > 0) {
            //update pointer location
            mPointerLocation = event.y
            //refresh view
            this.invalidate()
            return true
        } else {
            return super.onTouchEvent(event)
        }
    }

    /**
     * Initialize paint for ruler strokes
     */
    private fun initPaints() {
        //Initialize paint properties for ruler strokes
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeWidth = DEFAULT_STROKE_WIDTH
        mPaint.isAntiAlias = false
        mPaint.color = ContextCompat.getColor(context, R.color.black)

        //Initialize paint properties for label text
        mTextPaint.style = Paint.Style.FILL
        mTextPaint.textSize = LABEL_TEXT_SIZE
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
            mYDPI = dm.ydpi
            mHeightInches = height / mYDPI
        } else {
            Log.d("Error", "View not in activity, skipping measurements")
        }
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
        while (i < mHeightInches) {
            updatePaintColor(i)

            val lineWidth = getLineWidth(i)
            val strokeLocation = i * mYDPI
            canvas.drawLine(0f, strokeLocation, lineWidth.toFloat(), strokeLocation, mPaint)

            drawLabel(canvas, i, lineWidth - LABEL_TEXT_SIZE, strokeLocation + LABEL_TEXT_SIZE / 2)

            i += 0.0625f
        }
    }

    private fun drawMetricStrokes(canvas: Canvas) {
        var i = 0f
        val heightMetric = (mHeightInches * 2.54).toFloat() * 10
        while (i < heightMetric) {
            updatePaintColor(i / 10)

            val lineWidth = getLineWidth(i / 10)
            val strokeLocation = (i.toDouble() / 10.0 / 2.54).toFloat() * mYDPI
            canvas.drawLine(0f, strokeLocation, lineWidth.toFloat(), strokeLocation, mPaint)

            drawLabel(canvas, i / 10, lineWidth - LABEL_TEXT_SIZE, strokeLocation + LABEL_TEXT_SIZE / 2)

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

        val maxWidth = width / 2

        if (inches.toDouble() == floor) {
            return maxWidth
        } else if (inches - 0.5 == floor) {
            return maxWidth / 2
        } else if (inches - 0.25 == floor || inches + 0.25 == ceiling) {
            return maxWidth / 4
        }
        return maxWidth / 8
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

            canvas.save()
            canvas.rotate(90f, x, y)
            canvas.drawText(label, x, y, mTextPaint)
            canvas.restore()
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
        val circleRadius = width / 8
        val lineX = width.toFloat() - (circleRadius * 2).toFloat() - MARGIN_OFFSET
        val circleX = width.toFloat() - circleRadius.toFloat() - MARGIN_OFFSET
        canvas.drawLine(0f, mPointerLocation, lineX, mPointerLocation, mPaint)
        canvas.drawCircle(circleX, mPointerLocation, circleRadius.toFloat(), mPaint)

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

        val pointerLabel: String

        if (!isMetric) {
            //Round to tenth place
            pointerLabel = String.format(Locale.getDefault(), "%.2f", mPointerLocation / mYDPI)
        } else {
            //Round to tenth place
            pointerLabel = String.format(Locale.getDefault(), "%.2f", mPointerLocation * 2.54 / mYDPI)
        }

        //Draw Label in circle
        val circleRadius = width / 8
        val x = width.toFloat() - circleRadius.toFloat() - MARGIN_OFFSET - LABEL_TEXT_SIZE / 3
        val y = if (pointerLabel.length > 4) mPointerLocation - LABEL_TEXT_SIZE / 4 else mPointerLocation
        canvas.save()
        canvas.rotate(90f, x, y)
        canvas.drawText(pointerLabel, x - LABEL_TEXT_SIZE, y, mTextPaint)//offset text to center in circle
        canvas.restore()

        //Revert paint attributes
        mTextPaint.color = ContextCompat.getColor(context, R.color.black)
        mTextPaint.alpha = 255
    }

    fun toggleMetric() {
        isMetric = !isMetric
        this.invalidate()
    }

    fun setShowPointer(show: Boolean) {
        setPointerAlpha(if (show) 255 else 0)
    }

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
    fun animateShowHidePointer() {
        val visAnim = ObjectAnimator
                .ofInt(this@RulerView, "pointerAlpha", mPointerAlpha, targetAlpha)
        visAnim.duration = ANIMATION_DURATION.toLong()
        visAnim.start()
    }

    /**
     * Sets accent color to new color with smooth animation
     *
     * @param accentColor desired new accent color
     */
    fun animateAccentColor(accentColor: Int) {
        val colorAnim = ObjectAnimator.ofInt(
                this@RulerView, "accentColor", mAccentColor, accentColor)
        colorAnim.setEvaluator(ArgbEvaluator())
        colorAnim.duration = ANIMATION_DURATION.toLong()
        colorAnim.start()
    }

    /**
     * Redraws the view onscreen, used for setting of custom attributes at runtime
     */
    private fun refreshView() {
        invalidate()
        requestLayout()
    }

    companion object {
        val ANIMATION_DURATION = 200
    }
}
