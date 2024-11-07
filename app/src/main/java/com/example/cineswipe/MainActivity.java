package com.example.cineswipe;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerViewPopularMovies, recyclerViewTrendingMovies, recyclerViewTopRatedMovies, recyclerViewUpcomingMovies;
    private MovieHorizontalAdapter popularMovieHorizontalAdapter, trendingMovieHorizontalAdapter, topRatedMovieHorizontalAdapter, upcomingMovieHorizontalAdapter;
    private List<Movie> popularMovieList, trendingMovieList, topRatedMovieList, upcomingMovieList;
    private PreferencesHelper preferencesHelper;
    private static final long CACHE_EXPIRATION = 24 * 60 * 60 * 1000; // 24 hours in milliseconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseApp.initializeApp(this);


        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        preferencesHelper = new PreferencesHelper(this);

        setContentView(R.layout.activity_main);


        recyclerViewPopularMovies = findViewById(R.id.recyclerViewPopularMovies);
        recyclerViewTrendingMovies = findViewById(R.id.recyclerViewTrendingMovies);
        recyclerViewTopRatedMovies = findViewById(R.id.recyclerViewTopRatedMovies);
        recyclerViewUpcomingMovies = findViewById(R.id.recyclerViewUpcomingMovies);


        popularMovieList = new ArrayList<>();
        trendingMovieList = new ArrayList<>();
        topRatedMovieList = new ArrayList<>();
        upcomingMovieList = new ArrayList<>();


        popularMovieHorizontalAdapter = new MovieHorizontalAdapter(this, popularMovieList);
        trendingMovieHorizontalAdapter = new MovieHorizontalAdapter(this, trendingMovieList);
        topRatedMovieHorizontalAdapter = new MovieHorizontalAdapter(this, topRatedMovieList);
        upcomingMovieHorizontalAdapter = new MovieHorizontalAdapter(this, upcomingMovieList);


        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.movie_card_spacing);
        HorizontalSpaceItemDecoration decoration = new HorizontalSpaceItemDecoration(spacingInPixels);
        recyclerViewPopularMovies.addItemDecoration(decoration);
        recyclerViewTrendingMovies.addItemDecoration(decoration);
        recyclerViewTopRatedMovies.addItemDecoration(decoration);
        recyclerViewUpcomingMovies.addItemDecoration(decoration);


        recyclerViewPopularMovies.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerViewPopularMovies.setAdapter(popularMovieHorizontalAdapter);
        recyclerViewTrendingMovies.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerViewTrendingMovies.setAdapter(trendingMovieHorizontalAdapter);
        recyclerViewTopRatedMovies.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerViewTopRatedMovies.setAdapter(topRatedMovieHorizontalAdapter);
        recyclerViewUpcomingMovies.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerViewUpcomingMovies.setAdapter(upcomingMovieHorizontalAdapter);



        loadCachedData();


        if (isNetworkAvailable()) {
            fetchAllMovies();
        }


        fetchPopularMovies();
        fetchTrendingMovies();
        fetchTopRatedMovies();
        fetchUpcomingMovies();


        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

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

    private void loadCachedData() {

        List<Movie> cachedPopular = preferencesHelper.getCachedMovies("popular");
        List<Movie> cachedTrending = preferencesHelper.getCachedMovies("trending");
        List<Movie> cachedTopRated = preferencesHelper.getCachedMovies("toprated");
        List<Movie> cachedUpcoming = preferencesHelper.getCachedMovies("upcoming");

        if (cachedPopular != null && !cachedPopular.isEmpty() && preferencesHelper.isCacheExpired("popular", CACHE_EXPIRATION)) {
            popularMovieList.clear();
            popularMovieList.addAll(cachedPopular);
            popularMovieHorizontalAdapter.notifyDataSetChanged();
            Toast.makeText(this, "Showing cached popular movies", Toast.LENGTH_SHORT).show();
        }

        if (cachedTrending != null && !cachedTrending.isEmpty() && preferencesHelper.isCacheExpired("trending", CACHE_EXPIRATION)) {
            trendingMovieList.clear();
            trendingMovieList.addAll(cachedTrending);
            trendingMovieHorizontalAdapter.notifyDataSetChanged();
            Toast.makeText(this, "Showing cached trending movies", Toast.LENGTH_SHORT).show();
        }

        if (cachedTopRated != null && !cachedTopRated.isEmpty() && preferencesHelper.isCacheExpired("toprated", CACHE_EXPIRATION)) {
            topRatedMovieList.clear();
            topRatedMovieList.addAll(cachedTopRated);
            topRatedMovieHorizontalAdapter.notifyDataSetChanged();
            Toast.makeText(this, "Showing cached top-rated movies", Toast.LENGTH_SHORT).show();
        }

        if (cachedUpcoming != null && !cachedUpcoming.isEmpty() && preferencesHelper.isCacheExpired("upcoming", CACHE_EXPIRATION)) {
            upcomingMovieList.clear();
            upcomingMovieList.addAll(cachedUpcoming);
            upcomingMovieHorizontalAdapter.notifyDataSetChanged();
            Toast.makeText(this, "Showing cached upcoming movies", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchAllMovies() {
        fetchPopularMovies();
        fetchTrendingMovies();
        fetchTopRatedMovies();
        fetchUpcomingMovies();
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

                    // Cache the new data
                    preferencesHelper.saveMovies(popularMovieList, "popular");
                }
            }

            @Override
            public void onFailure(@NonNull Call<MovieResponse> call, @NonNull Throwable t) {
                if (!isNetworkAvailable()) {
                    // Load cached data if available
                    List<Movie> cachedMovies = preferencesHelper.getCachedMovies("popular");
                    if (cachedMovies != null && !cachedMovies.isEmpty()) {
                        popularMovieList.clear();
                        popularMovieList.addAll(cachedMovies);
                        popularMovieHorizontalAdapter.notifyDataSetChanged();
                        Toast.makeText(MainActivity.this, "Showing cached popular movies", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "No cached popular movies available", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
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


                    preferencesHelper.saveMovies(trendingMovieList, "trending");
                }
            }

            @Override
            public void onFailure(@NonNull Call<MovieResponse> call, @NonNull Throwable t) {
                if (!isNetworkAvailable()) {
                    // Load cached data if available
                    List<Movie> cachedMovies = preferencesHelper.getCachedMovies("trending");
                    if (cachedMovies != null && !cachedMovies.isEmpty()) {
                        trendingMovieList.clear();
                        trendingMovieList.addAll(cachedMovies);
                        trendingMovieHorizontalAdapter.notifyDataSetChanged();
                        Toast.makeText(MainActivity.this, "Showing cached trending movies", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "No cached trending movies available", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
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


                    preferencesHelper.saveMovies(topRatedMovieList, "toprated");
                }
            }

            @Override
            public void onFailure(@NonNull Call<MovieResponse> call, @NonNull Throwable t) {
                if (!isNetworkAvailable()) {

                    List<Movie> cachedMovies = preferencesHelper.getCachedMovies("toprated");
                    if (cachedMovies != null && !cachedMovies.isEmpty()) {
                        topRatedMovieList.clear();
                        topRatedMovieList.addAll(cachedMovies);
                        topRatedMovieHorizontalAdapter.notifyDataSetChanged();
                        Toast.makeText(MainActivity.this, "Showing cached top-rated movies", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "No cached top-rated movies available", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
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


                    preferencesHelper.saveMovies(upcomingMovieList, "upcoming");
                }
            }

            @Override
            public void onFailure(@NonNull Call<MovieResponse> call, @NonNull Throwable t) {
                if (!isNetworkAvailable()) {
                    List<Movie> cachedMovies = preferencesHelper.getCachedMovies("upcoming");
                    if (cachedMovies != null && !cachedMovies.isEmpty()) {
                        upcomingMovieList.clear();
                        upcomingMovieList.addAll(cachedMovies);
                        upcomingMovieHorizontalAdapter.notifyDataSetChanged();
                        Toast.makeText(MainActivity.this, "Showing cached upcoming movies", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "No cached upcoming movies available", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}