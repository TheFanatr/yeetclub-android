package com.yeetclub.android.adapter;

/**
 * Created by @santafebound on 2016-09-29.
 */

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.yeetclub.android.fragment.FeedFragment;
import com.yeetclub.android.fragment.NotificationsFragment;
import com.yeetclub.android.fragment.UsersListFragment;

public class FragmentPagerAdapter extends FragmentStatePagerAdapter {
    private int mNumOfTabs;

    public FragmentPagerAdapter(FragmentManager fm, int NumOfTabs) {
        super(fm);
        this.mNumOfTabs = NumOfTabs;
    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:
                return new FeedFragment();
            case 1:
                return new NotificationsFragment();
            case 2:
                return new UsersListFragment();
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }
}