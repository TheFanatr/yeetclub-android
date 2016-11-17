package com.yeetclub.profile;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
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
import com.yeetclub.android.R;
import com.yeetclub.comment.CommentActivity;
import com.yeetclub.comment.ReplyActivity;
import com.yeetclub.parse.ParseConstants;
import com.yeetclub.utility.NetworkHelper;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by @santafebound on 2015-11-07.
 */
public class UserProfileAdapter extends RecyclerView.Adapter<UserProfileAdapter.ViewHolder> {

    protected Context mContext;
    protected List<ParseObject> mYeets;
    private UserProfileAdapter adapter;

    public UserProfileAdapter(Context context, List<ParseObject> yeets) {
        super();

        this.mYeets = yeets;
        this.mContext = context;
        this.adapter = this;
    }

    /**
     * @param yeets A list derived from the main "Yeet" ParseObject (Yeet), from which also user information may be obtained via the _User pointer "author".
     */
    private void retrievePointerObjectIdForReply(ParseObject yeets) {
        /*String commentId = String.valueOf(yeets.getParseObject(ParseConstants.KEY_SENDER_POST_POINTER).getObjectId());*/

        // We retrieve the permanent objectId of the Yeet
        String userId = String.valueOf(yeets.getParseObject(ParseConstants.KEY_SENDER_AUTHOR_POINTER).getObjectId());
        String commentId = String.valueOf(yeets.getObjectId());

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
        Intent intent = new Intent(mContext, ReplyActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // ...and send along some information so that we can populate it with the relevant comments.
        intent.putExtra(ParseConstants.KEY_OBJECT_ID, commentId);
        intent.putExtra(ParseConstants.KEY_SENDER_ID, userId);
        mContext.startActivity(intent);
    }

    private void setLikeImageHolderResource(int position, ViewHolder holder) {
        ParseQuery<ParseObject> query = new ParseQuery<>(ParseConstants.CLASS_YEET);
        query.whereEqualTo(ParseConstants.KEY_OBJECT_ID, mYeets.get(position).getObjectId());
        query.findInBackground((comment, e) -> {
            // Find the single Comment object associated with the current ListAdapter position
            if (e == null) for (ParseObject yeetObject : comment) {

                // Create a list to store the likers of this Comment
                List<String> likedBy = yeetObject.getList("likedBy");
                /*System.out.println(likedBy);*/

                // If you are not on that list, then create a Like
                if (likedBy.contains(ParseUser.getCurrentUser().getObjectId())) {
                    // Set the image drawable to indicate that you liked this post
                    holder.likeImage.setImageResource(R.drawable.ic_action_like_feed_full);
                } else {
                    // Set the image drawable to indicate that you have not liked this post
                    holder.likeImage.setImageResource(R.drawable.ic_action_like_feed);
                }

            }
            else {
                Log.e("Error", e.getMessage());
            }
        });
    }

    private void setPremiumContent(ViewHolder holder, int visibility) {
        holder.premiumContent.setVisibility(visibility);
        holder.premiumContentText.setVisibility(visibility);
        Typeface tf_reg = Typeface.createFromAsset(mContext.getAssets(), "fonts/Lato-Regular.ttf");
        holder.premiumContentText.setTypeface(tf_reg);
    }

    private void createLike(int position) {

        ParseQuery<ParseObject> query = new ParseQuery<>(ParseConstants.CLASS_YEET);
        query.whereEqualTo(ParseConstants.KEY_OBJECT_ID, mYeets.get(position).getObjectId());
        query.findInBackground((comment, e) -> {

            // Find the single Comment object associated with the current ListAdapter position
            if (e == null) for (ParseObject yeetObject : comment) {

                // Create a list to store the likers of this Comment
                List<String> likedBy = yeetObject.getList("likedBy");
                /*System.out.println(likedBy);*/

                // If you are not on that list, then create a Like
                if (!(likedBy.contains(ParseUser.getCurrentUser().getObjectId()))) {

                    // Create new Like object
                    /*ParseObject newLike2 = new ParseObject(ParseConstants.CLASS_LIKE);
                    newLike2.put(ParseConstants.KEY_SENDER_ID, ParseUser.getCurrentUser());
                    newLike2.put(ParseConstants.KEY_COMMENT_OBJECT_ID, yeetObject);
                    Toast.makeText(getContext(), "Yeet liked", Toast.LENGTH_SHORT).show();
                    newLike2.saveInBackground();*/

                    // Add unique User objectId to likedBy array in Parse
                    yeetObject.addAllUnique("likedBy", Collections.singletonList(ParseUser.getCurrentUser().getObjectId()));
                    yeetObject.saveEventually();

                    // Increment the likeCount in the Comment feed
                    incrementLikeCount(yeetObject, position);

                    // Initiate Like notification
                    handleLikeNotification(yeetObject);

                } else {
                    Toast.makeText(mContext, "You already liked this Yeet", Toast.LENGTH_SHORT).show();
                }

            }
            else {
                Log.e("Error", e.getMessage());
            }
        });

    }

    private void handleLikeNotification(ParseObject yeetObject) {
        String userId = yeetObject.getParseObject(ParseConstants.KEY_SENDER_AUTHOR_POINTER).getObjectId();
        // Get the objectId of the top-level comment
        String commentId = yeetObject.getObjectId();
        /*System.out.println(commentId);*/
        String result = yeetObject.getString(ParseConstants.KEY_NOTIFICATION_TEXT);

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
        notification.put(ParseConstants.KEY_COMMENT_OBJECT_ID, commentId);
        notification.put(ParseConstants.KEY_NOTIFICATION_TEXT, " liked your yeet!");
        notification.put(ParseConstants.KEY_NOTIFICATION_TYPE, ParseConstants.TYPE_LIKE);
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

    private void incrementLikeCount(ParseObject yeetObject, int position) {
        // Increment likeCount on related Comment object
        yeetObject.increment("likeCount");
        mYeets.get(position).increment("likeCount");
        this.adapter.notifyDataSetChanged();
        yeetObject.saveEventually();
    }

    private void deleteComment(int position) {
        String currentUserObjectId = ParseUser.getCurrentUser().getObjectId();
        ParseQuery<ParseObject> query = new ParseQuery<>("Yeet");
        query.whereEqualTo(ParseConstants.KEY_OBJECT_ID, mYeets.get(position).getObjectId());
        query.whereContains(ParseConstants.KEY_SENDER_ID, currentUserObjectId);
        query.findInBackground((yeet, e) -> {
            if (e == null) {

                for (ParseObject yeetObject : yeet) {

                    if (yeetObject.getParseObject(ParseConstants.KEY_SENDER_AUTHOR_POINTER).getObjectId().equals((ParseUser.getCurrentUser().getObjectId()))) {

                        for (ParseObject delete : yeet) {

                            // Iterate over all messages and delete them
                            delete.deleteInBackground();

                            mYeets.remove(position);
                            notifyItemRemoved(position);
                            notifyItemRangeChanged(position, mYeets.size());
                            this.adapter.notifyItemRemoved(position);
                            this.adapter.notifyDataSetChanged();

                            Toast.makeText(mContext, R.string.message_deleted, Toast.LENGTH_SHORT).show();
                        }
                    }
                }

            } else {
                Log.e("Error", e.getMessage());
            }
        });
    }

    /**
     * @param yeets A list derived from the main "Yeet" ParseObject (Yeet), from which also user information may be obtained via the _User pointer "author".
     */
    private void retrievePointerObjectIdForComment(ParseObject yeets) {
        /*String commentId = String.valueOf(yeets.getParseObject(ParseConstants.KEY_SENDER_POST_POINTER).getObjectId());*/

        // We retrieve the permanent objectId of the Yeet
        String userId = String.valueOf(yeets.getParseObject(ParseConstants.KEY_SENDER_AUTHOR_POINTER).getObjectId());
        String commentId = String.valueOf(yeets.getObjectId());

        // We use the generated commentId to launch the comment activity so that we can populate it with relevant messages
        startCommentActivity(commentId, userId);
    }

    private void startCommentActivity(String commentId, String userId) {
        /**
         * If the previously generated commentId is empty, we return nothing. This probably only occurs in the rare instance that the comment was deleted
         * from the database.
         */
        if (commentId == null || commentId.isEmpty()) {
            return;
        }

        // Here we launch a generic commenty activity class...
        Intent intent = new Intent(mContext, CommentActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // ...and send along some information so that we can populate it with the relevant comments.
        intent.putExtra(ParseConstants.KEY_OBJECT_ID, commentId);
        intent.putExtra(ParseConstants.KEY_SENDER_ID, userId);
        mContext.startActivity(intent);
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
        Intent intent = new Intent(mContext, UserProfileActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // ...and send along some information so that we can populate it with the relevant user, i.e. either ourselves or another author if visiting from another feed or Yeet.
        intent.putExtra(ParseConstants.KEY_OBJECT_ID, userId);
        mContext.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        if (mYeets == null) {
            return 0;
        } else {
            return mYeets.size();
        }
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public UserProfileAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.yeet_listview_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(UserProfileAdapter.ViewHolder holder, int position) {

        final ParseObject yeets = mYeets.get(position);

        Date createdAt = yeets.getCreatedAt();
        long now = new Date().getTime();
        String convertedDate = DateUtils.getRelativeTimeSpanString(createdAt.getTime(), now, DateUtils.SECOND_IN_MILLIS).toString();

        downloadMessageImage(holder, position);

        setLikeImageHolderResource(position, holder);

        Boolean isRant = yeets.getBoolean("isRant");
        /*System.out.println(isRant);*/
        if (isRant) {
            int color = R.color.stroke;
            int bgColor = R.color.lightred;
            setRantTag(holder, color, bgColor);
        } else {
            int color = R.color.stroke;
            int bgColor = R.color.white;
            setRantTag(holder, color, bgColor);
        }

        holder.notificationText.setText(yeets.getString(ParseConstants.KEY_NOTIFICATION_TEXT));
        holder.time.setText(convertedDate);

        // Display Poll object
        if (yeets.getParseObject(ParseConstants.KEY_POLL_OBJECT) != null) {
            displayPollObject(holder, position, yeets);
        } else {
            holder.pollResultsLayout.setVisibility(View.GONE);
            holder.pollVoteLayout.setVisibility(View.GONE);
        }

        int likeCount_int = yeets.getInt(ParseConstants.KEY_LIKE_COUNT);
        String likeCount_string = Integer.toString(likeCount_int);
        holder.likeCount.setText(likeCount_string);

        int replyCount_int = yeets.getInt(ParseConstants.KEY_REPLY_COUNT);
        String replyCount_string = Integer.toString(replyCount_int);
        holder.replyCount.setText(replyCount_string);

        if (likeCount_int >= 4) {
            setPremiumContent(holder, View.VISIBLE);
        } else {
            setPremiumContent(holder, View.GONE);
        }

        /*fadeinViews(holder);*/

        downloadProfilePicture(holder, yeets);

        holder.messageImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.image_click));
                retrievePointerObjectIdForComment(yeets);
            }
        });

        holder.replyImage.setOnClickListener(view -> {
            view.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.image_click));
            retrievePointerObjectIdForReply(yeets);
        });

        holder.username.setOnClickListener(view -> retrievePointerObjectId(yeets));

        holder.fullName.setOnClickListener(view -> retrievePointerObjectId(yeets));

        holder.profilePicture.setOnClickListener(view -> {
            view.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.image_click));
            retrievePointerObjectId(yeets);
        });

        holder.likeImage.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.like_click));
            createLike(position);
        });

        holder.itemView.setOnClickListener(v -> {
            boolean isOnline = NetworkHelper.isOnline(mContext);
            if (!isOnline) {
                Toast.makeText(mContext, R.string.cannot_retrieve_messages, Toast.LENGTH_SHORT).show();
            } else {
                v.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.slide_down_dialog));
                retrievePointerObjectIdForComment(yeets);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            boolean isOnline = NetworkHelper.isOnline(mContext);
            if (!isOnline) {
                Toast.makeText(mContext, R.string.cannot_retrieve_messages, Toast.LENGTH_SHORT).show();
            } else {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                deleteComment(position);
            }
            return true;
        });

    }

    private void displayPollObject(UserProfileAdapter.ViewHolder holder, int position, ParseObject yeets) {

        boolean isOnline = NetworkHelper.isOnline(mContext);

        ParseQuery<ParseObject> pollQuery = new ParseQuery<>(ParseConstants.CLASS_POLL);
        pollQuery.whereEqualTo(ParseConstants.KEY_OBJECT_ID, mYeets.get(position).getParseObject(ParseConstants.KEY_POLL_OBJECT).getObjectId());
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
                    holder.pollResultsLayout.setVisibility(View.VISIBLE);
                    holder.pollVoteLayout.setVisibility(View.GONE);

                    // Set poll options text
                    holder.option1.setText(pollObject.getString(ParseConstants.KEY_POLL_OPTION1));
                    holder.option2.setText(pollObject.getString(ParseConstants.KEY_POLL_OPTION2));
                    holder.option3.setText(pollObject.getString(ParseConstants.KEY_POLL_OPTION3));
                    holder.option4.setText(pollObject.getString(ParseConstants.KEY_POLL_OPTION4));

                    toggleUnusedPollOptions(holder, pollObject);

                    // Total number of votes
                    int votedTotal_int = pollObject.getList("votedBy").size();
                    System.out.println("Total votes cast: " + Integer.toString(votedTotal_int));

                    if (votedTotal_int > 0) {
                        // Set poll options values
                        int value1_int = pollObject.getList("value1Array").size();
                        int value1_pct = ((value1_int / votedTotal_int) * 100);
                        String value1_string = Integer.toString(value1_pct);
                        holder.value1.setText(value1_string + " %");

                        int value2_int = pollObject.getList("value2Array").size();
                        int value2_pct = ((value2_int / votedTotal_int) * 100);
                        String value2_string = Integer.toString(value2_pct);
                        holder.value2.setText(value2_string + " %");

                        int value3_int = pollObject.getList("value3Array").size();
                        int value3_pct = ((value3_int / votedTotal_int) * 100);
                        String value3_string = Integer.toString(value3_pct);
                        holder.value3.setText(value3_string + " %");

                        int value4_int = pollObject.getList("value4Array").size();
                        int value4_pct = ((value4_int / votedTotal_int) * 100);
                        String value4_string = Integer.toString(value4_pct);
                        holder.value4.setText(value4_string + " %");
                    }

                } else {

                    // If you have not voted, show the vote options panel
                    holder.pollVoteLayout.setVisibility(View.VISIBLE);
                    holder.pollResultsLayout.setVisibility(View.GONE);

                    // Set poll options text
                    holder.vote1.setText(pollObject.getString(ParseConstants.KEY_POLL_OPTION1));
                    holder.vote2.setText(pollObject.getString(ParseConstants.KEY_POLL_OPTION2));
                    holder.vote3.setText(pollObject.getString(ParseConstants.KEY_POLL_OPTION3));
                    holder.vote4.setText(pollObject.getString(ParseConstants.KEY_POLL_OPTION4));

                    toggleUnusedPollVotes(holder, pollObject);

                    if (!(votedBy.contains(ParseUser.getCurrentUser().getObjectId()))) {
                        holder.vote1.setOnClickListener(v -> {
                            v.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.image_click));

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

                            // Update Poll data
                            this.adapter.notifyDataSetChanged();

                            // Toast
                            Toast.makeText(mContext, "Your votes are being recorded by the NSA!", Toast.LENGTH_SHORT).show();
                        });

                        holder.vote2.setOnClickListener(v -> {
                            v.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.image_click));

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

                            // Update Poll data
                            this.adapter.notifyDataSetChanged();

                            // Toast
                            Toast.makeText(mContext, "Your votes are being recorded by the NSA!", Toast.LENGTH_SHORT).show();
                        });

                        holder.vote3.setOnClickListener(v -> {
                            v.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.image_click));

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

                            // Update Poll data
                            this.adapter.notifyDataSetChanged();

                            // Toast
                            Toast.makeText(mContext, "Your votes are being recorded by the NSA!", Toast.LENGTH_SHORT).show();
                        });

                        holder.vote4.setOnClickListener(v -> {
                            v.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.image_click));

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

                            // Update Poll data
                            this.adapter.notifyDataSetChanged();

                            // Toast
                            Toast.makeText(mContext, "Your votes are being recorded by the NSA!", Toast.LENGTH_SHORT).show();
                        });
                    }

                }

            }
            else {
                e.printStackTrace();
            }
        });
    }


    private void toggleUnusedPollOptions(UserProfileAdapter.ViewHolder holder, ParseObject pollObject) {
        if (pollObject.getString(ParseConstants.KEY_POLL_OPTION3) != null) {
            holder.resultLayout3.setVisibility(View.VISIBLE);
        } else {
            holder.resultLayout3.setVisibility(View.GONE);
        }

        if (pollObject.getString(ParseConstants.KEY_POLL_OPTION4) != null) {
            holder.resultLayout4.setVisibility(View.VISIBLE);
        } else {
            holder.resultLayout4.setVisibility(View.GONE);
        }
    }


    private void toggleUnusedPollVotes(UserProfileAdapter.ViewHolder holder, ParseObject pollObject) {
        if (pollObject.getString(ParseConstants.KEY_POLL_OPTION3) != null) {
            holder.vote3.setVisibility(View.VISIBLE);
        } else {
            holder.vote3.setVisibility(View.GONE);
            if (holder.resultLayout3.getVisibility() == View.VISIBLE) {
                holder.resultLayout3.setVisibility(View.GONE);
            }
        }

        if (pollObject.getString(ParseConstants.KEY_POLL_OPTION4) != null) {
            holder.vote4.setVisibility(View.VISIBLE);
        } else {
            holder.vote4.setVisibility(View.GONE);
            if (holder.resultLayout4.getVisibility() == View.VISIBLE) {
                holder.resultLayout4.setVisibility(View.GONE);
            }
        }
    }


    private void downloadProfilePicture(ViewHolder holder, ParseObject yeets) {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo(ParseConstants.KEY_OBJECT_ID, yeets.getParseObject(ParseConstants.KEY_SENDER_AUTHOR_POINTER).getObjectId());

        query.findInBackground((user, e) -> {
            if (e == null) for (ParseObject userObject : user) {

                if (userObject.getParseFile("profilePicture") != null) {
                    String profilePictureURL = userObject.getParseFile("profilePicture").getUrl();

                    // Asynchronously display the profile picture downloaded from Parse
                    if (profilePictureURL != null) {

                        Picasso.with(mContext)
                                .load(profilePictureURL)
                                .placeholder(R.color.accent)
                                .into(holder.profilePicture);

                    } else {
                        holder.profilePicture.setImageResource(Integer.parseInt(String.valueOf(R.drawable.ic_profile_pic_add)));
                    }
                }

                if (!(userObject.getString(ParseConstants.KEY_AUTHOR_FULL_NAME).isEmpty())) {
                    holder.fullName.setText(userObject.getString(ParseConstants.KEY_AUTHOR_FULL_NAME));
                } else {
                    holder.fullName.setText(R.string.anonymous_fullName);
                }

                holder.username.setText(userObject.getString(ParseConstants.KEY_USERNAME));

            }
        });
    }

    private void downloadMessageImage(ViewHolder holder, int position) {
        ParseQuery<ParseObject> query = new ParseQuery<>(ParseConstants.CLASS_YEET);
        query.whereEqualTo(ParseConstants.KEY_OBJECT_ID, mYeets.get(position).getObjectId());
        query.findInBackground((user, e) -> {
            if (e == null) for (ParseObject userObject : user) {

                if (userObject.getParseFile("image") != null) {
                    String imageURL = userObject.getParseFile("image").getUrl();
                    /*Log.w(getClass().toString(), imageURL);*/

                    // Asynchronously display the message image downloaded from Parse
                    if (imageURL != null) {

                        holder.messageImage.setVisibility(View.VISIBLE);

                        Picasso.with(mContext)
                                .load(imageURL)
                                .placeholder(R.color.placeholderblue)
                                .into(holder.messageImage);

                    } else {
                        holder.messageImage.setVisibility(View.GONE);
                    }
                }

            }
        });
    }

    private void setRantTag(ViewHolder holder, int color, int bgColor) {
        holder.notificationText.setTextColor(ContextCompat.getColor(mContext, color));
        holder.itemView.setBackgroundColor(ContextCompat.getColor(mContext, bgColor));
    }

    /*private void fadeinViews(ViewHolder holder) {
        Animation animFadeIn;
        *//*Animation animFadeOut;*//*

        animFadeIn = AnimationUtils.loadAnimation(mContext, R.anim.fadein);
        *//*animFadeOut = AnimationUtils.loadAnimation(mContext, R.anim.fadeout);*//*

        holder.profilePicture.setAnimation(animFadeIn);
        holder.profilePicture.setVisibility(View.VISIBLE);

        holder.fullName.setAnimation(animFadeIn);
        holder.fullName.setVisibility(View.VISIBLE);

        holder.username.setAnimation(animFadeIn);
        holder.username.setVisibility(View.VISIBLE);
    }*/

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView username;
        TextView fullName;
        TextView notificationText;
        TextView time;
        ImageView profilePicture;
        ImageView likeImage;
        TextView likeCount;
        ImageView premiumContent;
        TextView premiumContentText;
        TextView replyCount;
        ImageView replyImage;
        ImageView messageImage;

        LinearLayout pollVoteLayout;
        LinearLayout pollResultsLayout;
        TextView option1;
        TextView option2;
        TextView option3;
        TextView option4;
        TextView value1;
        TextView value2;
        TextView value3;
        TextView value4;
        TextView vote1;
        TextView vote2;
        TextView vote3;
        TextView vote4;

        RelativeLayout resultLayout1;
        RelativeLayout resultLayout2;
        RelativeLayout resultLayout3;
        RelativeLayout resultLayout4;

        public ViewHolder(View itemView) {
            super(itemView);

            username = (TextView) itemView.findViewById(R.id.username);
            fullName = (TextView) itemView.findViewById(R.id.fullName);
            notificationText = (TextView) itemView.findViewById(R.id.messageText);
            time = (TextView) itemView.findViewById(R.id.time);
            profilePicture = (ImageView) (itemView.findViewById(R.id.profilePicture));
            likeImage = (ImageView) itemView.findViewById(R.id.likeImage);
            likeCount = (TextView) itemView.findViewById(R.id.likeCount);
            premiumContent = (ImageView) itemView.findViewById(R.id.premiumContent);
            premiumContentText = (TextView) itemView.findViewById(R.id.premiumContentText);
            replyCount = (TextView) itemView.findViewById(R.id.replyCount);
            replyImage = (ImageView) itemView.findViewById(R.id.replyImage);
            messageImage = (ImageView) itemView.findViewById(R.id.messageImage);

            pollVoteLayout = (LinearLayout) itemView.findViewById(R.id.pollVoteLayout);
            pollResultsLayout = (LinearLayout) itemView.findViewById(R.id.pollResultsLayout);
            option1 = (TextView) itemView.findViewById(R.id.option1);
            option2 = (TextView) itemView.findViewById(R.id.option2);
            option3 = (TextView) itemView.findViewById(R.id.option3);
            option4 = (TextView) itemView.findViewById(R.id.option4);
            value1 = (TextView) itemView.findViewById(R.id.value1);
            value2 = (TextView) itemView.findViewById(R.id.value2);
            value3 = (TextView) itemView.findViewById(R.id.value3);
            value4 = (TextView) itemView.findViewById(R.id.value4);
            vote1 = (TextView) itemView.findViewById(R.id.vote1);
            vote2 = (TextView) itemView.findViewById(R.id.vote2);
            vote3 = (TextView) itemView.findViewById(R.id.vote3);
            vote4 = (TextView) itemView.findViewById(R.id.vote4);

            resultLayout1 = (RelativeLayout) itemView.findViewById(R.id.resultLayout1);
            resultLayout2 = (RelativeLayout) itemView.findViewById(R.id.resultLayout2);
            resultLayout3 = (RelativeLayout) itemView.findViewById(R.id.resultLayout3);
            resultLayout4 = (RelativeLayout) itemView.findViewById(R.id.resultLayout4);

            /*fadeInViews();*/

        }

        /*private void fadeInViews() {
            fadeinViews(ViewHolder.this);
        }*/
    }

}
