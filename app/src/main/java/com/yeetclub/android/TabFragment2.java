package com.yeetclub.android;

/**
 * Created by @santafebound on 2016-09-29.
 */

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.yeetclub.notifications.NotificationsAdapter;
import com.yeetclub.parse.ParseConstants;
import com.yeetclub.utility.NetworkHelper;

import java.util.List;

public class TabFragment2 extends Fragment {

    private static final String TAG = "RecyclerViewFragment";
    private static final String KEY_LAYOUT_MANAGER = "layoutManager";

    private enum LayoutManagerType {
        LINEAR_LAYOUT_MANAGER
    }

    protected LayoutManagerType mCurrentLayoutManagerType;

    protected RecyclerView mRecyclerView;
    protected RecyclerView.LayoutManager mLayoutManager;

    protected List<ParseObject> mNotifications;
    protected SwipeRefreshLayout mSwipeRefreshLayout;

    public TabFragment2() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize dataset from remote server
        retrieveNotifications();
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Set view
        View rootView = inflater.inflate(R.layout.tab_fragment_2, container, false);
        rootView.setTag(TAG);

        // Initialize SwipeRefreshLayout
        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeRefreshLayout);

        // Is the network online?
        boolean isOnline = NetworkHelper.isOnline(getContext());

        // Hide or show views associated with network state
        rootView.findViewById(R.id.networkOfflineText).setVisibility(isOnline ? View.GONE : View.VISIBLE);
        rootView.findViewById(R.id.rl).setVisibility(isOnline ? View.GONE : View.VISIBLE);

        rootView.setFocusableInTouchMode(true);
        rootView.requestFocus();

        rootView.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    ViewPager viewPager = (ViewPager) getActivity().findViewById(R.id.pager);
                    viewPager.setCurrentItem(0, true);
                    return true;
                }
            }
            return false;
        });

        // Retrieve Data from Parse
        retrieveNotifications(rootView);

        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            if (!isOnline) {
                mSwipeRefreshLayout.setRefreshing(false);
            } else {
                // Set all current notifications to "read"
                setReadState();

                // Retrieve new notifications
                retrieveNotifications(rootView);
            }
        });

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);

        // LinearLayoutManager is used here, this will layout the elements in a similar fashion
        // to the way ListView would layout elements. The RecyclerView.LayoutManager defines how
        // elements are laid out.
        mLayoutManager = new LinearLayoutManager(getActivity());

        mCurrentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER;

        if (savedInstanceState != null) {
            // Restore saved layout manager type.
            mCurrentLayoutManagerType = (LayoutManagerType) savedInstanceState
                    .getSerializable(KEY_LAYOUT_MANAGER);
        }
        setRecyclerViewLayoutManager(mCurrentLayoutManagerType);

        setRecyclerViewLayoutManager(LayoutManagerType.LINEAR_LAYOUT_MANAGER);

        return rootView;
    }

    private void setReadState() {
        ParseQuery<ParseObject> query = new ParseQuery<>(ParseConstants.CLASS_NOTIFICATIONS);
        query.whereEqualTo(ParseConstants.KEY_RECIPIENT_ID, ParseUser.getCurrentUser().getObjectId());
        query.whereEqualTo(ParseConstants.KEY_READ_STATE, false);
        query.findInBackground((notifications, e) -> {

            if (mSwipeRefreshLayout.isRefreshing()) {
                mSwipeRefreshLayout.setRefreshing(false);
            }

            if (e == null) {

                for (ParseObject notificationObject : notifications) {
                    // System.out.println(notificationObject);
                    if (!(notificationObject.getBoolean(ParseConstants.KEY_READ_STATE))) {
                        notificationObject.put(ParseConstants.KEY_READ_STATE, true);
                        notificationObject.saveInBackground();
                    }

                }

            } else {
                Toast.makeText(getContext(), R.string.could_not_retrieve, Toast.LENGTH_SHORT).show();
                /*Log.w(getContext().toString(), e);*/
            }
        });
    }

    private void showViews(View rootView) {
        rootView.findViewById(R.id.noNotificationsMessage).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.noNotificationsMessage2).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.noNotificationsImage).setVisibility(View.VISIBLE);
    }

    public void setRecyclerViewLayoutManager(LayoutManagerType layoutManagerType) {
        int scrollPosition = 0;

        // If a layout manager has already been set, get current scroll position.
        if (mRecyclerView.getLayoutManager() != null) {
            scrollPosition = ((LinearLayoutManager) mRecyclerView.getLayoutManager())
                    .findFirstCompletelyVisibleItemPosition();
        }

        switch (layoutManagerType) {
            case LINEAR_LAYOUT_MANAGER:
                mLayoutManager = new LinearLayoutManager(getActivity());
                mCurrentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER;
                break;
            default:
                mLayoutManager = new LinearLayoutManager(getActivity());
                mCurrentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER;
        }

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.scrollToPosition(scrollPosition);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save currently selected layout manager.
        savedInstanceState.putSerializable(KEY_LAYOUT_MANAGER, mCurrentLayoutManagerType);
        super.onSaveInstanceState(savedInstanceState);
    }

    private void retrieveNotifications(View rootView) {
        ParseQuery<ParseObject> query = new ParseQuery<>(ParseConstants.CLASS_NOTIFICATIONS);
        query.whereEqualTo(ParseConstants.KEY_RECIPIENT_ID, ParseUser.getCurrentUser().getObjectId());
        query.addDescendingOrder(ParseConstants.KEY_CREATED_AT);
        query.findInBackground((notifications, e) -> {

            if (mSwipeRefreshLayout.isRefreshing()) {
                mSwipeRefreshLayout.setRefreshing(false);
            }

            if (e == null) {

                // We found messages!
                mNotifications = notifications;

                if (notifications.isEmpty()) {
                    showViews(rootView);
                }

                NotificationsAdapter adapter = new NotificationsAdapter(getContext(), notifications);
                adapter.setHasStableIds(true);
                mRecyclerView.setHasFixedSize(true);
                adapter.notifyDataSetChanged();
                mRecyclerView.setAdapter(adapter);

            }
        });
    }

    private void retrieveNotifications() {
        ParseQuery<ParseObject> query = new ParseQuery<>(ParseConstants.CLASS_NOTIFICATIONS);
        query.whereEqualTo(ParseConstants.KEY_RECIPIENT_ID, ParseUser.getCurrentUser().getObjectId());
        query.addDescendingOrder(ParseConstants.KEY_CREATED_AT);
        query.findInBackground((notifications, e) -> {

            if (mSwipeRefreshLayout.isRefreshing()) {
                mSwipeRefreshLayout.setRefreshing(false);
            }

            if (e == null) {

                // We found messages!
                mNotifications = notifications;

                NotificationsAdapter adapter = new NotificationsAdapter(getContext(), notifications);
                adapter.setHasStableIds(true);
                mRecyclerView.setHasFixedSize(true);
                adapter.notifyDataSetChanged();
                mRecyclerView.setAdapter(adapter);

            }
        });
    }
}
