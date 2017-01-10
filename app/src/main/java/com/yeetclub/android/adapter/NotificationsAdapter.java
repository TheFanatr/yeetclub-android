package com.yeetclub.android.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
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
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.yeetclub.android.activity.MediaPreviewActivity;
import com.yeetclub.android.R;
import com.yeetclub.android.activity.CommentActivity;
import com.yeetclub.android.activity.ReplyActivity;
import com.yeetclub.android.utility.RecyclerViewSimpleTextViewHolder;
import com.yeetclub.android.parse.ParseConstants;
import com.yeetclub.android.activity.UserProfileActivity;
import com.yeetclub.android.utility.NetworkHelper;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by @santafebound on 2015-11-07.
 */
public class NotificationsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final int LIKE = 0, COMMENT = 1;

    protected Context mContext;
    protected List<ParseObject> mNotifications;
    private NotificationsAdapter adapter;

    public NotificationsAdapter(Context context, List<ParseObject> yeets) {
        super();

        this.mNotifications = yeets;
        this.mContext = context;
        this.adapter = this;
    }

    private void launchCommentFromNotification(ParseObject notifications) {

        // We retrieve the permanent objectId of the Yeet
        String userId = String.valueOf(notifications.getParseObject(ParseConstants.KEY_SENDER_AUTHOR_POINTER).getObjectId());
        String commentId = String.valueOf(notifications.getString(ParseConstants.KEY_COMMENT_OBJECT_ID));

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
     * @param notifications A list derived from the main "Yeet" ParseObject (Yeet), from which also user information may be obtained via the _User pointer "author".
     */
    private void retrievePointerObjectId(ParseObject notifications) {
        // We want to retrieve the permanent user objectId from the author of the Yeet so that we can always launch the user's profile, even if the author changes their username in the future.
        String userId = String.valueOf(notifications.getParseObject(ParseConstants.KEY_SENDER_AUTHOR_POINTER).getObjectId());

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
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        RecyclerView.ViewHolder viewHolder;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case LIKE:
                View v1 = inflater.inflate(R.layout.notifications_listview_item, parent, false);
                viewHolder = new ViewHolder(v1);
                break;
            case COMMENT:
                View v2 = inflater.inflate(R.layout.yeet_listview_item, parent, false);
                viewHolder = new ViewHolder2(v2);
                break;
            default:
                View v = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
                viewHolder = new RecyclerViewSimpleTextViewHolder(v);
                break;
        }
        return viewHolder;
    }

    private void configureDefaultViewHolder(RecyclerViewSimpleTextViewHolder vh, int position) {
        vh.getLabel().setText((CharSequence) mNotifications.get(position));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        switch (holder.getItemViewType()) {
            case LIKE:
                ViewHolder vh1 = (ViewHolder) holder;
                configureViewHolder1(vh1, position);
                break;
            case COMMENT:
                ViewHolder2 vh2 = (ViewHolder2) holder;
                configureViewHolder2(vh2, position);
                break;
            default:
                RecyclerViewSimpleTextViewHolder vh = (RecyclerViewSimpleTextViewHolder) holder;
                configureDefaultViewHolder(vh, position);
                break;
        }
    }

    private void setNotificationTag(ViewHolder holder, int color, int bgColor) {
        holder.notificationText.setTextColor(ContextCompat.getColor(mContext, color));
        holder.itemView.setBackgroundColor(ContextCompat.getColor(mContext, bgColor));
    }

    private void downloadProfilePicture(ViewHolder holder, ParseObject notifications) {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo(ParseConstants.KEY_OBJECT_ID, notifications.getParseObject(ParseConstants.KEY_SENDER_AUTHOR_POINTER).getObjectId());
        query.fromLocalDatastore();
        query.findInBackground((user, e) -> {
            if (e == null) for (ParseObject userObject : user) {

                if (userObject.getParseFile(ParseConstants.KEY_PROFILE_PICTURE) != null) {
                    String profilePictureURL = userObject.getParseFile(ParseConstants.KEY_PROFILE_PICTURE).getUrl();

                    // Asynchronously display the profile picture downloaded from Parse
                    if (profilePictureURL != null) {

                        Picasso.with(mContext)
                                .load(profilePictureURL)
                                .networkPolicy(NetworkPolicy.OFFLINE)
                                .placeholder(R.color.placeholderblue)
                                .fit()
                                .into(holder.profilePicture);

                    } else {
                        holder.profilePicture.setImageResource(Integer.parseInt(String.valueOf(R.drawable.ic_profile_pic_add)));
                    }
                } else {
                    holder.profilePicture.setImageResource(Integer.parseInt(String.valueOf(R.drawable.ic_profile_pic_add)));
                }

                if (userObject.getString(ParseConstants.KEY_AUTHOR_FULL_NAME) != null) {
                    holder.fullName.setText(userObject.getString(ParseConstants.KEY_AUTHOR_FULL_NAME));
                } else {
                    holder.fullName.setText(R.string.anonymous_fullName);
                }

            }
        });
    }

    private void downloadProfilePicture(ViewHolder2 holder, ParseObject notifications) {
        // Asynchronously display the profile picture downloaded from parse
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo(ParseConstants.KEY_OBJECT_ID, notifications.getParseObject(ParseConstants.KEY_SENDER_AUTHOR_POINTER).getObjectId());
        query.fromLocalDatastore();
        query.findInBackground((user, e) -> {
            if (e == null) for (ParseObject userObject : user) {

                if (userObject.getParseFile("profilePicture") != null) {
                    String profilePictureURL = userObject.getParseFile("profilePicture").getUrl();

                    // Asynchronously display the profile picture downloaded from Parse
                    if (profilePictureURL != null) {

                        Picasso.with(mContext)
                                .load(profilePictureURL)
                                .networkPolicy(NetworkPolicy.OFFLINE)
                                .placeholder(R.color.placeholderblue)
                                .fit()
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

    private void createLike(int position) {

        final ParseObject notification = mNotifications.get(position);

        ParseQuery<ParseObject> query = new ParseQuery<>(ParseConstants.CLASS_YEET);
        query.whereEqualTo(ParseConstants.KEY_OBJECT_ID, notification.getString("commentObjectId"));
        query.fromLocalDatastore();
        query.findInBackground((comment, e) -> {
            // Find the single Comment object associated with the current ListAdapter position
            if (e == null) for (ParseObject yeetObject : comment) {

                // Create a list to store the likers of this Comment
                List<String> likedBy = yeetObject.getList("likedBy");
                /*System.out.println("Liked by: " + likedBy);*/

                // If you are not on that list, then create a Like
                if (!(likedBy.contains(ParseUser.getCurrentUser().getObjectId()))) {

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
        String result = yeetObject.getString(ParseConstants.KEY_NOTIFICATION_TEXT);
        /*System.out.println("Yeet text: " + result);*/

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

        notification.put(ParseConstants.KEY_SENDER_AUTHOR_POINTER, ParseUser.getCurrentUser());
        notification.put(ParseConstants.KEY_NOTIFICATION_BODY, result);
        notification.put(ParseConstants.KEY_SENDER_NAME, ParseUser.getCurrentUser().getUsername());
        notification.put(ParseConstants.KEY_RECIPIENT_ID, userId);
        notification.put(ParseConstants.KEY_COMMENT_OBJECT_ID, commentId);
        notification.put(ParseConstants.KEY_NOTIFICATION_TEXT, " liked your yeet!");
        notification.put(ParseConstants.KEY_NOTIFICATION_TYPE, ParseConstants.TYPE_LIKE);
        notification.put(ParseConstants.KEY_READ_STATE, false);

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
        mNotifications.get(position).increment("likeCount");
        this.adapter.notifyDataSetChanged();
        yeetObject.saveEventually();
    }

    private void configureViewHolder1(ViewHolder holder, int position) {
        final ParseObject notifications = mNotifications.get(position);

        Date createdAt = notifications.getCreatedAt();
        long now = new Date().getTime();
        String convertedDate = DateUtils.getRelativeTimeSpanString(createdAt.getTime(), now, DateUtils.SECOND_IN_MILLIS).toString();

        Typeface tf_bold = Typeface.createFromAsset(mContext.getAssets(), "fonts/Lato-Bold.ttf");
        Typeface tf_reg = Typeface.createFromAsset(mContext.getAssets(), "fonts/Lato-Regular.ttf");

        holder.fullName.setTypeface(tf_bold);

        String notificationText = notifications.getString(ParseConstants.KEY_NOTIFICATION_TEXT);
        holder.notificationText.setText(notificationText);

        String notificationBody = notifications.getString(ParseConstants.KEY_NOTIFICATION_BODY);
        holder.notificationBody.setText(notificationBody);

        Boolean isRead = notifications.getBoolean("read");
        /*System.out.println(isRead);*/
        if (isRead) {
            int color = R.color.stroke;
            int bgColor = R.color.white;
            setNotificationTag(holder, color, bgColor);
        } else {
            int color = R.color.stroke;
            int bgColor = R.color.light_blue;
            setNotificationTag(holder, color, bgColor);
        }

        holder.time.setText(convertedDate);
        /*Log.w(getClass().toString(), convertedDate + ": " + notificationBody);*/

        /*fadeinViews(holder);*/

        downloadProfilePicture(holder, notifications);

        if (notifications.getString(ParseConstants.KEY_NOTIFICATION_TYPE).equals(ParseConstants.TYPE_LIKE)) {
            holder.notificationsIcon.setImageResource(R.drawable.ic_action_like_feed_full);
        }

        if (notifications.getString(ParseConstants.KEY_NOTIFICATION_TYPE).equals(ParseConstants.TYPE_COMMENT)) {
            holder.notificationsIcon.setImageResource(R.drawable.ic_action_comment);
        }

        holder.fullName.setOnClickListener(v -> retrievePointerObjectId(notifications));

        holder.profilePicture.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.image_click));
            retrievePointerObjectId(notifications);
        });

        holder.notificationsIcon.setOnClickListener(v -> {
        });

        holder.itemView.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.slide_down_dialog));
            launchCommentFromNotification(notifications);
        });
    }

    private void configureViewHolder2(ViewHolder2 holder, int position) {
        final ParseObject yeet = mNotifications.get(position);
        /*System.out.println(yeet.getObjectId());*/

        Date createdAt = yeet.getCreatedAt();
        long now = new Date().getTime();
        String convertedDate = DateUtils.getRelativeTimeSpanString(createdAt.getTime(), now, DateUtils.SECOND_IN_MILLIS).toString();

        setLikeImageHolderResource(position, holder);

        if (!(yeet.getString(ParseConstants.KEY_NOTIFICATION_TEXT).isEmpty())) {
            holder.messageText.setText(yeet.getString(ParseConstants.KEY_NOTIFICATION_BODY));
        } else {
            holder.messageText.setVisibility(View.GONE);
        }

        holder.time.setText(convertedDate);

        downloadMessageImage(holder, position);

        int likeCount_int = yeet.getInt(ParseConstants.KEY_LIKE_COUNT);
        String likeCount_string = Integer.toString(likeCount_int);
        holder.likeCount.setText(likeCount_string);

        int replyCount_int = yeet.getInt(ParseConstants.KEY_REPLY_COUNT);
        String replyCount_string = Integer.toString(replyCount_int);
        holder.replyCount.setText(replyCount_string);

        if (likeCount_int >= 4) {
            setPremiumContent(holder, View.VISIBLE);
        } else {
            setPremiumContent(holder, View.GONE);
        }

        /*Boolean isRant = yeet.getBoolean("isRant");
        *//*System.out.println(isRant);*//*
        if (isRant) {
            int color = R.color.stroke;
            int bgColor = R.color.lightred;
            setRantTag(holder, color, bgColor);
        } else {
            int color = R.color.stroke;
            int bgColor = R.color.white;
            setRantTag(holder, color, bgColor);
        }*/

        /*fadeinViews(holder);*/

        downloadProfilePicture(holder, yeet);

        holder.messageImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.image_click));

                ParseQuery<ParseObject> imageQuery = new ParseQuery<>(ParseConstants.CLASS_YEET);
                imageQuery.whereEqualTo(ParseConstants.KEY_OBJECT_ID, yeet.getObjectId());
                imageQuery.fromLocalDatastore();
                imageQuery.findInBackground((user, e2) -> {
                    if (e2 == null) for (ParseObject userObject : user) {

                        if (userObject.getParseFile("image") != null) {
                            String imageURL = userObject.getParseFile("image").getUrl();
                            Log.w(getClass().toString(), imageURL);

                            // Asynchronously display the message image downloaded from Parse
                            if (imageURL != null) {

                                Intent intent = new Intent(mContext, MediaPreviewActivity.class);
                                intent.putExtra("imageUrl", imageURL);
                                mContext.startActivity(intent);

                            }

                        }
                    }
                });

            }
        });

        holder.replyImage.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.image_click));
            retrievePointerObjectIdForReply(yeet, position);
        });

        holder.username.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.image_click));
            retrievePointerObjectId(yeet);
        });

        holder.fullName.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.image_click));
            retrievePointerObjectId(yeet);
        });

        holder.profilePicture.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.image_click));
            retrievePointerObjectId(yeet);
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
                v.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.like_click));
                launchCommentFromNotification(yeet);
            }
        });
    }

    private void retrievePointerObjectIdForReply(ParseObject yeets, int position) {
        /*String commentId = String.valueOf(yeets.getParseObject(ParseConstants.KEY_SENDER_POST_POINTER).getObjectId());*/

        final ParseObject notification = mNotifications.get(position);

        // We retrieve the permanent objectId of the Yeet
        String userId = String.valueOf(yeets.getParseObject(ParseConstants.KEY_SENDER_AUTHOR_POINTER).getObjectId());
        String commentId = String.valueOf(notification.get("commentObjectId"));

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

    private void setLikeImageHolderResource(int position, ViewHolder2 holder) {
        ParseQuery<ParseObject> query = new ParseQuery<>(ParseConstants.CLASS_YEET);
        query.whereEqualTo(ParseConstants.KEY_OBJECT_ID, mNotifications.get(position).getObjectId());
        query.fromLocalDatastore();
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

    private void downloadMessageImage(ViewHolder2 holder, int position) {
        ParseQuery<ParseObject> query = new ParseQuery<>(ParseConstants.CLASS_YEET);
        query.whereEqualTo(ParseConstants.KEY_OBJECT_ID, mNotifications.get(position).getObjectId());
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

    private void setPremiumContent(ViewHolder2 holder, int visibility) {
        holder.premiumContent.setVisibility(visibility);
        holder.premiumContentText.setVisibility(visibility);
        Typeface tf_reg = Typeface.createFromAsset(mContext.getAssets(), "fonts/Lato-Regular.ttf");
        holder.premiumContentText.setTypeface(tf_reg);
    }

    @Override
    public int getItemCount() {
        if (mNotifications == null) {
            return 0;
        } else {
            return mNotifications.size();
        }
    }

    public int getItemViewType(int position) {
        final ParseObject notifications = mNotifications.get(position);
        if (notifications.getString(ParseConstants.KEY_NOTIFICATION_TYPE).equals(ParseConstants.TYPE_LIKE)) {
            return LIKE;
        } else {
            return COMMENT;
        }
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
    }*/

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView fullName;
        TextView notificationText;
        TextView notificationBody;
        TextView time;
        ImageView profilePicture;
        ImageView notificationsIcon;

        public ViewHolder(View itemView) {
            super(itemView);

            fullName = (TextView) itemView.findViewById(R.id.fullName);
            notificationText = (TextView) itemView.findViewById(R.id.notificationText);
            notificationBody = (TextView) itemView.findViewById(R.id.notificationBody);
            time = (TextView) itemView.findViewById(R.id.time);
            profilePicture = (ImageView) (itemView.findViewById(R.id.profilePicture));
            notificationsIcon = (ImageView) (itemView.findViewById(R.id.notificationsIcon));

            /*fadeInViews();*/

        }

        /*private void fadeInViews() {
            fadeinViews(ViewHolder.this);
        }*/
    }

    public class ViewHolder2 extends RecyclerView.ViewHolder {
        TextView username;
        TextView fullName;
        TextView replyCount;
        TextView messageText;
        ImageView messageImage;
        TextView time;
        ImageView profilePicture;
        ImageView likeImage;
        TextView likeCount;
        ImageView premiumContent;
        TextView premiumContentText;
        ImageView replyImage;
        LinearLayout messageImageLayout;

        public ViewHolder2(View itemView) {
            super(itemView);

            username = (TextView) itemView.findViewById(R.id.username);
            fullName = (TextView) itemView.findViewById(R.id.fullName);
            messageText = (TextView) itemView.findViewById(R.id.messageText);
            messageImage = (ImageView) itemView.findViewById(R.id.messageImage);
            time = (TextView) itemView.findViewById(R.id.time);
            profilePicture = (ImageView) (itemView.findViewById(R.id.profilePicture));
            messageImageLayout = (LinearLayout) itemView.findViewById(R.id.messageImageLayout);
            likeImage = (ImageView) itemView.findViewById(R.id.likeImage);
            likeCount = (TextView) itemView.findViewById(R.id.likeCount);
            replyCount = (TextView) itemView.findViewById(R.id.replyCount);
            replyImage = (ImageView) itemView.findViewById(R.id.replyImage);
            premiumContent = (ImageView) itemView.findViewById(R.id.premiumContent);
            premiumContentText = (TextView) itemView.findViewById(R.id.premiumContentText);

            /*fadeInViews();*/

        }

        /*private void fadeInViews() {
            fadeinViews(ViewHolder.this);
        }*/
    }

}
