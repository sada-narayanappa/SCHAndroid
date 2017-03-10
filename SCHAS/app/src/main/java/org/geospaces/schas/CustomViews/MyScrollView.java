package org.geospaces.schas.CustomViews;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ScrollView;

import org.geospaces.schas.R;

/**
 * Created by Erik on 2/26/2017.
 */

public class MyScrollView extends ScrollView {

    private int scrollY = 0;

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MyScrollView);
        scrollY = a.getInt(R.styleable.MyScrollView_scrollY, 0);
        a.recycle();
    }

    public MyScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    public MyScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public MyScrollView(Context context) {
        super(context);
    }

    @Override
    public void computeScroll() {
        if (isInEditMode()) {
            scrollTo(0, scrollY);
        }
    }

}