package abcmeasurecorp.com.measureit.activities

import abcmeasurecorp.com.measureit.R
import abcmeasurecorp.com.measureit.view.RulerView
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

/**
 * Created by Joel Anderson on 12/27/18.
 *
 *
 * Main Activity class, whose layout contains the ruler view
 * and all additional interactive views.
 */

class MainActivity : AppCompatActivity() {

    private lateinit var mPrefs: SharedPreferences
    private var mCurrentColor: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initUserPreferences()
    }

    private fun initViews() {
        pointerToggle.setOnClickListener { togglePointer() }
        unitsToggle.setOnClickListener { toggleUnits() }
        colorButton.setOnClickListener { showDialog() }
    }

    private fun initUserPreferences() {
        mPrefs = getPreferences(Context.MODE_PRIVATE)
        val showPointer = mPrefs.getBoolean(getString(R.string.ruler_show_pointer_pref_key), true)

        val isMetric: Boolean
        val metricKey = getString(R.string.ruler_is_metric_pref_key)
        isMetric = if (intent.extras != null && intent?.extras!!.containsKey(metricKey)) {
            intent?.extras!!.getBoolean(metricKey)
        } else mPrefs.getBoolean(metricKey, false)

        displayPreferences(showPointer, isMetric)
    }

    private fun displayPreferences(showPointer: Boolean, isMetric: Boolean) {
        mCurrentColor = mPrefs.getInt(getString(R.string.ruler_color_pref_key),
                ContextCompat.getColor(this@MainActivity, R.color.colorAccent))

        ruler.setShowPointer(showPointer)
        ruler.isMetric = isMetric
        ruler.accentColor = mCurrentColor
//        right_container.setBackgroundColor(mCurrentColor)

//        toggle_pointer_button.text = if (showPointer) getString(R.string.button_hide_pointer)
//        else getString(R.string.button_show_pointer)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && Build.VERSION.SDK_INT >= KITKAT) setImmersiveMode()
    }

    /**
     * Toggles units of ruler, toggles text on units button
     */
    private fun toggleUnits() {
        val editor = mPrefs.edit()
        editor.putBoolean(getString(R.string.ruler_is_metric_pref_key), !ruler.isMetric)
        editor.apply()
        ruler.toggleMetric()
    }

    /**
     * Toggles visibility of pointer, toggles text on pointer visibility button
     */
    private fun togglePointer() {
        val editor = mPrefs.edit()
        editor.putBoolean(getString(R.string.ruler_show_pointer_pref_key), !ruler.isPointerShown)
        editor.apply()

//        toggle_pointer_button.text = if (ruler.isPointerShown) getString(R.string.button_show_pointer)
//        else getString(R.string.button_hide_pointer)
        ruler.animateShowHidePointer()
    }

    /**
     * Creates random color, sets right container background and ruler accent colors
     */
    private fun setRandomColor() {
        val rnd = Random()
        mCurrentColor = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))

        animateBackgroundColor(mCurrentColor)
        ruler.animateAccentColor(mCurrentColor)
        saveColorSelection()
    }

    /**
     * Changes the background color of the right container with animation
     *
     * @param color desired new background color
     */
    private fun animateBackgroundColor(color: Int) = ObjectAnimator
            .ofInt(right_container, "backgroundColor", ruler.accentColor, color)
            .apply {
                duration = RulerView.ANIMATION_DURATION
                setEvaluator(ArgbEvaluator())
            }
            .start()

    /**
     * Sets fullscreen mode, hides system bars
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun setImmersiveMode() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showDialog() {
        mCurrentColor = ContextCompat.getColor(this@MainActivity, R.color.colorAccent)
        val dialog = showColorDialog()

        val button = dialog.findViewById<AppCompatImageView>(R.id.choose_color)
        val colorSpectrum = dialog.findViewById<ImageView>(R.id.color_spectrum)

        if (colorSpectrum != null && button != null) {
            val bitmap = (colorSpectrum.drawable as BitmapDrawable).bitmap
            colorSpectrum.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_MOVE -> handleColorSelection(event, colorSpectrum, bitmap, button)
                    MotionEvent.ACTION_UP -> view.performClick()
                }
                true
            }

            button.setOnClickListener { onColorSelected(dialog) }
        }
    }

    private fun showColorDialog() = AlertDialog.Builder(this)
            .setView(R.layout.color_picker)
            .setNegativeButton(R.string.cancel, null)
            .setNeutralButton(R.string.random_color
            ) { _, _ -> setRandomColor() }
            .show()

    private fun onColorSelected(dialog: AlertDialog) {
        animateBackgroundColor(mCurrentColor)
        ruler.animateAccentColor(mCurrentColor)
        dialog.cancel()
        saveColorSelection()
    }

    private fun handleColorSelection(event: MotionEvent,
                                     colorSpectrum: ImageView,
                                     bitmap: Bitmap,
                                     button: AppCompatImageView) {
        val pixel = getSelectedPixel(event, colorSpectrum, bitmap)
        if (pixel != 0) {
            mCurrentColor = Color.argb(
                    255, Color.red(pixel), Color.green(pixel), Color.blue(pixel))
            button.background.setColorFilter(mCurrentColor, PorterDuff.Mode.SRC_ATOP)
        }
    }

    private fun getSelectedPixel(event: MotionEvent,
                                 colorSpectrum: ImageView, bitmap: Bitmap): Int {
        val inverse = Matrix()
        colorSpectrum.imageMatrix.invert(inverse)
        val touchPoint = floatArrayOf(event.x, event.y)
        inverse.mapPoints(touchPoint)
        var currentX = touchPoint[0].toInt()
        var currentY = touchPoint[1].toInt()

        if (currentX < 0) currentX = 0
        if (currentX > bitmap.width - 1) currentX = bitmap.width - 1

        if (currentY < 0) currentY = 0
        if (currentY > bitmap.height - 1) currentY = bitmap.height - 1

        return bitmap.getPixel(currentX, currentY)
    }

    private fun saveColorSelection() = mPrefs
            .edit()
            .putInt(getString(R.string.ruler_color_pref_key), mCurrentColor)
            .apply()
}
