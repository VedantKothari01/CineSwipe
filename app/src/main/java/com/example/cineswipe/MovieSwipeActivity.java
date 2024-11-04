package com.example.cineswipe;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.yuyakaido.android.cardstackview.CardStackLayoutManager;
import com.yuyakaido.android.cardstackview.CardStackView;
import com.yuyakaido.android.cardstackview.Direction;
import com.yuyakaido.android.cardstackview.Duration;
import com.yuyakaido.android.cardstackview.StackFrom;
import com.yuyakaido.android.cardstackview.SwipeableMethod;
import com.yuyakaido.android.cardstackview.CardStackListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MovieSwipeActivity extends AppCompatActivity implements CardStackListener {
    private static final String TAG = "MovieSwipeActivity";
    private CardStackView cardStackView;
    private MovieCardAdapter adapter;
    private CardStackLayoutManager layoutManager;
    private MovieRecommendationSystem recommendationSystem;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ProgressBar progressBar;
    private boolean isLoading = false;
    private int currentPage = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_swipe);

        initializeViews();
        initializeFirebase();
        setupCardStackView();
        initializeRecommendationSystem();
        fetchUserPreferences();
    }

    private void initializeViews() {
        cardStackView = findViewById(R.id.cardStackView);
        progressBar = findViewById(R.id.progressBar);
        adapter = new MovieCardAdapter(this, new ArrayList<>());
        cardStackView.setAdapter(adapter);
    }

    private void initializeFirebase() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() == null) {
            handleUserNotLoggedIn();
            return;
        }
    }

    private void setupCardStackView() {
        layoutManager = new CardStackLayoutManager(this, this);
        layoutManager.setSwipeableMethod(SwipeableMethod.AutomaticAndManual);
        layoutManager.setStackFrom(StackFrom.Top);
        layoutManager.setVisibleCount(3);
        layoutManager.setTranslationInterval(8.0f);
        layoutManager.setScaleInterval(0.95f);
        layoutManager.setSwipeThreshold(0.3f);
        layoutManager.setMaxDegree(20.0f);
        layoutManager.setDirections(Arrays.asList(Direction.Left, Direction.Right));
        layoutManager.setCanScrollHorizontal(true);
        layoutManager.setCanScrollVertical(false);

        cardStackView.setLayoutManager(layoutManager);
    }

    private void initializeRecommendationSystem() {
        String userId = auth.getCurrentUser().getUid();
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        recommendationSystem = new MovieRecommendationSystem(userId, apiService);

        recommendationSystem.setOnBatchReadyListener(new MovieRecommendationSystem.OnMoviesBatchReadyListener() {
            @Override
            public void onMoviesBatchReady(List<Movie> movies) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (movies.isEmpty()) {
                        showEmptyState();
                    } else {
                        adapter.setMovies(movies);
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MovieSwipeActivity.this,
                            "Error loading movies: " + message,
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void fetchUserPreferences() {
        progressBar.setVisibility(View.VISIBLE);
        String userId = auth.getCurrentUser().getUid();

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        List<String> genres = (List<String>) document.get("genres");
                        if (genres != null && !genres.isEmpty()) {
                            recommendationSystem.fetchNextBatch(
                                    recommendationSystem.getOnBatchReadyListener());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user preferences", e);
                    progressBar.setVisibility(View.GONE);
                });
    }

    @Override
    public void onCardDragging(Direction direction, float ratio) {}

    @Override
    public void onCardSwiped(Direction direction) {
        int position = layoutManager.getTopPosition() - 1;
        if (position >= 0 && position < adapter.getItemCount()) {
            Movie swipedMovie = adapter.getMovieAt(position);
            boolean isLiked = direction == Direction.Right;

            recommendationSystem.handleSwipe(swipedMovie, isLiked);

            if (layoutManager.getTopPosition() >= adapter.getItemCount() - 5) {
                recommendationSystem.fetchNextBatch(
                        recommendationSystem.getOnBatchReadyListener());
            }
        }
    }

    @Override
    public void onCardRewound() {}

    @Override
    public void onCardCanceled() {}

    @Override
    public void onCardAppeared(View view, int position) {}

    @Override
    public void onCardDisappeared(View view, int position) {
        if (position >= adapter.getItemCount() - 3 && !isLoading) {
            recommendationSystem.fetchNextBatch(
                    recommendationSystem.getOnBatchReadyListener());
        }
    }

    private void handleUserNotLoggedIn() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void showEmptyState() {
        Toast.makeText(this, "No movies available", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cleanup if needed
    }
}