package com.example.cineswipe;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper {
    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_URL = "https://madminiproject-7f4c9-default-rtdb.asia-southeast1.firebasedatabase.app";
    private final DatabaseReference databaseReference;
    private static DatabaseHelper instance;

    public DatabaseHelper() {
        // Initialize Firebase Database with specific URL for Asia region
        FirebaseDatabase database = FirebaseDatabase.getInstance(DATABASE_URL);
        try {
            database.setPersistenceEnabled(true);
        } catch (Exception e) {
            Log.w(TAG, "Persistence has already been enabled", e);
        }
        databaseReference = database.getReference();
        Log.d(TAG, "DatabaseHelper initialized with reference: " + databaseReference.toString());
    }

    // Singleton pattern to ensure we don't enable persistence multiple times
    public static synchronized DatabaseHelper getInstance() {
        if (instance == null) {
            instance = new DatabaseHelper();
        }
        return instance;
    }

    public void saveUserPreferences(UserPreferences preferences, OnDataSaveListener listener) {
        if (preferences.getUserId() == null || preferences.getUserId().isEmpty()) {
            Log.e(TAG, "User ID is null or empty");
            listener.onFailure(new Exception("User ID cannot be null or empty"));
            return;
        }

        Log.d(TAG, "Attempting to save preferences for user: " + preferences.getUserId());

        DatabaseReference userRef = databaseReference.child("users")
                .child(preferences.getUserId());

        userRef.setValue(preferences)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully saved user preferences");
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save user preferences", e);
                    listener.onFailure(e);
                });
    }

    public void saveGenres(String userId, List<Integer> genres, OnDataSaveListener listener) {
        if (userId == null || userId.isEmpty()) {
            listener.onFailure(new Exception("User ID cannot be null or empty"));
            return;
        }

        // Allow empty genre list - user might want to reset their preferences
        if (genres == null) {
            genres = new ArrayList<>();
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

    public void getUserGenres(String userId, OnGenresLoadListener listener) {
        if (userId == null || userId.isEmpty()) {
            listener.onError(new Exception("User ID cannot be null or empty"));
            return;
        }

        DatabaseReference userRef = databaseReference.child("users").child(userId).child("favoriteGenres");

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Integer> genres = new ArrayList<>();

                if (dataSnapshot.exists()) {
                    for (DataSnapshot genreSnapshot : dataSnapshot.getChildren()) {
                        Integer genre = genreSnapshot.getValue(Integer.class);
                        if (genre != null) {
                            genres.add(genre);
                        }
                    }
                    Log.d(TAG, "Loaded genres for user " + userId + ": " + genres);
                } else {
                    Log.d(TAG, "No genres found for user " + userId);
                }

                listener.onGenresLoaded(genres);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error loading user genres", databaseError.toException());
                listener.onError(databaseError.toException());
            }
        });
    }

    public void deleteUserGenres(String userId, OnDataSaveListener listener) {
        if (userId == null || userId.isEmpty()) {
            listener.onFailure(new Exception("User ID cannot be null or empty"));
            return;
        }

        DatabaseReference userRef = databaseReference.child("users").child(userId).child("favoriteGenres");
        userRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully deleted genres for user: " + userId);
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete genres for user: " + userId, e);
                    listener.onFailure(e);
                });
    }

    public interface OnDataSaveListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OnGenresLoadListener {
        void onGenresLoaded(List<Integer> genres);
        void onError(Exception e);
    }

    // Helper method to keep database connection alive
    public void keepConnectionAlive() {
        DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                if (connected != null && connected) {
                    Log.d(TAG, "Connected to Firebase Database");
                } else {
                    Log.d(TAG, "Disconnected from Firebase Database");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Listener was cancelled", error.toException());
            }
        });
    }
}