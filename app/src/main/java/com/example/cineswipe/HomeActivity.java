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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.yuyakaido.android.cardstackview.CardStackLayoutManager;
import com.yuyakaido.android.cardstackview.CardStackListener;
import com.yuyakaido.android.cardstackview.CardStackView;
import com.yuyakaido.android.cardstackview.Direction;
import com.yuyakaido.android.cardstackview.StackFrom;
import com.yuyakaido.android.cardstackview.SwipeableMethod;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class HomeActivity extends AppCompatActivity implements CardStackListener {

    private CardStackView cardStackView;
    private CardStackLayoutManager manager;
    private MovieCardAdapter movieCardAdapter;
    private List<Movie> movieList;
    private List<Movie> likedMovies; // Declare the likedMovies list here
    private static final String API_KEY = Constants.API_KEY;
    private static final String TAG = "HomeActivity";
    private int currentPage = 1;
    private List<String> userGenres;
    private Set<String> addedMovieIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        likedMovies = new ArrayList<>();
        initializeCardStackView();

        Intent intent = getIntent();
        userGenres = intent.getStringArrayListExtra("USER_GENRES");
        if (userGenres != null && !userGenres.isEmpty()) {
            fetchMoviesByGenres(userGenres, currentPage);
        } else {
            Log.d(TAG, "No genres provided, fetching random movies instead.");
            fetchRandomMovies(currentPage);
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

    private void fetchMoviesByGenres(List<String> genres, int page) {
        if (genres == null || genres.isEmpty()) {
            Log.e(TAG, "No genres provided for fetching movies.");
            return;
        }

        // Fetch movies for two selected genres at a time
        List<String> genrePairs = new ArrayList<>();
        for (int i = 0; i < genres.size(); i++) {
            for (int j = i + 1; j < genres.size(); j++) {
                genrePairs.add(genres.get(i) + "," + genres.get(j)); // Create pairs
            }
        }

        // Fetch movies for each pair of genres
        for (String genrePair : genrePairs) {
            ApiService apiService = ApiClient.getRetrofitInstance().create(ApiService.class);
            Call<MovieResponse> call = apiService.getMoviesByGenres(API_KEY, genrePair, page);
            call.enqueue(new Callback<MovieResponse>() {
                @Override
                public void onResponse(@NonNull Call<MovieResponse> call, @NonNull Response<MovieResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<Movie> newMovies = response.body().getMovies();
                        for (Movie movie : newMovies) {
                            // Check if the movie ID has already been added
                            if (!addedMovieIds.contains(movie.getId())) {
                                addedMovieIds.add(movie.getId()); // Add to the set
                                movieCardAdapter.addMovie(movie); // Add to adapter (update your adapter method accordingly)
                            }
                        }
                        Log.d(TAG, "Fetched " + newMovies.size() + " movies for genres: " + genrePair);
                    } else {
                        Log.e(TAG, "Error fetching movies: " + response.code() + " - " + response.message());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<MovieResponse> call, @NonNull Throwable t) {
                    Log.e(TAG, "Network call failed: " + t.getMessage());
                }
            });
        }
    }
    private void fetchRandomMovies(int page) {
        ApiService apiService = ApiClient.getRetrofitInstance().create(ApiService.class);

        // Get a random sorting method
        String[] sortingMethods = {
                "popularity.desc",
                "revenue.desc",
                "vote_average.desc",
                "primary_release_date.desc",
                "vote_count.desc"
        };
        String randomSort = sortingMethods[(int) (Math.random() * sortingMethods.length)];

        Call<MovieResponse> call = apiService.getRandomMovies(API_KEY, "en-US", page, randomSort);

        call.enqueue(new Callback<MovieResponse>() {
            @Override
            public void onResponse(@NonNull Call<MovieResponse> call, @NonNull Response<MovieResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Movie> newMovies = response.body().getMovies();
                    // Shuffle the movies list for additional randomness
                    Collections.shuffle(newMovies);
                    movieCardAdapter.addMovies(newMovies);
                    Log.d(TAG, "Fetched " + newMovies.size() + " random movies");
                } else {
                    Log.e(TAG, "Error fetching random movies: " + response.code());
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


    @Override
    public void onCardDragging(Direction direction, float ratio) {

    }

    @Override
    public void onCardSwiped(Direction direction) {
        Log.d(TAG, "onCardSwiped: p=" + manager.getTopPosition() + " d=" + direction);

        if (direction == Direction.Right) {
            int currentMovieIndex = manager.getTopPosition() - 1;
            if (currentMovieIndex >= 0 && currentMovieIndex < movieList.size()) {
                Movie likedMovie = movieList.get(currentMovieIndex);
                likedMovies.add(likedMovie);
                saveLikedMovie(likedMovie);
                Toast.makeText(this, "Liked!", Toast.LENGTH_SHORT).show();
            }
        } else if (direction == Direction.Left) {
            Toast.makeText(this, "Passed", Toast.LENGTH_SHORT).show();
        }
        if (manager.getTopPosition() == movieList.size() - 5) {
            currentPage++;
            if (userGenres != null && !userGenres.isEmpty()) {
                fetchMoviesByGenres(userGenres, currentPage);
            } else {
                fetchRandomMovies(currentPage);
            }
        }
    }

    private void saveLikedMovie(Movie movie) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId)
                .update("likedMovies", FieldValue.arrayUnion(movie.getId())) // Store movie ID
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Liked movie added successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Error adding liked movie", e));
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