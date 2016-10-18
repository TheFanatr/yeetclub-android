/*
 *  Copyright (c) 2014, Parse, LLC. All rights reserved.
 *
 *  You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 *  copy, modify, and distribute this software in source code or binary form for use
 *  in connection with the web services and APIs provided by Parse.
 *
 *  As with any software that integrates with the Parse platform, your use of
 *  this software is subject to the Parse Terms of Service
 *  [https://www.parse.com/about/terms]. This copyright notice shall be
 *  included in all copies or substantial portions of the software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 *  FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 *  COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 *  IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.parse.ui;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseInstallation;
import com.parse.ParseTwitterUtils;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.twitter.Twitter;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.Random;

/**
 * Fragment for the user login screen.
 */
public class ParseLoginFragment extends ParseLoginFragmentBase {

    public interface ParseLoginFragmentListener {
        public void onSignUpClicked(String username, String password);

        public void onLoginHelpClicked();

        public void onLoginSuccess();
    }

    private static final String LOG_TAG = "ParseLoginFragment";
    private static final String USER_OBJECT_NAME_FIELD = "name";
    private static final String USER_OBJECT_USERNAME_FIELD = "username";

    private View parseLogin;
    private EditText usernameField;
    private EditText passwordField;
    private Button parseLoginButton;
    /*private Button parseLoginAnonymousButton;*/
    private Button parseSignupButton;
    private Button facebookLoginButton;
    private Button twitterLoginButton;
    private ParseLoginFragmentListener loginFragmentListener;
    private ParseOnLoginSuccessListener onLoginSuccessListener;

    private ParseLoginConfig config;

    public static ParseLoginFragment newInstance(Bundle configOptions) {
        ParseLoginFragment loginFragment = new ParseLoginFragment();
        loginFragment.setArguments(configOptions);
        return loginFragment;
    }

    private boolean allowFacebookLogin = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent,
                             Bundle savedInstanceState) {
        config = ParseLoginConfig.fromBundle(getArguments(), getActivity());

        View v = inflater.inflate(R.layout.com_parse_ui_parse_login_fragment,
                parent, false);
        parseLogin = v.findViewById(R.id.parse_login);
        usernameField = (EditText) v.findViewById(R.id.login_username_input);
        usernameField.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        passwordField = (EditText) v.findViewById(R.id.login_password_input);
        parseLoginButton = (Button) v.findViewById(R.id.parse_login_button);
        parseSignupButton = (Button) v.findViewById(R.id.parse_signup_button);
        /*facebookLoginButton = (Button) v.findViewById(R.id.facebook_login);*/
        /*twitterLoginButton = (Button) v.findViewById(R.id.twitter_login);*/

        Typeface tf_reg = Typeface.createFromAsset(getContext().getAssets(), "fonts/Lato-Regular.ttf");
        usernameField.setTypeface(tf_reg);
        passwordField.setTypeface(tf_reg);

        Typeface tf_bold = Typeface.createFromAsset(getContext().getAssets(), "fonts/Lato-Bold.ttf");
        parseLoginButton.setTypeface(tf_bold);
        parseSignupButton.setTypeface(tf_bold);

        if (allowParseLoginAndSignup()) {
            setUpParseLoginAndSignup();
        }
        if (allowFacebookLogin()) {
            setUpFacebookLogin();
        }
        if (allowTwitterLogin()) {
            setUpTwitterLogin();
        }
        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof ParseLoginFragmentListener) {
            loginFragmentListener = (ParseLoginFragmentListener) activity;
        } else {
            throw new IllegalArgumentException(
                    "Activity must implemement ParseLoginFragmentListener");
        }

        if (activity instanceof ParseOnLoginSuccessListener) {
            onLoginSuccessListener = (ParseOnLoginSuccessListener) activity;
        } else {
            throw new IllegalArgumentException(
                    "Activity must implemement ParseOnLoginSuccessListener");
        }

        if (activity instanceof ParseOnLoadingListener) {
            onLoadingListener = (ParseOnLoadingListener) activity;
        } else {
            throw new IllegalArgumentException(
                    "Activity must implemement ParseOnLoadingListener");
        }
    }

    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    private void setUpParseLoginAndSignup() {
        parseLogin.setVisibility(View.VISIBLE);

        if (config.isParseLoginEmailAsUsername()) {
            usernameField.setHint(R.string.com_parse_ui_email_input_hint);
            usernameField.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        }

        if (config.getParseLoginButtonText() != null) {
            parseLoginButton.setText(config.getParseLoginButtonText());
        }

        parseLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameField.getText().toString();
                String password = passwordField.getText().toString();

                if (username.length() == 0) {
                    if (config.isParseLoginEmailAsUsername()) {
                        showToast(R.string.com_parse_ui_no_email_toast);
                    } else {
                        showToast(R.string.com_parse_ui_no_username_toast);
                    }
                } else if (password.length() == 0) {
                    showToast(R.string.com_parse_ui_no_password_toast);
                } else {
                    loadingStart(true);
                    ParseUser.logInInBackground(username, password, new LogInCallback() {
                        @Override
                        public void done(ParseUser user, ParseException e) {
                            if (isActivityDestroyed()) {
                                return;
                            }

                            if (user != null) {
                                updateParseInstallation(ParseUser.getCurrentUser());

                                loadingFinish();
                                loginSuccess();
                            } else {
                                loadingFinish();
                                if (e != null) {
                                    debugLog(getString(R.string.com_parse_ui_login_warning_parse_login_failed) +
                                            e.toString());
                                    if (e.getCode() == ParseException.OBJECT_NOT_FOUND) {
                                        if (config.getParseLoginInvalidCredentialsToastText() != null) {
                                            showToast(config.getParseLoginInvalidCredentialsToastText());
                                        } else {
                                            showToast(R.string.com_parse_ui_parse_login_invalid_credentials_toast);
                                        }
                                        passwordField.selectAll();
                                        passwordField.requestFocus();
                                    } else {
                                        showToast(R.string.com_parse_ui_parse_login_failed_unknown_toast);
                                    }
                                }
                            }
                        }
                    });
                }
            }
        });

        if (config.getParseSignupButtonText() != null) {
            parseSignupButton.setText(config.getParseSignupButtonText());
        }

        parseSignupButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameField.getText().toString();
                String password = passwordField.getText().toString();

                loginFragmentListener.onSignUpClicked(username, password);
            }
        });
    }

    public void updateParseInstallation(ParseUser user) {

        // Update Installation
        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        installation.put("username", user.getUsername());
        if (user.get("profilePicture ") != null) {
            installation.put("profilePicture", user.get("profilePicture"));
        }
        if (!(user.getString("groupId").isEmpty())) {
            installation.put("groupId", user.getString(("groupId")));
        }
        installation.put("GCMSenderId", getString(R.string.gcm_sender_id));
        installation.put("userId", user.getObjectId());
        installation.saveInBackground();

    }

    private LogInCallback facebookLoginCallbackV4 = new LogInCallback() {
        @Override
        public void done(ParseUser user, ParseException e) {
            if (isActivityDestroyed()) {
                return;
            }

            if (user == null) {
                loadingFinish();
                if (e != null) {
                    showToast(R.string.com_parse_ui_facebook_login_failed_toast);
                    debugLog(getString(R.string.com_parse_ui_login_warning_facebook_login_failed) +
                            e.toString());
                }
            } else if (user.isNew()) {
                GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken(),
                        new GraphRequest.GraphJSONObjectCallback() {
                            @Override
                            public void onCompleted(JSONObject fbUser,
                                                    GraphResponse response) {
                  /*
                    If we were able to successfully retrieve the Facebook
                    user's name, let's set it on the fullName field.
                  */
                                ParseUser parseUser = ParseUser.getCurrentUser();
                                if (fbUser != null && parseUser != null
                                        && fbUser.optString("name").length() > 0) {
                                    parseUser.put(USER_OBJECT_NAME_FIELD, fbUser.optString("name"));
                                    // remove all whitespace, transform to lower case, and append a random 4 digit number. pretty unlikely for a collision on sign up.
                                    parseUser.put(USER_OBJECT_USERNAME_FIELD, fbUser.optString("name").replaceAll("\\s","").toLowerCase() + (new Random().nextInt(1000)));

                                    // Set designs (Array) column to empty
                                    String[] designs = new String[0];
                                    parseUser.put("designs", Arrays.asList(designs));

                                    parseUser.saveInBackground(new SaveCallback() {
                                        @Override
                                        public void done(ParseException e) {
                                            if (e != null) {
                                                debugLog(getString(
                                                        R.string.com_parse_ui_login_warning_facebook_login_user_update_failed) +
                                                        e.toString());
                                            }
                                            loginSuccess();
                                        }
                                    });
                                }
                                loginSuccess();
                            }
                        }
                ).executeAsync();
            } else {
                loginSuccess();
            }
        }
    };

    private void setUpFacebookLogin() {
        facebookLoginButton.setVisibility(View.VISIBLE);

        if (config.getFacebookLoginButtonText() != null) {
            facebookLoginButton.setText(config.getFacebookLoginButtonText());
        }

        facebookLoginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                loadingStart(false); // Facebook login pop-up already has a spinner
                if (config.isFacebookLoginNeedPublishPermissions()) {
                    ParseFacebookUtils.logInWithPublishPermissionsInBackground(getActivity(),
                            config.getFacebookLoginPermissions(), facebookLoginCallbackV4);
                } else {
                    ParseFacebookUtils.logInWithReadPermissionsInBackground(getActivity(),
                            config.getFacebookLoginPermissions(), facebookLoginCallbackV4);
                }
            }
        });
    }

    private void setUpTwitterLogin() {
        twitterLoginButton.setVisibility(View.VISIBLE);

        if (config.getTwitterLoginButtonText() != null) {
            twitterLoginButton.setText(config.getTwitterLoginButtonText());
        }

        twitterLoginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                loadingStart(false); // Twitter login pop-up already has a spinner
                ParseTwitterUtils.logIn(getActivity(), new LogInCallback() {
                    @Override
                    public void done(ParseUser user, ParseException e) {
                        if (isActivityDestroyed()) {
                            return;
                        }

                        if (user == null) {
                            loadingFinish();
                            if (e != null) {
                                showToast(R.string.com_parse_ui_twitter_login_failed_toast);
                                debugLog(getString(R.string.com_parse_ui_login_warning_twitter_login_failed) +
                                        e.toString());
                            }
                        } else if (user.isNew()) {
                            Twitter twitterUser = ParseTwitterUtils.getTwitter();
                            if (twitterUser != null
                                    && twitterUser.getScreenName().length() > 0) {
                /*
                  To keep this example simple, we put the users' Twitter screen name
                  into the name field of the Parse user object. If you want the user's
                  real name instead, you can implement additional calls to the
                  Twitter API to fetch it.
                */
                                user.put(USER_OBJECT_NAME_FIELD, twitterUser.getScreenName());
                                user.put(USER_OBJECT_USERNAME_FIELD, twitterUser.getScreenName());
                                user.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        if (e != null) {
                                            debugLog(getString(
                                                    R.string.com_parse_ui_login_warning_twitter_login_user_update_failed) +
                                                    e.toString());
                                        }
                                        loginSuccess();
                                    }
                                });
                            }
                        } else {
                            loginSuccess();
                        }
                    }
                });
            }
        });
    }

    private boolean allowParseLoginAndSignup() {
        if (!config.isParseLoginEnabled()) {
            return false;
        }

        if (usernameField == null) {
            debugLog(R.string.com_parse_ui_login_warning_layout_missing_username_field);
        }
        if (passwordField == null) {
            debugLog(R.string.com_parse_ui_login_warning_layout_missing_password_field);
        }
        if (parseLoginButton == null) {
            debugLog(R.string.com_parse_ui_login_warning_layout_missing_login_button);
        }
        if (parseSignupButton == null) {
            debugLog(R.string.com_parse_ui_login_warning_layout_missing_signup_button);
        }

        boolean result = (usernameField != null) && (passwordField != null)
                && (parseLoginButton != null) && (parseSignupButton != null);

        if (!result) {
            debugLog(R.string.com_parse_ui_login_warning_disabled_username_password_login);
        }
        return result;
    }

    private boolean allowFacebookLogin() {
        if (!config.isFacebookLoginEnabled()) {
            return false;
        }

        if (facebookLoginButton == null) {
            debugLog(R.string.com_parse_ui_login_warning_disabled_facebook_login);
            return false;
        } else {
            return true;
        }
    }

    private boolean allowTwitterLogin() {
        if (!config.isTwitterLoginEnabled()) {
            return false;
        }

        if (twitterLoginButton == null) {
            debugLog(R.string.com_parse_ui_login_warning_disabled_twitter_login);
            return false;
        } else {
            return true;
        }
    }

    private void loginSuccess() {
        onLoginSuccessListener.onLoginSuccess();
    }

}