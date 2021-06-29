package com.steelparrot.photogallery;

import android.net.Uri;

public class GalleryItem {

   // @SerializedName("title")
    private String mCaption;
  //  @SerializedName("id")
    private String mId;
   // @SerializedName("url_s")
    private String mUrl;

    private String mOwner;



    public String getCaption() {
        return mCaption;
    }

    public void setCaption(String caption) {
        mCaption = caption;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }


    @Override
    public String toString() {
        return mCaption;
    }

    public String getOwner() {
        return mOwner;
    }

    public void setOwner(String owner) {
        mOwner = owner;
    }

    // generate a photo page URL
    public Uri getPhotoPageUri() {
        return Uri.parse("https://www.flickr.com/photos/").buildUpon()
                .appendPath(mOwner)
                .appendPath(mId)
                .build();
    }
}
