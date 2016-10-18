package com.yeetclub.android;

/**
 * Created by @santafebound on 2016-09-29.
 */

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.yeetclub.feed.FeedAdapter;
import com.yeetclub.parse.ParseConstants;
import com.yeetclub.utility.NetworkHelper;

import java.util.Date;
import java.util.List;

public class TabFragment1 extends Fragment {

    private static final String TAG = "RecyclerViewFragment";
    private static final String KEY_LAYOUT_MANAGER = "layoutManager";

    private enum LayoutManagerType {
        LINEAR_LAYOUT_MANAGER
    }

    protected LayoutManagerType mCurrentLayoutManagerType;

    protected RecyclerView mRecyclerView;
    protected RecyclerView.LayoutManager mLayoutManager;

    protected List<ParseObject> mYeets;
    protected SwipeRefreshLayout mSwipeRefreshLayout;

    public TabFragment1() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        // Find loading views
        RelativeLayout noAdapterBackground = (RelativeLayout) getActivity().findViewById(R.id.noAdapterBackground);
        RelativeLayout noAdapterContainer = (RelativeLayout) getActivity().findViewById(R.id.noAdapterContainer);
        TextView noAdapterLogo = (TextView) getActivity().findViewById(R.id.noAdapterLogo);

        // Set loading views initially to visible
        noAdapterBackground.setVisibility(View.VISIBLE);
        noAdapterContainer.setVisibility(View.VISIBLE);
        noAdapterLogo.setVisibility(View.VISIBLE);

        // Initialize SwipeRefreshLayout
        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeRefreshLayout);

        // Is the network online?
        boolean isOnline = NetworkHelper.isOnline(getContext());

        // Hide or show views associated with network state
        rootView.findViewById(R.id.networkOfflineText).setVisibility(isOnline ? View.GONE : View.VISIBLE);
        rootView.findViewById(R.id.rl).setVisibility(isOnline ? View.GONE : View.VISIBLE);

        // Retrieve Data from remote server
        retrieveData(isOnline, noAdapterBackground, noAdapterContainer, noAdapterLogo);

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

    private void retrieveData(boolean isOnline, RelativeLayout noAdapterBackground, RelativeLayout noAdapterContainer, TextView noAdapterLogo) {

        String groupId = ParseUser.getCurrentUser().getString("groupId");
        ParseQuery<ParseObject> query = new ParseQuery<>(ParseConstants.CLASS_YEET);
        query.whereContains(ParseConstants.KEY_GROUP_ID, groupId);
        query.orderByDescending("lastReplyUpdatedAt");
        query.setLimit(1000);
        query.findInBackground((yeets, e) -> {

            if (mSwipeRefreshLayout.isRefreshing()) {
                mSwipeRefreshLayout.setRefreshing(false);
            }

            if (e == null) {

                // We found messages!
                mYeets = yeets;
                /*System.out.println(yeets);*/

                noAdapterBackground.setVisibility(View.GONE);
                noAdapterContainer.setVisibility(View.GONE);
                noAdapterLogo.setVisibility(View.GONE);

                FeedAdapter adapter = new FeedAdapter(getContext(), yeets);
                adapter.setHasStableIds(true);
                mRecyclerView.setHasFixedSize(true);
                adapter.notifyDataSetChanged();
                mRecyclerView.setAdapter(adapter);

                mSwipeRefreshLayout.setOnRefreshListener(() -> {
                    if (!isOnline) {
                        mSwipeRefreshLayout.setRefreshing(false);
                    } else {
                        Date onRefreshDate = new Date();
                        /*System.out.println(onRefreshDate.getTime());*/
                        refreshYeets(onRefreshDate, adapter);
                    }
                });
            } else {
                noAdapterBackground.setVisibility(View.VISIBLE);
                noAdapterContainer.setVisibility(View.VISIBLE);
                noAdapterLogo.setVisibility(View.VISIBLE);
            }
        });
    }

    private void refreshYeets(Date date, FeedAdapter adapter) {
        String groupId = ParseUser.getCurrentUser().getString("groupId");
        ParseQuery<ParseObject> query = new ParseQuery<>(ParseConstants.CLASS_YEET);
        query.whereContains(ParseConstants.KEY_GROUP_ID, groupId);
        query.orderByDescending("lastReplyUpdatedAt");
        if (date != null)
            query.whereLessThanOrEqualTo("createdAt", date);
        query.setLimit(1000);
        query.findInBackground((yeets, e) -> {

            if (mSwipeRefreshLayout.isRefreshing()) {
                mSwipeRefreshLayout.setRefreshing(false);
            }

            if (e == null) {
                // We found messages!
                mYeets.addAll(0, yeets); //This should append new messages to the top
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
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save currently selected layout manager.
        savedInstanceState.putSerializable(KEY_LAYOUT_MANAGER, mCurrentLayoutManagerType);
        super.onSaveInstanceState(savedInstanceState);
    }
}
