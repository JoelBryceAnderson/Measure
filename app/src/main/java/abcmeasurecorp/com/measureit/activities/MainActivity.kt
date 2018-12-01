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
    private var mNightMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initUserPreferences()
    }

    private fun initViews() {
        pointerToggle.setOnClickListener { togglePointer() }
        unitsToggle.setOnClickListener { toggleUnits() }
        nightModeButton.setOnClickListener { toggleNightMode() }
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
        mNightMode = mPrefs.getBoolean(getString(R.string.ruler_show_pointer_pref_key), false)

        ruler.setShowPointer(showPointer)
        ruler.isMetric = isMetric
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
        ruler.animateShowHidePointer()
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

    private fun toggleNightMode() {
        saveNightModePreference(!mNightMode)
    }

    private fun saveNightModePreference(nightMode: Boolean) = mPrefs
            .edit()
            .putBoolean(getString(R.string.ruler_night_mode_key), nightMode)
            .apply()
}
