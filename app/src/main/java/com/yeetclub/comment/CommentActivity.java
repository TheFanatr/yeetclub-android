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
import android.widget.RelativeLayout;
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
import com.yeetclub.android.DividerItemDecoration;
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

import static com.yeetclub.android.R.id.profilePicture;
import static com.yeetclub.android.R.raw.yeet;

/**
 * Created by @santafebound on 2015-11-07.
 */
public class CommentActivity extends AppCompatActivity {

    protected List<ParseObject> mYeets;
    protected SwipeRefreshLayout mSwipeRefreshLayout;

    private RecyclerView mRecyclerView;
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

                createTopLevelCommentObject(commentId, userId, isOnline);

                // Pass the commentId as a parameter to a function that retrieves all the comments associated with the top-level Yeet's objectId
                setSwipeRefreshLayout(isOnline, commentId, userId);

                Button submitReply = (Button) findViewById(R.id.submitReply);

                // Set typeface for Button and EditText
                Typeface tf_bold = Typeface.createFromAsset(getAssets(), "fonts/Lato-Bold.ttf");
                Typeface tf_reg = Typeface.createFromAsset(getAssets(), "fonts/Lato-Regular.ttf");
                submitReply.setTypeface(tf_bold);
                myEditText.setTypeface(tf_reg);

                submitReply.setOnClickListener(view -> {
                    view.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.image_click));
                    sendReply(myEditText, commentId, userId);
                });

                ParseQuery<ParseObject> query = new ParseQuery<>(ParseConstants.CLASS_YEET);
                // Query the Yeet class with the objectId of the Comment that was sent to this activity from the Intent bundle
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
                    } else {
                        // If the top-level Comment object no longer exists then notify the user
                        findViewById(R.id.noTopLevelCommentObject).setVisibility(View.VISIBLE);
                        findViewById(R.id.yeetDeleted).setVisibility(View.VISIBLE);
                    }

                });
            }
        }
    }

    private boolean initialise(String commentId, String userId) {

        boolean isOnline = NetworkHelper.isOnline(this);

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        retrieveYeets(commentId, userId, isOnline);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            if (!isOnline) {
                mSwipeRefreshLayout.setRefreshing(false);
            } else {
                retrieveYeets(commentId, userId, true);
                createTopLevelCommentObject(commentId, userId, true);
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

    private void createTopLevelCommentObject(String commentId, String userId, boolean isOnline) {

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
        ImageView topLevelProfilePicture = (ImageView) findViewById(profilePicture);

        LinearLayout pollVoteLayout = (LinearLayout) findViewById(R.id.pollVoteLayout);
        LinearLayout pollResultsLayout = (LinearLayout) findViewById(R.id.pollResultsLayout);

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
                        if (e == null) for (ParseObject userObject : user) {

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
                                topLevelFullName.setText(R.string.anonymous_fullName);
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

                    // Display Poll object
                    if (topLevelCommentObject.getParseObject(ParseConstants.KEY_POLL_OBJECT) != null) {
                        displayPollObject(topLevelCommentObject, commentId, userId);
                    } else {
                        pollResultsLayout.setVisibility(View.GONE);
                        pollVoteLayout.setVisibility(View.GONE);
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
                        createLike(topLevelCommentObject, commentId, userId, isOnline);
                    });

                    // Set profilePicture clickListener
                    topLevelProfilePicture.setOnClickListener(v -> {
                        v.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.image_click));
                        retrievePointerObjectId(topLevelCommentObject);
                    });

                }

            } else {
                Log.d("score", "Error: " + e.getMessage());
            }
        });
    }

    private void displayPollObject(ParseObject topLevelCommentObject, String commentId, String userId) {

        TextView option1 = (TextView) findViewById(R.id.option1);
        TextView option2 = (TextView) findViewById(R.id.option2);
        TextView option3 = (TextView) findViewById(R.id.option3);
        TextView option4 = (TextView) findViewById(R.id.option4);
        TextView value1 = (TextView) findViewById(R.id.value1);
        TextView value2 = (TextView) findViewById(R.id.value2);
        TextView value3 = (TextView) findViewById(R.id.value3);
        TextView value4 = (TextView) findViewById(R.id.value4);
        TextView vote1 = (TextView) findViewById(R.id.vote1);
        TextView vote2 = (TextView) findViewById(R.id.vote2);
        TextView vote3 = (TextView) findViewById(R.id.vote3);
        TextView vote4 = (TextView) findViewById(R.id.vote4);

        LinearLayout pollVoteLayout = (LinearLayout) findViewById(R.id.pollVoteLayout);
        LinearLayout pollResultsLayout = (LinearLayout) findViewById(R.id.pollResultsLayout);

        RelativeLayout resultLayout1 = (RelativeLayout) findViewById(R.id.resultLayout1);
        RelativeLayout resultLayout2 = (RelativeLayout) findViewById(R.id.resultLayout2);
        RelativeLayout resultLayout3 = (RelativeLayout) findViewById(R.id.resultLayout3);
        RelativeLayout resultLayout4 = (RelativeLayout) findViewById(R.id.resultLayout4);

        boolean isOnline = NetworkHelper.isOnline(getApplicationContext());

        ParseQuery<ParseObject> pollQuery = new ParseQuery<>(ParseConstants.CLASS_POLL);
        pollQuery.whereEqualTo(ParseConstants.KEY_OBJECT_ID, topLevelCommentObject.getParseObject(ParseConstants.KEY_POLL_OBJECT).getObjectId());
        /*System.out.println(mYeets.get(position).getParseObject(ParseConstants.KEY_POLL_OBJECT).getObjectId());*/
        if (!isOnline) {
            pollQuery.fromLocalDatastore();
        }
        pollQuery.findInBackground((poll, e) -> {
            if (e == null) for (ParseObject pollObject : poll) {

                List<String> votedBy = pollObject.getList("votedBy");
                /*System.out.println("Voted by: " + votedBy);*/

                if (votedBy.contains(ParseUser.getCurrentUser().getObjectId())) {

                    // If you have already voted, show the results panel
                    pollResultsLayout.setVisibility(View.VISIBLE);
                    pollVoteLayout.setVisibility(View.GONE);

                    // Set poll options text
                    option1.setText(pollObject.getString(ParseConstants.KEY_POLL_OPTION1));
                    option2.setText(pollObject.getString(ParseConstants.KEY_POLL_OPTION2));
                    option3.setText(pollObject.getString(ParseConstants.KEY_POLL_OPTION3));
                    option4.setText(pollObject.getString(ParseConstants.KEY_POLL_OPTION4));

                    toggleUnusedPollOptions(pollObject, resultLayout3, resultLayout4);

                    // Total number of votes
                    int votedTotal_int = pollObject.getList("votedBy").size();
                    System.out.println("Total votes cast: " + Integer.toString(votedTotal_int));

                    if (votedTotal_int > 0) {
                        // Set poll options values
                        int value1_int = pollObject.getList("value1Array").size();
                        int value1_pct = ((value1_int / votedTotal_int) * 100);
                        String value1_string = Integer.toString(value1_pct);
                        value1.setText(value1_string + " %");

                        int value2_int = pollObject.getList("value2Array").size();
                        int value2_pct = ((value2_int / votedTotal_int) * 100);
                        String value2_string = Integer.toString(value2_pct);
                        value2.setText(value2_string + " %");

                        int value3_int = pollObject.getList("value3Array").size();
                        int value3_pct = ((value3_int / votedTotal_int) * 100);
                        String value3_string = Integer.toString(value3_pct);
                        value3.setText(value3_string + " %");

                        int value4_int = pollObject.getList("value4Array").size();
                        int value4_pct = ((value4_int / votedTotal_int) * 100);
                        String value4_string = Integer.toString(value4_pct);
                        value4.setText(value4_string + " %");
                    }

                } else {

                    // If you have not voted, show the vote options panel
                    pollVoteLayout.setVisibility(View.VISIBLE);
                    pollResultsLayout.setVisibility(View.GONE);

                    // Set poll options text
                    vote1.setText(pollObject.getString(ParseConstants.KEY_POLL_OPTION1));
                    vote2.setText(pollObject.getString(ParseConstants.KEY_POLL_OPTION2));
                    vote3.setText(pollObject.getString(ParseConstants.KEY_POLL_OPTION3));
                    vote4.setText(pollObject.getString(ParseConstants.KEY_POLL_OPTION4));

                    toggleUnusedPollVotes(pollObject, vote3, resultLayout3, vote4, resultLayout4);

                    if (!(votedBy.contains(ParseUser.getCurrentUser().getObjectId()))) {
                        vote1.setOnClickListener(v -> {
                            v.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.image_click));

                            // Add unique User objectId to votedBy array in Parse
                            pollObject.addAllUnique("votedBy", Collections.singletonList(ParseUser.getCurrentUser().getObjectId()));
                            pollObject.increment(ParseConstants.KEY_POLL_VALUE1);
                            pollObject.addAllUnique("value1Array", Collections.singletonList(ParseUser.getCurrentUser().getObjectId()));
                            pollObject.saveEventually();

                            System.out.println("ObjectIds in the value1 Array: " + pollObject.getList("value1Array").toString());
                            System.out.println("CurrentUser ObjectId: " + ParseUser.getCurrentUser().getObjectId());

                            // Color in current user's poll selection
                            /*if (pollObject.getList("value1Array").contains(ParseUser.getCurrentUser().getObjectId())) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                    holder.resultLayout1.setBackground(ContextCompat.getDrawable(mContext, R.drawable.rounded_border_textview_selected));
                                }
                            };*/

                            // Refresh activity with commentId
                            createTopLevelCommentObject(commentId, userId, true);

                            // Toast
                            Toast.makeText(getApplicationContext(), "Your votes are being recorded by the NSA!", Toast.LENGTH_SHORT).show();
                        });

                        vote2.setOnClickListener(v -> {
                            v.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.image_click));

                            // Add unique User objectId to votedBy array in Parse
                            pollObject.addAllUnique("votedBy", Collections.singletonList(ParseUser.getCurrentUser().getObjectId()));
                            pollObject.increment(ParseConstants.KEY_POLL_VALUE2);
                            pollObject.addAllUnique("value2Array", Collections.singletonList(ParseUser.getCurrentUser().getObjectId()));
                            pollObject.saveEventually();

                            // Color in current user's poll selection
                            /*if (pollObject.getList("value2Array").contains(ParseUser.getCurrentUser().getObjectId())) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                    holder.resultLayout2.setBackground(ContextCompat.getDrawable(mContext, R.drawable.rounded_border_textview_selected));
                                }
                            };*/

                            // Refresh activity with commentId
                            createTopLevelCommentObject(commentId, userId, true);

                            // Toast
                            Toast.makeText(getApplicationContext(), "Your votes are being recorded by the NSA!", Toast.LENGTH_SHORT).show();
                        });

                        vote3.setOnClickListener(v -> {
                            v.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.image_click));

                            // Add unique User objectId to votedBy array in Parse
                            pollObject.addAllUnique("votedBy", Collections.singletonList(ParseUser.getCurrentUser().getObjectId()));
                            pollObject.increment(ParseConstants.KEY_POLL_VALUE3);
                            pollObject.addAllUnique("value3Array", Collections.singletonList(ParseUser.getCurrentUser().getObjectId()));
                            pollObject.saveEventually();

                            // Color in current user's poll selection
                            /*if (pollObject.getList("value3Array").contains(ParseUser.getCurrentUser().getObjectId())) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                    holder.resultLayout3.setBackground(ContextCompat.getDrawable(mContext, R.drawable.rounded_border_textview_selected));
                                }
                            };*/

                            // Refresh activity with commentId
                            createTopLevelCommentObject(commentId, userId, true);

                            // Toast
                            Toast.makeText(getApplicationContext(), "Your votes are being recorded by the NSA!", Toast.LENGTH_SHORT).show();
                        });

                        vote4.setOnClickListener(v -> {
                            v.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.image_click));

                            // Add unique User objectId to votedBy array in Parse
                            pollObject.addAllUnique("votedBy", Collections.singletonList(ParseUser.getCurrentUser().getObjectId()));
                            pollObject.increment(ParseConstants.KEY_POLL_VALUE4);
                            pollObject.addAllUnique("value4Array", Collections.singletonList(ParseUser.getCurrentUser().getObjectId()));
                            pollObject.saveEventually();

                            // Color in current user's poll selection
                            /*if (pollObject.getList("value4Array").contains(ParseUser.getCurrentUser().getObjectId())) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                    holder.resultLayout4.setBackground(ContextCompat.getDrawable(mContext, R.drawable.rounded_border_textview_selected));
                                }
                            };*/

                            // Refresh activity with commentId
                            createTopLevelCommentObject(commentId, userId, true);

                            // Toast
                            Toast.makeText(getApplicationContext(), "Your votes are being recorded by the NSA!", Toast.LENGTH_SHORT).show();
                        });
                    }

                }

            }
            else {
                e.printStackTrace();
            }
        });
    }


    private void toggleUnusedPollOptions(ParseObject pollObject, RelativeLayout resultLayout3, RelativeLayout resultLayout4) {
        if (pollObject.getString(ParseConstants.KEY_POLL_OPTION3) != null) {
            resultLayout3.setVisibility(View.VISIBLE);
        } else {
            resultLayout3.setVisibility(View.GONE);
        }

        if (pollObject.getString(ParseConstants.KEY_POLL_OPTION4) != null) {
            resultLayout4.setVisibility(View.VISIBLE);
        } else {
            resultLayout4.setVisibility(View.GONE);
        }
    }


    private void toggleUnusedPollVotes(ParseObject pollObject, TextView vote3, RelativeLayout resultLayout3, TextView vote4, RelativeLayout resultLayout4) {
        if (pollObject.getString(ParseConstants.KEY_POLL_OPTION3) != null) {
            vote3.setVisibility(View.VISIBLE);
        } else {
            vote3.setVisibility(View.GONE);
            if (resultLayout3.getVisibility() == View.VISIBLE) {
                resultLayout3.setVisibility(View.GONE);
            }
        }

        if (pollObject.getString(ParseConstants.KEY_POLL_OPTION4) != null) {
            vote4.setVisibility(View.VISIBLE);
        } else {
            vote4.setVisibility(View.GONE);
            if (resultLayout4.getVisibility() == View.VISIBLE) {
                resultLayout4.setVisibility(View.GONE);
            }
        }
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
     * @param topLevelCommentObject A list derived from the main "Yeet" ParseObject (Yeet), from which also user information may be obtained via the _User pointer "author".
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

    private void createLike(ParseObject topLevelCommentObject, String commentId, String userId, boolean isOnline) {

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
                    commentObject.saveEventually();

                    // Increment the likeCount in the Comment feed
                    incrementLikeCount(commentObject, commentId, userId, isOnline);

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
            notification.put(ParseConstants.KEY_SENDER_FULL_NAME, R.string.anonymous_fullName);
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

    private void incrementLikeCount(ParseObject commentObject, String commentId, String userId, boolean isOnline) {
        // Query Like class for all Like objects that contain the related Comment objectId
        ParseQuery<ParseObject> query2 = new ParseQuery<>(ParseConstants.CLASS_LIKE);
        query2.whereEqualTo(ParseConstants.KEY_COMMENT_OBJECT_ID, commentObject);
        query2.findInBackground((comment2, e2) -> {
            if (e2 == null) {

                // Increment likeCount on related Comment object
                commentObject.increment("likeCount");
                commentObject.saveEventually();

                retrieveYeets(commentId, userId, isOnline);
                createTopLevelCommentObject(commentId, userId, isOnline);

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
            message.put(ParseConstants.KEY_SENDER_FULL_NAME, R.string.anonymous_fullName);
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
            message.saveEventually();

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
                final MediaPlayer mp = MediaPlayer.create(this, yeet);
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

                    topLevelCommentObject.saveEventually();

                }

            } else {
                Log.d("score", "Error: " + e.getMessage());
            }
        });
    }

    /**
     * @param yeets A list derived from the main "Yeet" ParseObject (Yeet), from which also user information may be obtained via the _User pointer "author".
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
            notification.put(ParseConstants.KEY_SENDER_FULL_NAME, R.string.anonymous_fullName);
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
                // Retrieve all the comments associated with the top-level Yeet's objectId
                retrieveYeets(commentId, userId, true);
                createTopLevelCommentObject(commentId, userId, true);
            }
        });
    }

    private void retrieveYeets(String commentId, String userId, boolean isOnline) {
        ParseQuery<ParseObject> query = new ParseQuery<>(ParseConstants.CLASS_COMMENT);
        // Query the Comment class for comments that have a "post" column value equal to the objectId of the top-level Yeet
        query.whereContains(ParseConstants.KEY_SENDER_PARSE_OBJECT_ID, commentId);
        query.addAscendingOrder(ParseConstants.KEY_CREATED_AT);
        if (!isOnline) {
            query.fromLocalDatastore();
        }
        query.findInBackground((yeets, e) -> {

            if (mSwipeRefreshLayout.isRefreshing()) {
                mSwipeRefreshLayout.setRefreshing(false);
            }

            if (e == null) {

                // We found messages!
                mYeets = yeets;
                ParseObject.pinAllInBackground(mYeets);

                CommentAdapter adapter = new CommentAdapter(getApplicationContext(), yeets, commentId);
                adapter.setHasStableIds(true);
                mRecyclerView.setHasFixedSize(true);
                mRecyclerView.addItemDecoration(new DividerItemDecoration(getApplicationContext(), DividerItemDecoration.VERTICAL_LIST));
                adapter.notifyDataSetChanged();
                mRecyclerView.setAdapter(adapter);

                if (!isOnline) {
                    mSwipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(getApplicationContext(), getString(R.string.cannot_retrieve_messages), Toast.LENGTH_SHORT).show();
                } else {
                    setSwipeRefreshLayout(true, commentId, userId);
                }

            }
        });
    }


}