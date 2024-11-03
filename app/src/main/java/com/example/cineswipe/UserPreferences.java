package com.example.cineswipe;

import java.util.List;

public class UserPreferences {
    private String userId;
    private List<Integer> favoriteGenres;

    public UserPreferences(String userId, List<Integer> favoriteGenres) {
        this.userId = userId;
        this.favoriteGenres = favoriteGenres;
    }

    public String getUserId() {
        return userId;
    }

    public List<Integer> getFavoriteGenres() {
        return favoriteGenres;
    }
}
