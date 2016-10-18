package com.yeetclub.android;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;
import com.yeetclub.parse.ParseConstants;
import com.yeetclub.parse.ParseHelper;
import com.yeetclub.utility.NetworkHelper;

import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Created by @santafebound on 2015-11-07.
 */
public class EditProfileActivity extends AppCompatActivity {

    private static final int SELECT_PHOTO = 2;
    protected SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set typeface for action bar title
        Typeface tf_reg = Typeface.createFromAsset(getAssets(), "fonts/Lato-Regular.ttf");
        TextView feedTitle = (TextView) findViewById(R.id.edit_profile_title);
        feedTitle.setTypeface(tf_reg);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initiate ParseQuery
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        String userId = currentUser.getObjectId();

        boolean isOnline = NetworkHelper.isOnline(this);
        LinearLayout ll = (LinearLayout)findViewById(R.id.linearLayout);
        findViewById(R.id.networkOfflineText).setVisibility(isOnline ? View.GONE : View.VISIBLE);
        findViewById(R.id.rl).setVisibility(isOnline ? View.GONE : View.VISIBLE);
        ll.setVisibility(isOnline ? View.VISIBLE : View.GONE);
        findViewById(R.id.submitProfileChanges).setVisibility(isOnline ? View.VISIBLE : View.GONE);

        final EditText fullNameField = (EditText) findViewById(R.id.fullName);
        final EditText usernameField = (EditText) findViewById(R.id.username);
        final EditText bioField = (EditText) findViewById(R.id.bio);
        final EditText baeField = (EditText) findViewById(R.id.bae);
        final EditText websiteField = (EditText) findViewById(R.id.websiteLink);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            if (!isOnline) {
                mSwipeRefreshLayout.setRefreshing(false);
            } else {
                if (mSwipeRefreshLayout.isRefreshing()) {
                    mSwipeRefreshLayout.setRefreshing(false);
                }

                createProfileHeader(userId, fullNameField, usernameField, bioField, baeField, websiteField);
            }
        });

        setSubmitProfileChangesClickListener(fullNameField, usernameField, bioField, baeField, websiteField);

        setProfilePictureClickListener();

        // Populate the profile information from Parse
        createProfileHeader(userId, fullNameField, usernameField, bioField, baeField, websiteField);

    }

    private void setSubmitProfileChangesClickListener(EditText fullNameField, EditText usernameField, EditText bioField, EditText baeField, EditText websiteField) {
        findViewById(R.id.submitProfileChanges).setOnClickListener(v -> {

            // Update user
            ParseUser user1 = ParseUser.getCurrentUser();
            user1.put("name", fullNameField.getText().toString());
            user1.setUsername(usernameField.getText().toString().toLowerCase().replaceAll("\\s",""));
            user1.put("websiteLink", websiteField.getText().toString());
            user1.put("bio", bioField.getText().toString());
            user1.put("bae", baeField.getText().toString());

            user1.saveInBackground(e -> RefreshActivity());
        });
    }

    private void createProfileHeader(String userId, EditText fullNameField, EditText usernameField, EditText bioField, EditText baeField, EditText websiteField) {

        Typeface tf_reg = Typeface.createFromAsset(getAssets(), "fonts/Lato-Regular.ttf");

        // Set typefaces for text fields
        fullNameField.setTypeface(tf_reg);
        usernameField.setTypeface(tf_reg);
        bioField.setTypeface(tf_reg);
        baeField.setTypeface(tf_reg);
        websiteField.setTypeface(tf_reg);

        ParseUser user = ParseUser.getCurrentUser();

        fullNameField.setText(user.getString("name"));
        usernameField.setText(user.getUsername());
        bioField.setText(user.getString("bio"));
        baeField.setText(user.getString("bae"));
        websiteField.setText(user.getString("websiteLink"));

        ParseQuery<ParseUser> query = ParseUser.getQuery();
        Log.w("User ID", userId);
        // Query the User class with the objectId that was sent with us here from the Intent bundle
        query.whereContains(ParseConstants.KEY_OBJECT_ID, userId);
        query.findInBackground((headerUser, e) -> {

            if (e == null) {

                for (ParseObject headerUserObject : headerUser) {

                    if (headerUserObject.getString("name") != null) {
                        String topLevelFullNameText = headerUserObject.getString("name");
                        fullNameField.setText(topLevelFullNameText);
                    }

                    if (headerUserObject.getString("bio") != null) {
                        String headerBioText = headerUserObject.getString("bio");
                        bioField.setText(headerBioText);
                        bioField.setTypeface(tf_reg);
                    }

                    if (headerUserObject.getString("bae") != null) {
                        String headerBaeText = headerUserObject.getString("bae");
                        baeField.setText(headerBaeText.toUpperCase());
                        baeField.setTypeface(tf_reg);
                    }

                    if (headerUserObject.getString("websiteLink") != null) {
                        String headerWebsiteLinkText = headerUserObject.getString("websiteLink");
                        websiteField.setText(headerWebsiteLinkText);
                        websiteField.setTypeface(tf_reg);
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

        switch(requestCode) {
            case SELECT_PHOTO:
                if(resultCode == RESULT_OK){
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
        finish();
        Intent intent = new Intent(this, EditProfileActivity.class);
        startActivity(intent);
    }

    private void fadeInProfilePicture() {
        Animation animFadeIn;
        animFadeIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fadein);
        ImageView profilePicture = (ImageView) findViewById(R.id.profile_picture);
        profilePicture.setAnimation(animFadeIn);
        profilePicture.setVisibility(View.VISIBLE);
    }

    // Relaunches the activity
    public void RefreshActivity() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

}
