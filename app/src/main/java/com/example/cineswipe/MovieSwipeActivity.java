package com.example.cineswipe;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.yuyakaido.android.cardstackview.CardStackLayoutManager;
import com.yuyakaido.android.cardstackview.CardStackListener;
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
public class MovieSwipeActivity extends AppCompatActivity implements CardStackListener {
    private static final String TAG = "MovieSwipeActivity";
    private CardStackView cardStackView;
    private MovieCardAdapter adapter;
    private CardStackLayoutManager layoutManager;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private final String API_KEY = Constants.API_KEY;
    private List<Movie> likedMovies = new ArrayList<>();

    private int currentPage = 1;
    private boolean isLoading = false;
    private List<String> userGenres;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initializeComponents();
        setupCardStackView();
        fetchUserPreferences();
    }

    private void initializeComponents() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        cardStackView = findViewById(R.id.cardStackView);
        adapter = new MovieCardAdapter(this, new ArrayList<>());
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
        cardStackView.setAdapter(adapter);
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
                                userGenres = (List<String>) document.get("genres");
                                if (userGenres != null && !userGenres.isEmpty()) {
                                    fetchMoviesByGenres(userGenres, currentPage, false);
                                } else {
                                    Log.d(TAG, "No genres found for user. Fetching default movies.");
                                    fetchPopularMovies(currentPage, false);
                                }
                            } else {
                                Log.d(TAG, "No such document");
                                fetchPopularMovies(currentPage, false);
                            }
                        } else {
                            Log.d(TAG, "Get failed with ", task.getException());
                            fetchPopularMovies(currentPage, false);
                        }
                    });
        } else {
            Log.d(TAG, "User not logged in");
            handleUserNotLoggedIn();
        }
    }

    private void fetchMoviesByGenres(List<String> genres, int page, boolean isLoadingMore) {
        if (isLoading) return;
        isLoading = true;

        if (genres != null && !genres.isEmpty()) {
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            Call<MovieResponse> call = apiService.getMoviesByGenres(API_KEY, String.join(",", genres), page);

            call.enqueue(new Callback<MovieResponse>() {
                @Override
                public void onResponse(Call<MovieResponse> call, Response<MovieResponse> response) {
                    isLoading = false;
                    if (response.isSuccessful() && response.body() != null) {
                        List<Movie> movies = response.body().getMovies();
                        if (isLoadingMore) {
                            adapter.addMovies(movies);
                        } else {
                            adapter.setMovies(movies);
                        }
                        Log.d(TAG, "Fetched " + movies.size() + " movies by genres for page " + page);
                    } else {
                        Log.e(TAG, "Response unsuccessful: " + response.message());
                        if (!isLoadingMore) {
                            fetchPopularMovies(page, false);
                        }
                    }
                }

                @Override
                public void onFailure(Call<MovieResponse> call, Throwable t) {
                    isLoading = false;
                    Log.e(TAG, "API call failed: " + t.getMessage());
                    if (!isLoadingMore) {
                        fetchPopularMovies(page, false);
                    }
                }
            });
        }
    }

    private void fetchPopularMovies(int page, boolean isLoadingMore) {
        if (isLoading) return;
        isLoading = true;

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<MovieResponse> call = apiService.getPopularMovies(API_KEY, "en-US", page);

        call.enqueue(new Callback<MovieResponse>() {
            @Override
            public void onResponse(Call<MovieResponse> call, Response<MovieResponse> response) {
                isLoading = false;
                if (response.isSuccessful() && response.body() != null) {
                    List<Movie> movies = response.body().getMovies();
                    if (isLoadingMore) {
                        adapter.addMovies(movies);
                    } else {
                        adapter.setMovies(movies);
                    }
                    Log.d(TAG, "Fetched " + movies.size() + " popular movies for page " + page);
                } else {
                    Log.e(TAG, "Response unsuccessful: " + response.message());
                    showError("Failed to load movies");
                }
            }

            @Override
            public void onFailure(Call<MovieResponse> call, Throwable t) {
                isLoading = false;
                Log.e(TAG, "API call failed: " + t.getMessage());
                showError("Network error: " + t.getMessage());
            }
        });
    }

    private void loadMoreMovies() {
        currentPage++;
        if (userGenres != null && !userGenres.isEmpty()) {
            fetchMoviesByGenres(userGenres, currentPage, true);
        } else {
            fetchPopularMovies(currentPage, true);
        }
    }

    private void handleUserNotLoggedIn() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    //CardStackListener implementation
    @Override
    public void onCardDragging(Direction direction, float ratio) {
        Log.d(TAG, "onCardDragging: d=" + direction.name() + " ratio=" + ratio);
    }

    @Override
    public void onCardSwiped(Direction direction) {
        Log.d(TAG, "onCardSwiped: p=" + layoutManager.getTopPosition() + " d=" + direction);

        if (direction == Direction.Right) {
            int currentMovieIndex = layoutManager.getTopPosition() - 1; // Get the current movie index
            if (currentMovieIndex >= 0 && currentMovieIndex < adapter.getItemCount()) {
                Movie likedMovie = adapter.getMovieAt(currentMovieIndex);
                likedMovies.add(likedMovie); // Add the liked movie to the list
                saveLikedMovie(likedMovie); // Call a method to save the liked movie
                Toast.makeText(this, "Liked!", Toast.LENGTH_SHORT).show();
            }
        } else if (direction == Direction.Left) {
            Toast.makeText(this, "Passed", Toast.LENGTH_SHORT).show();
        }

        //Load more movies when reaching the end
        if (layoutManager.getTopPosition() >= adapter.getItemCount() - 5) {
            loadMoreMovies();
        }
    }

    private void saveLikedMovie(Movie movie) {
        //Save the liked movie to Firestore
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId)
                .update("likedMovies", FieldValue.arrayUnion(movie.getId())) // Store movie ID
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Liked movie added successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Error adding liked movie", e));
    }

    @Override
    public void onCardRewound() {
        Log.d(TAG, "onCardRewound: " + layoutManager.getTopPosition());
    }

    @Override
    public void onCardCanceled() {
        Log.d(TAG, "onCardCanceled: " + layoutManager.getTopPosition());
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