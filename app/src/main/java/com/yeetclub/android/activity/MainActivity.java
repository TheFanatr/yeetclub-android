package com.yeetclub.android.activity;

/**
 * Created by @santafebound on 2016-09-29.
 */

import android.app.FragmentManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.mikepenz.fastadapter.utils.RecyclerViewCacheUtil;
import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.itemanimators.AlphaCrossFadeAnimator;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.interfaces.OnCheckedChangeListener;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.mikepenz.materialdrawer.model.interfaces.Nameable;
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader;
import com.mikepenz.materialdrawer.util.DrawerImageLoader;
import com.parse.ParseUser;
import com.squareup.picasso.Picasso;
import com.yeetclub.android.R;
import com.yeetclub.android.adapter.FragmentPagerAdapter;
import com.yeetclub.android.rss.RssActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_fragment);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set typeface for action bar title
        TextView text = (TextView) findViewById(R.id.feed_title);
        Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/Lato-Bold.ttf");
        text.setTypeface(tf);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            getFragmentManager();
        }

        createNavigationDrawer(savedInstanceState, toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#169cee")));
        fab.setOnClickListener(view -> {

            Intent intent = new Intent(MainActivity.this, YeetActivity.class);
            startActivity(intent);

        });
    }


    public FragmentManager getFragmentManager() {
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.ic_tab_poo));
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.ic_tab_notification));
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.ic_tab_points));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        final PagerAdapter adapter = new FragmentPagerAdapter
                (getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }

        });

        viewPager.getAdapter().notifyDataSetChanged();

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            if (bundle.getString("fragment") != null) {
                if ("fragment2".equals(bundle.getString("fragment"))) {
                /*Log.w(getClass().toString(), bundle.getString("fragment"));*/
                    viewPager.setCurrentItem(1);
                } else {
                    viewPager.setCurrentItem(0);
                }
            }
        }

        return null;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings_feed, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, UserSettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    // Save AccountHeader result
    private AccountHeader headerResult = null;
    private Drawer result = null;


    private void createNavigationDrawer(Bundle savedInstanceState, Toolbar toolbar) {

        if (ParseUser.getCurrentUser().getParseFile("profilePicture") != null) {
            DrawerImageLoader.init(new AbstractDrawerImageLoader() {
                @Override
                public void set(ImageView imageView, Uri uri, Drawable placeholder) {
                    Picasso.with(imageView.getContext()).load(uri).placeholder(placeholder).into(imageView);
                    imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                }

                @Override
                public void cancel(ImageView imageView) {
                    Picasso.with(imageView.getContext()).cancelRequest(imageView);
                }

            });

            String profilePicture = ParseUser.getCurrentUser().getParseFile("profilePicture").getUrl();
            setProfile(savedInstanceState, toolbar, profilePicture);
        } else {
            DrawerImageLoader.init(new AbstractDrawerImageLoader() {
                @Override
                public void set(ImageView imageView, Uri uri, Drawable placeholder) {
                    Picasso.with(imageView.getContext()).load(uri).placeholder(placeholder).into(imageView);
                    //imageView.setImageResource(R.drawable.ic_profile_pic_add);
                    imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                }

                @Override
                public void cancel(ImageView imageView) {
                    Picasso.with(imageView.getContext()).cancelRequest(imageView);
                }

            });

            String profilePicture = "R.drawable.ic_profile_pic_add";
            setProfile(savedInstanceState, toolbar, profilePicture);
        }

    }


    private void setProfile(Bundle savedInstanceState, Toolbar toolbar, String profilePicture) {
        if (ParseUser.getCurrentUser().get("name") != null && ParseUser.getCurrentUser().getUsername() != null) {
            // If name and username exist, set to respective values from Parse
            String name = String.valueOf(ParseUser.getCurrentUser().get("name"));
            String username = String.valueOf(ParseUser.getCurrentUser().getUsername());
            setValues(savedInstanceState, toolbar, name, username, profilePicture);
        } else if (ParseUser.getCurrentUser().get("name") == null) {
            // If name does not exist, set name to nothing
            String name = "";
            String username = String.valueOf(ParseUser.getCurrentUser().getUsername());
            setValues(savedInstanceState, toolbar, name, username, profilePicture);
        }
    }


    private void setValues(Bundle savedInstanceState, Toolbar toolbar, String name, String username, String profilePicture) {
        final IProfile<ProfileDrawerItem> profile;
        if (profilePicture.equalsIgnoreCase("R.drawable.ic_profile_pic_add")) {
            profile = new ProfileDrawerItem().withName(name).withEmail(username).withIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_profile_pic_add)).withIdentifier(100);
        } else {
            profile = new ProfileDrawerItem().withName(name).withEmail(username).withIcon(profilePicture).withIdentifier(100);
        }
        // Create the AccountHeader
        headerResult = new AccountHeaderBuilder()
                .withActivity(this)
                .withTranslucentStatusBar(true)
                .withHeaderBackground(R.color.highlight)
                .addProfiles(
                        profile,
                        new ProfileSettingDrawerItem().withName(getString(R.string.drawer_item_edit_profile)).withIcon(new IconicsDrawable(this, GoogleMaterial.Icon.gmd_edit).actionBar().paddingDp(5).colorRes(R.color.material_drawer_primary_text)).withIdentifier(100000),
                        new ProfileSettingDrawerItem().withName(getString(R.string.drawer_item_logout)).withIcon(new IconicsDrawable(this, GoogleMaterial.Icon.gmd_exit_to_app).actionBar().paddingDp(5).colorRes(R.color.material_drawer_primary_text)).withIdentifier(100001)

                )
                .withOnAccountHeaderProfileImageListener(new AccountHeader.OnAccountHeaderProfileImageListener() {
                                                             @Override
                                                             public boolean onProfileImageClick(View view, IProfile profile, boolean current) {
                                                                 Intent intent = new Intent(getApplicationContext(), UserProfileActivity.class);
                                                                 startActivity(intent);
                                                                 return false;
                                                             }

                                                             @Override
                                                             public boolean onProfileImageLongClick(View view, IProfile profile, boolean current) {
                                                                 return false;
                                                             }
                                                         }
                )
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean current) {
                        if (profile instanceof IDrawerItem && profile.getIdentifier() == 100000) {
                            Intent intent = new Intent(getApplicationContext(), EditProfileActivity.class);
                            startActivity(intent);
                        } else if (profile instanceof IDrawerItem && profile.getIdentifier() == 100001) {
                            ParseUser.logOut();
                            Intent logOut = new Intent(getApplicationContext(), DispatchActivity.class);
                            startActivity(logOut);
                        }

                        //false if you have not consumed the event and it should close the drawer
                        return false;
                    }
                })
                .withSavedInstance(savedInstanceState)
                .build();

        // Create the Drawer
        result = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .withHasStableIds(true)
                .withItemAnimator(new AlphaCrossFadeAnimator())
                .withAccountHeader(headerResult)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName(R.string.drawer_item_profile).withIcon(FontAwesome.Icon.faw_user).withIdentifier(1).withSelectable(false),
                        new PrimaryDrawerItem().withName(R.string.drawer_item_create).withIcon(FontAwesome.Icon.faw_paint_brush).withIdentifier(2).withSelectable(false),
                        new PrimaryDrawerItem().withName(R.string.drawer_item_yaanich_news).withIcon(FontAwesome.Icon.faw_newspaper_o).withIdentifier(3).withSelectable(false),
                        new PrimaryDrawerItem().withName(R.string.drawer_item_settings).withIcon(FontAwesome.Icon.faw_cog).withIdentifier(4).withSelectable(false)
                )
                .withOnDrawerItemClickListener((view, position, drawerItem) -> {

                    if (drawerItem != null) {
                        Intent intent = null;
                        if (drawerItem.getIdentifier() == 1) {
                            intent = new Intent(this, UserProfileActivity.class);
                        } else if (drawerItem.getIdentifier() == 2) {
                            intent = new Intent(this, YeetActivity.class);
                        } else if (drawerItem.getIdentifier() == 3) {
                            intent = new Intent(this, RssActivity.class);
                        } else if (drawerItem.getIdentifier() == 4) {
                            intent = new Intent(this, UserSettingsActivity.class);
                        }
                        if (intent != null) {
                            this.startActivity(intent);
                        }
                    }
                    return false;
                })
                .withSavedInstance(savedInstanceState)
                .withShowDrawerOnFirstLaunch(true)
                .build();

        new RecyclerViewCacheUtil<IDrawerItem>().withCacheSize(2).apply(result.getRecyclerView(), result.getDrawerItems());

        if (savedInstanceState == null) {
            result.setSelection(21, false);
            headerResult.setActiveProfile(profile);
        }

        /*result.updateBadge(5, new StringHolder(10 + ""));*/
    }


    private OnCheckedChangeListener onCheckedChangeListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(IDrawerItem drawerItem, CompoundButton buttonView, boolean isChecked) {
            if (drawerItem instanceof Nameable) {
                Log.i("material-drawer", "DrawerItem: " + ((Nameable) drawerItem).getName() + " - toggleChecked: " + isChecked);
            } else {
                Log.i("material-drawer", "toggleChecked: " + isChecked);
            }
        }
    };


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        TextView checkFilePermission = (TextView) findViewById(R.id.checkFilePermission);

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getFragmentManager();
            checkFilePermission.setVisibility(View.GONE);
        } else {
            checkFilePermission.setVisibility(View.VISIBLE);
        }
    }


    public void enableFilePermission(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            getFragmentManager();
        }
    }


    public void onResume() {
        super.onResume();
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //add the values which need to be saved from the drawer to the bundle
        outState = result.saveInstanceState(outState);
        //add the values which need to be saved from the accountHeader to the bundle
        outState = headerResult.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }


    @Override
    public void onBackPressed() {

        if (result != null && result.isDrawerOpen()) {
            result.closeDrawer();
        } else {
            super.onBackPressed();
        }
    }
}
