package com.example.cineswipe;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
@SuppressWarnings("deprecation")
public class MovieDetailActivity extends AppCompatActivity {

    private ImageView backdropImageView;
    private TextView movieTitleTextView;
    private TextView releaseDateTextView;
    private TextView overviewTextView;
    private TextView ratingTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        backdropImageView = findViewById(R.id.backdropImageView);
        movieTitleTextView = findViewById(R.id.movieTitleTextView);
        releaseDateTextView = findViewById(R.id.releaseDateTextView);
        overviewTextView = findViewById(R.id.overviewTextView);
        ratingTextView = findViewById(R.id.ratingTextView);

        Intent intent = getIntent();
        Movie movie = (Movie) intent.getSerializableExtra("MOVIE_DATA");

        if (movie != null) {
            movieTitleTextView.setText(movie.getTitle());
            releaseDateTextView.setText("Release Date: " + movie.getReleaseDate());
            overviewTextView.setText("Overview: " + movie.getOverview());
            ratingTextView.setText("Rating: " + movie.getVoteAverage());

            Glide.with(this)
                    .load(movie.getBackdropUrl())
                    .into(backdropImageView);
        }
    }
}
