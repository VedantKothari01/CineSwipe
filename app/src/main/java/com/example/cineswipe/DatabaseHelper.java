package com.example.cineswipe;

import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
public class DatabaseHelper {
    private static final String TAG = "DatabaseHelper";
    private DatabaseReference databaseReference;

    public DatabaseHelper() {
        // Initialize the database reference in constructor
        databaseReference = FirebaseDatabase.getInstance()
                .getReferenceFromUrl("https://madminiproject-7f4c9-default-rtdb.asia-southeast1.firebasedatabase.app");
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

    public interface OnDataSaveListener {
        void onSuccess();
        void onFailure(Exception e);
    }
}