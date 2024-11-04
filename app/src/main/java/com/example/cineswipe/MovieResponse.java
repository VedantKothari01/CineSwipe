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
}
