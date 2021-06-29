package com.steelparrot.photogallery;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends VisibleFragment {
    private RecyclerView mPhotoRecyclerView;
    private static final String TAG = "PhotoGalleryFragment";
    private static final int COLUMN_SIZE = 200;
    private List<GalleryItem> mItems = new ArrayList<>();
    private ThumbnailDownloader<Integer> mThumbnailDownloader;

    private int previousTotal = 0;
    private int pastVisibilesItems, visibleItemCount, totalItemCount;
    private boolean loading = true;
    private int currentPage=0;
    private final int maxPage = 5;
    private int mGridColumns=3;
    private GridLayoutManager mGridLayoutManager;
    private ProgressBar mProgressBar;



    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        updateItems();



        Handler responseHandler = new Handler(); // by default will attach itself to the Looper for the current thread. -- Handler created in onCreate() , deci will be attached to the main thread's Looper.
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(new ThumbnailDownloader.ThumbnailDownloadListener<Integer>() {
            @Override
            public void onThumbnailDownloaded(Integer target, Bitmap thumbnail) {
                mPhotoRecyclerView.getAdapter().notifyItemChanged(target);
            }
        });
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG,"Background thread has started!");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG,"Background thread destroyed!");
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery,menu);

        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG,"QuerySubmitText: "+ query);
                QueryPreferences.setStoredQuery(getActivity(),query);
                searchView.onActionViewCollapsed();
                updateItems();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG,"QueryTextChange: "+newText);
                return false;
            }
        });

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String query = QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query,false);
            }
        });

        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if(QueryPreferences.isAlarmOn(getActivity())){
            toggleItem.setTitle(R.string.stop_polling);
        }
        else {
            toggleItem.setTitle(R.string.start_polling);
        }

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(),null);
                updateItems();
                return true;

            case R.id.menu_item_toggle_polling:
                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) {
                    /*boolean shouldStartAlarm = !_PollService.isServiceAlarmOn(getActivity());
                    _PollService.setServiceAlarm(getActivity(),shouldStartAlarm);*/
                    boolean isAlarmOn = QueryPreferences.isAlarmOn(getActivity());
                    if(!isAlarmOn)
                    {
                        Intent intent = _PollService.newIntent(getActivity());
                        _PollService.enqueueWork(getActivity(),intent);
                    }
                    QueryPreferences.setAlarmOn(getActivity(),!isAlarmOn);
                }
                getActivity().invalidateOptionsMenu();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateItems() {
        String query = QueryPreferences.getStoredQuery(getActivity());
        new FetchItemsTask(query).execute();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mGridLayoutManager = new GridLayoutManager(getActivity(),mGridColumns);

        mPhotoRecyclerView = (RecyclerView) v.findViewById(R.id.photo_recycler_view);
        mPhotoRecyclerView.setLayoutManager(mGridLayoutManager);
        mProgressBar = (ProgressBar) v.findViewById(R.id.loading_indicator);


  /*      mPhotoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mPhotoRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                // Adjust the columns to fit based on width of RecyclerView
                int width = mPhotoRecyclerView.getWidth();
                mGridColumns = width / COLUMN_SIZE;
                mGridLayoutManager = new GridLayoutManager(getActivity(),mGridColumns);
                mPhotoRecyclerView.setLayoutManager(mGridLayoutManager);
                setupAdapter();
            }
        });*/

        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {

                    int begin = Math.max(mGridLayoutManager.findFirstVisibleItemPosition()-10,0);
                    int end = Math.min(mGridLayoutManager.findLastVisibleItemPosition()+10,mItems.size()-1);
                    for(int i=begin;i<=end;i++) {
                        String url = mItems.get(i).getUrl();
                        if(mThumbnailDownloader.mThumbnailDownloadCache.get(url)==null) {
                            Log.i(TAG,"Requesting download at position: " + i);
                            mThumbnailDownloader.queueThumbnail(i,url);
                        }
                    }

                    if(!loading && dy > 0 && currentPage < maxPage && ((GridLayoutManager) mPhotoRecyclerView.getLayoutManager()).findLastVisibleItemPosition() >= (mItems.size()-1))  {
                        loading=true;
                        Toast.makeText(getActivity(),"Page: "+String.valueOf(currentPage+1),Toast.LENGTH_SHORT).show();
                        currentPage++;
                        updateItems();
                    }
            }
        });

        if(mPhotoRecyclerView.getAdapter()==null) {
            setupAdapter();
        }
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
        mThumbnailDownloader.clearCache();
    }

    private void setupAdapter() {
        if(isAdded()) {
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private ImageView mItemImageView;
        private GalleryItem mGalleryItem;


        public PhotoHolder(@NonNull View itemView) {
            super(itemView);
            mItemImageView = (ImageView) itemView.findViewById(R.id.item_image_view);
            itemView.setOnClickListener(this);
        }

        // classic version of doing the binding -- uses ThumbnailDownloader and Handler,Looper, requestHandler and responseHandler.
        public void bindDrawable(Drawable drawable) {
            mItemImageView.setImageDrawable(drawable);
        }

   /*     // Picasso version of binding -- handles both the ThumbnailDownloader work and FlickrFetch.
        public void bindGalleryItem(GalleryItem galleryItem) {
            Picasso.get().load(galleryItem.getUrl()).placeholder(R.drawable.bill_up_close).into(mItemImageView);
        }

        */
        public void bindGalleryItem(GalleryItem galleryItem)
        {
            mGalleryItem = galleryItem;
        }

        @Override
        public void onClick(View view) {
          /*  Intent implicit_intent = new Intent(Intent.ACTION_VIEW, mGalleryItem.getPhotoPageUri());
            startActivity(implicit_intent);*/

            Intent intent = PhotoPageActivity.newIntent(getActivity(), mGalleryItem.getPhotoPageUri());
            startActivity(intent);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {

        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @NonNull
        @Override
        public PhotoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.list_item_gallery,parent,false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoHolder holder, int position) {

            GalleryItem galleryItem = mGalleryItems.get(position);
            holder.bindGalleryItem(galleryItem);

            // Picasso version of the code
            // holder.bindGalleryItem(galleryItem);

            String url = galleryItem.getUrl();
            Bitmap bitmap = mThumbnailDownloader.mThumbnailDownloadCache.get(url);
            if(bitmap==null) {
                Drawable placeHolder = getResources().getDrawable(R.drawable.ic_launcher_background);
                holder.bindDrawable(placeHolder);
                mThumbnailDownloader.queueThumbnail(position,url);
            }
            else
            {
                Drawable drawable = new BitmapDrawable(getResources(),bitmap);
                holder.bindDrawable(drawable);
            }

            //mThumbnailDownloader.queueThumbnail(holder,galleryItem.getUrl());

        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }


    // first parameter to specify input parameters for execute -- alias for doInBackground(String... params) here. -- pased to execute("first param", "second param", "etc."); -- variable number of parameters

    // second type parameter allows you to specify the type for sending progress updates.
    private class FetchItemsTask extends AsyncTask<Void,Void,List<GalleryItem>> {
        private String mQuery;

        public FetchItemsTask(String query) {
            mQuery = query;
        }

        @Override
        protected List<GalleryItem> doInBackground(Void... voids) {
            FlickrFetch.setPage(currentPage);
            if(mQuery==null) {
                return new FlickrFetch().fetchRecentPhotos();
            }
            else
            {
                return new FlickrFetch().searchPhotos(mQuery);
            }
        }

        @Override
        protected void onPostExecute(List<GalleryItem> galleryItems) {
            // set visibility View.GONE for progressBar
            mProgressBar.setVisibility(View.GONE);
            mPhotoRecyclerView.setVisibility(View.VISIBLE);

            mItems = galleryItems;
            setupAdapter();

            loading=false;
            mPhotoRecyclerView.getAdapter().notifyDataSetChanged();
        }

    }

}
