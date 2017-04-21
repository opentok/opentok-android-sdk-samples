# Project 7: Multiparty using ConstraintLayout

Note: Read the README.md file in the Project 1 folder before starting here.

Sample #6 shows a simple multiparty app which uses basic layouts to display the different video
views of the participants.

However, when building a complex app, there is a better option in order to lay out the views:
the relatively new `ViewGroup` called `ConstraintLayout`. Using this layout has several benefits.
One  is that you can use a single layout to specify all the view positions instead of having to
nest several different layouts.

To use this sample application, install the ConstraintLayout for Android. In the Android Studio
SDK Manager command, navigate to Appearance & Behavior > System Settings > Android SDK. Then click
the SDK Tools tab and install ConstraintLayout for Android (listed under Support Repository).

## Using Constraint Layout to display the views

In order to specify the layout parameters, the application uses the `ConstraintSet` class.
This class lets you specify the relations between two views in the form of _view A is above
view B_ or _view A is the same size as view B_.

For example, to specify that a view is above another view, the application uses a helper method
inside the `ConstraintSetHelper` class:

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

Once all constraints are added to the `ConstraintSet`, we apply them to the ConstraintLayout:

```java
public void applyToLayout(ConstraintLayout layout, boolean animated) {
    if (animated) {
        TransitionManager.beginDelayedTransition(layout);
    }
    set.applyTo(layout);
}
```

See the `ConstraintSetHelper` class for more details about how to set up the constraints for different view positions.

In order to actually position the views, the application calculates the constraints whenever a new
view is added or removed, in callback methods like the `SessionsListener.onStreamReceived()` method.
Those callback add the view to the container and call `calculateLayout()`, which includes logic for
view positioning.

In this sample we have four possible layouts:

1. The publisher is alone, so it will be displayed fullscreen.

2. There is just one subscriber. In this case the screen is split in two. The publisher will be
   displayed above the subscriber.

3. There are more than one subscriber, and they are an even number. In this case, the application
  displays the Publisher using the full width of the container above all subscribers, and
  subscribers are displayed in rows two by two.

4. There are more than one subscriber, and they are an odd number. In this case, the container space
   is like a grid and all views will be displayed two by two, with the publisher in the upper-left
   corner.

The `calculateLayout()` method implements this logic. For example, for the second case (where there
is just one subscriber), here is a simplified version of the code:

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

See the code itself for the other cases.

## Next steps

* See the Android [developers guide](https://developer.android.com/training/constraint-layout)
for Constraint Layout

* For details on the full OpenTok Android API, see the [reference
documentation](https://tokbox.com/opentok/libraries/client/android/reference/index.html).
