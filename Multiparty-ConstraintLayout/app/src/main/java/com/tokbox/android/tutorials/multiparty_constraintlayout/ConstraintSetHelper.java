package com.tokbox.android.tutorials.multiparty_constraintlayout;

import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.transition.TransitionManager;

public class ConstraintSetHelper {
    private ConstraintSet set;
    private int container;

    public ConstraintSetHelper(int containerViewId) {
        set = new ConstraintSet();
        container = containerViewId;
    }

    public void layoutViewAboveView(int aboveViewId, int belowViewId) {
        //  above
        //  below

        set.constrainHeight(aboveViewId, 0);
        set.constrainHeight(belowViewId, 0);
        set.connect(aboveViewId, ConstraintSet.BOTTOM, belowViewId, ConstraintSet.TOP);
        set.connect(belowViewId, ConstraintSet.TOP, aboveViewId, ConstraintSet.BOTTOM);
    }

    public void layoutViewWithTopBound(int viewId, int topBound) {
        set.connect(viewId, ConstraintSet.TOP, topBound, ConstraintSet.TOP);
    }

    public void layoutViewWithBottomBound(int viewId, int bottomBound) {
        set.connect(viewId, ConstraintSet.BOTTOM, bottomBound, ConstraintSet.BOTTOM);
    }

    private void layoutViewNextToView(int leftViewId, int rightViewId, int leftBoundViewId, int rightBoundViewId) {
        // leftBound | leftView | rightView \ rightBound

        set.constrainWidth(leftViewId, 0);
        set.constrainWidth(rightViewId, 0);

        set.connect(leftViewId, ConstraintSet.LEFT, leftBoundViewId, ConstraintSet.LEFT);
        set.connect(leftViewId, ConstraintSet.RIGHT, rightViewId, ConstraintSet.LEFT);

        set.connect(rightViewId, ConstraintSet.RIGHT, rightBoundViewId, ConstraintSet.RIGHT);
        set.connect(rightViewId, ConstraintSet.LEFT, leftViewId, ConstraintSet.RIGHT);
    }

    private void layoutViewOccupyingAllRow(int viewId, int leftBoundViewId, int rightBoundViewId) {
        // leftBound | view | rightBound
        set.constrainWidth(viewId, 0);

        set.connect(viewId, ConstraintSet.LEFT, leftBoundViewId, ConstraintSet.LEFT);
        set.connect(viewId, ConstraintSet.RIGHT, rightBoundViewId, ConstraintSet.RIGHT);
    }

    public void layoutTwoViewsOccupyingAllRow(int leftViewId, int rightViewId) {
        layoutViewNextToView(leftViewId, rightViewId, container, container);
    }

    public void layoutViewAllContainerWide(int viewId, int containerId) {
        layoutViewOccupyingAllRow(viewId, containerId, containerId);
    }

    public void applyToLayout(ConstraintLayout layout, boolean animated) {
        if (animated) {
            TransitionManager.beginDelayedTransition(layout);
        }
        set.applyTo(layout);
    }

    public void layoutViewFullScreen(int viewId) {
        layoutViewAllContainerWide(viewId, container);
        layoutViewWithTopBound(viewId, container);
        layoutViewWithBottomBound(viewId, container);
    }
}
