package de.appmotion.popularmovies.data;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Defines table and column names for the popularmovies database. Furthermore the Content provider constants for accessing data in this
 * contract are defined
 */
public final class PopularMoviesContract {

  /*
   * Content provider constants
   * Define the possible paths for accessing data in this contract
   */
  // The authority, which is how your code knows which Content Provider to access
  public static final String AUTHORITY = "de.appmotion.popularmovies";

  // The base content URI = "content://" + <authority>
  public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);

  // This is the path for the "favorite_movies" directory
  public static final String PATH_FAVORITE_MOVIES = "favorite_movies";

  // To prevent someone from accidentally instantiating the contract class,
  // make the constructor private.
  private PopularMoviesContract() {
  }

  /* Inner class that defines the table contents */
  public static final class FavoriteMovieEntry implements BaseColumns {

    // FavoriteMovieEntry content URI = base content URI + path
    public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_FAVORITE_MOVIES).build();

    // Static final members for the table name and each of the db columns
    public static final String TABLE_NAME = "favorite_movies";
    public static final String COLUMN_MOVIE_ID = "movie_id";
    public static final String COLUMN_MOVIE_TITLE = "movie_title";
    public static final String COLUMN_MOVIE_IMAGE_URL = "movie_image_url";
    public static final String COLUMN_TIMESTAMP = "timestamp";
  }
}
