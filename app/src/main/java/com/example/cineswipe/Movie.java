package com.example.cineswipe;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class Movie implements Serializable {
    private String id;
    private String title;
    private String release_date;
    private String poster_path;
    private String backdrop_path;
    private String overview;
    private double vote_average;
    private List<Genre> genres;
    private int runtime;
    private List<CastMember> cast;

    public Movie() {
    }

    public Movie(String id, String title, String release_date, String poster_path,
                 String backdrop_path, String overview, double vote_average,
                 List<Genre> genres, int runtime, List<CastMember> cast) {
        this.id = id;
        this.title = title;
        this.release_date = release_date;
        this.poster_path = poster_path;
        this.backdrop_path = backdrop_path;
        this.overview = overview;
        this.vote_average = vote_average;
        this.genres = genres;
        this.runtime = runtime;
        this.cast = cast;
    }

    // Getters
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

    public String getGenres() {
        StringBuilder genreNames = new StringBuilder();
        if (genres != null) {
            for (Genre genre : genres) {
                genreNames.append(genre.getName()).append(", ");
            }
            if (genreNames.length() > 0) {
                genreNames.setLength(genreNames.length() - 2);
            }
        }
        return genreNames.toString();
    }

    public String getRuntime() {
        return (runtime > 0) ? runtime + " min" : "N/A";
    }

    public String getCast() {
        StringBuilder castNames = new StringBuilder();
        if (cast != null) {
            for (CastMember member : cast) {
                castNames.append(member.getName()).append(", ");
            }
            if (castNames.length() > 0) {
                castNames.setLength(castNames.length() - 2);
            }
        }
        return castNames.toString();
    }

    public String getPosterUrl() {
        return (poster_path != null) ? "https://image.tmdb.org/t/p/w500" + poster_path : null;
    }

    public String getBackdropUrl() {
        return (backdrop_path != null) ? "https://image.tmdb.org/t/p/w500" + backdrop_path : null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Movie movie = (Movie) obj;
        return Double.compare(movie.vote_average, vote_average) == 0 &&
                runtime == movie.runtime &&
                id.equals(movie.id) &&
                title.equals(movie.title) &&
                release_date.equals(movie.release_date) &&
                Objects.equals(poster_path, movie.poster_path) &&
                Objects.equals(backdrop_path, movie.backdrop_path) &&
                overview.equals(movie.overview) &&
                Objects.equals(genres, movie.genres) && // Include genres
                Objects.equals(cast, movie.cast); // Include cast
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, release_date, poster_path, backdrop_path,
                overview, vote_average, genres, runtime, cast); // Include genres, runtime, cast
    }
}

// Genre class for holding genre information
class Genre {
    private int id;
    private String name;

    public Genre(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

// CastMember class for holding cast member information
class CastMember {
    private String name;

    public CastMember(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
