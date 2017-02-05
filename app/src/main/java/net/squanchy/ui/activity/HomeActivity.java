package net.squanchy.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ListView;

import net.squanchy.R;
import net.squanchy.analytics.Analytics;
import net.squanchy.model.Model;
import net.squanchy.model.UpdatesManager;
import net.squanchy.model.data.Level;
import net.squanchy.model.data.Track;
import net.squanchy.model.managers.TracksManager;
import net.squanchy.social.SocialFeedActivity;
import net.squanchy.ui.adapter.item.EventListItem;
import net.squanchy.ui.dialog.FilterDialog;
import net.squanchy.ui.dialog.IrrelevantTimezoneDialogFragment;
import net.squanchy.ui.drawer.DrawerAdapter;
import net.squanchy.ui.drawer.DrawerManager;
import net.squanchy.ui.drawer.DrawerMenu;
import net.squanchy.ui.drawer.DrawerMenuItem;
import net.squanchy.utils.DateUtils;
import net.squanchy.utils.KeyboardUtils;
import net.squanchy.utils.ScheduleManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

public class HomeActivity extends StateActivity implements FilterDialog.OnFilterApplied {

    private Analytics analytics;

    private DrawerManager mFrManager;
    private DrawerAdapter mAdapter;
    private String mPresentTitle;
    private int mSelectedItem = 0;
    private boolean isIntentHandled = false;

    private Toolbar mToolbar;
    private DrawerLayout mDrawerLayout;

    public FilterDialog mFilterDialog;
    private boolean mIsDrawerItemClicked;

    private UpdatesManager.DataUpdatedListener updateReceiver = requestIds -> initFilterDialog();

    static void startThisActivity(Activity activity) {
        Intent intent = new Intent(activity, HomeActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        analytics = Analytics.from(this);

        setContentView(R.layout.ac_main);

        initToolbar();
        initNavigationDrawer();
        initNavigationDrawerList();
        initFilterDialog();

        initFragmentManager();
        if (getIntent().getExtras() != null) {
            isIntentHandled = true;
        }
        handleIntent(getIntent());
        showIrrelevantTimezoneDialogIfNeeded();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(null);
        if (!isIntentHandled) {
            handleIntent(intent);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        Model.getInstance()
                .getUpdatesManager()
                .registerUpdateListener(updateReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();

        Model.getInstance()
                .getUpdatesManager()
                .unregisterUpdateListener(updateReceiver);
    }

    @Override
    protected void onDestroy() {
        // TODO remove this?
        analytics.trackEvent("Application", getString(R.string.action_close));
        super.onDestroy();
    }

    @Override
    public void onNewFilterApplied() {
        mFrManager.reloadPrograms(DrawerMenu.DrawerItem.values()[mSelectedItem]);
    }

    private void initToolbar() {
        mPresentTitle = getString(DrawerMenu.MENU_STRING_RES_ARRAY[0]);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(mPresentTitle);
        setSupportActionBar(mToolbar);
    }

    private void initNavigationDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.app_name, R.string.app_name);

        mDrawerLayout.setDrawerListener(drawerToggle);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        mDrawerLayout.closeDrawers();
        mDrawerLayout.setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                KeyboardUtils.hideKeyboard(getCurrentFocus());
            }

            @Override
            public void onDrawerOpened(View drawerView) {
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                if (mIsDrawerItemClicked) {
                    mIsDrawerItemClicked = false;
                    changeFragment();
                }
            }

            @Override
            public void onDrawerStateChanged(int newState) {
            }
        });

        drawerToggle.syncState();
    }

    private void initNavigationDrawerList() {
        List<DrawerMenuItem> menu = getNavigationDrawerItems();
        mAdapter = new DrawerAdapter(this, menu);
        mAdapter.setDrawerItemClickListener(position -> onItemClick(position));

        ListView listView = (ListView) findViewById(R.id.leftDrawer);
        listView.addHeaderView(
                getLayoutInflater().inflate(R.layout.nav_drawer_header, listView, false),
                null,
                false);
        listView.setAdapter(mAdapter);
    }

    public void initFilterDialog() {
        new AsyncTask<Void, Void, List<EventListItem>>() {
            @Override
            protected List<EventListItem> doInBackground(Void... params) {
                TracksManager tracksManager = Model.getInstance().getTracksManager();
                List<Track> trackList = tracksManager.getTracks();
                List<Level> levelList = tracksManager.getLevels();

                Collections.sort(trackList, (track1, track2) -> {
                    String name1 = track1.getName();
                    String name2 = track2.getName();
                    return name1.compareToIgnoreCase(name2);
                });

                String[] tracks = new String[trackList.size()];
                String[] levels = new String[levelList.size()];

                for (int i = 0; i < trackList.size(); i++) {
                    tracks[i] = trackList.get(i).getName();
                }

                for (int i = 0; i < levelList.size(); i++) {
                    levels[i] = levelList.get(i).getName();
                }
                mFilterDialog = FilterDialog.newInstance(tracks, levels);
                mFilterDialog.setData(levelList, trackList);
                return null;
            }

            @Override
            protected void onPostExecute(List<EventListItem> eventListItems) {
            }
        }.execute();
    }

    public void closeFilterDialog() {
        if (mFilterDialog != null) {
            if (mFilterDialog.isAdded()) {
                mFilterDialog.dismissAllowingStateLoss();
            }
            mFilterDialog.clearFilter();
        }
    }

    private void handleIntent(Intent intent) {
        if (intent.getExtras() != null) {
            long eventId = intent.getLongExtra(EventDetailsActivity.EXTRA_EVENT_ID, -1);
            long day = intent.getLongExtra(EventDetailsActivity.EXTRA_DAY, -1);
            redirectToDetails(eventId, day);
            isIntentHandled = false;
            new ScheduleManager(this).cancelAlarm(eventId);
        }
    }

    private void redirectToDetails(long id, long day) {
        Intent intent = new Intent(this, EventDetailsActivity.class);
        intent.putExtra(EventDetailsActivity.EXTRA_EVENT_ID, id);
        intent.putExtra(EventDetailsActivity.EXTRA_DAY, day);
        startActivity(intent);
    }

    private void onItemClick(int position) {
        mDrawerLayout.closeDrawers();
        if (mSelectedItem == position) {
            return;
        }
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        mSelectedItem = position;
        mIsDrawerItemClicked = true;
    }

    private void changeFragment() {
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        DrawerMenuItem item = mAdapter.getItem(mSelectedItem);
        if (isSocialFeedAt(mSelectedItem)) {
            startActivity(new Intent(this, SocialFeedActivity.class));
        } else if (!item.isGroup() && mFrManager != null) {
            mFrManager.setFragment(DrawerMenu.DrawerItem.values()[mSelectedItem]);
            mPresentTitle = getString(DrawerMenu.MENU_STRING_RES_ARRAY[mSelectedItem]);
            mToolbar.setTitle(mPresentTitle);

            mAdapter.setSelectedPos(mSelectedItem);
            mAdapter.notifyDataSetChanged();

            analytics.trackEvent(mPresentTitle + " screen", getString(R.string.action_open));
        }
    }

    private boolean isSocialFeedAt(int position) {
        return DrawerMenu.DrawerItem.values()[mSelectedItem] == DrawerMenu.DrawerItem.SOCIAL_MEDIA;
    }

    private void initFragmentManager() {
        mFrManager = new DrawerManager(getSupportFragmentManager(), R.id.fragment_container);
        analytics.trackEvent(getString(R.string.Sessions) + " screen", getString(R.string.action_open));
        mFrManager.setFragment(DrawerMenu.DrawerItem.PROGRAM);
    }

    private List<DrawerMenuItem> getNavigationDrawerItems() {
        List<DrawerMenuItem> result = new ArrayList<>();

        for (int i = 0; i < DrawerMenu.MENU_STRING_RES_ARRAY.length; i++) {
            DrawerMenuItem menuItem = new DrawerMenuItem();
            String name = getString(DrawerMenu.MENU_STRING_RES_ARRAY[i]);

            menuItem.setId(i);
            menuItem.setName(name);
            menuItem.setGroup(false);
            menuItem.setIconRes(DrawerMenu.MENU_ICON_RES[i]);
            menuItem.setSelIconRes(DrawerMenu.MENU_ICON_RES_SEL[i]);

            result.add(menuItem);
        }

        return result;
    }

    private void showIrrelevantTimezoneDialogIfNeeded() {
        if (!isCurrentTimezoneRelevant()
                && IrrelevantTimezoneDialogFragment.canPresentMessage(this)
                && !isFinishing()) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(new IrrelevantTimezoneDialogFragment(), IrrelevantTimezoneDialogFragment.TAG);
            ft.commitAllowingStateLoss();
        }
    }

    public boolean isCurrentTimezoneRelevant() {
        TimeZone eventTimezone = DateUtils.getTimeZone(this);
        TimeZone curentZone = TimeZone.getDefault();
        return curentZone.getID().equals(eventTimezone.getID());
    }
}