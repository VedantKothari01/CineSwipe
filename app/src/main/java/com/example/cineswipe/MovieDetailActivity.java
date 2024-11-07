package com.example.cineswipe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.List;
import java.util.stream.Collectors;
public class MovieDetailActivity extends AppCompatActivity {
    private static final String API_KEY = Constants.API_KEY;
    private static final long CACHE_EXPIRATION = 24 * 60 * 60 * 1000; // 24 hours in milliseconds
    private ApiService apiService;
    private ProgressBar progressBar;
    private View contentLayout;
    private Movie currentMovie;
    private PreferencesHelper preferencesHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        progressBar = findViewById(R.id.progressBar);
        contentLayout = findViewById(R.id.contentLayout);
        preferencesHelper = new PreferencesHelper(this);

        Intent intent = getIntent();
        currentMovie = (Movie) intent.getSerializableExtra("MOVIE_DATA");

        if (currentMovie != null) {
            progressBar.setVisibility(View.VISIBLE);
            contentLayout.setVisibility(View.GONE);

            apiService = ApiClient.getClient().create(ApiService.class);

            // Check if movie details are cached
            Movie cachedMovie = preferencesHelper.getCachedMovieDetails(currentMovie.getId());
            if (cachedMovie != null && preferencesHelper.isCacheExpired(currentMovie.getId(), CACHE_EXPIRATION)) {
                currentMovie = cachedMovie;
                updateUI(currentMovie);
                progressBar.setVisibility(View.GONE);
                contentLayout.setVisibility(View.VISIBLE);
            } else {
                // Fetch complete movie details
                fetchMovieDetails(currentMovie.getId());
                // Fetch cast separately
                fetchMovieCredits(currentMovie.getId());
            }
        }
    }

    private void fetchMovieDetails(String movieId) {
        Call<Movie> detailsCall = apiService.getMovieDetails(
                movieId,
                API_KEY,
                "genres"
        );

        detailsCall.enqueue(new Callback<Movie>() {
            @Override
            public void onResponse(Call<Movie> call, Response<Movie> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentMovie = response.body();
                    // Cache the movie details
                    preferencesHelper.saveMovieDetails(currentMovie);
                    updateUI(currentMovie);
                }
                progressBar.setVisibility(View.GONE);
                contentLayout.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(Call<Movie> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                contentLayout.setVisibility(View.VISIBLE);
            }
        });
    }

    private void fetchMovieCredits(String movieId) {
        Call<CreditsResponse> creditsCall = apiService.getMovieCredits(movieId, API_KEY);

        creditsCall.enqueue(new Callback<CreditsResponse>() {
            @Override
            public void onResponse(Call<CreditsResponse> call, Response<CreditsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateCastInfo(response.body().getCast());
                }
            }

            @Override
            public void onFailure(Call<CreditsResponse> call, Throwable t) {
                // Handle error
                TextView castView = findViewById(R.id.castTextView);
                castView.setText("Unable to load cast information");
            }
        });
    }

    private void updateCastInfo(List<CreditsResponse.Cast> cast) {
        TextView castView = findViewById(R.id.castTextView);
        if (cast != null && !cast.isEmpty()) {
            // Get first 5 cast members
            String castText = cast.stream()
                    .limit(5)
                    .map(castMember -> castMember.getName() + " as " + castMember.getCharacter())
                    .collect(Collectors.joining("\n"));
            castView.setText(castText);
        } else {
            castView.setText("Cast information not available");
        }
    }

    private void updateUI(Movie movie) {
        ImageView backdropImageView = findViewById(R.id.backdropImageView);
        if (movie.getBackdropUrl() != null) {
            Glide.with(this)
                    .load(movie.getBackdropUrl())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(backdropImageView);
        }

        TextView titleView = findViewById(R.id.movieTitleTextView);
        TextView ratingView = findViewById(R.id.ratingTextView);
        TextView releaseDateView = findViewById(R.id.releaseDateTextView);
        TextView genresView = findViewById(R.id.genresTextView);
        TextView runtimeView = findViewById(R.id.runtimeTextView);
        TextView overviewView = findViewById(R.id.overviewTextView);

        titleView.setText(movie.getTitle());
        ratingView.setText(String.format("★ %.1f/10", movie.getVoteAverage()));
        releaseDateView.setText(movie.getReleaseDate());
        genresView.setText(movie.getGenres());
        runtimeView.setText(movie.getRuntime());
        overviewView.setText(movie.getOverview());
    }
}