# Project 7: Multiparty using ConstraintLayout

Note: Read the README.md file in the Project 1 folder before starting here.

In the Project of the Sample #6 we built a simple multiparty app which uses basic Layouts in order to display the different video views of the participants.

However, when building a complex app, there is a better option in order to lay out the views. The recommended way is to use the relatively new `ViewGroup` called `ConstraintLayout`. Using this layout has several benefits, one of them is that you can use a single layout to specify all the view positions instead of having to nest several different layouts.

## Using Constraint Layout to display the views

In order to specify the layout parameters, we will use the sdk class `ConstraintSet`, that class will allow us to specify the relations between two views in the form of, _view A is above view B_, or _view A is the same size of view B_.

For example, to specify that a view is above another view, we have built a helper method inside the `ConstraintSetHelper` class with this code:

```java
public void layoutViewAboveView(int aboveViewId, int belowViewId) {
    //  above
    //  below

    set.constrainHeight(aboveViewId, 0);
    set.constrainHeight(belowViewId, 0);
    set.connect(aboveViewId, ConstraintSet.BOTTOM, belowViewId, ConstraintSet.TOP);
    set.connect(belowViewId, ConstraintSet.TOP, aboveViewId, ConstraintSet.BOTTOM);
}
```

Once we have all our constraints added to the `ConstraintSet`, we will apply them to the ConstraintLayout itself. We do that using this code:

```java
public void applyToLayout(ConstraintLayout layout, boolean animated) {
    if (animated) {
        TransitionManager.beginDelayedTransition(layout);
    }
    set.applyTo(layout);
}
```

See `ConstraintSetHelper` class for more details about how we set up the constraints for different view positions.

In order to actually position the views, we need to calculate the constraints whenever a new view is added or removed, that happens in the OpenTok SDK callbacks like Session's listener `onStreamReceived`. In those listener methods we just add the view to the container and call `calculateLayout()` which is where all the logic of view positioning happens.

In this sample we have 4 possible lay outs:
1. The publisher is alone, so it will be displayed in full screen
2. There are just one subscriber. In this case the screen will be splitted in two, publisher will be above and subscriber below
3. There are more than one subscriber and they are an even number. In that case we will display the Publisher ocuppying all the container wide above all subscribers and subscribers will be displayed in rows two by two.
4. There are more than one subscriber and they are an odd number. In that case the container space will be set like a grid and all views will be displayed two by two leaving the publisher in the upper left corner.

All that code is inside `calculateLayout()` method. For example, this is the simplified code just showing what happens for case number 2)

```java
private void calculateLayout() {
    ConstraintSetHelper set = new ConstraintSetHelper(R.id.main_container);

    int size = mSubscribers.size();
    if (size == 1) {
        // Publisher
        // Subscriber
        set.layoutViewAboveView(R.id.publisher_view_id, getResIdForSubscriberIndex(0));
        set.layoutViewWithTopBound(R.id.publisher_view_id, R.id.main_container);
        set.layoutViewWithBottomBound(getResIdForSubscriberIndex(0), R.id.main_container);
        set.layoutViewAllContainerWide(R.id.publisher_view_id, R.id.main_container);
        set.layoutViewAllContainerWide(getResIdForSubscriberIndex(0), R.id.main_container);
    }
    set.applyToLayout(mContainer, true);
}
```

Please refer to the code itself to see the rest of the cases.

## Next steps

* See Android Developers [guide](https://developer.android.com/training/constraint-layout/index.html) to Constraint Layout
* For details on the full OpenTok Android API, see the [reference
documentation](https://tokbox.com/opentok/libraries/client/android/reference/index.html).
