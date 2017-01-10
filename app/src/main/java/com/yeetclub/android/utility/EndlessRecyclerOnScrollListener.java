package com.yeetclub.android.utility;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

/**
 * Created by Bluebery on 10/25/2015.
 */
public abstract class EndlessRecyclerOnScrollListener extends RecyclerView.OnScrollListener {

    private static final int VISIBLE_THRESHOLD = 5; // The minimum amount of items to have below your current scroll position before loading more
    private LinearLayoutManager mLinearLayoutManager;

    public abstract void onLoadMore();

    public EndlessRecyclerOnScrollListener(LinearLayoutManager linearLayoutManager) {
        this.mLinearLayoutManager = linearLayoutManager;
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        int totalItemCount = mLinearLayoutManager.getItemCount();
        int firstVisibleItem = mLinearLayoutManager.findFirstVisibleItemPosition();
        int visibleItemCount = recyclerView.getChildCount();

        int lastItemVisible = firstVisibleItem + visibleItemCount;

        // once the last visible item is within VISIBLE_THRESHOLD from the bottom, we want to load more
        if ((totalItemCount - lastItemVisible) <= VISIBLE_THRESHOLD) {
            onLoadMore();
        }
    }
}
