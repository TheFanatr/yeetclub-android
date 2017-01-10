package com.yeetclub.android.utility;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.yeetclub.android.R;
import com.yeetclub.android.adapter.FeedAdapter;

/**
 * Created by Bluebery on 10/25/2015.
 */
public abstract class LoadingRowRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int ROW_VIEW_TYPE_LOADING = 72398; // obscure number
    private boolean mContainsLoadingRow;

    protected abstract int getContentDataSize();

    protected abstract int getViewType(int position);

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case ROW_VIEW_TYPE_LOADING:
                return new LoadingViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.loading_row, parent, false));
        }

        throw new IllegalArgumentException("viewType is not ROW_VIEW_TYPE_LOADING. You must handle all other values of viewType (defined by getViewType) before calling super.");
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        // empty
    }

    public abstract void onBindViewHolder(FeedAdapter.ViewHolder holder, int position);

    @Override
    public final int getItemCount() {
        return mContainsLoadingRow ? (getContentDataSize() + 1) : getContentDataSize();
    }

    @Override
    public int getItemViewType(int position) {
        return (position == getContentDataSize()) ? ROW_VIEW_TYPE_LOADING : getViewType(position);
    }

    /**
     * Sets a boolean which is used by getItemCount and in turn getItemViewType to determine which view type the row should be (loading view vs. other view).
     * Should only be called when there is more results to load in an upcoming api request (determined by calling fragment).
     */
    public void toggleLoadingRowOn() {
        mContainsLoadingRow = true;
    }

    /**
     * Checks to see if a loading row exists by checking an instance boolean and removes the row / clears the boolean.
     * This helps to 'replace' a loading row with a different row.
     */
    public void toggleLoadingRowOff() {
        if (mContainsLoadingRow) {
            mContainsLoadingRow = false;

            // removes the loading row explicitly instead of allowing it to be 'pushed' down when new user suggestion rows are added.
            // this is only required to maintain consistency with the rest of the app.
            int position = getContentDataSize();
            if (position >= 0) {
                notifyItemRemoved(position);
            }
        }
    }

    protected class LoadingViewHolder extends RecyclerView.ViewHolder {

        public LoadingViewHolder(View v) {
            super(v);
        }
    }
}
