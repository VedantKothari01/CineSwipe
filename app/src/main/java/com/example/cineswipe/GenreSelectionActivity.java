package com.example.cineswipe;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GenreSelectionActivity extends AppCompatActivity {

    private DatabaseReference databaseReference;
    private List<Integer> selectedGenreIds;
    private LinearLayout genreLayout;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_genre_selection);

        // Initialize Firebase and list for storing selected genre IDs
        databaseReference = FirebaseDatabase.getInstance().getReference();
        selectedGenreIds = new ArrayList<>();
        genreLayout = findViewById(R.id.genreLayout);

        // Initialize the API service
        apiService = ApiClient.getRetrofitInstance().create(ApiService.class);

        // Fetch genres from the TMDB API
        fetchGenresFromApi();

        Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> {
            Log.d("GenreSelection", "Save button clicked");
            saveGenres();
        });
    }

    private String getCurrentUserId() {
        return Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
    }

    private void fetchGenresFromApi() {
        apiService.getGenres(Constants.API_KEY, "en-US").enqueue(new Callback<GenreResponse>() {
            @Override
            public void onResponse(Call<GenreResponse> call, Response<GenreResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displayGenres(response.body().getGenres());
                } else {
                    Toast.makeText(GenreSelectionActivity.this, "Failed to load genres.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GenreResponse> call, Throwable t) {
                Toast.makeText(GenreSelectionActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("GenreSelection", "Error fetching genres", t);
            }
        });
    }

    private void displayGenres(List<GenreResponse.Genre> genres) {
        for (GenreResponse.Genre genre : genres) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(genre.getName());
            checkBox.setTextSize(16);
            checkBox.setTextColor(getResources().getColor(R.color.white));

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedGenreIds.add(genre.getId());
                } else {
                    selectedGenreIds.remove((Integer) genre.getId());
                }
            });

            genreLayout.addView(checkBox);
        }
    }

    private void saveGenres() {
        if (selectedGenreIds.isEmpty()) {
            Toast.makeText(this, "Please select at least one genre.", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("GenreSelection", "Selected genre IDs: " + selectedGenreIds);

        // Save selected genre IDs to Firebase
        databaseReference.child("preferredGenres").setValue(selectedGenreIds)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Genres saved successfully!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, HomeActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to save genres. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
