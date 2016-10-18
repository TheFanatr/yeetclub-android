package com.yeetclub.comment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.yeetclub.android.R;
import com.yeetclub.parse.ParseConstants;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by @santafebound on 2015-11-07.
 */
public class ReplyActivity extends AppCompatActivity {

    public final static String SELECTED_FEED_OBJECT_ID = "com.yeetclub.android.SELECTED_FEED_OBJECT_ID";
    public final static String SELECTED_USER_OBJECT_ID = "com.yeetclub.android.SELECTED_USER_OBJECT_ID";

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Layout
        setContentView(R.layout.activity_reply);

        // Action bar
        assert getSupportActionBar() != null;
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Set focus on EditText immediately
        EditText myEditText = (EditText) findViewById(R.id.addCommentTextField);
        if (myEditText.requestFocus()) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }

        // Method to limit message line count to 6 lines as a maximum
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            public void afterTextChanged(Editable s) {
                if (null != myEditText.getLayout() && myEditText.getLayout().getLineCount() > 6) {
                    myEditText.getText().delete(myEditText.getText().length() - 1, myEditText.getText().length());
                }
            }
        };
        myEditText.addTextChangedListener(watcher);
        myEditText.setError(null);
        myEditText.getBackground().mutate().setColorFilter(
                ContextCompat.getColor(getApplicationContext(), R.color.white),
                PorterDuff.Mode.SRC_ATOP);

        // Reep Button
        Button submitComment = (Button) findViewById(R.id.submitComment);

        // Set typeface for Button and EditText
        Typeface tf_bold = Typeface.createFromAsset(getAssets(), "fonts/Lato-Bold.ttf");
        submitComment.setTypeface(tf_bold);
        Typeface tf_reg = Typeface.createFromAsset(getAssets(), "fonts/Lato-Regular.ttf");
        myEditText.setTypeface(tf_reg);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            // If we have the commentId from FeedAdapter or UserProfileAdapter...
            if (bundle.getString(ParseConstants.KEY_OBJECT_ID) != null) {
                String commentId = bundle.getString(ParseConstants.KEY_OBJECT_ID);
                String userId = bundle.getString(ParseConstants.KEY_SENDER_ID);

                setupTopLevelCommentText(commentId);

                // Reep Button Action
                submitComment.setOnClickListener(view -> {

                    view.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.image_click));
                    sendReply(myEditText, commentId, userId);

                });
            }
        }
    }

    private void setupTopLevelCommentText(String commentId) {
        ParseQuery<ParseObject> query = new ParseQuery<>(ParseConstants.CLASS_YEET);
        // Query the Yeet class with the objectId of the Comment that was sent with us here from the Intent bundle
        query.whereContains(ParseConstants.KEY_OBJECT_ID, commentId);
        query.findInBackground((topLevelComment, e) -> {
            if (e == null) {

                for (ParseObject topLevelCommentObject : topLevelComment) {

                    // System.out.println(commentId);

                    // Increment the reply count for the feed
                    String topLevelCommentResult = topLevelCommentObject.getString(ParseConstants.KEY_NOTIFICATION_TEXT);
                    TextView topLevelCommentText = (TextView) findViewById(R.id.topLevelCommentText);
                    topLevelCommentText.setText("In reply to: " + topLevelCommentResult);

                    Typeface tf_reg = Typeface.createFromAsset(getAssets(), "fonts/Lato-Regular.ttf");
                    topLevelCommentText.setTypeface(tf_reg);

                }

            } else {
                Log.d("score", "Error: " + e.getMessage());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            if (bundle.getString(ParseConstants.KEY_OBJECT_ID) != null) {
                String commentId = bundle.getString(ParseConstants.KEY_OBJECT_ID);
                setupTopLevelCommentText(commentId);
            }
        }

    }

    private ParseObject sendReply(EditText myEditText, String commentId, String userId) {

        // Initiate creation of Comment object
        ParseObject message = new ParseObject(ParseConstants.CLASS_COMMENT);

        // Sender author ObjectId
        message.put(ParseConstants.KEY_SENDER_ID, ParseUser.getCurrentUser().getObjectId());

        // Send Username
        message.put(ParseConstants.KEY_SENDER_NAME, ParseUser.getCurrentUser().getUsername());

        // Send Full Name
        if (!(ParseUser.getCurrentUser().get("name").toString().isEmpty())) {
            message.put(ParseConstants.KEY_SENDER_FULL_NAME, ParseUser.getCurrentUser().get("name"));
        } else {
            message.put(ParseConstants.KEY_SENDER_FULL_NAME, "Anonymous Lose");
        }

        // Send author Pointer
        message.put(ParseConstants.KEY_SENDER_AUTHOR_POINTER, ParseUser.getCurrentUser());

        // Initialize "likedBy" Array column
        String[] likedBy = new String[0];
        message.put(ParseConstants.KEY_LIKED_BY, Arrays.asList(likedBy));

        // Send the comment ObjectId of the top-level Yeet this comment belongs to
        message.put(ParseConstants.KEY_SENDER_PARSE_OBJECT_ID, commentId);

        // Send comment message
        String result = myEditText.getText().toString();
        // System.out.println(result);
        message.put(ParseConstants.KEY_COMMENT_TEXT, result);

        // Send Profile Picture
        if (ParseUser.getCurrentUser().getParseFile("profilePicture") != null) {
            message.put(ParseConstants.KEY_SENDER_PROFILE_PICTURE, ParseUser.getCurrentUser().getParseFile("profilePicture").getUrl());
        }

        // Conditions for sending message
        if (!(result.length() > 140 || result.length() <= 0)) {
            message.saveInBackground();

            // Send notification
            if (!userId.equals(ParseUser.getCurrentUser().getObjectId())) {
                sendReplyPushNotification(userId, result);
                ParseObject notification = createCommentMessage(userId, result, commentId);
                send(notification);
                Toast.makeText(getApplicationContext(), "Greet reep there, bub!", Toast.LENGTH_LONG).show();
            }

            updateYeetPriority(commentId);

            startCommentActivity(commentId, userId);

            // Play "Yeet" sound!
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplication());
            int storedPreference = preferences.getInt("sound", 1);
            // System.out.println("Application Sounds: " + storedPreference);
            if (storedPreference != 0) {
                final MediaPlayer mp = MediaPlayer.create(this, R.raw.yeet);
                mp.start();
            }

            Toast.makeText(getApplicationContext(), "Great reep there, bub!", Toast.LENGTH_LONG).show();

        } else {
            // Play "Oh Hell Nah" sound!
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplication());
            int storedPreference = preferences.getInt("sound", 1);
            // System.out.println("Application Sounds: " + storedPreference);
            if (storedPreference != 0) {
                final MediaPlayer mp = MediaPlayer.create(this, R.raw.nah);
                mp.start();
            }

            if (result.length() > 140) {
                Toast.makeText(getApplicationContext(), "Watch it, bub! Reeps must be less than 140 characters.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "Gotta reep somethin', bub!", Toast.LENGTH_LONG).show();
            }
        }

        return message;
    }

    private void startCommentActivity(String commentId, String userId) {
        Intent intent = new Intent(getApplicationContext(), CommentActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // ...and send along some information so that we can populate it with the relevant comments.
        intent.putExtra(ParseConstants.KEY_OBJECT_ID, commentId);
        intent.putExtra(ParseConstants.KEY_SENDER_ID, userId);
        getApplicationContext().startActivity(intent);
    }

    public void TitleClicked(View view) {

        finish();

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
                    // System.out.println(e);
                    Log.d(getClass().toString(), "ANNOUNCEMENT FAILURE");
                }
            }
        });
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

            } else {
                Log.d("score", "Error: " + e.getMessage());
            }
        });
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
}

