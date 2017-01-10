package com.yeetclub.android.activity;

import com.parse.ui.ParseLoginDispatchActivity;

/**
 * Created by @santafebound on 2015-11-07.
 */
public class DispatchActivity extends ParseLoginDispatchActivity {

    @Override
    protected Class<?> getTargetClass() {
        return MainActivity.class;
    }
}