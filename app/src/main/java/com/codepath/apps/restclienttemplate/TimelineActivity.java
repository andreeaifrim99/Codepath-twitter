package com.codepath.apps.restclienttemplate;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.codepath.apps.restclienttemplate.models.Tweet;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.parceler.Parcels;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

public class TimelineActivity extends AppCompatActivity {

    public static final int COMPOSE_TWEET_REQUEST_CODE = 100;
    private SwipeRefreshLayout swipeContainer;
    private EndlessRecyclerViewScrollListener scrollListener;
    MenuItem miActionProgressItem;

    TwitterClient client;
    TweetAdapter tweetAdapter;
    ArrayList<Tweet> tweets;
    RecyclerView rvTweets;
    long maxId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline);

        client = TwitterApp.getRestClient(this); //added this statement

        //find RV
        rvTweets = (RecyclerView) findViewById(R.id.rvTweet);
        //instantiate the data source (arraylist)
        tweets = new ArrayList<>();
        //construct adapter from datasource
        tweetAdapter = new TweetAdapter(tweets);
        //RV setup (layout manager, use adapter)
        LinearLayoutManager managerLin = new LinearLayoutManager(this);
        rvTweets.setLayoutManager(managerLin);
        //set adapter
        rvTweets.setAdapter(tweetAdapter);

        //should set a twitter icon on the action bar

        //getSupportActionBar().setTitle("Twitter");
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#03A9F4")));
        //getSupportActionBar().setLogo(getDrawable(R.drawable.ic_launcher_twitter));

        getSupportActionBar().setIcon(R.drawable.ic_launcher_twitter);
        getSupportActionBar().setDisplayUseLogoEnabled(true);

        // Lookup the swipe container view
        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Your code to refresh the list here.
                // Make sure you call swipeContainer.setRefreshing(false)
                // once the network request has completed successfully.
                fetchTimelineAsync(0);
            }
        });

        //used for endless scrolling
        scrollListener = new EndlessRecyclerViewScrollListener(managerLin) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                // Triggered only when new data needs to be appended to the list
                // Add whatever code is needed to append new items to the bottom of the list
                loadNextDataFromApi(page);
            }
        };
        // Adds the scroll listener to RecyclerView
        rvTweets.addOnScrollListener(scrollListener);
        //end endless scrolling

        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
    }

    //method for endless scrolling
    public void loadNextDataFromApi(int offset) {
        // Send an API request to retrieve appropriate paginated data
        //  --> Send the request including an offset value (i.e `page`) as a query parameter.
        //  --> Deserialize and construct new model objects from the API response
        //  --> Append the new data objects to the existing set of items inside the array of items
        //  --> Notify the adapter of the new items made with `notifyItemRangeInserted()`
        maxId = tweets.get(tweets.size() - 1).uid; //gets id of last tweet
        populateTimeline(maxId);
    }

    //used to refresh
    public void fetchTimelineAsync(int page) {
        // Send the network request to fetch the updated data
        // `client` here is an instance of Android Async HTTP
        // getHomeTimeline is an example endpoint.
                // Remember to CLEAR OUT old items before appending in the new ones
        //showProgressBar();
        tweetAdapter.clear();
        // ...the data has come back, add new items to your adapter...
        /*for (int i = 0; i < json.length(); i++) {
                   tweets.add(Tweet.fromJSON(json.getJSONObject(i)));
               }*/

        populateTimeline(maxId);
                // Now we call setRefreshing(false) to signal refresh has finished
        swipeContainer.setRefreshing(false);
        //hideProgressBar();
    }


    //next 3 methods are used to make the progress bar
    public void showProgressBar() {
        // Show progress item
        miActionProgressItem.setVisible(true);
    }

    public void hideProgressBar() {
        // Hide progress item
        miActionProgressItem.setVisible(false);
    }
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Store instance of the menu item containing progress
        miActionProgressItem = menu.findItem(R.id.miActionProgress);
        populateTimeline(maxId);

        // Return to finish
        return super.onPrepareOptionsMenu(menu);
    }
    //end progress bar methods


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_timeline, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.miCompose:
                composeMessage();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == COMPOSE_TWEET_REQUEST_CODE && resultCode == RESULT_OK) {
            //this is mock code, refer to previous code to extract Parcelable //CHANGE
            Tweet resultTweet = Parcels.unwrap(data.getParcelableExtra(ComposeActivity.RESULT_TWEET_KEY));

            tweets.add(0, resultTweet);
            tweetAdapter.notifyItemInserted(0);
            rvTweets.scrollToPosition(0);
            Toast.makeText(this, "success", Toast.LENGTH_LONG).show();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void composeMessage() {
        //open ComposeActivity to create a new tweet
        Intent composeTweet = new Intent(this, ComposeActivity.class);
        startActivityForResult(composeTweet, COMPOSE_TWEET_REQUEST_CODE);
    }

    private void populateTimeline(long maxId) {
        showProgressBar();

        client.getHomeTimeline(maxId, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                //Log.d("TwitterClient", response.toString());
                //iterate through the JSON array
                //for each entry deserialize
                for (int i = 0; i < response.length(); i++) {
                    //convert each object to a Tweet
                    //add Tweet to data source
                    //notofy adapter that we've added an item
                    try {
                        Tweet tweet = Tweet.fromJSON(response.getJSONObject(i));
                        tweets.add(tweet);
                        tweetAdapter.notifyItemInserted(tweets.size() - 1);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                hideProgressBar();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.d("TwitterClient", response.toString());
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Log.d("TwitterClient", responseString.toString());
                throwable.printStackTrace();
                hideProgressBar();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                Log.d("TwitterClient", errorResponse.toString());
                throwable.printStackTrace();
                hideProgressBar();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                Log.d("TwitterClient", errorResponse.toString());
                throwable.printStackTrace();
                hideProgressBar();
            }
        });
    }
}
