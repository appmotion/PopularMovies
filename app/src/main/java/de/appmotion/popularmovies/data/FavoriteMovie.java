package de.appmotion.popularmovies.data;

import android.database.Cursor;
import de.appmotion.popularmovies.data.source.local.MovieContract;

/**
 * Immutable model class for a favorite Movie.
 */
public final class FavoriteMovie extends Movie {

  /**
   * Use this constructor to return a FavoriteMovie from a Cursor
   *
   * @return {@link FavoriteMovie}
   */
  public static FavoriteMovie from(Cursor cursor) {
    final FavoriteMovie favoriteMovie = new FavoriteMovie();
    favoriteMovie.setId(cursor.getLong(cursor.getColumnIndexOrThrow(MovieContract.FavoriteMovieEntry._ID)));
    favoriteMovie.setMovieId(cursor.getLong(cursor.getColumnIndexOrThrow(MovieContract.FavoriteMovieEntry.COLUMN_MOVIE_ID)));
    favoriteMovie.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(MovieContract.FavoriteMovieEntry.COLUMN_MOVIE_TITLE)));
    favoriteMovie.setImageUrl(cursor.getString(cursor.getColumnIndexOrThrow(MovieContract.FavoriteMovieEntry.COLUMN_MOVIE_IMAGE_URL)));
    return favoriteMovie;
  }
}
