package com.example.cineswipe;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DatabaseHelper {
    private DatabaseReference databaseReference;

    public DatabaseHelper() {
        databaseReference = FirebaseDatabase.getInstance().getReference("userPreferences");
    }

    public void saveUserPreferences(UserPreferences preferences, OnDataSaveListener listener) {
        databaseReference.child(preferences.getUserId()).setValue(preferences)
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
