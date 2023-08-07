package com.example.alarmapp
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout

class FadeAwayBehavior(context: Context, attrs: AttributeSet) :
    CoordinatorLayout.Behavior<TextView>(context, attrs) {

    private var startValue = 1.0f // Initial alpha value (fully opaque)
    private var endValue = 0.0f   // Final alpha value (fully transparent)



    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: TextView,
        dependency: View
    ): Boolean {
        // Listen to changes in the AppBarLayout (the collapsing toolbar)
        return dependency is AppBarLayout
    }

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: TextView,
        dependency: View
    ): Boolean {


        // Calculate the percentage of collapse based on the AppBarLayout's scroll range
        val collapsePercentage = (-dependency.y / dependency.height)*3

        // Calculate the alpha value based on the collapsePercentage
        val alphaValue = startValue + collapsePercentage * (endValue - startValue)
        // Set the alpha value to the text view
        child.alpha = alphaValue


        // Calculate the translation value based on the collapsePercentage (optional)
        val translationValue = collapsePercentage * child.height
        child.translationY = -translationValue





        return true
    }
}

class FadeInBehavior(context: Context, attrs: AttributeSet) :
    CoordinatorLayout.Behavior<TextView>(context, attrs) {

    private var isScrollingDown = false
    private val startValue = 0.0f // Initial alpha value (fully transparent)
    private val endValue = 1.0f   // Final alpha value (fully opaque)

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: TextView,
        dependency: View
    ): Boolean {
        // Listen to changes in the AppBarLayout (the collapsing toolbar)
        return dependency is AppBarLayout
    }

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: TextView,
        dependency: View
    ): Boolean {
        // Calculate the percentage of collapse based on the total scroll range
        val collapsePercentage = -dependency.y / dependency.height

        // Calculate the alpha value based on the collapsePercentage
        val alphaValue = startValue + collapsePercentage * (endValue - startValue)

        // Set the alpha value to the AppBarLayout title
        child.alpha = alphaValue

        val newScrollingDown = dependency.y > (dependency.layoutParams.height / 2)
        if (newScrollingDown != isScrollingDown) {
            // Only request layout if the scroll direction changes to avoid unnecessary layout pass
            isScrollingDown = newScrollingDown
            child.requestLayout()
        }

        return true
    }
}


