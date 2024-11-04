package com.example.cineswipe;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @GET("movie/popular")
    Call<MovieResponse> getPopularMovies(
            @Query("api_key") String apiKey,
            @Query("language") String language,
            @Query("page") int page
    );

    @GET("movie/upcoming")
    Call<MovieResponse> getUpcomingMovies(
            @Query("api_key") String apiKey,
            @Query("language") String language,
            @Query("page") int page
    );

    @GET("movie/top_rated")
    Call<MovieResponse> getTopRatedMovies(
            @Query("api_key") String apiKey,
            @Query("language") String language,
            @Query("page") int page
    );

    @GET("trending/movie/day")
    Call<MovieResponse> getTrendingMovies(
            @Query("api_key") String apiKey,
            @Query("language") String language,
            @Query("page") int page
    );

    @GET("genre/movie/list")
    Call<GenreResponse> getGenres(
            @Query("api_key") String apiKey,
            @Query("language") String language
    );

    @GET("discover/movie")
    Call<MovieResponse> getMoviesByGenres(
            @Query("api_key") String apiKey,
            @Query("with_genres") String genres,
            @Query("page") int page
    );

    @GET("movie/{movie_id}")
    Call<Movie> getMovieDetails(
            @Path("movie_id") String movieId,
            @Query("api_key") String apiKey,
            @Query("append_to_response") String appendToResponse
    );

    // Add new endpoint for movie credits (cast)
    @GET("movie/{movie_id}/credits")
    Call<CreditsResponse> getMovieCredits(
            @Path("movie_id") String movieId,
            @Query("api_key") String apiKey
    );

    @GET("discover/movie")
    Call<MovieResponse> getRandomMovies(
            @Query("api_key") String apiKey,
            @Query("language") String language,
            @Query("page") int page,
            @Query("sort_by") String sortBy
    );

}
