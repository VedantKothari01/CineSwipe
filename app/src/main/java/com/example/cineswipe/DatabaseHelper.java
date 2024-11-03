package com.example.cineswipe;

import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DatabaseHelper {
    private DatabaseReference databaseReference;

    public DatabaseHelper() {
        databaseReference = FirebaseDatabase.getInstance()
                .getReferenceFromUrl("https://madminiproject-7f4c9-default-rtdb.asia-southeast1.firebasedatabase.app");
    }

    public void saveUserPreferences(UserPreferences preferences, OnDataSaveListener listener) {
        // Print the data to be saved for debugging
        Log.d("DatabaseHelper", "User ID: " + preferences.getUserId());
        Log.d("DatabaseHelper", "Favorite Genres: " + preferences.getFavoriteGenres());

        // Create a reference to the user's preferences
        DatabaseReference userRef = databaseReference.child(preferences.getUserId());

        // Save the favorite genres to the database
        userRef.child("favoriteGenres").setValue(preferences.getFavoriteGenres())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        listener.onSuccess();
                    } else {
                        listener.onFailure(task.getException());
                    }
                });
    }


    public interface OnDataSaveListener {
        void onSuccess();
        void onFailure(Exception e);
    }
}
