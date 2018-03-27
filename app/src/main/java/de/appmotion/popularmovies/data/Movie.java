package de.appmotion.popularmovies.data;

import android.database.Cursor;
import de.appmotion.popularmovies.data.source.local.MovieContract;

/**
 * Immutable model class for a Movie.
 */
public final class Movie {

  private long mId;
  private long mMovieId;
  private String mTitle;
  private String mImageUrl;
  private double mPopularity;
  private double mVoteAverage;

  /**
   * Use this constructor to return a Movie from a Cursor
   *
   * @return {@link Movie}
   */
  public static Movie from(Cursor cursor) {
    final Movie movie = new Movie();
    movie.setId(cursor.getLong(cursor.getColumnIndexOrThrow(MovieContract.MovieEntry._ID)));
    movie.setMovieId(cursor.getLong(cursor.getColumnIndexOrThrow(MovieContract.MovieEntry.COLUMN_MOVIE_ID)));
    movie.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(MovieContract.MovieEntry.COLUMN_MOVIE_TITLE)));
    movie.setImageUrl(cursor.getString(cursor.getColumnIndexOrThrow(MovieContract.MovieEntry.COLUMN_MOVIE_IMAGE_URL)));
    movie.setPopularity(cursor.getDouble(cursor.getColumnIndexOrThrow(MovieContract.MovieEntry.COLUMN_MOVIE_POPULARITY)));
    movie.setVoteAverage(cursor.getDouble(cursor.getColumnIndexOrThrow(MovieContract.MovieEntry.COLUMN_MOVIE_VOTE_AVERAGE)));
    return movie;
  }

  public long getId() {
    return mId;
  }

  public void setId(long id) {
    mId = id;
  }

  public long getMovieId() {
    return mMovieId;
  }

  public void setMovieId(long movieId) {
    mMovieId = movieId;
  }

  public String getTitle() {
    return mTitle;
  }

  public void setTitle(String title) {
    mTitle = title;
  }

  public String getImageUrl() {
    return mImageUrl;
  }

  public void setImageUrl(String imageUrl) {
    mImageUrl = imageUrl;
  }

  public double getPopularity() {
    return mPopularity;
  }

  public void setPopularity(double popularity) {
    mPopularity = popularity;
  }

  public double getVoteAverage() {
    return mVoteAverage;
  }

  public void setVoteAverage(double voteAverage) {
    mVoteAverage = voteAverage;
  }
}
