package com.yeetclub.comment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
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
import com.yeetclub.parse.ParseConstants;
import com.yeetclub.profile.UserProfileActivity;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by @santafebound on 2015-11-07.
 */
public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {

    protected Context mContext;
    protected List<ParseObject> mYeets;
    private CommentAdapter adapter;
    private String commentId;

    public CommentAdapter(Context context, List<ParseObject> yeets, String commentId) {
        super();

        this.mContext = context;
        this.mYeets = yeets;
        this.commentId = commentId;
        this.adapter = this;
    }

    private void setLikeImageHolderResource(int position, ViewHolder holder) {
        ParseQuery<ParseObject> query = new ParseQuery<>(ParseConstants.CLASS_COMMENT);
        query.whereEqualTo(ParseConstants.KEY_OBJECT_ID, mYeets.get(position).getObjectId());
        query.fromLocalDatastore();
        query.findInBackground((comment, e) -> {
            // Find the single Comment object associated with the current ListAdapter position
            if (e == null) for (ParseObject commentObject : comment) {

                // Create a list to store the likers of this Comment
                List<String> likedBy = commentObject.getList("likedBy");
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

    private void createLike(int position) {
        ParseQuery<ParseObject> query = new ParseQuery<>(ParseConstants.CLASS_COMMENT);
        query.whereEqualTo(ParseConstants.KEY_OBJECT_ID, mYeets.get(position).getObjectId());
        query.fromLocalDatastore();
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
                    incrementLikeCount(commentObject, position);

                    // Initiate Like notification
                    handleLikeNotification(commentObject);

                } else {
                    Toast.makeText(mContext, "You already liked this Yeet", Toast.LENGTH_SHORT).show();
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
        String commentId = commentObject.getString(ParseConstants.KEY_SENDER_PARSE_OBJECT_ID);
        String result = commentObject.getString(ParseConstants.KEY_COMMENT_TEXT);

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

    private void setPremiumContent(ViewHolder holder, int visibility) {
        holder.premiumContent.setVisibility(visibility);
        holder.premiumContentText.setVisibility(visibility);
        Typeface tf_reg = Typeface.createFromAsset(mContext.getAssets(), "fonts/Lato-Regular.ttf");
        holder.premiumContentText.setTypeface(tf_reg);
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

    private void incrementLikeCount(ParseObject commentObject, int position) {
        // Increment likeCount on related Comment object
        commentObject.increment("likeCount");
        this.adapter.notifyDataSetChanged();
        commentObject.saveEventually();
    }

    private void deleteComment(int position) {
        String currentUserObjectId = ParseUser.getCurrentUser().getObjectId();
        ParseQuery<ParseObject> query = new ParseQuery<>("Comment");
        query.whereEqualTo(ParseConstants.KEY_OBJECT_ID, mYeets.get(position).getObjectId());
        query.whereContains(ParseConstants.KEY_SENDER_ID, currentUserObjectId);
        query.fromLocalDatastore();
        query.findInBackground((yeet, e) -> {
            if (e == null) {

                for (ParseObject yeetObject : yeet) {

                    if (yeetObject.getParseObject(ParseConstants.KEY_SENDER_AUTHOR_POINTER).getObjectId().equals((ParseUser.getCurrentUser().getObjectId()))) {

                        for (ParseObject delete : yeet) {

                            decrementReplyCount(yeetObject);
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

    private void decrementReplyCount(ParseObject yeetObject) {
        ParseQuery<ParseObject> query = new ParseQuery<>(ParseConstants.CLASS_YEET);
        query.whereEqualTo(ParseConstants.KEY_OBJECT_ID, yeetObject.getString(ParseConstants.KEY_SENDER_PARSE_OBJECT_ID));
        query.fromLocalDatastore();
        query.findInBackground((yeet, e) -> {

            // Find the single Comment object associated with the current ListAdapter position
            if (e == null) for (ParseObject yeetObject2 : yeet) {

                /*System.out.println(yeetObject2.getObjectId());
                System.out.println(yeetObject.getString(ParseConstants.KEY_SENDER_PARSE_OBJECT_ID));

                Log.w(getClass().toString(), "Do we get here?");*/
                if (!((yeetObject2.getInt("replyCount")) == 0)) {
                    yeetObject2.increment("replyCount", -1);
                    yeetObject2.saveEventually();
                }

            }
        });

    }

    /**
     *
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
    public CommentAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(mContext).inflate(R.layout.comment_listview_item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        final ParseObject yeets = mYeets.get(position);

        Date createdAt = yeets.getCreatedAt();
        long now = new Date().getTime();
        String convertedDate = DateUtils.getRelativeTimeSpanString(createdAt.getTime(), now, DateUtils.SECOND_IN_MILLIS).toString();

        setLikeImageHolderResource(position, holder);

        holder.notificationText.setText(yeets.getString(ParseConstants.KEY_COMMENT_TEXT));

        downloadMessageImage(holder, position);

        int likeCount_int = yeets.getInt(ParseConstants.KEY_LIKE_COUNT);
        String likeCount_string = Integer.toString(likeCount_int);
        holder.likeCount.setText(likeCount_string);

        if (likeCount_int >= 4) {
            setPremiumContent(holder, View.VISIBLE);
        } else {
            setPremiumContent(holder, View.GONE);
        }

        holder.time.setText(convertedDate);

        /*holder.fadeInViews();*/
        downloadProfilePicture(holder, yeets);

        holder.username.setOnClickListener(v -> retrievePointerObjectId(yeets));

        holder.fullName.setOnClickListener(v -> retrievePointerObjectId(yeets));

        holder.profilePicture.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.image_click));
            retrievePointerObjectId(yeets);
        });

        holder.likeImage.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.like_click));
            createLike(position);
        });

        holder.itemView.setOnLongClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            deleteComment(position);
            return true;
        });

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

    private void downloadProfilePicture(ViewHolder holder, ParseObject yeets) {
        // Asynchronously display the profile picture downloaded from parse
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo(ParseConstants.KEY_OBJECT_ID, yeets.getParseObject(ParseConstants.KEY_SENDER_AUTHOR_POINTER).getObjectId());
        query.fromLocalDatastore();
        query.findInBackground((user, e) -> {
            if (e == null) for (ParseObject userObject : user) {

                if (userObject.getParseFile("profilePicture") != null) {
                    String profilePictureURL = userObject.getParseFile("profilePicture").getUrl();

                    // Asynchronously display the profile picture downloaded from Parse
                    if (profilePictureURL != null) {

                        Picasso.with(mContext)
                                .load(profilePictureURL)
                                .placeholder(R.color.placeholderblue)
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
        ParseQuery<ParseObject> query = new ParseQuery<>(ParseConstants.CLASS_COMMENT);
        query.whereEqualTo(ParseConstants.KEY_OBJECT_ID, mYeets.get(position).getObjectId());
        query.fromLocalDatastore();
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

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView username;
        TextView fullName;
        TextView notificationText;
        ImageView messageImage;
        TextView time;
        TextView likeCount;
        ImageView profilePicture;
        ImageView likeImage;
        ImageView premiumContent;
        TextView premiumContentText;
        LinearLayout messageImageLayout;

        public ViewHolder(View itemView) {
            super(itemView);

            username = (TextView) itemView.findViewById(R.id.username);
            fullName = (TextView) itemView.findViewById(R.id.fullName);
            notificationText = (TextView) itemView.findViewById(R.id.notificationText);
            messageImage = (ImageView) itemView.findViewById(R.id.messageImage);
            time = (TextView) itemView.findViewById(R.id.time);
            profilePicture = (ImageView) itemView.findViewById(R.id.profilePicture);
            likeImage = (ImageView) itemView.findViewById(R.id.likeImage);
            likeCount = (TextView) itemView.findViewById(R.id.likeCount);
            premiumContent = (ImageView) itemView.findViewById(R.id.premiumContent);
            premiumContentText = (TextView) itemView.findViewById(R.id.premiumContentText);
            messageImageLayout = (LinearLayout) itemView.findViewById(R.id.messageImageLayout);

            /*fadeInViews();*/
        }

        /*private void fadeInViews() {
            fadeinViews(ViewHolder.this);
        }*/
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

}
