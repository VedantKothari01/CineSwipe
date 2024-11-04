package com.example.cineswipe;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerViewPopularMovies, recyclerViewLikedMovies, recyclerViewTrendingMovies, recyclerViewTopRatedMovies, recyclerViewUpcomingMovies;
    private MovieHorizontalAdapter popularMovieHorizontalAdapter, trendingMovieHorizontalAdapter, topRatedMovieHorizontalAdapter, upcomingMovieHorizontalAdapter;
    private List<Movie> popularMovieList, trendingMovieList, topRatedMovieList, upcomingMovieList;
    private MovieHorizontalAdapter likedMovieHorizontalAdapter;
    private List<Movie> likedMovieList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        // Check if user is logged in
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        likedMovieList = new ArrayList<>();
        likedMovieHorizontalAdapter = new MovieHorizontalAdapter(this, likedMovieList);

        setContentView(R.layout.activity_main);

        // Initialize RecyclerViews
        recyclerViewLikedMovies = findViewById(R.id.recyclerViewLikedMovies);
        recyclerViewLikedMovies.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerViewLikedMovies.setAdapter(likedMovieHorizontalAdapter);

        recyclerViewPopularMovies = findViewById(R.id.recyclerViewPopularMovies);
        recyclerViewTrendingMovies = findViewById(R.id.recyclerViewTrendingMovies);
        recyclerViewTopRatedMovies = findViewById(R.id.recyclerViewTopRatedMovies);
        recyclerViewUpcomingMovies = findViewById(R.id.recyclerViewUpcomingMovies);

        // Initialize movie lists
        popularMovieList = new ArrayList<>();
        trendingMovieList = new ArrayList<>();
        topRatedMovieList = new ArrayList<>();
        upcomingMovieList = new ArrayList<>();

        // Initialize adapters
        popularMovieHorizontalAdapter = new MovieHorizontalAdapter(this, popularMovieList);
        trendingMovieHorizontalAdapter = new MovieHorizontalAdapter(this, trendingMovieList);
        topRatedMovieHorizontalAdapter = new MovieHorizontalAdapter(this, topRatedMovieList);
        upcomingMovieHorizontalAdapter = new MovieHorizontalAdapter(this, upcomingMovieList);

        // Set up adapters for RecyclerViews
        recyclerViewPopularMovies.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerViewPopularMovies.setAdapter(popularMovieHorizontalAdapter);
        recyclerViewTrendingMovies.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerViewTrendingMovies.setAdapter(trendingMovieHorizontalAdapter);
        recyclerViewTopRatedMovies.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerViewTopRatedMovies.setAdapter(topRatedMovieHorizontalAdapter);
        recyclerViewUpcomingMovies.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerViewUpcomingMovies.setAdapter(upcomingMovieHorizontalAdapter);

        // Fetch movies for each category
        fetchLikedMovies();
        fetchPopularMovies();
        fetchTrendingMovies();
        fetchTopRatedMovies();
        fetchUpcomingMovies();

        // Initialize Bottom Navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        // Set the item selected listener
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(MainActivity.this, HomeActivity.class));
                return true;
            } else if (itemId == R.id.nav_movies) {
                recyclerViewPopularMovies.smoothScrollToPosition(0);
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(MainActivity.this, GenreSelectionActivity.class));
                return true;
            }
            return false;
        });
    }

    private void fetchLikedMovies() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> likedMovieIds = (List<String>) documentSnapshot.get("likedMovies");
                        if (likedMovieIds != null) {
                            fetchMoviesByIds(likedMovieIds);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Error fetching liked movies", Toast.LENGTH_SHORT).show());
    }

    private void fetchMoviesByIds(List<String> likedMovieIds) {
        String apiKey = Constants.API_KEY; // Ensure you have your API key
        ApiService apiService = ApiClient.getRetrofitInstance().create(ApiService.class);

        for (String movieId : likedMovieIds) {
            Call<MovieResponse> call = apiService.getMovieById(movieId, apiKey); // Ensure this returns MovieResponse
            call.enqueue(new Callback<MovieResponse>() {
                @Override
                public void onResponse(@NonNull Call<MovieResponse> call, @NonNull Response<MovieResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        likedMovieList.add(response.body().getFirstMovie()); // Get the first movie from the response
                        likedMovieHorizontalAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<MovieResponse> call, @NonNull Throwable t) {
                    Toast.makeText(MainActivity.this, "Error fetching liked movie: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void fetchPopularMovies() {
        String apiKey = Constants.API_KEY;
        String language = "en-US";
        int page = 1;
        ApiService apiService = ApiClient.getRetrofitInstance().create(ApiService.class);
        Call<MovieResponse> call = apiService.getPopularMovies(apiKey, language, page);
        call.enqueue(new Callback<MovieResponse>() {
            @Override
            public void onResponse(@NonNull Call<MovieResponse> call, @NonNull Response<MovieResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    popularMovieList.clear();
                    popularMovieList.addAll(response.body().getMovies());
                    popularMovieHorizontalAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to fetch popular movies", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<MovieResponse> call, @NonNull Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchTrendingMovies() {
        String apiKey = Constants.API_KEY;
        String language = "en-US";
        int page = 1;
        ApiService apiService = ApiClient.getRetrofitInstance().create(ApiService.class);
        Call<MovieResponse> call = apiService.getTrendingMovies(apiKey, language, page);
        call.enqueue(new Callback<MovieResponse>() {
            @Override
            public void onResponse(@NonNull Call<MovieResponse> call, @NonNull Response<MovieResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    trendingMovieList.clear();
                    trendingMovieList.addAll(response.body().getMovies());
                    trendingMovieHorizontalAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to fetch trending movies", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<MovieResponse> call, @NonNull Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchTopRatedMovies() {
        String apiKey = Constants.API_KEY;
        String language = "en-US";
        int page = 1;
        ApiService apiService = ApiClient.getRetrofitInstance().create(ApiService.class);
        Call<MovieResponse> call = apiService.getTopRatedMovies(apiKey, language, page);
        call.enqueue(new Callback<MovieResponse>() {
            @Override
            public void onResponse(@NonNull Call<MovieResponse> call, @NonNull Response<MovieResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    topRatedMovieList.clear();
                    topRatedMovieList.addAll(response.body().getMovies());
                    topRatedMovieHorizontalAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to fetch top rated movies", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<MovieResponse> call, @NonNull Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchUpcomingMovies() {
        String apiKey = Constants.API_KEY;
        String language = "en-US";
        int page = 1;
        ApiService apiService = ApiClient.getRetrofitInstance().create(ApiService.class);
        Call<MovieResponse> call = apiService.getUpcomingMovies(apiKey, language, page);
        call.enqueue(new Callback<MovieResponse>() {
            @Override
            public void onResponse(@NonNull Call<MovieResponse> call, @NonNull Response<MovieResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    upcomingMovieList.clear();
                    upcomingMovieList.addAll(response.body().getMovies());
                    upcomingMovieHorizontalAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to fetch upcoming movies", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<MovieResponse> call, @NonNull Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
