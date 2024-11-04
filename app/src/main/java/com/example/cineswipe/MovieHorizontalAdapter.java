package com.example.cineswipe;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;


public class MovieHorizontalAdapter extends RecyclerView.Adapter<MovieHorizontalAdapter.MovieHorizontalViewHolder> {

    private final Context context;
    private final List<Movie> movieList;

    public MovieHorizontalAdapter(Context context, List<Movie> movieList) {
        this.context = context;
        this.movieList = movieList != null ? movieList : new ArrayList<>();
    }

    @NonNull
    @Override
    public MovieHorizontalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_movie_horizontal, parent, false);
        return new MovieHorizontalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieHorizontalViewHolder holder, int position) {
        Movie movie = movieList.get(position);
        holder.movieTitle.setText(movie.getTitle());

        // Load the poster image using Glide
        Glide.with(context)
                .load(movie.getPosterUrl())
                .error(R.drawable.ic_launcher_background)
                .into(holder.moviePoster);

        // Set click listener to navigate to the detail page
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MovieDetailActivity.class);
            intent.putExtra("MOVIE_DATA", movie);
            context.startActivity(intent);
        });

    }

    @Override
    public int getItemCount() {
        return movieList.size();
    }

    //Method to update the movie list using DiffUtil
    public void setMovies(List<Movie> newMovies) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new MovieDiffCallback(movieList, newMovies));
        movieList.clear();
        movieList.addAll(newMovies);
        diffResult.dispatchUpdatesTo(this);
    }

    public static class MovieHorizontalViewHolder extends RecyclerView.ViewHolder {
        ImageView moviePoster;
        TextView movieTitle;

        public MovieHorizontalViewHolder(@NonNull View itemView) {
            super(itemView);
            moviePoster = itemView.findViewById(R.id.moviePoster);
            movieTitle = itemView.findViewById(R.id.movieTitle);
        }
    }
}