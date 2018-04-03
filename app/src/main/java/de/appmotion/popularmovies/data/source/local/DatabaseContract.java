package de.appmotion.popularmovies.data.source.local;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Defines table and column names for the popularmovies database. Furthermore the Content provider constants for accessing data in this
 * contract are defined.
 */
public final class DatabaseContract {

  /*
   * Content provider constants
   * Define the possible paths for accessing data in this contract
   */
  // The authority, which is how your code knows which Content Provider to access
  public static final String AUTHORITY = "de.appmotion.popularmovies";

  // The base content URI = "content://" + <authority>
  public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);

  // This is the path for the "popular movie" directory
  public static final String PATH_MOVIE_POPULAR = "movie_popular";
  // This is the path for the "top rated movie" directory
  public static final String PATH_MOVIE_TOP_RATED = "movie_top_rated";
  // This is the path for the "favorite movie" directory
  public static final String PATH_MOVIE_FAVORITE = "movie_favorite";

  // To prevent someone from accidentally instantiating the contract class,
  // make the constructor private.
  private DatabaseContract() {
  }

  public static class MovieEntry implements BaseColumns {
    // Static final members for the table name and each of the db columns
    public static final String COLUMN_MOVIE_ID = "movie_id";
    public static final String COLUMN_MOVIE_TITLE = "movie_title";
    public static final String COLUMN_MOVIE_IMAGE_URL = "movie_image_url";
    public static final String COLUMN_MOVIE_POPULARITY = "movie_popularity";
    public static final String COLUMN_MOVIE_VOTE_AVERAGE = "movie_vote_average";
    public static final String COLUMN_MOVIE_RELEASE_DATE = "movie_release_date";
    public static final String COLUMN_MOVIE_OVERVIEW = "movie_overview";
    public static final String COLUMN_TIMESTAMP = "timestamp";
  }

  public static final class MoviePopularEntry extends MovieEntry {
    // MovieEntry content URI = base content URI + path
    public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_MOVIE_POPULAR).build();
    public static final String TABLE_NAME = "movie_popular";
  }

  public static final class MovieTopRatedEntry extends MovieEntry {
    // MovieEntry content URI = base content URI + path
    public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_MOVIE_TOP_RATED).build();
    public static final String TABLE_NAME = "movie_top_rated";
  }

  public static final class MovieFavoriteEntry extends MovieEntry {
    // FavoriteMovieEntry content URI = base content URI + path
    public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_MOVIE_FAVORITE).build();
    public static final String TABLE_NAME = "movie_favorite";
  }
}
