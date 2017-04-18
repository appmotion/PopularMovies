package de.appmotion.popularmovies.data;

import android.provider.BaseColumns;

public final class PopularMoviesContract {
  // To prevent someone from accidentally instantiating the contract class,
  // make the constructor private.
  private PopularMoviesContract() {
  }

  /* Inner class that defines the table contents */
  public static final class FavoritelistEntry implements BaseColumns {
    // Static final members for the table name and each of the db columns
    public static final String TABLE_NAME = "favoritelist";
    public static final String COLUMN_MOVIE_ID = "movieId";
    public static final String COLUMN_MOVIE_TITLE = "movieTitle";
    public static final String COLUMN_MOVIE_IMAGE_URL = "movieImageUrl";
    public static final String COLUMN_TIMESTAMP = "timestamp";
  }
}
