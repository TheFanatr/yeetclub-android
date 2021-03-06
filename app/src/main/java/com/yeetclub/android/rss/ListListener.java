package com.yeetclub.android.rss;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import java.util.List;

/**
 * Class implements a list listener.
 * @author ITCuties
 */
public class ListListener implements OnItemClickListener {
    // Our listener will contain a reference to the list of RSS Items
    // List item's reference
    List<com.yeetclub.android.rss.RssItem> listItems;
    // And a reference to a calling activity
    // Calling activity reference
    Activity activity;
    /** We will set those references in our constructor.*/
    public ListListener(List<com.yeetclub.android.rss.RssItem> aListItems, Activity anActivity) {
        listItems = aListItems;
        activity  = anActivity;
    }

    /** Start a browser with url from the rss item.*/
    public void onItemClick(AdapterView parent, View view, int pos, long id) {
        // We create an Intent which is going to display data
        Intent i = new Intent(Intent.ACTION_VIEW);
        // We have to set data for our new Intent
        i.setData(Uri.parse(listItems.get(pos).getLink()));
        // And start activity with our Intent
        activity.startActivity(i);
    }
}
