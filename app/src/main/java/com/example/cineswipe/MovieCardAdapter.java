package com.example.cineswipe;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

public class MovieCardAdapter extends RecyclerView.Adapter<MovieCardAdapter.MovieViewHolder> {
    private Context context;
    private List<Movie> movieList;
    private static final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500";

    public MovieCardAdapter(Context context, List<Movie> movieList) {
        this.context = context;
        this.movieList = movieList != null ? movieList : new ArrayList<>();
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_movie_card, parent, false);
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.height = (int) (parent.getHeight() * 0.9);
        view.setLayoutParams(layoutParams);
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        Movie movie = movieList.get(position);

        holder.movieTitle.setText(movie.getTitle());

        if (holder.movieDescription != null && movie.getOverview() != null) {
            holder.movieDescription.setText(movie.getOverview());
        }

        RequestOptions requestOptions = new RequestOptions()
                .transforms(new CenterCrop(), new RoundedCorners(16));
        Glide.with(context)
                .load(IMAGE_BASE_URL + movie.getPosterPath())
                .apply(requestOptions)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.error_movie)
                .into(holder.moviePoster);
    }

    @Override
    public int getItemCount() {
        return movieList.size();
    }

    public void setMovies(List<Movie> movies) {
        this.movieList.clear();
        this.movieList.addAll(movies);
        notifyDataSetChanged();
    }

    public void addMovie(Movie movie) {
        movieList.add(movie);
        notifyItemInserted(movieList.size() - 1);
    }

    public void addMovies(List<Movie> newMovies) {
        int startIndex = movieList.size();
        movieList.addAll(newMovies);
        notifyItemRangeInserted(startIndex, newMovies.size());
    }

    public Movie getMovieAt(int currentMovieIndex) {
        if (currentMovieIndex >= 0 && currentMovieIndex < movieList.size()) {
            return movieList.get(currentMovieIndex);
        }
        return null;
    }

    static class MovieViewHolder extends RecyclerView.ViewHolder {
        ImageView moviePoster;
        TextView movieTitle;
        TextView movieDescription;

        MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            moviePoster = itemView.findViewById(R.id.moviePoster);
            movieTitle = itemView.findViewById(R.id.movieTitle);
            movieDescription = itemView.findViewById(R.id.movieDescription);
        }
    }
}
