package com.example.cineswipe;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GenreSelectionActivity extends AppCompatActivity {

    private DatabaseReference databaseReference;
    private List<String> selectedGenres;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_genre_selection);

        // Initialize Firebase database reference
        databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(getCurrentUserId());
        selectedGenres = new ArrayList<>();

        LinearLayout genreLayout = findViewById(R.id.genreLayout);
        String[] genres = {"Action", "Comedy", "Drama", "Horror", "Romance"};

        // Create CheckBoxes for each genre
        for (String genre : genres) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(genre);
            checkBox.setTextSize(16);
            checkBox.setTextColor(getResources().getColor(R.color.white));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedGenres.add(genre);
                } else {
                    selectedGenres.remove(genre);
                }
            });
            genreLayout.addView(checkBox);
        }

        saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> saveGenres());
    }

    private String getCurrentUserId() {
        // Get the current user ID from FirebaseAuth
        return Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
    }

    private void saveGenres() {
        // Save selected genres to the Firebase database
        databaseReference.child("preferredGenres").setValue(selectedGenres)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Show success message
                        Toast.makeText(this, "Genres saved successfully!", Toast.LENGTH_SHORT).show();
                        // Start HomeActivity only after genres are saved successfully
                        startActivity(new Intent(this, HomeActivity.class));
                        finish(); // Finish this activity so user can't return to it
                    } else {
                        // Show error message
                        Toast.makeText(this, "Failed to save genres. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
