package com.yeetclub.android.activity;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.yeetclub.android.R;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;

public class MediaPreviewActivity extends AppCompatActivity {

    private SubsamplingScaleImageView imageView;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_preview);
        try {
            locateImageView();
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings_media_preview, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.main_action_rotate) {
            //rotating image
            imageView.setOrientation((imageView.getOrientation() + 90) % 360);
        }

        return super.onOptionsItemSelected(item);
    }

    private void locateImageView() throws URISyntaxException, IOException {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            if (bundle.getString("imageUrl") != null) {
                String imageUrl = bundle.getString("imageUrl");
                Log.w(getClass().toString(), imageUrl);

                imageView = (SubsamplingScaleImageView) findViewById(R.id.image);

                try {
                    URL url = new URL(imageUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    Bitmap myBitmap = BitmapFactory.decodeStream(input);

                    imageView.setImage(ImageSource.bitmap(myBitmap));

                } catch (IOException e) {
                    // Log exception
                    Log.w(getClass().toString(), e);
                }

            }
        }
    }
}