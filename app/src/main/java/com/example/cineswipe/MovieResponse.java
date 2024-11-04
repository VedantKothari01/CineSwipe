package com.example.cineswipe;

import java.util.List;

public class MovieResponse {
    private List<Movie> results;

    public List<Movie> getMovies() {
        return results;
    }

    public void setResults(List<Movie> results) {
        this.results = results;
    }

    // Method to get a single movie by index
    public Movie getMovie(int index) {
        if (results != null && index >= 0 && index < results.size()) {
            return results.get(index);
        }
        return null; // Return null if index is invalid
    }

    // Optional: Method to get the first movie in the list
    public Movie getFirstMovie() {
        if (results != null && !results.isEmpty()) {
            return results.get(0);
        }
        return null; // Return null if results is empty
    }
}
