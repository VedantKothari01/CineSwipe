package com.example.cineswipe;

import android.content.Context;
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

// Adapter class for displaying movie cards
public class MovieCardAdapter extends RecyclerView.Adapter<MovieViewHolder> {

    private final Context context;
    private List<Movie> movies;

    public MovieCardAdapter(Context context, List<Movie> movies) {
        this.context = context;
        this.movies = new ArrayList<>(movies);
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_movie_card, parent, false);
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        Movie movie = movies.get(position);
        holder.bind(movie);
    }

    @Override
    public int getItemCount() {
        return movies.size();
    }

    // Method to update the movie list
    public void setMovies(List<Movie> newMovies) {
        if (newMovies == null) {
            return;
        }

        // Use DiffUtil for better performance when updating the list
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new MoviesDiffCallback(movies, newMovies));
        movies.clear();
        movies.addAll(newMovies);
        diffResult.dispatchUpdatesTo(this);
    }
}

class MovieViewHolder extends RecyclerView.ViewHolder {
    private final ImageView posterImageView; // ImageView for movie poster
    private final TextView titleTextView; // TextView for movie title

    public MovieViewHolder(@NonNull View itemView) {
        super(itemView);
        // Initialize your views here
        posterImageView = itemView.findViewById(R.id.moviePoster);
        titleTextView = itemView.findViewById(R.id.movieTitle);
    }

    public void bind(Movie movie) {
        titleTextView.setText(movie.getTitle()); // Set the movie title

        // Load the poster image using a library like Glide or Picasso
        Glide.with(itemView.getContext())
                .load("https://image.tmdb.org/t/p/w500" + movie.getPosterPath()) // Construct the full image URL
                .into(posterImageView);
    }
}

// DiffUtil callback for optimizing the RecyclerView updates
class MoviesDiffCallback extends DiffUtil.Callback {

    private final List<Movie> oldList;
    private final List<Movie> newList;

    public MoviesDiffCallback(List<Movie> oldList, List<Movie> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).getId() == newList.get(newItemPosition).getId(); // Assuming Movie has a unique ID
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).equals(newList.get(newItemPosition)); // Assuming Movie class has a proper equals() method
    }
}
