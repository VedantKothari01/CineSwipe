package com.example.cineswipe;

import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.*;

public class MovieRecommendationSystem {
    private static final int MINIMUM_CARDS_THRESHOLD = 5;
    private static final int BATCH_SIZE = 20;
    private static final float LIKE_WEIGHT_DELTA = 0.15f;
    private static final float DISLIKE_WEIGHT_DELTA = -0.07f;
    private static final float MIN_WEIGHT = 0.1f;
    private static final float MAX_WEIGHT = 2.0f;

    private final String userId;
    private final DatabaseReference userPrefsRef;
    private final ApiService apiService;
    private List<Movie> currentBatch;
    private Set<String> seenMovieIds;  // Changed from Long to String
    private Map<Integer, Float> genreWeights;
    private int currentPage = 1;
    private boolean isLoading = false;

    public MovieRecommendationSystem(String userId, ApiService apiService) {
        this.userId = userId;
        this.apiService = apiService;
        this.currentBatch = new ArrayList<>();
        this.seenMovieIds = new HashSet<>();  // Now stores String IDs
        this.genreWeights = new HashMap<>();
        this.userPrefsRef = FirebaseDatabase.getInstance().getReference("user_preferences").child(userId);
        initializeGenreWeights();
    }

    private void initializeGenreWeights() {
        userPrefsRef.child("genres").get().addOnSuccessListener(dataSnapshot -> {
            if (dataSnapshot.exists()) {
                List<Integer> initialGenres = (List<Integer>) dataSnapshot.getValue();
                for (Integer genreId : initialGenres) {
                    genreWeights.put(genreId, 1.0f);
                }
            }
        });

        // Also load previously adjusted weights if they exist
        userPrefsRef.child("genreWeights").get().addOnSuccessListener(dataSnapshot -> {
            if (dataSnapshot.exists()) {
                Map<String, Float> savedWeights = (Map<String, Float>) dataSnapshot.getValue();
                if (savedWeights != null) {
                    savedWeights.forEach((genreId, weight) ->
                            genreWeights.put(Integer.parseInt(genreId), weight));
                }
            }
        });
    }

    public void fetchNextBatch(OnMoviesBatchReadyListener listener) {
        if (isLoading) {
            return; // Prevent multiple simultaneous requests
        }

        if (currentBatch.size() > MINIMUM_CARDS_THRESHOLD) {
            listener.onMoviesBatchReady(currentBatch);
            return;
        }

        isLoading = true;

        // Sort genres by weight to prioritize higher weighted genres
        String weightedGenreIds = genreWeights.entrySet().stream()
                .sorted(Map.Entry.<Integer, Float>comparingByValue().reversed())
                .limit(3) // Get top 3 genres
                .map(entry -> String.valueOf(entry.getKey()))
                .reduce((a, b) -> a + "," + b)
                .orElse("");

        apiService.getMoviesByGenres(Constants.API_KEY, weightedGenreIds, currentPage)
                .enqueue(new retrofit2.Callback<MovieResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<MovieResponse> call,
                                           retrofit2.Response<MovieResponse> response) {
                        isLoading = false;
                        if (response.isSuccessful() && response.body() != null) {
                            List<Movie> newMovies = response.body().getMovies();

                            // Filter out already seen movies
                            List<Movie> unseenMovies = newMovies.stream()
                                    .filter(movie -> !seenMovieIds.contains(movie.getId()))
                                    .collect(java.util.stream.Collectors.toList());

                            if (unseenMovies.isEmpty() && currentPage < 1000) {
                                // If all movies in this page were seen, try next page
                                currentPage++;
                                fetchNextBatch(listener);
                                return;
                            }

                            currentBatch.addAll(unseenMovies);
                            currentPage++;
                            listener.onMoviesBatchReady(currentBatch);
                        } else {
                            listener.onError("Failed to fetch movies");
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<MovieResponse> call, Throwable t) {
                        isLoading = false;
                        listener.onError(t.getMessage());
                    }
                });
    }

    public void handleSwipe(Movie movie, boolean isLiked) {
        // Mark movie as seen (now using String ID)
        seenMovieIds.add(movie.getId());

        // Remove the swiped movie from current batch
        currentBatch.remove(movie);

        // Update genre weights based on user preference
        if (movie.getGenreIds() != null) {
            float weightDelta = isLiked ? LIKE_WEIGHT_DELTA : DISLIKE_WEIGHT_DELTA;

            // Calculate average current weight of movie's genres
            float avgCurrentWeight = (float) movie.getGenreIds().stream()
                    .mapToDouble(genreId -> genreWeights.getOrDefault(genreId, 1.0f))
                    .average()
                    .orElse(1.0);

            // Apply weighted delta based on current weights
            for (Integer genreId : movie.getGenreIds()) {
                float currentWeight = genreWeights.getOrDefault(genreId, 1.0f);
                // Adjust weight more if current weight is different from average
                float adjustedDelta = weightDelta * (1 + Math.abs(currentWeight - avgCurrentWeight));
                float newWeight = Math.min(MAX_WEIGHT,
                        Math.max(MIN_WEIGHT, currentWeight + adjustedDelta));
                genreWeights.put(genreId, newWeight);
            }
        }

        // Save updated preferences to Firebase
        Map<String, Object> updates = new HashMap<>();
        updates.put("genreWeights", genreWeights);
        if (isLiked) {
            updates.put("likedMovies/" + movie.getId(), true);
            updates.put("likedMoviesTimestamp/" + movie.getId(), System.currentTimeMillis());
        }
        userPrefsRef.updateChildren(updates);

        // If batch is running low, fetch more
        if (currentBatch.size() <= MINIMUM_CARDS_THRESHOLD) {
            fetchNextBatch(new OnMoviesBatchReadyListener() {
                @Override
                public void onMoviesBatchReady(List<Movie> movies) {
                    // Batch replenished successfully
                }

                @Override
                public void onError(String message) {
                    // Log error but don't disrupt user experience
                    Log.e("MovieRecommendationSystem", "Error fetching next batch: " + message);
                }
            });
        }
    }

    public interface OnMoviesBatchReadyListener {
        void onMoviesBatchReady(List<Movie> movies);
        void onError(String message);
    }
}