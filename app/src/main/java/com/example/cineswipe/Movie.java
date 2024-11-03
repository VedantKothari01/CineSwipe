package com.example.cineswipe;

import java.io.Serializable;
import java.util.Objects;

public class Movie implements Serializable {
    private String id; // Unique identifier for the movie
    private String title;
    private String release_date;
    private String poster_path;
    private String backdrop_path;
    private String overview;
    private double vote_average;

    public Movie(String id, String title, String release_date, String poster_path, String backdrop_path, String overview, double vote_average) {
        this.id = id; // Initialize id
        this.title = title;
        this.release_date = release_date;
        this.poster_path = poster_path;
        this.backdrop_path = backdrop_path;
        this.overview = overview;
        this.vote_average = vote_average;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getReleaseDate() {
        return release_date;
    }

    public String getPosterPath() {
        return poster_path;
    }

    public String getBackdropPath() {
        return backdrop_path;
    }

    public String getOverview() {
        return overview;
    }

    public double getVoteAverage() {
        return vote_average;
    }

    public String getPosterUrl() {
        return "https://image.tmdb.org/t/p/w500" + poster_path;
    }

    public String getBackdropUrl() {
        return "https://image.tmdb.org/t/p/w500" + backdrop_path;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Movie movie = (Movie) obj;
        return Double.compare(movie.vote_average, vote_average) == 0 &&
                id.equals(movie.id) && // Include id in equality check
                title.equals(movie.title) &&
                release_date.equals(movie.release_date) &&
                poster_path.equals(movie.poster_path) &&
                backdrop_path.equals(movie.backdrop_path) &&
                overview.equals(movie.overview);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, release_date, poster_path, backdrop_path, overview, vote_average); // Include id in hash code
    }
}
