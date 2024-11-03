package com.example.cineswipe;

import java.util.List;

public class GenreResponse {
    private List<Genre> genres;

    public List<Genre> getGenres() {
        return genres;
    }

    public static class Genre {
        private int id;
        private String name;

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}
