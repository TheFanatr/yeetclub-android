package com.yeetclub.android.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.parse.ParseObject;
import com.parse.ParseUser;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.yeetclub.android.R;
import com.yeetclub.android.activity.UserProfileActivity;
import com.yeetclub.android.parse.ParseConstants;

import java.util.List;

/**
 * Created by @santafebound on 2015-11-07.
 */
public class UsersListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context mContext;
    private List<ParseUser> mUsers;

    public UsersListAdapter(Context context, List<ParseUser> users) {
        super();

        this.mUsers = users;
        this.mContext = context;
        UsersListAdapter adapter = this;
    }


    /**
     * @param user The current ParseUser
     */
    private void retrievePointerObjectId(ParseObject user) {
        // We want to retrieve the permanent user objectId from the author of the Yeet so that we can always launch the user's profile, even if the author changes their username in the future.
        String userId = String.valueOf(user.getObjectId());

        // We use the generated userId to launch the user profile depending on whether we arrive to the profile as ourselves or are visiting externally from another feed or Yeet
        startGalleryActivity(userId);
    }


    private void startGalleryActivity(String userId) {
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
    public UsersListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.users_listview_item, parent, false);
        return new UsersListAdapter.ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ViewHolder vh1 = (ViewHolder) holder;
        configureViewHolder1(vh1, position);
    }


    private void configureViewHolder1(ViewHolder holder, int position) {
        // Define a single ParseObject from a list of ParseUser objects, i.e. private List<ParseUser> mUsers;
        final ParseObject user = mUsers.get(position);

        // Define Typeface Lato-Bold
        Typeface tfBold = Typeface.createFromAsset(mContext.getAssets(), "fonts/Lato-Bold.ttf");

        // Retrieve full name
        holder.fullName.setTypeface(tfBold);
        if (user.getString(ParseConstants.KEY_AUTHOR_FULL_NAME) != null) {
            holder.fullName.setText(user.getString(ParseConstants.KEY_AUTHOR_FULL_NAME));
        } else {
            holder.fullName.setText(R.string.anonymous_fullName);
        }
        holder.fullName.setOnClickListener(v -> retrievePointerObjectId(user));

        // Retrieve username
        holder.username.setText(user.getString(ParseConstants.KEY_USERNAME));
        holder.username.setOnClickListener(v -> retrievePointerObjectId(user));

        // Retrieve profile picture
        downloadProfilePicture(holder, user);

        // Launch user profile from profile picture
        holder.profilePicture.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.image_click));
            retrievePointerObjectId(user);
        });

        // Launch user profile from itemView
        holder.itemView.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.image_click));
            retrievePointerObjectId(user);
        });
    }


    private void downloadProfilePicture(ViewHolder holder, ParseObject user) {
        if (user.getParseFile(ParseConstants.KEY_PROFILE_PICTURE) != null) {
            String profilePictureURL = user.getParseFile(ParseConstants.KEY_PROFILE_PICTURE).getUrl();

            // Asynchronously display the profile picture downloaded from Parse
            if (profilePictureURL != null) {

                Picasso.with(mContext)
                        .load(profilePictureURL)
                        .networkPolicy(NetworkPolicy.OFFLINE)
                        .placeholder(R.color.placeholderblue)
                        .fit()
                        .into(holder.profilePicture);

            } else {
                holder.profilePicture.setImageResource(R.drawable.ic_profile_pic_add);
            }
        } else {
            holder.profilePicture.setImageResource(R.drawable.ic_profile_pic_add);
        }
    }


    @Override
    public int getItemCount() {
        if (mUsers == null) {
            return 0;
        } else {
            return mUsers.size();
        }
    }


    @Override
    public int getItemViewType(int position) {
        return position;
    }


    private class ViewHolder extends RecyclerView.ViewHolder {
        TextView fullName;
        TextView username;
        ImageView profilePicture;
        TextView points;
        TextView rank;

        ViewHolder(View itemView) {
            super(itemView);

            fullName = (TextView) itemView.findViewById(R.id.fullName);
            username = (TextView) itemView.findViewById(R.id.username);
            profilePicture = (ImageView) (itemView.findViewById(R.id.profilePicture));

        }
    }

}
