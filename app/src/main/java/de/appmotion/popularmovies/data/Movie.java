package de.appmotion.popularmovies.data;

import android.database.Cursor;
import de.appmotion.popularmovies.data.source.local.MovieContract;

/**
 * Model class for a Movie.
 */
public class Movie {

  private long mId;
  private long mMovieId;
  private String mTitle;
  private String mImageUrl;

  /**
   * Use this constructor to return a Movie from a Cursor
   *
   * @return {@link Movie}
   */
  public static Movie from(Cursor cursor) {
    final Movie movie = new Movie();
    movie.setId(cursor.getLong(cursor.getColumnIndexOrThrow(MovieContract.FavoriteMovieEntry._ID)));
    movie.setMovieId(cursor.getLong(cursor.getColumnIndexOrThrow(MovieContract.FavoriteMovieEntry.COLUMN_MOVIE_ID)));
    movie.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(MovieContract.FavoriteMovieEntry.COLUMN_MOVIE_TITLE)));
    movie.setImageUrl(cursor.getString(cursor.getColumnIndexOrThrow(MovieContract.FavoriteMovieEntry.COLUMN_MOVIE_IMAGE_URL)));
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
}
