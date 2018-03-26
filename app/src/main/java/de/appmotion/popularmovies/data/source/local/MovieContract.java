package de.appmotion.popularmovies.data.source.local;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Defines table and column names for the popularmovies database. Furthermore the Content provider constants for accessing data in this
 * contract are defined.
 */
public final class MovieContract {

  /*
   * Content provider constants
   * Define the possible paths for accessing data in this contract
   */
  // The authority, which is how your code knows which Content Provider to access
  public static final String AUTHORITY = "de.appmotion.popularmovies";

  // The base content URI = "content://" + <authority>
  public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);

  // This is the path for the "movie" directory
  public static final String PATH_MOVIE = "movie";
  // This is the path for the "favorite_movie" directory
  public static final String PATH_FAVORITE_MOVIE = "favorite_movie";

  // To prevent someone from accidentally instantiating the contract class,
  // make the constructor private.
  private MovieContract() {
  }

  public static final class MovieEntry implements BaseColumns {

    // MovieEntry content URI = base content URI + path
    public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_MOVIE).build();

    // Static final members for the table name and each of the db columns
    public static final String TABLE_NAME = "movie";
    public static final String COLUMN_MOVIE_ID = "movie_id";
    public static final String COLUMN_MOVIE_TITLE = "movie_title";
    public static final String COLUMN_MOVIE_IMAGE_URL = "movie_image_url";
    public static final String COLUMN_TIMESTAMP = "timestamp";
  }

  public static final class FavoriteMovieEntry implements BaseColumns {

    // FavoriteMovieEntry content URI = base content URI + path
    public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_FAVORITE_MOVIE).build();

    // Static final members for the table name and each of the db columns
    public static final String TABLE_NAME = "favorite_movie";
    public static final String COLUMN_MOVIE_ID = "movie_id";
    public static final String COLUMN_MOVIE_TITLE = "movie_title";
    public static final String COLUMN_MOVIE_IMAGE_URL = "movie_image_url";
    public static final String COLUMN_TIMESTAMP = "timestamp";
  }
}
