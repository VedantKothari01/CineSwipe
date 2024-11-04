package com.example.cineswipe;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GenreSelectionActivity extends AppCompatActivity {
    private static final String TAG = "GenreSelection";
    private static final String DATABASE_URL = "https://madminiproject-7f4c9-default-rtdb.asia-southeast1.firebasedatabase.app";
    private DatabaseHelper databaseHelper;
    private List<Integer> selectedGenreIds;
    private LinearLayout genreLayout;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_genre_selection);

        // Initialize DatabaseHelper and list for storing selected genre IDs
        databaseHelper = new DatabaseHelper();
        selectedGenreIds = new ArrayList<>();
        genreLayout = findViewById(R.id.genreLayout);

        // Initialize the API service
        apiService = ApiClient.getRetrofitInstance().create(ApiService.class);

        // Fetch genres from the TMDB API
        fetchGenresFromApi();

        Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> {
            Log.d(TAG, "Save button clicked");
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
                Log.e(TAG, "Error fetching genres", t);
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
                    Log.d(TAG, "Genre added: " + genre.getId());
                } else {
                    selectedGenreIds.remove((Integer) genre.getId());
                    Log.d(TAG, "Genre removed: " + genre.getId());
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

        // Convert selected genre IDs to a comma-separated string for SharedPreferences
        StringBuilder selectedGenresStringBuilder = new StringBuilder();
        for (Integer genreId : selectedGenreIds) {
            selectedGenresStringBuilder.append(genreId).append(",");
        }
        if (selectedGenresStringBuilder.length() > 0) {
            selectedGenresStringBuilder.setLength(selectedGenresStringBuilder.length() - 1);
        }

        // Save selected genres in SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("CineSwipePrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("selectedGenres", selectedGenresStringBuilder.toString());
        editor.apply();

        // Save genres to Firebase
        String userId = getCurrentUserId();
        Log.d(TAG, "Saving genres for user: " + userId);
        databaseHelper.saveGenres(userId, selectedGenreIds, new DatabaseHelper.OnDataSaveListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(GenreSelectionActivity.this, "Genres saved successfully!", Toast.LENGTH_SHORT).show();

                // Pass selected genres to HomeActivity
                Intent intent = new Intent(GenreSelectionActivity.this, HomeActivity.class);
                ArrayList<String> genreIdStrings = new ArrayList<>();
                for (Integer genreId : selectedGenreIds) {
                    genreIdStrings.add(String.valueOf(genreId));
                }
                intent.putStringArrayListExtra("USER_GENRES", genreIdStrings);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(GenreSelectionActivity.this,
                        "Failed to save genres in Firebase: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failed to save genres in Firebase", e);
            }
        });
    }


}
