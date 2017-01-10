package com.yeetclub.android.parse;

import android.graphics.Bitmap;
import android.util.Log;

import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

/**
 * Created by @santafebound on 2015-11-07.
 */
public class ParseHelper {

    private static String TAG = "ParseHelper";

    public static boolean isCurrentUser(String userID) {
        ParseUser currentUser = ParseUser.getCurrentUser();
        return currentUser != null && currentUser.getObjectId().equals(userID);
    }

    public static YeetClubUser GetUserInformation(String objectId) throws ParseException {
        return CreateHyperCycleUser(
                (objectId == null || isCurrentUser(objectId)) ?
                        ParseUser.getCurrentUser() :

                        ParseUser.getQuery().get(objectId)
        );
    }

    public static YeetClubUser CreateHyperCycleUser(ParseUser userObject) {

        // sanity check
        if (userObject == null) {
            return null;
        }

        // try to fetch, if not just use what we have for current user
        try {
            userObject.fetchIfNeeded();
        } catch (ParseException e1) {
            Log.d(TAG, e1.toString());
        }

        YeetClubUser user = new YeetClubUser();

        user.setName(userObject.getString("name"));
        user.setUsername(userObject.getUsername());
        user.setBio(userObject.getString("bio"));
        user.setBae(userObject.getString("bae"));
        user.setWebsiteLink(userObject.getString("websiteLink"));

        // get the image
        ParseFile image = (ParseFile) userObject.get("profilePicture");

        if (image != null) {
            user.setProfilePictureURL(image.getUrl());
        }

        return user;
    }

    public static void UploadProfilePictureToCurrentUser(Bitmap bitmap) {

        // sanity check
        if (bitmap == null) {
            Log.d(TAG, "Unable to save profile picture. imageUri is null.");
            return;
        }

        Bitmap thumbnail = Bitmap.createScaledBitmap(bitmap, 360, 540, false);

        // Convert it to byte
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 30, stream);
        byte[] thumbnailData = stream.toByteArray();

        // Create the ParseFile
        ParseFile file = new ParseFile(UUID.randomUUID() + ".jpeg", thumbnailData);
        file.saveInBackground();

        ParseUser currentUser = ParseUser.getCurrentUser();

        if (currentUser == null) {
            Log.d(TAG, "Unable to save profile picture. Current user is null");
            return;
        }

        currentUser.put("profilePicture", file);

        // save the new object
        currentUser.saveInBackground(new SaveCallback() {
            public void done(ParseException e) {
                if (e == null) {
                    Log.d(TAG, "Successfully saved Parse user with profile picture");
                } else {
                    Log.d(TAG, e.toString());
                }
            }
        });
    }

}
