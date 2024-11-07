package com.example.cineswipe;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;

public class PreferencesHelper {
    private static final String PREF_NAME = "CineSwipePrefs";
    private static final String KEY_MOVIES = "cached_movies";
    private static final String KEY_MOVIE_DETAILS = "cached_movie_details";
    private static final String KEY_LAST_UPDATE = "last_update_time";
    private final SharedPreferences preferences;
    private final Gson gson;

    public PreferencesHelper(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void saveMovies(List<Movie> movies, String category) {
        String moviesJson = gson.toJson(movies);
        preferences.edit()
                .putString(KEY_MOVIES + "_" + category, moviesJson)
                .putLong(KEY_LAST_UPDATE + "_" + category, System.currentTimeMillis())
                .apply();
    }

    public List<Movie> getCachedMovies(String category) {
        String moviesJson = preferences.getString(KEY_MOVIES + "_" + category, null);
        if (moviesJson != null) {
            Type type = new TypeToken<List<Movie>>(){}.getType();
            return gson.fromJson(moviesJson, type);
        }
        return null;
    }

    public void saveMovieDetails(Movie movie) {
        String movieJson = gson.toJson(movie);
        preferences.edit()
                .putString(KEY_MOVIE_DETAILS + "_" + movie.getId(), movieJson)
                .putLong(KEY_LAST_UPDATE + "_" + movie.getId(), System.currentTimeMillis())
                .apply();
    }

    public Movie getCachedMovieDetails(String movieId) {
        String movieJson = preferences.getString(KEY_MOVIE_DETAILS + "_" + movieId, null);
        if (movieJson != null) {
            return gson.fromJson(movieJson, Movie.class);
        }
        return null;
    }

    public boolean isCacheExpired(String category, long expirationTime) {
        long lastUpdateTime = preferences.getLong(KEY_LAST_UPDATE + "_" + category, 0);
        return System.currentTimeMillis() - lastUpdateTime <= expirationTime;
    }
}