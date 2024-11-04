package com.example.cineswipe;

import android.util.Log;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.List;

public class DatabaseHelper {
    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_URL = "https://madminiproject-7f4c9-default-rtdb.asia-southeast1.firebasedatabase.app";
    private DatabaseReference databaseReference;

    public DatabaseHelper() {
        // Initialize Firebase Database with specific URL for Asia region
        FirebaseDatabase database = FirebaseDatabase.getInstance(DATABASE_URL);
        database.setPersistenceEnabled(true);
        databaseReference = database.getReference();
        Log.d(TAG, "DatabaseHelper initialized with reference: " + databaseReference.toString());
    }

    public void saveUserPreferences(UserPreferences preferences, OnDataSaveListener listener) {
        if (preferences.getUserId() == null) {
            Log.e(TAG, "User ID is null");
            listener.onFailure(new Exception("User ID cannot be null"));
            return;
        }

        if (preferences.getFavoriteGenres() == null) {
            Log.e(TAG, "Favorite genres list is null");
            listener.onFailure(new Exception("Favorite genres cannot be null"));
            return;
        }

        Log.d(TAG, "Attempting to save preferences for user: " + preferences.getUserId());
        Log.d(TAG, "Genres to save: " + preferences.getFavoriteGenres());

        DatabaseReference userRef = databaseReference.child("users")
                .child(preferences.getUserId());

        userRef.child("favoriteGenres")
                .setValue(preferences.getFavoriteGenres())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Successfully saved genres to Firebase");
                        listener.onSuccess();
                    } else {
                        Log.e(TAG, "Failed to save genres to Firebase", task.getException());
                        listener.onFailure(task.getException());
                    }
                });
    }

    public void saveGenres(String userId, List<Integer> genres, OnDataSaveListener listener) {
        if (userId == null || userId.isEmpty()) {
            listener.onFailure(new Exception("User ID cannot be null or empty"));
            return;
        }

        if (genres == null || genres.isEmpty()) {
            listener.onFailure(new Exception("Genres list cannot be null or empty"));
            return;
        }

        Log.d(TAG, "Saving genres for user: " + userId);
        Log.d(TAG, "Genres: " + genres);

        DatabaseReference userRef = databaseReference.child("users").child(userId);
        userRef.child("favoriteGenres")
                .setValue(genres)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully saved genres");
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save genres", e);
                    listener.onFailure(e);
                });
    }

    public interface OnDataSaveListener {
        void onSuccess();
        void onFailure(Exception e);
    }

}