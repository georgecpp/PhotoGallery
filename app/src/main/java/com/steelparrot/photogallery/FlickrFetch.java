package com.steelparrot.photogallery;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FlickrFetch {

    private static int mPage=1;
    private static final String TAG = "FlickrFetch";
    private static final String API_KEY = "50fc024dac86a07f1444317069d5b6d2";
    private static final String FETCH_RECENTS_METHOD = "flickr.photos.getRecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";
    private static final Uri ENDPOINT = Uri.parse("https://api.flickr.com/services/rest/").buildUpon()
            .appendQueryParameter("method","flickr.photos.getRecent")
            .appendQueryParameter("api_key",API_KEY)
            .appendQueryParameter("format","json")
            .appendQueryParameter("nojsoncallback","1")
            .appendQueryParameter("extras","url_s")
            .appendQueryParameter("page",String.valueOf(mPage)).build();


    public static void setPage(int page) {
        mPage = page;
    }

    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            if(connection.getResponseCode()!= HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() + ": with "+urlSpec);
            }
            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead=in.read(buffer))>0) {
                out.write(buffer,0,bytesRead);
            }
            out.close();
            return out.toByteArray();
        }
        finally {
            connection.disconnect();
        }
    }

    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }


    private String buildUrl(String method, String query) {
        Uri.Builder uriBuilder = ENDPOINT.buildUpon().appendQueryParameter("method", method);

        if(method.equals(SEARCH_METHOD)) {
            uriBuilder.appendQueryParameter("text",query);
        }

        return uriBuilder.build().toString();
    }

    //*********
    public List<GalleryItem> fetchRecentPhotos() {
        String url = buildUrl(FETCH_RECENTS_METHOD,null);
        return downloadGalleryItems(url);
    }

    public List<GalleryItem> searchPhotos(String query) {
        String url = buildUrl(SEARCH_METHOD,query);
        return downloadGalleryItems(url);
    }

    //*********

    private List<GalleryItem> downloadGalleryItems(String url) {

        List<GalleryItem> items = new ArrayList<>();

        try {
            String jsonString = getUrlString(url);
            Log.i(TAG, "Received JSON: "+jsonString);


            JSONObject jsonBody = new JSONObject(jsonString);
            parseItems(items,jsonBody);

//            // GSON Approach
//            Gson gson = new Gson();
//            GalleryPage galleryPage = gson.fromJson(jsonString,GalleryPage.class);
//            items = galleryPage.getGalleryItemList();

        }
        catch (IOException ioe) {
            Log.e(TAG,"Failed to fetch items",ioe);
        }
        catch (JSONException e) {
            Log.e(TAG, "Failed to parse JSON:",e);
        }
        return items;
    }


    private void parseItems(List<GalleryItem> items, JSONObject jsonBody) throws IOException, JSONException {

        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");
        for(int i=0;i<photoJsonArray.length();i++)
        {
            JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);

            GalleryItem galleryItem = new GalleryItem();

            galleryItem.setId(photoJsonObject.getString("id"));
            galleryItem.setCaption(photoJsonObject.getString("title"));

            if(!photoJsonObject.has("url_s")) {
                continue;
            }
            galleryItem.setUrl(photoJsonObject.getString("url_s"));

            galleryItem.setOwner(photoJsonObject.getString("owner"));


            items.add(galleryItem);
        }
    }
}
