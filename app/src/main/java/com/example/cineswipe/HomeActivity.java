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

import com.google.firebase.auth.FirebaseAuth;
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
    private static final String TAG = "HomeActivity";
    private CardStackView cardStackView;
    private CardStackLayoutManager manager;
    private MovieCardAdapter movieCardAdapter;
    private MovieRecommendationSystem recommendationSystem;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        apiService = ApiClient.getRetrofitInstance().create(ApiService.class);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        recommendationSystem = new MovieRecommendationSystem(userId, apiService);

        initializeCardStackView();
        loadInitialMovies();
    }

    private void initializeCardStackView() {
        cardStackView = findViewById(R.id.cardStackView);
        manager = new CardStackLayoutManager(this, this);
        movieCardAdapter = new MovieCardAdapter(this, new ArrayList<>());

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

    private void loadInitialMovies() {
        recommendationSystem.fetchNextBatch(new MovieRecommendationSystem.OnMoviesBatchReadyListener() {
            @Override
            public void onMoviesBatchReady(List<Movie> movies) {
                movieCardAdapter.setMovies(movies);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(HomeActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onCardSwiped(Direction direction) {
        int position = manager.getTopPosition() - 1;
        Movie swipedMovie = movieCardAdapter.getMovieAt(position);
        boolean isLiked = direction == Direction.Right;

        // Update recommendations based on swipe
        recommendationSystem.handleSwipe(swipedMovie, isLiked);

        // Show appropriate toast
        if (isLiked) {
            Toast.makeText(this, "Added to favorites!", Toast.LENGTH_SHORT).show();
        }

        // Check if we need to load more movies
        if (manager.getTopPosition() >= movieCardAdapter.getItemCount() - 5) {
            loadMoreMovies();
        }
    }

    private void loadMoreMovies() {
        recommendationSystem.fetchNextBatch(new MovieRecommendationSystem.OnMoviesBatchReadyListener() {
            @Override
            public void onMoviesBatchReady(List<Movie> movies) {
                movieCardAdapter.addMovies(movies);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(HomeActivity.this, "Error loading more movies: " + message,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // CardStackListener implementation methods
    @Override
    public void onCardDragging(Direction direction, float ratio) {
        Log.d(TAG, "onCardDragging: d=" + direction.name() + " ratio=" + ratio);
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