package com.yeetclub.android.rss;

import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.yeetclub.android.R;
import com.yeetclub.android.utility.NetworkHelper;

import java.util.List;

/**
 * Class implements a list listener.
 * @author ITCuties
 */
public class RssActivity extends AppCompatActivity {

    // A reference to the local object
    private RssActivity local;
    protected SwipeRefreshLayout mSwipeRefreshLayout;

    /**
     * This method creates main application view
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set view
        setContentView(R.layout.activity_rss);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set typeface for action bar title
        Typeface tf_reg = Typeface.createFromAsset(getAssets(), "fonts/Lato-Regular.ttf");
        TextView feedTitle = (TextView) findViewById(R.id.feed_title);
        feedTitle.setTypeface(tf_reg);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        initialise();

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Retrieve News
        getRssDataTask();

        if (getRssDataTask()) {
            mSwipeRefreshLayout.setRefreshing(false);
        }

    }

    private boolean initialise() {
        boolean isOnline = NetworkHelper.isOnline(this);

        getRssDataTask();

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            if (!isOnline) {
                mSwipeRefreshLayout.setRefreshing(false);
            } else {
                // Retrieve News
                getRssDataTask();

                if (getRssDataTask()) {
                    mSwipeRefreshLayout.setRefreshing(false);
                }

            }
        });

        return isOnline;
    }

    private boolean getRssDataTask() {
        // Set reference to this activity
        local = this;

        GetRSSDataTask task = new GetRSSDataTask();
        // Start download RSS task
        task.execute("http://www.saanichnews.com/news/index.rss");

        // Debug the thread name
        Log.d(getClass().toString(), Thread.currentThread().getName());

        return true;
    }

    private class GetRSSDataTask extends AsyncTask<String, Void, List<RssItem> > {
        @Override
        protected List<RssItem> doInBackground(String... urls) {

            // Debug the task thread name
            Log.d(getClass().toString(), Thread.currentThread().getName());

            try {
                // Create RSS reader
                RssReader rssReader = new RssReader(urls[0]);

                // Parse RSS, get items
                return rssReader.getItems();

            } catch (Exception e) {
                Log.e(getClass().toString(), e.getMessage());
            }

            return null;
        }

        @Override
        protected void onPostExecute(List<RssItem> result) {

            // Get a ListView from main view
            ListView itcItems = (ListView) findViewById(R.id.listMainView);

            // Create a list adapter
            ArrayAdapter<RssItem> adapter = new ArrayAdapter<>(local, R.layout.simple_rss_list_item_1, result);
            // Set list adapter for the ListView
            itcItems.setAdapter(adapter);

            // Set list view item click listener
            itcItems.setOnItemClickListener(new ListListener(result, local));
            itcItems.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.image_click));

        }
    }
}