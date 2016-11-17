package com.yeetclub.profile;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;
import com.yeetclub.android.EditProfileActivity;
import com.yeetclub.android.R;
import com.yeetclub.android.YeetActivity;
import com.yeetclub.parse.ParseConstants;
import com.yeetclub.parse.ParseHelper;
import com.yeetclub.utility.NetworkHelper;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

/**
 * Created by @santafebound on 2015-11-07.
 */
public class UserProfileActivity extends AppCompatActivity {

    private static final int SELECT_PHOTO = 2;
    protected List<ParseObject> mYeets;
    protected SwipeRefreshLayout mSwipeRefreshLayout;

    private RecyclerView recyclerView;
    private UserProfileAdapter adapter;

    public UserProfileActivity() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_profile);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Set typeface for action bar title
        Typeface tf_reg = Typeface.createFromAsset(getAssets(), "fonts/Lato-Regular.ttf");
        TextView feedTitle = (TextView) findViewById(R.id.feed_title);
        feedTitle.setTypeface(tf_reg);

        boolean isOnline = NetworkHelper.isOnline(this);

        Bundle bundle = getIntent().getExtras();
        // If the bundle is not null then we have arrived at another user's profile
        if (bundle != null) {
            if (bundle.getString(ParseConstants.KEY_OBJECT_ID) != null) {
                String userId = bundle.getString(ParseConstants.KEY_OBJECT_ID);

                initialise(userId);

                // Populate profile with Yeets from user we are visiting
                setSwipeRefreshLayout(isOnline, userId);

                if (userId != null && userId.equals(ParseUser.getCurrentUser().getObjectId())) {
                    setProfilePictureClickListener();
                }

                // Set up profile header for user we are visiting
                createProfileHeader(userId);
            }
        } else {
            String userId = ParseUser.getCurrentUser().getObjectId();
            setSwipeRefreshLayout(isOnline, userId);

            initialise(userId);

            setProfilePictureClickListener();

            // Populate profile with Yeets from current user, i.e. self.
            createProfileHeader(userId);
        }

        // Floating action button
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#169cee")));
        fab.setOnClickListener(view -> {

            Intent intent = new Intent(UserProfileActivity.this, YeetActivity.class);
            startActivity(intent);

        });

    }

    private boolean initialise(String userId) {

        boolean isOnline = NetworkHelper.isOnline(this);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        retrieveYeets(userId, isOnline);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            if (!isOnline) {
                mSwipeRefreshLayout.setRefreshing(false);
            } else {
                createProfileHeader(userId);
                retrieveYeets(userId, isOnline);
            }
        });
        return isOnline;
    }

    private void setProfilePictureClickListener() {

        ImageView profilePicture = (ImageView) findViewById(R.id.profile_picture);
        profilePicture.setOnClickListener(view -> {

            view.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.image_click));
            ChangeProfilePicture();

        });
    }

    public void ChangeProfilePicture() {

        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, SELECT_PHOTO);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch (requestCode) {
            case SELECT_PHOTO:
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = imageReturnedIntent.getData();
                    InputStream imageStream = null;
                    try {
                        imageStream = getContentResolver().openInputStream(selectedImage);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    Bitmap yourSelectedImage = BitmapFactory.decodeStream(imageStream);
                    Bitmap croppedThumbnail = ThumbnailUtils.extractThumbnail(yourSelectedImage, 144, 144, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
                    ParseHelper.UploadProfilePictureToCurrentUser(croppedThumbnail);
                    RefreshGalleryActivity();
                }
        }
    }

    // Relaunches UserProfileActivity
    public void RefreshGalleryActivity() {
        Toast.makeText(getApplicationContext(), "Profile picture uploaded successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, UserProfileActivity.class);
        finish();
        startActivity(intent);
    }

    private void setSwipeRefreshLayout(boolean isOnline, String userId) {
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            if (!isOnline) {
                mSwipeRefreshLayout.setRefreshing(false);
            } else {
                if (mSwipeRefreshLayout.isRefreshing()) {
                    mSwipeRefreshLayout.setRefreshing(false);
                }

                retrieveYeets(userId, true);

                if (userId != null && userId.equals(ParseUser.getCurrentUser().getObjectId())) {
                    createProfileHeader(userId);
                }

            }
        });
    }

    private void createProfileHeader(String userId) {
        TextView topLevelFullName = (TextView) findViewById(R.id.fullName);
        TextView topLevelBio = (TextView) findViewById(R.id.bio);
        TextView topLevelBae = (TextView) findViewById(R.id.bae);
        TextView topLevelWebsiteLink = (TextView) findViewById(R.id.websiteLink);

        Typeface tf_reg = Typeface.createFromAsset(getAssets(), "fonts/Lato-Regular.ttf");
        Typeface tf_black = Typeface.createFromAsset(getAssets(), "fonts/Lato-Black.ttf");

        ParseQuery<ParseUser> query = ParseUser.getQuery();
        Log.w("User ID", userId);
        // Query the User class with the objectId that was sent with us here from the Intent bundle
        query.whereContains(ParseConstants.KEY_OBJECT_ID, userId);
        query.findInBackground((headerUser, e) -> {

            if (e == null) {

                for (ParseObject headerUserObject : headerUser) {

                    if (headerUserObject.getString("name") != null) {
                        String topLevelFullNameText = headerUserObject.getString("name");
                        topLevelFullName.setText(topLevelFullNameText);
                        topLevelFullName.setTypeface(tf_black);
                    } else {
                        topLevelFullName.setVisibility(View.GONE);
                    }

                    if (headerUserObject.getString("bio") != null) {
                        String headerBioText = headerUserObject.getString("bio");
                        topLevelBio.setText(headerBioText);
                        topLevelBio.setTypeface(tf_reg);
                    } else {
                        topLevelBio.setVisibility(View.GONE);
                    }

                    if (headerUserObject.getString("bae") != null) {
                        String headerBaeText = headerUserObject.getString("bae");
                        topLevelBae.setText(headerBaeText.toUpperCase());
                        topLevelBae.append(" IS BAE");
                        topLevelBae.setTypeface(tf_reg);
                    } else {
                        topLevelBae.setVisibility(View.GONE);
                    }

                    if (headerUserObject.getString("websiteLink") != null) {
                        String headerWebsiteLinkText = headerUserObject.getString("websiteLink");
                        topLevelWebsiteLink.setText(headerWebsiteLinkText);
                        topLevelWebsiteLink.setTypeface(tf_reg);
                    } else {
                        topLevelWebsiteLink.setVisibility(View.GONE);
                    }

                    if (headerUserObject.getParseFile("profilePicture") != null) {

                        Picasso.with(getApplicationContext())
                                .load(headerUserObject.getParseFile("profilePicture").getUrl())
                                .placeholder(R.color.placeholderblue)
                                .memoryPolicy(MemoryPolicy.NO_CACHE).into(((ImageView) findViewById(R.id.profile_picture)));

                        fadeInProfilePicture();
                    }

                }

            }
        });
    }

    private void fadeInProfilePicture() {
        Animation animFadeIn;
        animFadeIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fadein);
        ImageView profilePicture = (ImageView) findViewById(R.id.profile_picture);
        profilePicture.setAnimation(animFadeIn);
        profilePicture.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void retrieveYeets(String userId, Boolean isOnline) {
        ParseQuery<ParseObject> query = new ParseQuery<>(ParseConstants.CLASS_YEET);
        query.whereContains(ParseConstants.KEY_SENDER_AUTHOR_POINTER, userId);
        query.addDescendingOrder(ParseConstants.KEY_CREATED_AT);
        query.findInBackground((yeets, e) -> {

            if (mSwipeRefreshLayout.isRefreshing()) {
                mSwipeRefreshLayout.setRefreshing(false);
            }

            if (e == null) {

                // We found messages!
                mYeets = yeets;
                ParseObject.pinAllInBackground(mYeets);

                UserProfileAdapter adapter = new UserProfileAdapter(getApplicationContext(), yeets);
                adapter.setHasStableIds(true);
                /*RecyclerViewHeader header = (RecyclerViewHeader) findViewById(R.id.header);
                header.attachTo(recyclerView);*/
                recyclerView.setAdapter(adapter);

                mSwipeRefreshLayout.setOnRefreshListener(() -> {
                    if (!isOnline) {
                        mSwipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(getApplicationContext(), getString(R.string.cannot_retrieve_messages), Toast.LENGTH_SHORT).show();
                    } else {
                        Date onRefreshDate = new Date();
                        /*System.out.println(onRefreshDate.getTime());*/
                        refreshYeets(userId, onRefreshDate, adapter);
                    }
                });

            }
        });
    }

    private void refreshYeets(String userId, Date date, UserProfileAdapter adapter) {
        ParseQuery<ParseObject> query = new ParseQuery<>(ParseConstants.CLASS_YEET);
        query.whereContains(ParseConstants.KEY_SENDER_AUTHOR_POINTER, userId);
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
                mYeets.removeAll(yeets);
                mYeets.addAll(0, yeets); //This should append new messages to the top
                adapter.notifyDataSetChanged();
                ParseObject.pinAllInBackground(mYeets);

                /*System.out.println(yeets);*/
                if (recyclerView.getAdapter() == null) {
                    adapter.setHasStableIds(true);
                    recyclerView.setHasFixedSize(true);
                    adapter.notifyDataSetChanged();
                    recyclerView.setAdapter(adapter);
                } else {
                    adapter.notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            if (bundle.getString(ParseConstants.KEY_OBJECT_ID) != null) {
                String userId = bundle.getString(ParseConstants.KEY_OBJECT_ID);

                if (userId != null && userId.equals(ParseUser.getCurrentUser().getObjectId())) {
                    inflater.inflate(R.menu.settings_profile, menu);
                }
            }
        } else {
            inflater.inflate(R.menu.settings_profile, menu);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit_profile:
                Intent intent = new Intent(this, EditProfileActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}