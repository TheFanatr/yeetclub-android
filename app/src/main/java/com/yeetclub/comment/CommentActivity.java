package com.yeetclub.comment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.transition.Fade;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.squareup.picasso.Picasso;
import com.yeetclub.android.MediaPreviewActivity;
import com.yeetclub.android.R;
import com.yeetclub.feed.FeedAdapter;
import com.yeetclub.parse.ParseConstants;
import com.yeetclub.profile.UserProfileActivity;
import com.yeetclub.utility.NetworkHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by @santafebound on 2015-11-07.
 */
public class CommentActivity extends AppCompatActivity {

    protected List<ParseObject> mYeets;
    protected SwipeRefreshLayout mSwipeRefreshLayout;

    private RecyclerView recyclerView;
    private FeedAdapter adapter;

    public CommentActivity() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_comment);

        setupWindowAnimations();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        EditText myEditText = (EditText) findViewById(R.id.addCommentTextField);
        myEditText.setOnTouchListener((view, event) -> {

            view.setFocusable(true);
            view.setFocusableInTouchMode(true);

            return false;
        });

        myEditText.setError(null);
        myEditText.getBackground().mutate().setColorFilter(
                ContextCompat.getColor(getApplicationContext(), R.color.white),
                PorterDuff.Mode.SRC_ATOP);

        boolean isOnline = NetworkHelper.isOnline(this);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            // If we have the commentId from FeedAdapter or UserProfileAdapter...
            if (bundle.getString(ParseConstants.KEY_OBJECT_ID) != null) {
                String commentId = bundle.getString(ParseConstants.KEY_OBJECT_ID);
                String userId = bundle.getString(ParseConstants.KEY_SENDER_ID);

                initialise(commentId, userId);

                createTopLevelCommentObject(commentId, userId);

                // Pass the commentId as a parameter to a function that retrieves all the comments associated with the top-level tweet's objectId
                setSwipeRefreshLayout(isOnline, commentId, userId);

                Button submitReply = (Button) findViewById(R.id.submitReply);

                // Set typeface for Button and EditText
                Typeface tf_bold = Typeface.createFromAsset(getAssets(), "fonts/Lato-Bold.ttf");
                submitReply.setTypeface(tf_bold);

                Typeface tf_reg = Typeface.createFromAsset(getAssets(), "fonts/Lato-Regular.ttf");
                myEditText.setTypeface(tf_reg);

                submitReply.setOnClickListener(view -> {

                    view.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.image_click));
                    sendReply(myEditText, commentId, userId);

                });

                ParseQuery<ParseObject> query = new ParseQuery<>(ParseConstants.CLASS_YEET);
                // Query the Yeet class with the objectId of the Comment that was sent with us here from the Intent bundle
                query.whereContains(ParseConstants.KEY_OBJECT_ID, commentId);
                query.findInBackground((topLevelComment, e) -> {
                    if (e == null) {

                        for (ParseObject topLevelCommentObject : topLevelComment) {

                            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                            fab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#169cee")));
                            fab.setOnClickListener(view -> {

                                view.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.image_click));
                                retrievePointerObjectIdForReply(topLevelCommentObject);

                            });
                        }
                    }
                });
            }
        }

        findViewById(R.id.networkOfflineText).setVisibility(isOnline ? View.GONE : View.VISIBLE);
        findViewById(R.id.rl).setVisibility(isOnline ? View.GONE : View.VISIBLE);

    }

    private boolean initialise(String commentId, String userId) {

        boolean isOnline = NetworkHelper.isOnline(this);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        retrieveYeets(commentId, userId);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            if (!isOnline) {
                mSwipeRefreshLayout.setRefreshing(false);
            } else {
                retrieveYeets(commentId, userId);
                createTopLevelCommentObject(commentId, userId);
            }
        });
        return isOnline;
    }

    private void setupWindowAnimations() {
        Fade fade = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            fade = (Fade) TransitionInflater.from(this).inflateTransition(R.transition.activity_fade);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setEnterTransition(fade);
        }
    }

    private void createTopLevelCommentObject(String commentId, String userId) {

        ImageView topLevelMessageImage = (ImageView) findViewById(R.id.topLevelMessageImage);
        TextView topLevelUsername = (TextView) findViewById(R.id.topLevelUsername);
        TextView topLevelFullName = (TextView) findViewById(R.id.topLevelFullName);
        TextView topLevelMessage = (TextView) findViewById(R.id.topLevelMessageText);
        TextView topLevelTime = (TextView) findViewById(R.id.time);
        TextView topLevelLikeCount = (TextView) findViewById(R.id.likeCount);
        ImageView topLevelLikeImage = (ImageView) findViewById(R.id.likeImage);
        ImageView topLevelReplyImage = (ImageView) findViewById(R.id.replyImage);
        TextView topLevelReplyCount = (TextView) findViewById(R.id.replyCount);
        LinearLayout topLevelLinearLayout = (LinearLayout) findViewById(R.id.listView_item);
        ImageView topLevelProfilePicture = (ImageView) findViewById(R.id.profilePicture);
        LinearLayout topLevelMessageImageLayout = (LinearLayout) findViewById(R.id.messageImageLayout);

        ParseQuery<ParseObject> query = new ParseQuery<>(ParseConstants.CLASS_YEET);
        // Query the Yeet class with the objectId of the Comment that was sent with us here from the Intent bundle
        query.whereContains(ParseConstants.KEY_OBJECT_ID, commentId);
        query.findInBackground((topLevelComment, e) -> {
            if (e == null) {

                for (ParseObject topLevelCommentObject : topLevelComment) {

                    // Set username
                    String topLevelUserNameText = topLevelCommentObject.getString("senderName");
                    topLevelUsername.setText(topLevelUserNameText);

                    setLikeImageHolderResource(topLevelCommentObject);

                    topLevelReplyImage.setOnClickListener(view -> {
                        view.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.image_click));
                        retrievePointerObjectIdForReply(topLevelCommentObject);
                    });

                    Boolean isRant = topLevelCommentObject.getBoolean("isRant");
                    /*System.out.println(isRant);*/
                    if (isRant) {
                        int color = R.color.stroke;
                        int bgColor = R.color.lightred;
                        setRantTag(topLevelMessage, topLevelLinearLayout, color, bgColor);
                    } else {
                        int color = R.color.stroke;
                        int bgColor = R.color.white;
                        setRantTag(topLevelMessage, topLevelLinearLayout, color, bgColor);
                    }

                    // Set username clickListener
                    topLevelUsername.setOnClickListener(v -> {
                        v.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.image_click));
                        retrievePointerObjectId(topLevelCommentObject);
                    });

                    ParseQuery<ParseUser> query2 = ParseUser.getQuery();
                    query2.whereEqualTo(ParseConstants.KEY_OBJECT_ID, topLevelCommentObject.getParseObject(ParseConstants.KEY_SENDER_AUTHOR_POINTER).getObjectId());
                    query2.findInBackground((user, e2) -> {
                        for (ParseObject userObject : user) {

                            if (userObject.getParseFile("profilePicture") != null) {
                                String profilePictureURL = userObject.getParseFile("profilePicture").getUrl();

                                // Asynchronously display the profile picture downloaded from Parse
                                if (profilePictureURL != null) {

                                    Picasso.with(getApplicationContext())
                                            .load(profilePictureURL)
                                            .placeholder(R.color.placeholderblue)
                                            .into(topLevelProfilePicture);

                                } else {
                                    topLevelProfilePicture.setImageResource(Integer.parseInt(String.valueOf(R.drawable.ic_profile_pic_add)));
                                }
                            }

                            if (!(userObject.getString(ParseConstants.KEY_AUTHOR_FULL_NAME).isEmpty())) {
                                topLevelFullName.setText(userObject.getString(ParseConstants.KEY_AUTHOR_FULL_NAME));
                            } else {
                                topLevelFullName.setText("Anonymous Lose");
                            }

                            topLevelUsername.setText(userObject.getString(ParseConstants.KEY_USERNAME));

                        }
                    });

                    // Set fullName clickListener
                    topLevelFullName.setOnClickListener(v -> {
                        v.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.image_click));
                        retrievePointerObjectId(topLevelCommentObject);
                    });

                    // Set message body
                    String topLevelMessageText = topLevelCommentObject.getString("notificationText");

                    if (!(topLevelMessageText.isEmpty())) {
                        topLevelMessage.setText(topLevelMessageText);
                    } else {
                        topLevelMessage.setVisibility(View.GONE);
                    }

                    // Set time
                    Date createdAt = topLevelCommentObject.getCreatedAt();
                    long now = new Date().getTime();
                    String convertedDate = DateUtils.getRelativeTimeSpanString(createdAt.getTime(), now, DateUtils.SECOND_IN_MILLIS).toString();
                    topLevelTime.setText(convertedDate);

                    // Set likeCount value
                    int likeCount_int = topLevelCommentObject.getInt(ParseConstants.KEY_LIKE_COUNT);
                    String likeCount_string = Integer.toString(likeCount_int);
                    topLevelLikeCount.setText(likeCount_string);

                    if (likeCount_int >= 4) {
                        setPremiumContent(View.VISIBLE);
                    } else {
                        setPremiumContent(View.GONE);
                    }

                    downloadMessageImage(topLevelMessageImage, topLevelCommentObject);

                    topLevelMessageImage.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                    topLevelMessageImage.setAdjustViewBounds(true);
                    topLevelMessageImage.setScaleType(ImageView.ScaleType.CENTER_CROP);

                    topLevelMessageImage.setOnClickListener(v -> {
                        ParseQuery<ParseObject> imageQuery = new ParseQuery<>(ParseConstants.CLASS_YEET);
                        imageQuery.whereEqualTo(ParseConstants.KEY_OBJECT_ID, topLevelCommentObject.getObjectId());
                        imageQuery.findInBackground((user, e2) -> {
                            if (e2 == null) for (ParseObject userObject : user) {

                                if (userObject.getParseFile("image") != null) {
                                    String imageURL = userObject.getParseFile("image").getUrl();
                                    Log.w(getClass().toString(), imageURL);

                                    // Asynchronously display the message image downloaded from Parse
                                    if (imageURL != null) {

                                        Intent intent = new Intent(getApplicationContext(), MediaPreviewActivity.class);
                                        intent.putExtra("imageUrl", imageURL);
                                        this.startActivity(intent);

                                    }

                                }
                            }
                        });
                    });

                    // Set replyCount value
                    int replyCount_int = topLevelCommentObject.getInt(ParseConstants.KEY_REPLY_COUNT);
                    String replyCount_string = Integer.toString(replyCount_int);
                    topLevelReplyCount.setText(replyCount_string);

                    List<String> likedBy = topLevelCommentObject.getList("likedBy");
                    if ((likedBy.contains(ParseUser.getCurrentUser().getObjectId()))) {
                        topLevelLikeImage.setImageResource(R.drawable.ic_action_like_feed_full);
                    }

                    topLevelLikeImage.setOnClickListener(v -> {
                        v.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.image_click));
                        createLike(topLevelCommentObject, commentId, userId);
                    });

                    // Set profilePicture clickListener
                    ImageView profilePicture = (ImageView) findViewById(R.id.profilePicture);
                    profilePicture.setOnClickListener(v -> {
                        v.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.image_click));
                        retrievePointerObjectId(topLevelCommentObject);
                    });

                }

            }
        });
    }

    private void setRantTag(TextView topLevelMessage, LinearLayout topLevelLinearLayout, int color, int bgColor) {
        topLevelMessage.setTextColor(ContextCompat.getColor(getApplicationContext(), color));
        topLevelLinearLayout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), bgColor));
    }

    private void downloadMessageImage(ImageView topLevelMessageImage, ParseObject topLevelCommentObject) {
        ParseQuery<ParseObject> query = new ParseQuery<>(ParseConstants.CLASS_YEET);
        query.whereEqualTo(ParseConstants.KEY_OBJECT_ID, topLevelCommentObject.getObjectId());
        query.findInBackground((user, e) -> {
            if (e == null) for (ParseObject userObject : user) {

                if (userObject.getParseFile("image") != null) {
                    String imageURL = userObject.getParseFile("image").getUrl();
                    /*Log.w(getClass().toString(), imageURL);*/

                    // Asynchronously display the message image downloaded from Parse
                    if (imageURL != null) {

                        topLevelMessageImage.setVisibility(View.VISIBLE);

                        Picasso.with(getApplicationContext())
                                .load(imageURL)
                                .placeholder(R.color.placeholderblue)
                                .into(topLevelMessageImage);

                    } else {
                        topLevelMessageImage.setVisibility(View.GONE);
                    }
                }

            }
        });
    }

    /**
     * @param topLevelCommentObject A list derived from the main "tweet" ParseObject (Yeet), from which also user information may be obtained via the _User pointer "author".
     */
    private void retrievePointerObjectIdForReply(ParseObject topLevelCommentObject) {
        /*String commentId = String.valueOf(yeets.getParseObject(ParseConstants.KEY_SENDER_POST_POINTER).getObjectId());*/

        // We retrieve the permanent objectId of the Yeet
        String userId = String.valueOf(topLevelCommentObject.getParseObject(ParseConstants.KEY_SENDER_AUTHOR_POINTER).getObjectId());
        String commentId = String.valueOf(topLevelCommentObject.getObjectId());

        // We use the generated commentId to launch the comment activity so that we can populate it with relevant messages
        startReplyActivity(commentId, userId);
    }

    private void startReplyActivity(String commentId, String userId) {
        /**
         * If the previously generated commentId is empty, we return nothing. This probably only occurs in the rare instance that the comment was deleted
         * from the database.
         */
        if (commentId == null || commentId.isEmpty()) {
            return;
        }

        // Here we launch a generic commenty activity class...
        Intent intent = new Intent(getApplicationContext(), ReplyActivity.class);

        // ...and send along some information so that we can populate it with the relevant comments.
        intent.putExtra(ParseConstants.KEY_OBJECT_ID, commentId);
        intent.putExtra(ParseConstants.KEY_SENDER_ID, userId);
        startActivity(intent);
    }

    private void setPremiumContent(int visibility) {
        ImageView topLevelPremiumContent = (ImageView) findViewById(R.id.premiumContent);
        topLevelPremiumContent.setVisibility(visibility);

        TextView topLevelPremiumContentText = (TextView) findViewById(R.id.premiumContentText);
        topLevelPremiumContentText.setVisibility(visibility);

        Typeface tf_reg = Typeface.createFromAsset(getAssets(), "fonts/Lato-Regular.ttf");
        topLevelPremiumContentText.setTypeface(tf_reg);
    }

    private void setLikeImageHolderResource(ParseObject topLevelCommentObject) {
        ParseQuery<ParseObject> query = new ParseQuery<>(ParseConstants.CLASS_YEET);
        query.whereEqualTo(ParseConstants.KEY_OBJECT_ID, topLevelCommentObject.getObjectId());
        query.findInBackground((comment, e) -> {
            // Find the single Comment object associated with the current ListAdapter position
            if (e == null) for (ParseObject yeetObject : comment) {

                // Create a list to store the likers of this Comment
                List<String> likedBy = yeetObject.getList("likedBy");
                /*System.out.println(likedBy);*/

                // If you are not on that list, then create a Like
                if (likedBy.contains(ParseUser.getCurrentUser().getObjectId())) {
                    // Set the image drawable to indicate that you liked this post
                    ImageView topLevelLikeImage = (ImageView) findViewById(R.id.likeImage);
                    topLevelLikeImage.setImageResource(R.drawable.ic_action_like_feed_full);
                } else {
                    // Set the image drawable to indicate that you have not liked this post
                    ImageView topLevelLikeImage = (ImageView) findViewById(R.id.likeImage);
                    topLevelLikeImage.setImageResource(R.drawable.ic_action_like_feed);
                }

            }
            else {
                Log.e("Error", e.getMessage());
            }
        });
    }

    private void createLike(ParseObject topLevelCommentObject, String commentId, String userId) {

        /*System.out.println(topLevelCommentObject.getObjectId());*/

        ParseQuery<ParseObject> query = new ParseQuery<>(ParseConstants.CLASS_YEET);
        query.whereEqualTo(ParseConstants.KEY_OBJECT_ID, topLevelCommentObject.getObjectId());
        query.findInBackground((comment, e) -> {
            // Find the single Comment object associated with the current ListAdapter position
            if (e == null) for (ParseObject commentObject : comment) {

                // Create a list to store the likers of this Comment
                List<String> likedBy = commentObject.getList("likedBy");
                /*System.out.println(likedBy);*/

                // If you are not on that list, then create a Like
                if (!(likedBy.contains(ParseUser.getCurrentUser().getObjectId()))) {

                    // Add unique User objectId to likedBy array in Parse
                    commentObject.addAllUnique("likedBy", Collections.singletonList(ParseUser.getCurrentUser().getObjectId()));
                    commentObject.saveInBackground();

                    // Increment the likeCount in the Comment feed
                    incrementLikeCount(commentObject, commentId, userId);

                    // Initiate Like notification
                    handleLikeNotification(commentObject);

                } else {
                    Toast.makeText(getApplicationContext(), "You already liked this Yeet", Toast.LENGTH_SHORT).show();
                }

            }
            else {
                Log.e("Error", e.getMessage());
            }
        });

    }

    private void handleLikeNotification(ParseObject commentObject) {
        String userId = commentObject.getParseObject(ParseConstants.KEY_SENDER_AUTHOR_POINTER).getObjectId();
        // Get the objectId of the top-level comment
        String commentId = commentObject.getObjectId();
        /*System.out.println(commentId);*/
        String result = commentObject.getString(ParseConstants.KEY_NOTIFICATION_TEXT);

        // Send notification to NotificationsActivity
        if (!userId.equals(ParseUser.getCurrentUser().getObjectId())) {
            // Send push notification
            sendLikePushNotification(userId, result);
            ParseObject notification = createLikeMessage(userId, result, commentId);
            send(notification);
        }
    }

    private void sendLikePushNotification(String userId, String result) {
        final Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("result", result);
        params.put("username", ParseUser.getCurrentUser().getUsername());
        params.put("useMasterKey", true); //Must have this line

        ParseCloud.callFunctionInBackground("pushLike", params, new FunctionCallback<String>() {
            public void done(String result, ParseException e) {
                if (e == null) {
                    Log.d(getClass().toString(), "ANNOUNCEMENT SUCCESS");
                } else {
                    /*System.out.println(e);*/
                    Log.d(getClass().toString(), "ANNOUNCEMENT FAILURE");
                }
            }
        });
    }

    protected ParseObject createLikeMessage(String userId, String result, String commentId) {

        ParseObject notification = new ParseObject(ParseConstants.CLASS_NOTIFICATIONS);
        notification.put(ParseConstants.KEY_SENDER_ID, ParseUser.getCurrentUser().getObjectId());
        notification.put(ParseConstants.KEY_SENDER_AUTHOR_POINTER, ParseUser.getCurrentUser());

        if (!(ParseUser.getCurrentUser().get("name").toString().isEmpty())) {
            notification.put(ParseConstants.KEY_SENDER_FULL_NAME, ParseUser.getCurrentUser().get("name"));
        } else {
            notification.put(ParseConstants.KEY_SENDER_FULL_NAME, "Anonymous Lose");
        }

        notification.put(ParseConstants.KEY_NOTIFICATION_BODY, result);
        notification.put(ParseConstants.KEY_SENDER_NAME, ParseUser.getCurrentUser().getUsername());
        notification.put(ParseConstants.KEY_RECIPIENT_ID, userId);
        notification.put(ParseConstants.KEY_OBJECT_ID, commentId);
        notification.put(ParseConstants.KEY_NOTIFICATION_TEXT, " liked your yeet!");
        notification.put(ParseConstants.KEY_READ_STATE, false);
        notification.put(ParseConstants.KEY_NOTIFICATION_TYPE, ParseConstants.TYPE_LIKE);

        if (ParseUser.getCurrentUser().getParseFile("profilePicture") != null) {
            notification.put(ParseConstants.KEY_SENDER_PROFILE_PICTURE, ParseUser.getCurrentUser().getParseFile("profilePicture").getUrl());
        }

        return notification;
    }

    private void incrementLikeCount(ParseObject commentObject, String commentId, String userId) {
        // Query Like class for all Like objects that contain the related Comment objectId
        ParseQuery<ParseObject> query2 = new ParseQuery<>(ParseConstants.CLASS_LIKE);
        query2.whereEqualTo(ParseConstants.KEY_COMMENT_OBJECT_ID, commentObject);
        query2.findInBackground((comment2, e2) -> {
            if (e2 == null) {

                // Increment likeCount on related Comment object
                commentObject.increment("likeCount");
                commentObject.saveInBackground();

                retrieveYeets(commentId, userId);
                createTopLevelCommentObject(commentId, userId);

            } else {
                Log.e("Error", e2.getMessage());
            }
        });

    }

    private ParseObject sendReply(EditText myEditText, String commentId, String userId) {
        ParseObject message = new ParseObject(ParseConstants.CLASS_COMMENT);

        // Sender objectId
        message.put(ParseConstants.KEY_SENDER_ID, ParseUser.getCurrentUser().getObjectId());

        // Sender username
        message.put(ParseConstants.KEY_SENDER_NAME, ParseUser.getCurrentUser().getUsername());

        if (!(ParseUser.getCurrentUser().get("name").toString().isEmpty())) {
            message.put(ParseConstants.KEY_SENDER_FULL_NAME, ParseUser.getCurrentUser().get("name"));
        } else {
            message.put(ParseConstants.KEY_SENDER_FULL_NAME, "Anonymouse Lose");
        }

        // Sender "author" pointer
        message.put(ParseConstants.KEY_SENDER_AUTHOR_POINTER, ParseUser.getCurrentUser());

        // Initialize "likedBy" Array column
        String[] likedBy = new String[0];
        message.put(ParseConstants.KEY_LIKED_BY, Arrays.asList(likedBy));

        // Sender ParseObject objectId
        message.put(ParseConstants.KEY_SENDER_PARSE_OBJECT_ID, commentId);

        // Sender comment text
        String result = myEditText.getText().toString();
        /*System.out.println(result);*/
        message.put(ParseConstants.KEY_COMMENT_TEXT, result);

        // Sender profile picture
        if (ParseUser.getCurrentUser().getParseFile("profilePicture") != null) {
            message.put(ParseConstants.KEY_SENDER_PROFILE_PICTURE, ParseUser.getCurrentUser().getParseFile("profilePicture").getUrl());
        }

        // If the reply is less than 140 characters, upload it to Parse
        if (!(result.length() > 140 || result.length() <= 0)) {
            message.saveInBackground();

            updateYeetPriority(commentId);

            Intent intent = getIntent();
            intent.putExtra(ParseConstants.KEY_OBJECT_ID, commentId);
            finish();
            startActivity(intent);

            View view = this.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }

            // Play "Yeet" sound!
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplication());
            int storedPreference = preferences.getInt("sound", 1);
            /*System.out.println("Application Sounds: " + storedPreference);*/
            if (storedPreference != 0) {
                final MediaPlayer mp = MediaPlayer.create(this, R.raw.yeet);
                mp.start();
            }

            if (!userId.equals(ParseUser.getCurrentUser().getObjectId())) {
                sendReplyPushNotification(userId, result);
                ParseObject notification = createCommentMessage(userId, result, commentId);
                send(notification);
                Toast.makeText(getApplicationContext(), "Greet reep there, bub!", Toast.LENGTH_LONG).show();
            }

        } else {
            // Play "Oh Hell Nah" sound!
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplication());
            int storedPreference = preferences.getInt("sound", 1);
            /*System.out.println("Application Sounds: " + storedPreference);*/
            if (storedPreference != 0) {
                final MediaPlayer mp = MediaPlayer.create(this, R.raw.nah);
                mp.start();
            }

            if (result.length() > 140) {
                Toast.makeText(getApplicationContext(), "Watch it, bub! Yeets must be less than 140 characters.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "Gotta yeet somethin', bub!", Toast.LENGTH_LONG).show();
            }
        }

        return message;
    }

    private void sendReplyPushNotification(String userId, String result) {
        final Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("result", result);
        params.put("username", ParseUser.getCurrentUser().getUsername());
        params.put("useMasterKey", true); //Must have this line

        ParseCloud.callFunctionInBackground("pushReply", params, new FunctionCallback<String>() {
            public void done(String result, ParseException e) {
                if (e == null) {
                    Log.d(getClass().toString(), "ANNOUNCEMENT SUCCESS");
                } else {
                    /*System.out.println(e);*/
                    Log.d(getClass().toString(), "ANNOUNCEMENT FAILURE");
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void updateYeetPriority(String commentId) {
        ParseQuery<ParseObject> query = new ParseQuery<>(ParseConstants.CLASS_YEET);
        // Query the Yeet class with the objectId of the Comment that was sent with us here from the Intent bundle
        query.whereContains(ParseConstants.KEY_OBJECT_ID, commentId);
        query.findInBackground((topLevelComment, e) -> {
            if (e == null) {

                for (ParseObject topLevelCommentObject : topLevelComment) {

                    // Increment the reply count for the feed
                    topLevelCommentObject.increment("replyCount", 1);

                    // Update lastReplyUpdatedAt so that when we query the feed, the top-level comment that was replied to will be pushed back to the top
                    Date myDate = new Date();
                    topLevelCommentObject.put("lastReplyUpdatedAt", myDate);

                    topLevelCommentObject.saveInBackground();

                }

            }
        });
    }

    /**
     * @param yeets A list derived from the main "tweet" ParseObject (Yeet), from which also user information may be obtained via the _User pointer "author".
     */
    private void retrievePointerObjectId(ParseObject yeets) {
        // We want to retrieve the permanent user objectId from the author of the Yeet so that we can always launch the user's profile, even if the author changes their username in the future.
        String userId = String.valueOf(yeets.getParseObject(ParseConstants.KEY_SENDER_AUTHOR_POINTER).getObjectId());

        // We use the generated userId to launch the user profile depending on whether we arrive to the profile as ourselves or are visiting externally from another feed or Yeet
        startGalleryActivity(userId);
    }

    public void startGalleryActivity(String userId) {
        // If the previously generated userId is empty, we return nothing. This probably only occurs in the rare instance that the author was deleted from the database.
        if (userId == null || userId.isEmpty()) {
            return;
        }

        // Here we launch a generic user profile class...
        Intent intent = new Intent(getApplicationContext(), UserProfileActivity.class);

        // ...and send along some information so that we can populate it with the relevant user, i.e. either ourselves or another author if visiting from another feed or Yeet.
        intent.putExtra(ParseConstants.KEY_OBJECT_ID, userId);
        startActivity(intent);
    }

    protected ParseObject createCommentMessage(String userId, String result, String commentId) {

        ParseObject notification = new ParseObject(ParseConstants.CLASS_NOTIFICATIONS);
        notification.put(ParseConstants.KEY_SENDER_ID, ParseUser.getCurrentUser().getObjectId());
        notification.put(ParseConstants.KEY_SENDER_AUTHOR_POINTER, ParseUser.getCurrentUser());

        if (!(ParseUser.getCurrentUser().get("name").toString().isEmpty())) {
            notification.put(ParseConstants.KEY_SENDER_FULL_NAME, ParseUser.getCurrentUser().get("name"));
        } else {
            notification.put(ParseConstants.KEY_SENDER_FULL_NAME, "Anonymous Lose");
        }

        notification.put(ParseConstants.KEY_NOTIFICATION_BODY, result);
        notification.put(ParseConstants.KEY_SENDER_NAME, ParseUser.getCurrentUser().getUsername());
        notification.put(ParseConstants.KEY_RECIPIENT_ID, userId);
        notification.put(ParseConstants.KEY_COMMENT_OBJECT_ID, commentId);
        notification.put(ParseConstants.KEY_NOTIFICATION_TEXT, " reeped to your yeet!");
        notification.put(ParseConstants.KEY_NOTIFICATION_TYPE, ParseConstants.TYPE_COMMENT);
        notification.put(ParseConstants.KEY_READ_STATE, false);
        if (ParseUser.getCurrentUser().getParseFile("profilePicture") != null) {
            notification.put(ParseConstants.KEY_SENDER_PROFILE_PICTURE, ParseUser.getCurrentUser().getParseFile("profilePicture").getUrl());
        }
        return notification;
    }

    protected void send(ParseObject notification) {
        notification.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    // success!

                } else {
                    // notification failed to send!

                }
            }
        });
    }

    private void setSwipeRefreshLayout(boolean isOnline, String commentId, String userId) {
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            if (!isOnline) {
                mSwipeRefreshLayout.setRefreshing(false);
            } else {
                // Retrieve all the comments associated with the top-level tweet's objectId
                retrieveYeets(commentId, userId);
                createTopLevelCommentObject(commentId, userId);
            }
        });
    }

    private void retrieveYeets(String commentId, String userId) {
        ParseQuery<ParseObject> query = new ParseQuery<>(ParseConstants.CLASS_COMMENT);
        // Query the Comment class for comments that have a "post" column value equal to the objectId of the top-level tweet
        query.whereContains(ParseConstants.KEY_SENDER_PARSE_OBJECT_ID, commentId);
        query.addAscendingOrder(ParseConstants.KEY_CREATED_AT);
        query.findInBackground((yeets, e) -> {

            if (mSwipeRefreshLayout.isRefreshing()) {
                mSwipeRefreshLayout.setRefreshing(false);
            }

            if (e == null) {

                // We found messages!
                mYeets = yeets;

                CommentAdapter adapter = new CommentAdapter(getApplicationContext(), yeets);

                if (recyclerView.getAdapter() != null) {
                    adapter.setHasStableIds(true);
                    recyclerView.hasFixedSize();
                    recyclerView.setAdapter(adapter);
                } else {
                    adapter.notifyDataSetChanged();
                }

            }
        });
    }


}