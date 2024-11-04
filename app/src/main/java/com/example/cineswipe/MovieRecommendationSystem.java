package com.example.cineswipe;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.*;

public class MovieRecommendationSystem {
    private static final String TAG = "MovieRecommendationSystem";
    private static final int MINIMUM_CARDS_THRESHOLD = 5;
    private static final int BATCH_SIZE = 20;
    private static final float LIKE_WEIGHT_DELTA = 0.15f;
    private static final float DISLIKE_WEIGHT_DELTA = -0.07f;
    private static final float MIN_WEIGHT = 0.1f;
    private static final float MAX_WEIGHT = 2.0f;

    private final String userId;
    private final ApiService apiService;
    private final FirebaseFirestore db;
    private List<Movie> currentBatch;
    private Set<String> seenMovieIds;
    private Map<Integer, Float> genreWeights;
    private int currentPage = 1;
    private boolean isLoading = false;
    private OnMoviesBatchReadyListener batchReadyListener;

    public MovieRecommendationSystem(String userId, ApiService apiService) {
        this.userId = userId;
        this.apiService = apiService;
        this.db = FirebaseFirestore.getInstance();
        this.currentBatch = new ArrayList<>();
        this.seenMovieIds = new HashSet<>();
        this.genreWeights = new HashMap<>();
        initializeGenreWeights();
    }

    public void setOnBatchReadyListener(OnMoviesBatchReadyListener listener) {
        this.batchReadyListener = listener;
    }

    private void initializeGenreWeights() {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Initialize genre weights from user preferences
                        List<Long> initialGenres = (List<Long>) documentSnapshot.get("genres");
                        if (initialGenres != null) {
                            for (Long genreId : initialGenres) {
                                genreWeights.put(genreId.intValue(), 1.0f);
                            }
                        }

                        // Load previously saved weights
                        Map<String, Double> savedWeights =
                                (Map<String, Double>) documentSnapshot.get("genreWeights");
                        if (savedWeights != null) {
                            savedWeights.forEach((genreId, weight) ->
                                    genreWeights.put(Integer.parseInt(genreId), weight.floatValue()));
                        }

                        // Load seen movies
                        Map<String, Boolean> seenMovies =
                                (Map<String, Boolean>) documentSnapshot.get("seenMovies");
                        if (seenMovies != null) {
                            seenMovieIds.addAll(seenMovies.keySet());
                        }

                        // Initial fetch after weights are loaded
                        if (batchReadyListener != null) {
                            fetchNextBatch(batchReadyListener);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error initializing genre weights", e));
    }

    public void fetchNextBatch(OnMoviesBatchReadyListener listener) {
        if (isLoading) {
            Log.d(TAG, "Fetch in progress, skipping request");
            return;
        }

        if (currentBatch.size() > MINIMUM_CARDS_THRESHOLD) {
            Log.d(TAG, "Sufficient movies in batch, returning current batch");
            listener.onMoviesBatchReady(new ArrayList<>(currentBatch));
            return;
        }

        isLoading = true;
        Log.d(TAG, "Fetching new batch of movies, page: " + currentPage);

        // Get top weighted genres
        String weightedGenreIds = getTopWeightedGenres(3);

        apiService.getMoviesByGenres(Constants.API_KEY, weightedGenreIds, currentPage)
                .enqueue(new retrofit2.Callback<MovieResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<MovieResponse> call,
                                           retrofit2.Response<MovieResponse> response) {
                        isLoading = false;

                        if (response.isSuccessful() && response.body() != null) {
                            List<Movie> newMovies = response.body().getMovies();
                            processNewMovies(newMovies, listener);
                        } else {
                            String error = "Failed to fetch movies: " +
                                    (response.errorBody() != null ? response.errorBody().toString() : "Unknown error");
                            Log.e(TAG, error);
                            listener.onError(error);
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<MovieResponse> call, Throwable t) {
                        isLoading = false;
                        String error = "Network error: " + t.getMessage();
                        Log.e(TAG, error, t);
                        listener.onError(error);
                    }
                });
    }

    private String getTopWeightedGenres(int limit) {
        return genreWeights.entrySet().stream()
                .sorted(Map.Entry.<Integer, Float>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> String.valueOf(entry.getKey()))
                .reduce((a, b) -> a + "," + b)
                .orElse("");
    }

    private void processNewMovies(List<Movie> newMovies, OnMoviesBatchReadyListener listener) {
        List<Movie> unseenMovies = newMovies.stream()
                .filter(movie -> !seenMovieIds.contains(movie.getId()))
                .collect(java.util.stream.Collectors.toList());

        if (unseenMovies.isEmpty() && currentPage < 1000) {
            currentPage++;
            fetchNextBatch(listener);
            return;
        }

        currentBatch.addAll(unseenMovies);
        currentPage++;

        Log.d(TAG, "Added " + unseenMovies.size() + " new movies to batch");
        listener.onMoviesBatchReady(new ArrayList<>(currentBatch));
    }

    public void handleSwipe(Movie movie, boolean isLiked) {
        if (movie == null) {
            Log.e(TAG, "Attempted to handle swipe for null movie");
            return;
        }

        seenMovieIds.add(movie.getId());
        currentBatch.remove(movie);

        updateGenreWeights(movie, isLiked);
        savePreferencesToFirestore(movie, isLiked);

        if (currentBatch.size() <= MINIMUM_CARDS_THRESHOLD && batchReadyListener != null) {
            fetchNextBatch(batchReadyListener);
        }
    }

    private void updateGenreWeights(Movie movie, boolean isLiked) {
        Integer[] genreIds = movie.getGenreIds();
        if (genreIds == null || genreIds.length == 0) {
            Log.d(TAG, "No genre IDs for movie: " + movie.getId());
            return;
        }

        float weightDelta = isLiked ? LIKE_WEIGHT_DELTA : DISLIKE_WEIGHT_DELTA;

        float avgCurrentWeight = (float) Arrays.stream(genreIds)
                .filter(Objects::nonNull)
                .mapToDouble(genreId -> genreWeights.getOrDefault(genreId, 1.0f))
                .average()
                .orElse(1.0);

        // Update weights for each genre
        Arrays.stream(genreIds)
                .filter(Objects::nonNull)
                .forEach(genreId -> {
                    float currentWeight = genreWeights.getOrDefault(genreId, 1.0f);
                    float adjustedDelta = weightDelta * (1 + Math.abs(currentWeight - avgCurrentWeight));
                    float newWeight = Math.min(MAX_WEIGHT,
                            Math.max(MIN_WEIGHT, currentWeight + adjustedDelta));
                    genreWeights.put(genreId, newWeight);
                });
    }

    private void savePreferencesToFirestore(Movie movie, boolean isLiked) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("genreWeights", genreWeights);
        updates.put("seenMovies/" + movie.getId(), true);

        if (isLiked) {
            updates.put("likedMovies/" + movie.getId(), true);
            updates.put("likedMoviesTimestamp/" + movie.getId(), System.currentTimeMillis());
        }

        db.collection("users").document(userId)
                .update(updates)
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error updating user preferences", e));
    }

    public interface OnMoviesBatchReadyListener {
        void onMoviesBatchReady(List<Movie> movies);
        void onError(String message);
    }
}