package com.example.cineswipe;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.yuyakaido.android.cardstackview.CardStackLayoutManager;
import com.yuyakaido.android.cardstackview.CardStackListener;
import com.yuyakaido.android.cardstackview.CardStackView;
import com.yuyakaido.android.cardstackview.Direction;
import com.yuyakaido.android.cardstackview.StackFrom;
import com.yuyakaido.android.cardstackview.SwipeableMethod;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity implements CardStackListener {

    private CardStackView cardStackView;
    private CardStackLayoutManager manager;
    private MovieCardAdapter movieCardAdapter;
    private List<Movie> movieList;
    private static final String API_KEY = Constants.API_KEY;
    private static final String TAG = "HomeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initializeCardStackView();

        // Fetch and display movies based on genres
        Intent intent = getIntent();
        List<String> userGenres = intent.getStringArrayListExtra("USER_GENRES");
        if (userGenres != null) {
            fetchMoviesByGenres(userGenres);
        } else {
            Log.e(TAG, "No genres provided for fetching movies.");
            // Fallback to popular movies if no genres are provided
            fetchPopularMovies();
        }
    }

    private void initializeCardStackView() {
        cardStackView = findViewById(R.id.cardStackView);
        manager = new CardStackLayoutManager(this, this);
        movieList = new ArrayList<>();
        movieCardAdapter = new MovieCardAdapter(this, movieList);

        // Configure the card stack manager
        manager.setStackFrom(StackFrom.Top);
        manager.setVisibleCount(3);
        manager.setTranslationInterval(8.0f);
        manager.setScaleInterval(0.95f);
        manager.setSwipeThreshold(0.3f);
        manager.setMaxDegree(20.0f);
        manager.setDirections(Direction.HORIZONTAL);
        manager.setCanScrollHorizontal(true);
        manager.setSwipeableMethod(SwipeableMethod.AutomaticAndManual);

        cardStackView.setLayoutManager(manager);
        cardStackView.setAdapter(movieCardAdapter);
    }

    private void fetchMoviesByGenres(List<String> genres) {
        if (genres == null || genres.isEmpty()) {
            Log.e(TAG, "No genres provided for fetching movies.");
            return;
        }

        String genreIds = TextUtils.join(",", genres);
        ApiService apiService = ApiClient.getRetrofitInstance().create(ApiService.class);
        Call<MovieResponse> call = apiService.getMoviesByGenres(API_KEY, genreIds, 1);

        call.enqueue(new Callback<MovieResponse>() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onResponse(@NonNull Call<MovieResponse> call, Response<MovieResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    movieList.clear();
                    movieList.addAll(response.body().getMovies());
                    movieCardAdapter.notifyDataSetChanged();
                    Log.d(TAG, "Fetched " + movieList.size() + " movies");
                } else {
                    Log.e(TAG, "Error fetching movies: " + response.code() + " - " + response.message());
                    Toast.makeText(HomeActivity.this, "Failed to fetch movies", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<MovieResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Network call failed: " + t.getMessage());
                Toast.makeText(HomeActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchPopularMovies() {
        ApiService apiService = ApiClient.getRetrofitInstance().create(ApiService.class);
        Call<MovieResponse> call = apiService.getPopularMovies(API_KEY, "en-US", 1);

        call.enqueue(new Callback<MovieResponse>() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onResponse(@NonNull Call<MovieResponse> call, @NonNull Response<MovieResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    movieList.clear();
                    movieList.addAll(response.body().getMovies());
                    movieCardAdapter.notifyDataSetChanged();
                    Log.d(TAG, "Fetched " + movieList.size() + " popular movies");
                } else {
                    Log.e(TAG, "Error fetching popular movies: " + response.code());
                    Toast.makeText(HomeActivity.this, "Failed to fetch movies", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<MovieResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Network call failed: " + t.getMessage());
                Toast.makeText(HomeActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // CardStackListener implementation methods
    @Override
    public void onCardDragging(Direction direction, float ratio) {
        Log.d(TAG, "onCardDragging: d=" + direction.name() + " ratio=" + ratio);
    }

    @Override
    public void onCardSwiped(Direction direction) {
        Log.d(TAG, "onCardSwiped: p=" + manager.getTopPosition() + " d=" + direction);
        // Handle swipe based on direction (like/dislike logic)
        if (direction == Direction.Right) {
            // Handle like
            Toast.makeText(this, "Liked!", Toast.LENGTH_SHORT).show();
        } else if (direction == Direction.Left) {
            // Handle dislike
            Toast.makeText(this, "Passed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCardRewound() {
        Log.d(TAG, "onCardRewound: " + manager.getTopPosition());
    }

    @Override
    public void onCardCanceled() {
        Log.d(TAG, "onCardCanceled: " + manager.getTopPosition());
    }

    @Override
    public void onCardAppeared(View view, int position) {
        Log.d(TAG, "onCardAppeared: " + position);
    }

    @Override
    public void onCardDisappeared(View view, int position) {
        Log.d(TAG, "onCardDisappeared: " + position);
    }
}