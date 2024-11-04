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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GenreSelectionActivity extends AppCompatActivity {
    private static final String TAG = "GenreSelection";
    private DatabaseHelper databaseHelper;
    private Set<Integer> selectedGenreIds;
    private LinearLayout genreLayout;
    private ApiService apiService;
    private boolean isSaving = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_genre_selection);

        // Initialize components
        initializeComponents();

        Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> {
            if (!isSaving) {
                saveGenres();
            }
        });

        fetchUserGenres();
    }

    private void initializeComponents() {
        databaseHelper = new DatabaseHelper();
        selectedGenreIds = new HashSet<>(); // Using HashSet for unique values
        genreLayout = findViewById(R.id.genreLayout);
        apiService = ApiClient.getRetrofitInstance().create(ApiService.class);

        // Fetch genres from API
        fetchGenresFromApi();
    }

    private String getCurrentUserId() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            // Handle the case where user is not logged in
            Toast.makeText(this, "Please log in to continue", Toast.LENGTH_LONG).show();
            // Redirect to login activity
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return null;
        }
        return auth.getCurrentUser().getUid();
    }

    private void fetchUserGenres() {
        String userId = getCurrentUserId();
        if (userId == null) return;

        databaseHelper.getUserGenres(userId, new DatabaseHelper.OnGenresLoadListener() {
            @Override
            public void onGenresLoaded(List<Integer> genres) {
                selectedGenreIds.addAll(genres);
                updateCheckboxes();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading user genres", e);
            }
        });
    }

    private void updateCheckboxes() {
        for (int i = 0; i < genreLayout.getChildCount(); i++) {
            if (genreLayout.getChildAt(i) instanceof CheckBox) {
                CheckBox checkBox = (CheckBox) genreLayout.getChildAt(i);
                Object tag = checkBox.getTag();
                if (tag != null && tag instanceof Integer) {
                    checkBox.setChecked(selectedGenreIds.contains((Integer) tag));
                }
            }
        }
    }

    private void fetchGenresFromApi() {
        apiService.getGenres(Constants.API_KEY, "en-US").enqueue(new Callback<GenreResponse>() {
            @Override
            public void onResponse(Call<GenreResponse> call, Response<GenreResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displayGenres(response.body().getGenres());
                } else {
                    Toast.makeText(GenreSelectionActivity.this,
                            "Failed to load genres. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GenreResponse> call, Throwable t) {
                Toast.makeText(GenreSelectionActivity.this,
                        "Network error. Please check your connection.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error fetching genres", t);
            }
        });
    }

    private void displayGenres(List<GenreResponse.Genre> genres) {
        genreLayout.removeAllViews(); // Clear existing views
        for (GenreResponse.Genre genre : genres) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(genre.getName());
            checkBox.setTextSize(16);
            checkBox.setTextColor(getResources().getColor(R.color.white));
            checkBox.setTag(genre.getId()); // Store genre ID as tag

            // Set initial state based on previously selected genres
            checkBox.setChecked(selectedGenreIds.contains(genre.getId()));

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedGenreIds.add(genre.getId());
                } else {
                    selectedGenreIds.remove(genre.getId());
                }
                Log.d(TAG, "Selected genres: " + selectedGenreIds);
            });
            genreLayout.addView(checkBox);
        }
    }

    private void saveGenres() {
        String userId = getCurrentUserId();
        if (userId == null) return;

        isSaving = true; // Set saving flag

        // Convert Set to List for saving
        List<Integer> genresList = new ArrayList<>(selectedGenreIds);

        databaseHelper.saveGenres(userId, genresList, new DatabaseHelper.OnDataSaveListener() {
            @Override
            public void onSuccess() {
                isSaving = false; // Reset saving flag
                Toast.makeText(GenreSelectionActivity.this,
                        "Preferences updated successfully!", Toast.LENGTH_SHORT).show();

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
                isSaving = false; // Reset saving flag
                Toast.makeText(GenreSelectionActivity.this,
                        "Failed to save preferences. Please try again.",
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failed to save genres", e);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up any remaining callbacks or listeners if necessary
    }
}