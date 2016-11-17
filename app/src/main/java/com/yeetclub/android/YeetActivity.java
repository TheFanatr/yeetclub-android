package com.yeetclub.android;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.yeetclub.parse.ParseConstants;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static android.view.View.GONE;
import static com.yeetclub.android.R.raw.yeet;

/**
 * Created by @santafebound on 2015-11-07.
 */
public class YeetActivity extends AppCompatActivity {

    private static final int SELECT_PHOTO = 2;

    public final static String SELECTED_FEED_OBJECT_ID = "com.yeetclub.android.SELECTED_FEED_OBJECT_ID";
    public final static String SELECTED_USER_OBJECT_ID = "com.yeetclub.android.SELECTED_USER_OBJECT_ID";

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        assert getSupportActionBar() != null;
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

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
                if (null != myEditText.getLayout() && myEditText.getLayout().getLineCount() > 6)  {
                    myEditText.getText().delete(myEditText.getText().length() - 1, myEditText.getText().length());
                }
            }
        };

        myEditText.addTextChangedListener(watcher);

        // Method to submit message when keyboard Enter key is pressed
        /*myEditText.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN)
            {
                switch (keyCode)
                {
                    case KeyEvent.KEYCODE_DPAD_CENTER:
                    case KeyEvent.KEYCODE_ENTER:
                        sendYeet(myEditText);
                        return true;
                    default:
                        break;
                }
            }
            return false;
        });*/

        myEditText.setError(null);
        myEditText.getBackground().mutate().setColorFilter(
                ContextCompat.getColor(getApplicationContext() , R.color.white),
                PorterDuff.Mode.SRC_ATOP);

        Button submitComment = (Button) findViewById(R.id.submitComment);
        Button startRant = (Button) findViewById(R.id.startRant);
        Button exitRant = (Button) findViewById(R.id.exitRant);
        Button submitRant = (Button) findViewById(R.id.submitRant);

        // Set typeface for Button and EditText
        Typeface tf_bold = Typeface.createFromAsset(getAssets(), "fonts/Lato-Bold.ttf");
        submitComment.setTypeface(tf_bold);
        startRant.setTypeface(tf_bold);
        exitRant.setTypeface(tf_bold);
        submitRant.setTypeface(tf_bold);

        Typeface tf_reg = Typeface.createFromAsset(getAssets(), "fonts/Lato-Regular.ttf");
        myEditText.setTypeface(tf_reg);

        submitComment.setOnClickListener(view -> {

            Boolean isRanting = ParseUser.getCurrentUser().getBoolean("isRanting");

            view.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.image_click));

            String rantId = "";
            sendYeet(myEditText, isRanting, rantId);

        });

        submitRant.setOnClickListener(view -> {

            ParseQuery<ParseUser> query = ParseUser.getQuery();
            query.whereEqualTo(ParseConstants.KEY_OBJECT_ID, ParseUser.getCurrentUser().getObjectId());
            query.findInBackground((user, e) -> {
                if (e == null) for (ParseObject userObject : user) {

                    Boolean isRanting = userObject.getBoolean("isRanting");

                    view.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.image_click));

                    String rantId = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("rantId", "");
                    sendYeet(myEditText, isRanting, rantId);

                }
            });

        });

        startRant.setOnClickListener(view -> {

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(YeetActivity.this);
            dialogBuilder.setTitle("Warning: Entering Rant Mode");
            dialogBuilder.setMessage("Ranting may disturb your friends. Are you sure you wish to proceed?");
            dialogBuilder.setPositiveButton("Yes", (dialog, which) -> {

                // Start rant mode
                turnOnRanting();

                // Create single UUID for this particular rant
                UUID randomUUID = UUID.randomUUID();
                String rantId = String.valueOf(randomUUID);

                // Stores a single UUID as a preference to be used for each successive rant submission until the rant is complete
                SharedPreferences myRantId = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = myRantId.edit();
                editor.putString("rantId", rantId);
                editor.commit();

                // Send everyone in the group a push notification
                sendRantPushNotification();

                view.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.image_click));

                submitRant.setVisibility(View.VISIBLE);
                slideUp(submitRant);

                submitComment.setVisibility(GONE);
                slideDown(submitComment);

                exitRant.setVisibility(View.VISIBLE);
                slideUp(exitRant);

                startRant.setVisibility(GONE);
                slideDown(startRant);

            });
            dialogBuilder.setNegativeButton("No", (dialog, which) -> {
            });
            AlertDialog alertDialog = dialogBuilder.create();
            alertDialog.show();

        });

        exitRant.setOnClickListener(view -> {

            // Turn off rant mode
            turnOffRanting();

            if (!(myEditText.getText().toString().isEmpty())) {
                // Send everyone in the group a push notification
                sendRantStopPushNotification();
                Toast.makeText(getApplicationContext(), "Doesn't that just nicely feel betts?", Toast.LENGTH_LONG).show();
            }

            finish();

            findViewById(R.id.exitPoll).setVisibility(GONE);
            findViewById(R.id.addOption).setVisibility(GONE);

        });

    }

    private void slideDown(Button buttonView) {
        buttonView.setVisibility(View.VISIBLE);
        buttonView.setAlpha(0.0f);
        buttonView.animate()
                .translationY(-(buttonView.getHeight()))
                .alpha(1.0f);
    }

    private void slideUp(Button buttonView) {
        buttonView.setVisibility(View.VISIBLE);
        buttonView.setAlpha(0.0f);
        buttonView.animate()
                .translationY(buttonView.getHeight())
                .alpha(1.0f);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_create_poll:
                showPollOptions();
                return true;
            case R.id.action_upload_image:
                UploadImageToFeed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showPollOptions() {
        findViewById(R.id.pollOption1TextInputLayout).setVisibility(View.VISIBLE);
        findViewById(R.id.pollOption2TextInputLayout).setVisibility(View.VISIBLE);
        findViewById(R.id.exitPoll).setVisibility(View.VISIBLE);
        findViewById(R.id.addOption).setVisibility(View.VISIBLE);
    }

    public void UploadImageToFeed() {

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

                    findViewById(R.id.uploadImageCover).setVisibility(View.VISIBLE);

                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                    LayoutInflater inflater = this.getLayoutInflater();
                    final View dialogView = inflater.inflate(R.layout.text_input_caption, null);
                    dialogBuilder.setView(dialogView);

                    dialogBuilder.setTitle("Caption?");
                    dialogBuilder.setMessage("Yeet something ints, you monk!");
                    dialogBuilder.setPositiveButton("Yeet", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            //do something with edt.getText().toString();
                            final EditText edt = (EditText) dialogView.findViewById(R.id.edit1);

                            Uri selectedImage = imageReturnedIntent.getData();
                            InputStream imageStream = null;
                            try {
                                imageStream = getContentResolver().openInputStream(selectedImage);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                                Log.w(getClass().toString(), "Image upload failed");
                            }
                            Bitmap yourSelectedImage = BitmapFactory.decodeStream(imageStream);
                            sendImage(yourSelectedImage, edt);

                            findViewById(R.id.uploadImageCover).setVisibility(GONE);
                        }
                    });
                    dialogBuilder.setNegativeButton("Skip", new DialogInterface.OnClickListener() {
                        @SuppressLint("SetTextI18n")
                        public void onClick(DialogInterface dialog, int whichButton) {
                            //pass
                            final EditText edt = (EditText) dialogView.findViewById(R.id.edit1);

                            Uri selectedImage = imageReturnedIntent.getData();
                            InputStream imageStream = null;
                            try {
                                imageStream = getContentResolver().openInputStream(selectedImage);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                                Log.w(getClass().toString(), "Image upload failed");
                            }
                            Bitmap yourSelectedImage = BitmapFactory.decodeStream(imageStream);
                            sendImage(yourSelectedImage, edt);

                            findViewById(R.id.uploadImageCover).setVisibility(GONE);
                        }
                    });
                    AlertDialog b = dialogBuilder.create();
                    b.show();

                }
        }
    }

    private void turnOnRanting() {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo(ParseConstants.KEY_OBJECT_ID, ParseUser.getCurrentUser().getObjectId());
        query.findInBackground((user, e) -> {
            if (e == null) for (ParseObject userObject : user) {

                userObject.put("isRanting", true);
                userObject.saveEventually();

            }
        });
    }

    private void turnOffRanting() {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo(ParseConstants.KEY_OBJECT_ID, ParseUser.getCurrentUser().getObjectId());
        query.findInBackground((user, e) -> {
            if (e == null) for (ParseObject userObject : user) {

                userObject.put("isRanting", false);
                userObject.saveEventually();

            }
        });
    }

    private void sendRantPushNotification() {
        final Map<String, Object> params = new HashMap<>();
        params.put("username", ParseUser.getCurrentUser().getUsername());
        params.put("groupId", ParseUser.getCurrentUser().getString("groupId"));
        params.put("userId", ParseUser.getCurrentUser().getObjectId());
        params.put("useMasterKey", true); //Must have this line

        ParseCloud.callFunctionInBackground("pushRant", params, new FunctionCallback<String>() {
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

    private void sendRantStopPushNotification() {
        final Map<String, Object> params = new HashMap<>();
        params.put("username", ParseUser.getCurrentUser().getUsername());
        params.put("groupId", ParseUser.getCurrentUser().getString("groupId"));
        params.put("userId", ParseUser.getCurrentUser().getObjectId());
        params.put("useMasterKey", true); //Must have this line

        ParseCloud.callFunctionInBackground("pushRantStop", params, new FunctionCallback<String>() {
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

    private ParseObject sendImage(Bitmap bitmap, EditText edt) {

        EditText myEditText = (EditText) findViewById(R.id.addCommentTextField);

        Boolean isRanting = ParseUser.getCurrentUser().getBoolean("isRanting");
        String rantId = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("rantId", "");

        ParseObject message = new ParseObject(ParseConstants.CLASS_YEET);
        message.put(ParseConstants.KEY_SENDER_ID, ParseUser.getCurrentUser().getObjectId());
        message.put(ParseConstants.KEY_SENDER_NAME, ParseUser.getCurrentUser().getUsername());

        if (!(ParseUser.getCurrentUser().get("name").toString().isEmpty())) {
            message.put(ParseConstants.KEY_SENDER_FULL_NAME, ParseUser.getCurrentUser().get("name"));
        } else {
            message.put(ParseConstants.KEY_SENDER_FULL_NAME, R.string.anonymous_fullName);
        }

        message.put(ParseConstants.KEY_SENDER_AUTHOR_POINTER, ParseUser.getCurrentUser());

        if (isRanting == false) {
            message.put("isRant", false);
        } else {
            message.put("isRant", true);
            message.put("rantId", rantId);
        }

        Date myDate = new Date();
        message.put("lastReplyUpdatedAt", myDate);

        // Initialize "likedBy" Array column
        String[] likedBy = new String[0];
        message.put(ParseConstants.KEY_LIKED_BY, Arrays.asList(likedBy));

        message.put(ParseConstants.KEY_NOTIFICATION_TEXT, edt.getText().toString());

        String groupId = ParseUser.getCurrentUser().getString("groupId");
        message.put(ParseConstants.KEY_GROUP_ID, groupId);

        if (ParseUser.getCurrentUser().getParseFile("profilePicture") != null) {
            message.put(ParseConstants.KEY_SENDER_PROFILE_PICTURE, ParseUser.getCurrentUser().getParseFile("profilePicture").getUrl());
        }

        Bitmap thumbnail = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), false);
        // Convert it to byte
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] bitmap2 = stream.toByteArray();

        // Create the ParseFile
        ParseFile file = new ParseFile(UUID.randomUUID() + ".jpeg", bitmap2);
        file.saveInBackground();

        message.put("image", file);

        message.saveInBackground();

        if (isRanting == false) {
            finish();

            findViewById(R.id.exitPoll).setVisibility(GONE);
            findViewById(R.id.addOption).setVisibility(GONE);
        }

        if (isRanting == false) {
            // Play "Yeet" sound!
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplication());
            int storedPreference = preferences.getInt("sound", 1);
            // System.out.println("Application Sounds: " + storedPreference);
            if (storedPreference != 0) {
                final MediaPlayer mp = MediaPlayer.create(this, yeet);
                mp.start();
            }
        } else {
            // Play "Rant" sound!
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplication());
            int storedPreference = preferences.getInt("sound", 1);
            // System.out.println("Application Sounds: " + storedPreference);
            if (storedPreference != 0) {
                final MediaPlayer mp = MediaPlayer.create(this, R.raw.do_it);
                mp.start();
            }
        }

        if (isRanting == false) {
            Toast.makeText(getApplicationContext(), "Image upload successful, bub!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Go inn, you mag! Keep ranting.", Toast.LENGTH_LONG).show();
            TextView previousRantText = (TextView) findViewById(R.id.previousRantText);
            previousRantText.setVisibility(View.VISIBLE);
            previousRantText.setText("Previous Reet: ");
            previousRantText.append("Image");

            myEditText.setText("");
            myEditText.requestFocus();
        }

        return message;
    }

    /*
    //TODO
    3. At least two poll options must be submitted with a Yeet when poll mode is activated
    4. Button to iterate over number of poll options visible, i.e. 2, 3 and 4 (show/hide TextInputLayers)
     */
    private ParseObject sendYeet(EditText myEditText, Boolean isRanting, String rantId) {
        ParseObject message = new ParseObject(ParseConstants.CLASS_YEET);
        message.put(ParseConstants.KEY_SENDER_ID, ParseUser.getCurrentUser().getObjectId());
        message.put(ParseConstants.KEY_SENDER_NAME, ParseUser.getCurrentUser().getUsername());

        if (!(ParseUser.getCurrentUser().get("name").toString().isEmpty())) {
            message.put(ParseConstants.KEY_SENDER_FULL_NAME, ParseUser.getCurrentUser().get("name"));
        } else {
            message.put(ParseConstants.KEY_SENDER_FULL_NAME, R.string.anonymous_fullName);
        }

        message.put(ParseConstants.KEY_SENDER_AUTHOR_POINTER, ParseUser.getCurrentUser());

        if (isRanting == false) {
            message.put("isRant", false);
        } else {
            message.put("isRant", true);
            message.put("rantId", rantId);
        }

        Date myDate = new Date();
        message.put("lastReplyUpdatedAt", myDate);

        // Initialize "likedBy" Array column
        String[] likedBy = new String[0];
        message.put(ParseConstants.KEY_LIKED_BY, Arrays.asList(likedBy));

        String result = myEditText.getText().toString();
        // System.out.println(result);
        message.put(ParseConstants.KEY_NOTIFICATION_TEXT, result);

        String groupId = ParseUser.getCurrentUser().getString("groupId");
        message.put(ParseConstants.KEY_GROUP_ID, groupId);

        if (ParseUser.getCurrentUser().getParseFile("profilePicture") != null) {
            message.put(ParseConstants.KEY_SENDER_PROFILE_PICTURE, ParseUser.getCurrentUser().getParseFile("profilePicture").getUrl());
        }

        createPollObject(message);

        if (!(result.length() > 140 || result.length() <= 0)) {
            message.saveEventually();

            if (isRanting == false) {
                finish();

                findViewById(R.id.exitPoll).setVisibility(GONE);
                findViewById(R.id.addOption).setVisibility(GONE);
            }

            if (isRanting == false) {
                // Play "Yeet" sound!
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplication());
                int storedPreference = preferences.getInt("sound", 1);
                // System.out.println("Application Sounds: " + storedPreference);
                if (storedPreference != 0) {
                    final MediaPlayer mp = MediaPlayer.create(this, yeet);
                    mp.start();
                }
            } else {
                // Play "Rant" sound!
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplication());
                int storedPreference = preferences.getInt("sound", 1);
                // System.out.println("Application Sounds: " + storedPreference);
                if (storedPreference != 0) {
                    final MediaPlayer mp = MediaPlayer.create(this, R.raw.do_it);
                    mp.start();
                }
            }


            if (isRanting == false) {
                finish();

                findViewById(R.id.exitPoll).setVisibility(GONE);
                findViewById(R.id.addOption).setVisibility(GONE);

                Toast.makeText(getApplicationContext(), "Great yeet there, bub!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "Go inn, you mag! Keep ranting.", Toast.LENGTH_LONG).show();
                TextView previousRantText = (TextView) findViewById(R.id.previousRantText);
                previousRantText.setVisibility(View.VISIBLE);
                previousRantText.setText("Previous Reet: ");
                previousRantText.append(myEditText.getText().toString());

                myEditText.setText("");
                myEditText.requestFocus();
            }

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
                Toast.makeText(getApplicationContext(), "Watch it, bub! Yeets must be less than 140 characters.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "Gotta yeet somethin', bub!", Toast.LENGTH_LONG).show();
            }
        }

        return message;
    }


    private void createPollObject(ParseObject message) {
        EditText pollOption1 = (EditText) findViewById(R.id.pollOption1);
        EditText pollOption2 = (EditText) findViewById(R.id.pollOption2);
        EditText pollOption3 = (EditText) findViewById(R.id.pollOption3);
        EditText pollOption4 = (EditText) findViewById(R.id.pollOption4);

        if (!(pollOption1.getText().toString().isEmpty() && pollOption2.getText().toString().isEmpty())) {
            ParseObject pollObject = new ParseObject(ParseConstants.CLASS_POLL);

            pollObject.put(ParseConstants.KEY_POLL_OPTION1, pollOption1.getText().toString());
            pollObject.put(ParseConstants.KEY_POLL_OPTION2, pollOption2.getText().toString());

            if (!(pollOption3.getText().toString().isEmpty())) {
                pollObject.put(ParseConstants.KEY_POLL_OPTION3, pollOption3.getText().toString());
            }

            if (!(pollOption4.getText().toString().isEmpty())) {
                pollObject.put(ParseConstants.KEY_POLL_OPTION4, pollOption4.getText().toString());
            }

            String[] votedBy = new String[0];
            pollObject.put("votedBy", Arrays.asList(votedBy));

            String[] value1Array = new String[0];
            pollObject.put("value1Array", Arrays.asList(value1Array));

            String[] value2Array = new String[0];
            pollObject.put("value2Array", Arrays.asList(value2Array));

            String[] value3Array = new String[0];
            pollObject.put("value3Array", Arrays.asList(value3Array));

            String[] value4Array = new String[0];
            pollObject.put("value4Array", Arrays.asList(value4Array));

            try {
                pollObject.save();
            } catch (ParseException e) {
                e.printStackTrace();
            }

            message.put(ParseConstants.KEY_POLL_OBJECT, pollObject);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        Boolean isRanting = ParseUser.getCurrentUser().getBoolean("isRanting");
        if (isRanting) {

            EditText myEditText = (EditText) findViewById(R.id.addCommentTextField);
            if (!(myEditText.getText().toString().isEmpty())) {
                sendRantStopPushNotification(); // Send everyone in the group a push notification
                Toast.makeText(getApplicationContext(), "Doesn't that just nicely feel betts?", Toast.LENGTH_LONG).show();
            }
        }

        turnOffRanting(); // Turn off ranting when activity is destroyed so users aren't locked into rant mode

    }


    public void ExitPoll(View view) {
        findViewById(R.id.pollOption1TextInputLayout).setVisibility(GONE);
        findViewById(R.id.pollOption2TextInputLayout).setVisibility(GONE);
        findViewById(R.id.pollOption3TextInputLayout).setVisibility(GONE);
        findViewById(R.id.pollOption4TextInputLayout).setVisibility(GONE);
        findViewById(R.id.exitPoll).setVisibility(GONE);
        findViewById(R.id.addOption).setVisibility(GONE);
    }


    public void TitleClicked(View view) {
        Boolean isRanting = ParseUser.getCurrentUser().getBoolean("isRanting");
        if (isRanting) {

            EditText myEditText = (EditText) findViewById(R.id.addCommentTextField);
            if (!(myEditText.getText().toString().isEmpty())) {
                sendRantStopPushNotification(); // Send everyone in the group a push notification
                Toast.makeText(getApplicationContext(), "Doesn't that just nicely feel betts?", Toast.LENGTH_LONG).show();
            }
        }

        turnOffRanting(); // Turn off ranting when activity is destroyed so users aren't locked into rant mode
        finish();

        findViewById(R.id.exitPoll).setVisibility(GONE);
        findViewById(R.id.addOption).setVisibility(GONE);
    }


    int index = 2;
    public void AddOption(View view) {
        switch (index) {
            case 0:
                index = 1;
                findViewById(R.id.pollOption3TextInputLayout).setVisibility(View.VISIBLE);
                break;
            case 1:
                index = 2;
                findViewById(R.id.pollOption4TextInputLayout).setVisibility(View.VISIBLE);
                break;
            case 2:
                index = 0;
                findViewById(R.id.pollOption3TextInputLayout).setVisibility(View.GONE);
                findViewById(R.id.pollOption4TextInputLayout).setVisibility(View.GONE);
                break;
        }
    }
}

