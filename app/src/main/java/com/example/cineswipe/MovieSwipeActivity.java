package com.example.cineswipe;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.yuyakaido.android.cardstackview.CardStackLayoutManager;
import com.yuyakaido.android.cardstackview.CardStackView;
import com.yuyakaido.android.cardstackview.Direction;
import com.yuyakaido.android.cardstackview.StackFrom;
import com.yuyakaido.android.cardstackview.SwipeableMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MovieSwipeActivity extends AppCompatActivity {

    private CardStackView cardStackView;
    private MovieHorizontalAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private final String API_KEY = Constants.API_KEY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        cardStackView = findViewById(R.id.cardStackView);
        adapter = new MovieHorizontalAdapter(this, new ArrayList<>());
        cardStackView.setAdapter(adapter);

        CardStackLayoutManager layoutManager = new CardStackLayoutManager(this);
        layoutManager.setSwipeableMethod(SwipeableMethod.Manual);
        layoutManager.setStackFrom(StackFrom.None);
        layoutManager.setVisibleCount(3);
        layoutManager.setDirections(Arrays.asList(Direction.Right, Direction.Left));
        layoutManager.setCanScrollVertical(false);
        layoutManager.setTranslationInterval(8.0f);
        layoutManager.setScaleInterval(0.1f);
        layoutManager.setSwipeThreshold(0.5f);
        layoutManager.setMaxDegree(20.0f);
        cardStackView.setLayoutManager(layoutManager);

        fetchUserPreferences();
    }

    private void fetchUserPreferences() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (userId != null) {
            db.collection("users").document(userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                List<String> genres = (List<String>) document.get("genres");
                                if (genres != null && !genres.isEmpty()) {
                                    fetchMoviesByGenres(genres);
                                } else {
                                    Log.d("MovieSwipeActivity", "No genres found for user. Fetching default movies.");
                                    fetchPopularMovies();
                                }
                            } else {
                                Log.d("MovieSwipeActivity", "No such document");
                            }
                        } else {
                            Log.d("MovieSwipeActivity", "Get failed with ", task.getException());
                        }
                    });
        } else {
            Log.d("MovieSwipeActivity", "User not logged in");
            handleUserNotLoggedIn();
        }
    }

    private void handleUserNotLoggedIn() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void fetchMoviesByGenres(List<String> genres) {
        if (genres != null && !genres.isEmpty()) {
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            Call<MovieResponse> call = apiService.getMoviesByGenres(API_KEY, String.join(",", genres), 1);

            call.enqueue(new Callback<MovieResponse>() {
                @Override
                public void onResponse(Call<MovieResponse> call, Response<MovieResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<Movie> movies = response.body().getMovies();
                        adapter.setMovies(movies);
                    } else {
                        Log.d("MovieSwipeActivity", "Response unsuccessful: " + response.message());
                    }
                }

                @Override
                public void onFailure(Call<MovieResponse> call, Throwable t) {
                    Log.d("MovieSwipeActivity", "API call failed: " + t.getMessage());
                }
            });
        } else {
            Log.d("MovieSwipeActivity", "No genres found for user.");
        }
    }

    private void fetchPopularMovies() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<MovieResponse> call = apiService.getPopularMovies(API_KEY, "en-US", 1);

        call.enqueue(new Callback<MovieResponse>() {
            @Override
            public void onResponse(Call<MovieResponse> call, Response<MovieResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Movie> movies = response.body().getMovies();
                    adapter.setMovies(movies);
                } else {
                    Log.d("MovieSwipeActivity", "Response unsuccessful: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<MovieResponse> call, Throwable t) {
                Log.d("MovieSwipeActivity", "API call failed: " + t.getMessage());
            }
        });
    }
}
