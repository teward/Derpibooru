package derpibooru.derpy.ui.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.EnumSet;

import butterknife.Bind;
import butterknife.ButterKnife;
import derpibooru.derpy.ImageDownload;
import derpibooru.derpy.R;
import derpibooru.derpy.data.server.DerpibooruImageDetailed;
import derpibooru.derpy.data.server.DerpibooruImageInteraction;
import derpibooru.derpy.data.server.DerpibooruImageThumb;
import derpibooru.derpy.ui.presenters.ImageInteractionPresenter;
import derpibooru.derpy.ui.views.AccentColorIconButton;
import derpibooru.derpy.ui.views.ImageBottomBarView;
import derpibooru.derpy.ui.views.ImageTagView;
import derpibooru.derpy.ui.views.ImageTopBarView;
import uk.co.senab.photoview.PhotoViewAttacher;

public class ImageActivityMainFragment extends Fragment {
    public static final String EXTRAS_IS_USER_LOGGED_IN = "derpibooru.derpy.IsLoggedIn";
    private static final int REQUEST_WRITE_STORAGE = 142;

    @Bind(R.id.imageView) ImageView imageView;
    @Bind(R.id.imageTopBar) ImageTopBarView topBar;
    @Bind(R.id.imageBottomBar) ImageBottomBarView bottomBar;

    private ImageInteractionPresenter mInteractionPresenter;
    private ImageActivityMainFragmentHandler mActivityCallbacks;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.activity_image_fragment_main, container, false);
        ButterKnife.bind(this, v);
        setHasOptionsMenu(true);
        bottomBar.initializeWithFragmentManager(getChildFragmentManager());
        setBottomBarCallbackHandler();
        if (getArguments().containsKey(ImageListFragment.EXTRAS_IMAGE_THUMB)) {
            displayFromImageThumb();
        } else {
            boolean isUserLogged = getArguments().getBoolean(EXTRAS_IS_USER_LOGGED_IN);
            display(isUserLogged);
        }
        return v;
    }

    public void setActivityCallbacks(ImageActivityMainFragmentHandler handler) {
        mActivityCallbacks = handler;
    }

    public void onDetailedImageFetched() {
        display(getArguments().getBoolean(EXTRAS_IS_USER_LOGGED_IN));
    }

    public void resetView() {
        setBottomBarCallbackHandler();
    }

    private void display(boolean isLoggedIn) {
        mActivityCallbacks.setToolbarTitle(String.format("#%d", mActivityCallbacks.getImage().getThumb().getId()));
        loadImageIfNotShownAlready(mActivityCallbacks.getImage().getThumb().getLargeImageUrl());
        if (mInteractionPresenter == null) {
            initializeInteractionPresenter(null, isLoggedIn);
        } else if (isLoggedIn) {
            mInteractionPresenter.enableInteractions();
        }
        mInteractionPresenter.refreshInfo(
                mActivityCallbacks.getImage().getThumb().getFaves(),
                mActivityCallbacks.getImage().getThumb().getUpvotes(),
                mActivityCallbacks.getImage().getThumb().getDownvotes());
        bottomBar.setInfoFromDetailed(mActivityCallbacks.getImage());
    }

    private void displayFromImageThumb() {
        DerpibooruImageThumb thumb = getArguments().getParcelable(ImageListFragment.EXTRAS_IMAGE_THUMB);
        mActivityCallbacks.setToolbarTitle(String.format("#%d", thumb.getId()));
        loadImageIfNotShownAlready(thumb.getLargeImageUrl());
        /* do not enable image interactions yet, wait for DerpibooruImageDetailed to load */
        initializeInteractionPresenter(thumb, false);
        mInteractionPresenter.refreshInfo(thumb.getFaves(), thumb.getUpvotes(), thumb.getDownvotes());
    }

    private void loadImageIfNotShownAlready(String url) {
        if (imageView.getVisibility() == View.GONE) {
            imageView.setVisibility(View.VISIBLE);
            if (url.endsWith(".gif")) {
                Glide.with(this).load(url).diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .listener(new GlideRequestListener(imageView))
                        .into(imageView);
            } else {
                Glide.with(this).load(url).diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .listener(new GlideRequestListener(imageView))
                        .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                        .into(imageView);
            }
        }
    }

    private void setBottomBarCallbackHandler() {
        bottomBar.setTagListener(new ImageTagView.OnTagClickListener() {
            @Override
            public void onTagClicked(int tagId) {
                mActivityCallbacks.openTagInformation(tagId);
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_image_activity_main_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionDownloadImage:
                if (mActivityCallbacks != null) {
                    if (ContextCompat.checkSelfPermission(
                            getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        startImageDownload();
                    } else {
                        requestPermissions(new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, REQUEST_WRITE_STORAGE);
                    }
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if ((requestCode == REQUEST_WRITE_STORAGE)
                && (grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            startImageDownload();
        }
    }

    private void startImageDownload() {
        new ImageDownload(getContext(), mActivityCallbacks.getImage().getThumb().getId(),
                          mActivityCallbacks.getImage().getTags(),
                          mActivityCallbacks.getImage().getDownloadUrl())
                .start();
    }

    private void initializeInteractionPresenter(final DerpibooruImageThumb thumbToBeUsedIfDetailedImageIsNotAvailable,
                                                boolean isUserLoggedIn) {
        mInteractionPresenter = new ImageInteractionPresenter(getContext(), isUserLoggedIn) {
            @Nullable
            @Override
            protected AccentColorIconButton getScoreButton() {
                return topBar.getScoreButton();
            }

            @Nullable
            @Override
            protected AccentColorIconButton getFaveButton() {
                return bottomBar.getFaveButton();
            }

            @Nullable
            @Override
            protected AccentColorIconButton getUpvoteButton() {
                return topBar.getUpvoteButton();
            }

            @Nullable
            @Override
            protected AccentColorIconButton getDownvoteButton() {
                return topBar.getDownvoteButton();
            }

            @Override
            protected int getIdForImageInteractions() {
                return mActivityCallbacks.getImage().getThumb().getIdForImageInteractions();
            }

            @NonNull
            @Override
            protected EnumSet<DerpibooruImageInteraction.InteractionType> getInteractions() {
                if (mActivityCallbacks.getImage() == null) {
                    return thumbToBeUsedIfDetailedImageIsNotAvailable.getImageInteractions();
                } else {
                    return mActivityCallbacks.getImage().getThumb().getImageInteractions();
                }
            }

            @Override
            protected void addInteraction(DerpibooruImageInteraction.InteractionType interaction) {
                mActivityCallbacks.getImage().getThumb().getImageInteractions().add(interaction);
            }

            @Override
            protected void removeInteraction(DerpibooruImageInteraction.InteractionType interaction) {
                mActivityCallbacks.getImage().getThumb().getImageInteractions().remove(interaction);
            }

            @Override
            protected void onInteractionFailed() {
                /* TODO: pop up an error screen */
            }

            @Override
            protected void onInteractionCompleted(DerpibooruImageInteraction result) {
                mActivityCallbacks.getImage().getThumb().setFaves(result.getFavorites());
                mActivityCallbacks.getImage().getThumb().setUpvotes(result.getUpvotes());
                mActivityCallbacks.getImage().getThumb().setDownvotes(result.getDownvotes());
                super.onInteractionCompleted(result);
            }

            @SuppressWarnings("ConstantConditions")
            @Override
            public void refreshInfo(int faves, int upvotes, int downvotes) {
                /* prevent icons from blending into the background by disabling tint toggle on touch
                 * (only in case there was no user interaction) */
                getFaveButton().setToggleIconTintOnTouch(
                        getInteractions().contains(DerpibooruImageInteraction.InteractionType.Fave));
                getUpvoteButton().setToggleIconTintOnTouch(
                        getInteractions().contains(DerpibooruImageInteraction.InteractionType.Upvote));
                getDownvoteButton().setToggleIconTintOnTouch(
                        getInteractions().contains(DerpibooruImageInteraction.InteractionType.Downvote));
                super.refreshInfo(faves, upvotes, downvotes);
            }
        };
    }

    private class GlideRequestListener implements RequestListener<String, GlideDrawable> {
        private ImageView mImageView;

        GlideRequestListener(ImageView glideTarget) {
            mImageView = glideTarget;
        }

        @Override
        public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
            Log.e("ImageActivity", "Failed to load the image with Glide", e);
            return false;
        }

        @Override
        public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target,
                                       boolean isFromMemoryCache, boolean isFirstResource) {
            if (getView() != null) getView().findViewById(R.id.progressImage).setVisibility(View.GONE);
            attachPhotoView(mImageView);
            return false;
        }

        private void attachPhotoView(ImageView target) {
            new PhotoViewAttacher(target).setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
                @Override
                public void onViewTap(View view, float x, float y) {
                    if (mActivityCallbacks.isToolbarVisible()) {
                        mActivityCallbacks.setToolbarVisible(false);
                        topBar.setVisibility(View.INVISIBLE);
                        bottomBar.setVisibility(View.INVISIBLE);
                    } else {
                        mActivityCallbacks.setToolbarVisible(true);
                        topBar.setVisibility(View.VISIBLE);
                        bottomBar.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
    }

    public interface ImageActivityMainFragmentHandler {
        DerpibooruImageDetailed getImage();
        boolean isToolbarVisible();
        void setToolbarTitle(String title);
        void setToolbarVisible(boolean visible);
        void openTagInformation(int tagId);
    }
}
