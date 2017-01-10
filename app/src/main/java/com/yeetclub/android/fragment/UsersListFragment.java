package com.yeetclub.android.fragment;

/**
 * Created by @santafebound on 2016-09-29.
 */

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
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
import com.yalantis.phoenix.PullToRefreshView;
import com.yeetclub.android.R;
import com.yeetclub.android.adapter.UsersListAdapter;
import com.yeetclub.android.parse.ParseConstants;
import com.yeetclub.android.utility.DividerItemDecoration;
import com.yeetclub.android.utility.NetworkHelper;

import java.util.List;

public class UsersListFragment extends Fragment {

    private static final String TAG = "UsersListFragment";
    private static final String KEY_LAYOUT_MANAGER = "layoutManager";

    private enum LayoutManagerType {
        LINEAR_LAYOUT_MANAGER
    }

    protected LayoutManagerType mCurrentLayoutManagerType;

    protected RecyclerView mRecyclerView;
    protected RecyclerView.LayoutManager mLayoutManager;

    protected List<ParseUser> mUsers;
    protected PullToRefreshView mSwipeRefreshLayout;

    public UsersListFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Is the network online?
        boolean isOnline = NetworkHelper.isOnline(getContext());

        // Initialize dataset from remote server
        retrieveUsers(isOnline);
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Set view
        View rootView = inflater.inflate(R.layout.tab_fragment_1, container, false);
        rootView.setTag(TAG);

        // Initialize SwipeRefreshLayout
        mSwipeRefreshLayout = (PullToRefreshView) rootView.findViewById(R.id.swipeRefreshLayout);

        // Is the network online?
        boolean isOnline = NetworkHelper.isOnline(getContext());

        rootView.setFocusableInTouchMode(true);
        rootView.requestFocus();

        // Return to first fragment on back press
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

        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            if (!isOnline) {
                mSwipeRefreshLayout.setRefreshing(false);
                Toast.makeText(getContext(), getString(R.string.cannot_retrieve_messages), Toast.LENGTH_SHORT).show();
            } else {
                // Retrieve new notifications
                retrieveUsers(true);
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
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save currently selected layout manager.
        savedInstanceState.putSerializable(KEY_LAYOUT_MANAGER, mCurrentLayoutManagerType);
        super.onSaveInstanceState(savedInstanceState);
    }

    private void retrieveUsers(boolean isOnline) {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo(ParseConstants.KEY_GROUP_ID, ParseUser.getCurrentUser().getString(ParseConstants.KEY_GROUP_ID));
        query.addAscendingOrder(ParseConstants.KEY_USERNAME);
        if (!isOnline) {
            query.fromLocalDatastore();
        }
        query.findInBackground((users, e) -> {

            mSwipeRefreshLayout.setRefreshing(false);

            if (e == null) {

                // We found messages!
                mUsers = users;
                ParseObject.pinAllInBackground(mUsers);
                /*System.out.println(yeets);*/

                UsersListAdapter adapter = new UsersListAdapter(getContext(), users);
                adapter.setHasStableIds(true);
                mRecyclerView.setHasFixedSize(true);
                adapter.notifyDataSetChanged();
                mRecyclerView.setAdapter(adapter);

                mSwipeRefreshLayout.setOnRefreshListener(() -> {
                    if (!isOnline) {
                        mSwipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(getContext(), getString(R.string.cannot_retrieve_messages), Toast.LENGTH_SHORT).show();
                    } else {
                        retrieveUsers(true);
                    }
                });
            }
        });
    }
}
