package com.codepath.apps.restclienttemplate;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.codepath.apps.restclienttemplate.models.Tweet;

import org.parceler.Parcels;

public class TweetDetailsActivity extends AppCompatActivity {

    Tweet tweet;
    TextView tvBody;
    TextView tvName;
    TextView tvDate;
    TextView tvHandle;
    ImageView ivImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tweet_details);

        tvBody = (TextView) findViewById(R.id.tvBody);
        tvName = (TextView) findViewById(R.id.tvName);
        tvHandle = (TextView) findViewById(R.id.tvHandle);
        tvDate = (TextView) findViewById(R.id.tvDate);
        ivImage = (ImageView) findViewById(R.id.ivImage);
        tweet = (Tweet) Parcels.unwrap(getIntent().getParcelableExtra(Tweet.class.getSimpleName()));
        //Log.d("MovieDetailsActivity", String.format("Showing details for '%s'", movie.getTitle()));

        //get image with Glide?
        Glide.with(this).load(tweet.user.profileImageUrl).into(ivImage);

        tvBody.setText(tweet.body);
        tvName.setText(tweet.user.name);
        tvHandle.setText("@" + tweet.user.screenName);
        tvDate.setText(tweet.createdAt);

    }
}
