package com.dexdrip.stephenblack.nightwatch;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.util.Log;

import com.dexdrip.stephenblack.nightwatch.utils.NavDrawerBuilder;

import java.util.List;
import java.util.Objects;

public class NavigationDrawerFragment extends Fragment {
    private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";
    private NavigationDrawerCallbacks mCallbacks;
    private ActionBarDrawerToggle mDrawerToggle;
    private int mCurrentSelectedPosition = 0;
    private boolean mFromSavedInstanceState;
    private boolean mUserLearnedDrawer; 
    private ListView mDrawerListView;
    private DrawerLayout mDrawerLayout;
    public NavDrawerBuilder navDrawerBuilder;
    private View mFragmentContainerView;
    int menu_position;
    private List<Intent> intent_list;

    public NavigationDrawerFragment() { }
 @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).apply();
        mUserLearnedDrawer = sp.getBoolean(PREF_USER_LEARNED_DRAWER, false);

        if (savedInstanceState != null) {
            mCurrentSelectedPosition = 0;
            mFromSavedInstanceState = true;
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mDrawerListView = (ListView) inflater.inflate(R.layout.fragment_navigation_drawer, container, false);
        mDrawerListView.setOnItemClickListener((parent, view, position, id) -> selectItem(position));
        navDrawerBuilder = new NavDrawerBuilder(getActivity());
        intent_list = navDrawerBuilder.nav_drawer_intents;
        return mDrawerListView;
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }
    public void setUp(int fragmentId, DrawerLayout drawerLayout, String current_activity, Context context) {
        mDrawerLayout = drawerLayout;
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);


        navDrawerBuilder = new NavDrawerBuilder(getActivity());
        List<String> menu_option_list = navDrawerBuilder.nav_drawer_options;
        String[] menu_options = menu_option_list.toArray(new String[menu_option_list.size()]);
        intent_list = navDrawerBuilder.nav_drawer_intents;

        mFragmentContainerView = Objects.requireNonNull(getActivity()).findViewById(fragmentId);
        mDrawerLayout = drawerLayout;
        mCurrentSelectedPosition = menu_position;
        mDrawerListView.setItemChecked(mCurrentSelectedPosition, true);

        ActionBar actionBar = getActionBar(); 

        if ( actionBar != null ) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        } else {
            Log.e("NavigationDrawerFrag", "Exception with getActionBar: ");
        }

        try {
            mDrawerListView.setAdapter(new ArrayAdapter<>(
                    actionBar.getThemedContext(),
                    android.R.layout.simple_list_item_activated_1,
                    android.R.id.text1,
                    menu_options
            ));

        } catch (NullPointerException e) {
            try {
                mDrawerListView.setAdapter(new ArrayAdapter<>(
                        getActivity().getActionBar().getThemedContext(),
                        android.R.layout.simple_list_item_activated_1,
                        android.R.id.text1,
                        menu_options
                ));
            } catch (NullPointerException ex) {
                Log.d("NavigationDrawerFrag", "Got second null pointer: " + ex.toString());
            }
        }
        mDrawerToggle = new ActionBarDrawerToggle(
                getActivity(),
                mDrawerLayout,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (!isAdded()) { return; }
                getActivity().invalidateOptionsMenu();
            }
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (!isAdded()) { return; }

                if (!mUserLearnedDrawer) {
                    mUserLearnedDrawer = true;
                    SharedPreferences sp = PreferenceManager
                            .getDefaultSharedPreferences(getActivity());
                    sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).apply();
                }
                getActivity().invalidateOptionsMenu();
            }
        };
        
        if (!mUserLearnedDrawer && !mFromSavedInstanceState) {
            mDrawerLayout.openDrawer(mFragmentContainerView);
        } 

        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });
        mDrawerLayout.addDrawerListener(mDrawerToggle);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mCallbacks = (NavigationDrawerCallbacks) context;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement NavigationDrawerCallbacks.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    private void selectItem(int position) {
        mCurrentSelectedPosition = position;
        if (mDrawerListView != null) {
            mDrawerListView.setItemChecked(position, true);
        }
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(mFragmentContainerView);
        }
        if (mCallbacks != null) {
            mCallbacks.onNavigationDrawerItemSelected(position);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    private ActionBar getActionBar() {
        return ((AppCompatActivity) getActivity()).getSupportActionBar();
    }

    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mDrawerListView);
    }
    public void openDrawer() {
        if (mDrawerLayout != null) mDrawerLayout.openDrawer(mDrawerListView);
    }
    public void closeDrawer() {
        if (mDrawerLayout != null) mDrawerLayout.closeDrawer(mDrawerListView);
    }
    public void swapContext(int position) {
        if (position != menu_position) {
            Intent[] intent_array = intent_list.toArray(new Intent[intent_list.size()]);
            startActivity(intent_array[position]);
            if(menu_position != 0) {
                getActivity().finish();
            }
        }
    }
    public interface NavigationDrawerCallbacks {
        void onNavigationDrawerItemSelected(int position);
    }
}
