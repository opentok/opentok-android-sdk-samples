package com.tokbox.sample.multipartyconstraintlayout

import android.os.Build
import android.transition.TransitionManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet

class ConstraintSetHelper(containerViewId: Int) {
    private val set = ConstraintSet()

    private val container = containerViewId

    fun layoutViewAboveView(aboveViewId: Int, belowViewId: Int) {
        //  above
        //  below
        set.constrainHeight(aboveViewId, 0)
        set.constrainHeight(belowViewId, 0)
        set.connect(aboveViewId, ConstraintSet.BOTTOM, belowViewId, ConstraintSet.TOP)
        set.connect(belowViewId, ConstraintSet.TOP, aboveViewId, ConstraintSet.BOTTOM)
    }

    fun layoutViewWithTopBound(viewId: Int, topBound: Int) {
        set.connect(viewId, ConstraintSet.TOP, topBound, ConstraintSet.TOP)
    }

    fun layoutViewWithBottomBound(viewId: Int, bottomBound: Int) {
        set.connect(viewId, ConstraintSet.BOTTOM, bottomBound, ConstraintSet.BOTTOM)
    }

    private fun layoutViewNextToView(leftViewId: Int, rightViewId: Int, leftBoundViewId: Int, rightBoundViewId: Int) {
        // leftBound | leftView | rightView | rightBound
        set.constrainWidth(leftViewId, 0)
        set.constrainWidth(rightViewId, 0)
        set.connect(leftViewId, ConstraintSet.LEFT, leftBoundViewId, ConstraintSet.LEFT)
        set.connect(leftViewId, ConstraintSet.RIGHT, rightViewId, ConstraintSet.LEFT)
        set.connect(rightViewId, ConstraintSet.RIGHT, rightBoundViewId, ConstraintSet.RIGHT)
        set.connect(rightViewId, ConstraintSet.LEFT, leftViewId, ConstraintSet.RIGHT)
    }

    private fun layoutViewOccupyingAllRow(viewId: Int, leftBoundViewId: Int, rightBoundViewId: Int) {
        // leftBound | view | rightBound
        set.constrainWidth(viewId, 0)
        set.connect(viewId, ConstraintSet.LEFT, leftBoundViewId, ConstraintSet.LEFT)
        set.connect(viewId, ConstraintSet.RIGHT, rightBoundViewId, ConstraintSet.RIGHT)
    }

    fun layoutTwoViewsOccupyingAllRow(leftViewId: Int, rightViewId: Int) {
        layoutViewNextToView(leftViewId, rightViewId, container, container)
    }

    fun layoutViewAllContainerWide(viewId: Int, containerId: Int) {
        layoutViewOccupyingAllRow(viewId, containerId, containerId)
    }

    fun applyToLayout(layout: ConstraintLayout?, animated: Boolean) {
        if (animated) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                TransitionManager.beginDelayedTransition(layout)
            }
        }
        set.applyTo(layout)
    }

    fun layoutViewFullScreen(viewId: Int) {
        layoutViewAllContainerWide(viewId, container)
        layoutViewWithTopBound(viewId, container)
        layoutViewWithBottomBound(viewId, container)
    }

    fun layoutViewHeightPercent(viewId: Int, value: Float) {
        set.constrainPercentHeight(viewId, value)
    }

    fun layoutViewWidthPercent(viewId: Int, value: Float) {
        set.constrainPercentWidth(viewId, value)
    }
}