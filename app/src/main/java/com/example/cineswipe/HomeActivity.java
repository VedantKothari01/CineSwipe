package com.example.cineswipe;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.yuyakaido.android.cardstackview.CardStackLayoutManager;
import com.yuyakaido.android.cardstackview.CardStackView;
import com.yuyakaido.android.cardstackview.Direction;
import com.yuyakaido.android.cardstackview.StackFrom;
import com.yuyakaido.android.cardstackview.SwipeableMethod;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    private CardStackView cardStackView;
    private MovieCardAdapter movieCardAdapter;
    private CardStackLayoutManager cardStackLayoutManager;
    private List<Movie> movieList;
    private static final String API_KEY = Constants.API_KEY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        cardStackView = findViewById(R.id.cardStackView);
        movieList = new ArrayList<>();
        movieCardAdapter = new MovieCardAdapter(this, movieList);

        // Configure CardStackLayoutManager
        cardStackLayoutManager = new CardStackLayoutManager(this);
        cardStackLayoutManager.setStackFrom(StackFrom.Top);
        cardStackLayoutManager.setVisibleCount(3);
        cardStackLayoutManager.setTranslationInterval(8.0f);
        cardStackLayoutManager.setScaleInterval(0.95f);
        cardStackLayoutManager.setSwipeThreshold(0.3f);
        cardStackLayoutManager.setMaxDegree(20.0f);
        cardStackLayoutManager.setDirections(Direction.FREEDOM);
        cardStackLayoutManager.setSwipeableMethod(SwipeableMethod.Automatic);
        cardStackLayoutManager.setCanScrollHorizontal(true);

        cardStackView.setLayoutManager(cardStackLayoutManager);
        cardStackView.setAdapter(movieCardAdapter);

        // Fetch and display movies based on genres
        Intent intent = getIntent();
        List<String> userGenres = intent.getStringArrayListExtra("USER_GENRES");
        if (userGenres != null) {
            fetchMoviesByGenres(userGenres);
        } else {
            Log.e("HomeActivity", "No genres provided for fetching movies.");
        }
    }

    private void fetchMoviesByGenres(List<String> genres) {
        if (genres == null || genres.isEmpty()) {
            Log.e("HomeActivity", "No genres provided for fetching movies.");
            return;
        }

        String genreIds = TextUtils.join(",", genres);
        ApiService apiService = ApiClient.getRetrofitInstance().create(ApiService.class);
        Call<MovieResponse> call = apiService.getMoviesByGenres(API_KEY, genreIds, 1);

        call.enqueue(new Callback<MovieResponse>() {
            @Override
            public void onResponse(Call<MovieResponse> call, Response<MovieResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    movieList.clear();
                    movieList.addAll(response.body().getMovies());
                    movieCardAdapter.notifyDataSetChanged();
                } else {
                    Log.e("HomeActivity", "Error fetching movies: " + response.code() + " - " + response.message());
                }
            }

            @Override
            public void onFailure(Call<MovieResponse> call, Throwable t) {
                Log.e("HomeActivity", "Network call failed: " + t.getMessage());
            }
        });
    }
}
