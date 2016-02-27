package derpibooru.derpy.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.FrameLayout;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import derpibooru.derpy.R;
import derpibooru.derpy.data.internal.NavigationDrawerItem;

abstract class NavigationDrawerFragmentActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {
    private NavigationDrawerLayout mNavigationDrawer;

    private int mSelectedMenuItemId;

    @Bind(R.id.toolbar) Toolbar mToolbar;
    @Bind(R.id.navigationView) NavigationView mNavigationView;

    /**
     * Provides a List of NavigationDrawerItem used for fragment navigation.
     * @return a List used for fragment navigation
     */
    protected abstract List<NavigationDrawerItem> getFragmentNavigationItems();

    /**
     * Provides the view that holds visible fragments.
     * @return the root view
     */
    protected abstract FrameLayout getContentLayout();

    /**
     * Provides an initialized instance of a fragment requested from a menu item. Override
     * this method to provide additional arguments depending on the fragment class:
     * <pre>{@code      @Override
     * protected Fragment getFragmentInstance(NavigationDrawerItem fragmentMenuItem) {
     *     Fragment f = super.getFragmentInstance(fragmentMenuItem);
     *     if (f instanceof RequiredFragmentClass) {
     *         f.getArguments().put ...
     *     }
     *     return f;
     * }}</pre>
     * @param fragmentMenuItem fragment menu item
     * @return initialized fragment
     */
    protected Fragment getFragmentInstance(NavigationDrawerItem fragmentMenuItem)
            throws IllegalAccessException, InstantiationException {
        Fragment f = fragmentMenuItem.getFragmentClass().newInstance();
        f.setArguments(new Bundle());
        if (fragmentMenuItem.getFragmentArguments() != null) {
            f.setArguments(fragmentMenuItem.getFragmentArguments());
        }
        return f;
    }

    @Nullable
    protected Fragment getCurrentFragment() {
        return getSupportFragmentManager().findFragmentById(getContentLayout().getId());
    }

    protected NavigationDrawerLayout getNavigationDrawerLayout() {
        return mNavigationDrawer;
    }

    protected int getSelectedMenuItemId() {
        return mSelectedMenuItemId;
    }

    protected void setActiveMenuItem() {
        mNavigationDrawer.selectMenuItem(mSelectedMenuItemId);
    }

    protected void initializeNavigationDrawer() {
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);
        mNavigationDrawer = new NavigationDrawerLayout(
                this, ((DrawerLayout) findViewById(R.id.drawerLayout)), mToolbar, mNavigationView) {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                return NavigationDrawerFragmentActivity.this.onNavigationItemSelected(item);
            }
        };
    }

    protected boolean onNavigationItemSelected(MenuItem item) {
        boolean result =  isCurrentFragmentItemSelected(item.getItemId())
                || isAnotherFragmentItemSelected(item.getItemId());
        return result;
    }

    protected void navigateTo(NavigationDrawerItem item) {
        try {
            FragmentTransaction transaction =
                    getSupportFragmentManager().beginTransaction();
            if (getCurrentFragment() != null) {
                transaction.addToBackStack(null);
            }
            transaction
                    .replace(getContentLayout().getId(), getFragmentInstance(item), item.getToolbarTitle())
                    .commit();
            setMenuItemAndTitleFor(item);
        } catch (Exception t) {
            Log.e("DrawerFragmentActivity", "failed to initialize a Fragment class", t);
        }
    }

    private boolean isCurrentFragmentItemSelected(int menuId) {
        if (menuId == mSelectedMenuItemId) {
            mNavigationDrawer.closeDrawer();
            return true;
        }
        return false;
    }

    private boolean isAnotherFragmentItemSelected(int menuId) {
        for (NavigationDrawerItem item : getFragmentNavigationItems()) {
            if (item.getNavigationViewItemId() == menuId) {
                mNavigationDrawer.deselectMenuItem(mSelectedMenuItemId);
                mNavigationDrawer.closeDrawer();
                navigateTo(item);
            }
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().addOnBackStackChangedListener(this);
    }

    @Override
    public void onBackStackChanged() {
        final Fragment fragment = getCurrentFragment();
        if (fragment != null) {
            NavigationDrawerItem item = Iterables.find(
                    getFragmentNavigationItems(), new Predicate<NavigationDrawerItem>() {
                        @Override
                        public boolean apply(NavigationDrawerItem item) {
                            return item.getToolbarTitle().equals(fragment.getTag());
                        }
                    });
            setMenuItemAndTitleFor(item);
        }
    }

    private void setMenuItemAndTitleFor(NavigationDrawerItem fragmentItem) {
        mSelectedMenuItemId = fragmentItem.getNavigationViewItemId();
        mNavigationDrawer.selectMenuItem(mSelectedMenuItemId);
        mToolbar.setTitle(fragmentItem.getToolbarTitle());
    }

    @Override
    public void onBackPressed() {
        if (mNavigationDrawer.isDrawerOpen()) {
            mNavigationDrawer.closeDrawer();
        } else {
            super.onBackPressed();
        }
    }
}
