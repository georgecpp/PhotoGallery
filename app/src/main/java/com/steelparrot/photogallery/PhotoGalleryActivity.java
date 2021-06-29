package com.steelparrot.photogallery;

import androidx.fragment.app.Fragment;

import android.content.Context;
import android.content.Intent;

public class PhotoGalleryActivity extends SingleFragmentActivity {

    public static Intent newIntent(Context context) {
        return new Intent(context,PhotoGalleryActivity.class);
    }
    @Override
    protected Fragment createFragment() {
        return PhotoGalleryFragment.newInstance();
    }
    
}