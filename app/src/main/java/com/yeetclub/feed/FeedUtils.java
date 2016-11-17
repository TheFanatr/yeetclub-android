package com.yeetclub.feed;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Bluebery on 7/29/2015.
 */
public class FeedUtils {

    private static final long MINUTE_IN_MILLIS = 1000 * 60;
    private static final long HOUR_IN_MILLIS = MINUTE_IN_MILLIS * 60;
    private static final long DAY_IN_MILLIS = HOUR_IN_MILLIS * 24;
    private static final long WEEK_IN_MILLIS = DAY_IN_MILLIS * 7;

    public static void CopyStream(InputStream is, OutputStream os) {
        final int buffer_size = 1024;
        try {
            byte[] bytes = new byte[buffer_size];
            for (; ; ) {
                int count = is.read(bytes, 0, buffer_size);
                if (count == -1)
                    break;
                os.write(bytes, 0, count);
            }
        } catch (Exception ex) {
        }
    }

    // take the difference in time (as milliseconds) and return the number of minutes, or hours, or days,
    // or weeks depending on which bracket the time fits in.
    public static String MSToDate(long deltaMS) {

        if (deltaMS < MINUTE_IN_MILLIS) {
            return "0m";
        } else if (deltaMS < HOUR_IN_MILLIS) {
            return MSToMinute(deltaMS);
        } else if (deltaMS < DAY_IN_MILLIS) {
            return MSToHour(deltaMS);
        } else if (deltaMS < WEEK_IN_MILLIS) {
            return MSToDay(deltaMS);
        } else {
            return MSToWeek(deltaMS);
        }

    }

    private static String MSToMinute(long deltaMS) {
        int minutes = (int) (deltaMS / MINUTE_IN_MILLIS);
        return minutes + "m";
    }

    private static String MSToHour(long deltaMS) {
        int hours = (int) (deltaMS / HOUR_IN_MILLIS);
        return hours + "h";
    }

    private static String MSToDay(long deltaMS) {
        int days = (int) (deltaMS / DAY_IN_MILLIS);
        return days + "d";
    }

    private static String MSToWeek(long deltaMS) {
        int weeks = (int) (deltaMS / WEEK_IN_MILLIS);
        return weeks + "w";
    }

    // sets the hyper cycle image view to visible,
    // animates larger, pauses, animates smaller and then hides
    public static void AddAlphaScaleShowHideAnimation(final ImageView saveImage) {

        // set the image to visible with the animation ending properties
        saveImage.setVisibility(View.VISIBLE);
        saveImage.setScaleX(0.5f);
        saveImage.setScaleY(0.5f);
        saveImage.setAlpha(0.0f);

        // start the animation, scale up and to 1 alpha first
        saveImage.animate().setDuration(300).scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animator) {

                super.onAnimationEnd(animator);

                // clear any animations and remove all listeners on the animator
                saveImage.clearAnimation();
                animator.removeAllListeners();

                // let the view be visible for a moment (given in ms) before animating down size
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        // start animating, scale down and to 0 alpha
                        saveImage.animate().setDuration(200).scaleX(0.5f).scaleY(0.5f).alpha(0.0f).setListener(new AnimatorListenerAdapter() {

                            @Override
                            public void onAnimationEnd(Animator animator) {
                                super.onAnimationEnd(animator);
                                saveImage.setVisibility(View.GONE);
                            }
                        }).start();
                    }
                }, 400);
            }
        }).start();
    }
}
