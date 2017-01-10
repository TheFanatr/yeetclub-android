package com.yeetclub.android.fragment;

/**
 * Created by @santafebound on 2016-09-29.
 */

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.yalantis.phoenix.PullToRefreshView;
import com.yeetclub.android.R;
import com.yeetclub.android.adapter.FeedAdapter;
import com.yeetclub.android.parse.ParseConstants;
import com.yeetclub.android.utility.DividerItemDecoration;
import com.yeetclub.android.utility.NetworkHelper;

import java.util.Date;
import java.util.List;

public class FeedFragment extends Fragment {

    private static final String TAG = "FeedFragment";
    private static final String KEY_LAYOUT_MANAGER = "layoutManager";

    private enum LayoutManagerType {
        LINEAR_LAYOUT_MANAGER
    }

    protected LayoutManagerType mCurrentLayoutManagerType;

    protected RecyclerView mRecyclerView;
    protected RecyclerView.LayoutManager mLayoutManager;

    protected List<ParseObject> mYeets;
    protected PullToRefreshView mSwipeRefreshLayout;

    public FeedFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Is the network online?
        boolean isOnline = NetworkHelper.isOnline(getContext());

        // Retrieve Data from remote server
        retrieveData(isOnline);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Set view
        View rootView = inflater.inflate(R.layout.tab_fragment_1, container, false);
        rootView.setTag(TAG);

        // Initialize SwipeRefreshLayout
        mSwipeRefreshLayout = (PullToRefreshView) rootView.findViewById(R.id.swipeRefreshLayout);

        // Set RecyclerView layout
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

    private void retrieveData(boolean isOnline) {
        String groupId = ParseUser.getCurrentUser().getString("groupId");
        ParseQuery<ParseObject> query = new ParseQuery<>(ParseConstants.CLASS_YEET);
        query.whereContains(ParseConstants.KEY_GROUP_ID, groupId);
        query.orderByDescending("lastReplyUpdatedAt");
        query.setLimit(1000);
        if (!isOnline) {
            query.fromLocalDatastore();
        }
        query.findInBackground((yeets, e) -> {

            mSwipeRefreshLayout.setRefreshing(false);

            if (e == null) {

                // We found messages!
                mYeets = yeets;
                ParseObject.pinAllInBackground(mYeets);
                /*System.out.println(yeets);*/

                FeedAdapter adapter = new FeedAdapter(getContext(), yeets);
                adapter.setHasStableIds(true);
                mRecyclerView.setHasFixedSize(true);
                adapter.notifyDataSetChanged();
                mRecyclerView.setAdapter(adapter);

                mSwipeRefreshLayout.setOnRefreshListener(() -> {
                    if (!isOnline) {
                        mSwipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(getContext(), getString(R.string.cannot_retrieve_messages), Toast.LENGTH_SHORT).show();
                    } else {
                        Date onRefreshDate = new Date();
                        /*System.out.println(onRefreshDate.getTime());*/
                        refreshData(onRefreshDate, adapter);
                    }
                });
            }
        });
    }

    private void refreshData(Date date, FeedAdapter adapter) {
        String groupId = ParseUser.getCurrentUser().getString("groupId");
        ParseQuery<ParseObject> query = new ParseQuery<>(ParseConstants.CLASS_YEET);
        query.whereContains(ParseConstants.KEY_GROUP_ID, groupId);
        query.orderByDescending("lastReplyUpdatedAt");
        if (date != null)
            query.whereLessThanOrEqualTo("createdAt", date);
        query.setLimit(1000);
        query.findInBackground((yeets, e) -> {

            mSwipeRefreshLayout.setRefreshing(false);

            if (e == null) {
                // We found messages!
                mYeets.removeAll(yeets);
                mYeets.addAll(0, yeets); //This should append new messages to the top
                adapter.notifyDataSetChanged();
                ParseObject.pinAllInBackground(mYeets);

                /*System.out.println(yeets);*/
                if (mRecyclerView.getAdapter() == null) {
                    adapter.setHasStableIds(true);
                    mRecyclerView.setHasFixedSize(true);
                    adapter.notifyDataSetChanged();
                    mRecyclerView.setAdapter(adapter);
                } else {
                    adapter.notifyDataSetChanged();
                }
            }
        });
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
}
