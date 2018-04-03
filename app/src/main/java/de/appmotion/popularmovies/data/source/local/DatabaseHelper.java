package de.appmotion.popularmovies.data.source.local;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Manages a local database for movie data.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

  private static DatabaseHelper sInstance;

  // The database name
  private static final String DATABASE_NAME = "movie.db";

  // If you change the database schema, you must increment the database version or the onUpgrade method will not be called.
  private static final int DATABASE_VERSION = 2;

  public static synchronized DatabaseHelper getInstance(Context context) {

    // Use the application context, which will ensure that you
    // don't accidentally leak an Activity's context.
    // See this article for more information: http://bit.ly/6LRzfx
    if (sInstance == null) {
      sInstance = new DatabaseHelper(context.getApplicationContext());
    }
    return sInstance;
  }

  /**
   * Constructor should be private to prevent direct instantiation.
   * make call to static method "getInstance()" instead.
   */
  private DatabaseHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  /**
   * Called when the database is created for the first time. This is where the creation of
   * tables and the initial population of the tables should happen.
   *
   * @param sqLiteDatabase The database.
   */
  @Override public void onCreate(SQLiteDatabase sqLiteDatabase) {

    /* Create a table to hold popular movie data
     *
     * If the INTEGER PRIMARY KEY column is not explicitly given a value, then it will be filled
     * automatically with an unused integer, usually one more than the largest _ID currently in
     * use. This is true regardless of whether or not the AUTOINCREMENT keyword is used.
     * <p>
     * If the AUTOINCREMENT keyword appears after INTEGER PRIMARY KEY, that changes the automatic
     * _ID assignment algorithm to prevent the reuse of _IDs over the lifetime of the database.
     * In other words, the purpose of AUTOINCREMENT is to prevent the reuse of _IDs from previously
     * deleted rows.
     */
    final String SQL_CREATE_MOVIE_POPULAR_TABLE = "CREATE TABLE "
        + DatabaseContract.MoviePopularEntry.TABLE_NAME
        + " ("
        + DatabaseContract.MovieEntry._ID
        + " INTEGER PRIMARY KEY AUTOINCREMENT, "
        + DatabaseContract.MovieEntry.COLUMN_MOVIE_ID
        + " INTEGER NOT NULL, "
        + DatabaseContract.MovieEntry.COLUMN_MOVIE_TITLE
        + " TEXT, "
        + DatabaseContract.MovieEntry.COLUMN_MOVIE_IMAGE_URL
        + " TEXT, "
        + DatabaseContract.MovieEntry.COLUMN_MOVIE_POPULARITY
        + " REAL, "
        + DatabaseContract.MovieEntry.COLUMN_MOVIE_VOTE_AVERAGE
        + " REAL, "
        + DatabaseContract.MovieEntry.COLUMN_MOVIE_RELEASE_DATE
        + " TEXT, "
        + DatabaseContract.MovieEntry.COLUMN_MOVIE_OVERVIEW
        + " TEXT, "
        + DatabaseContract.MovieEntry.COLUMN_TIMESTAMP
        + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
        /*
         * To ensure this table can only contain one Movie entry per Movie ID, we declare
         * the ID column to be unique. We also specify "ON CONFLICT REPLACE". This tells
         * SQLite that if we have a Movie entry for a certain ID and we attempt to
         * insert another Movie entry with that ID, we replace the old Movie entry.
         */
        + " UNIQUE ("
        + DatabaseContract.MovieEntry.COLUMN_MOVIE_ID
        + ") ON CONFLICT REPLACE"
        + ");";

    /*
     * After we've spelled out our SQLite table creation statement above, we actually execute
     * that SQL with the execSQL method of our SQLite database object.
     */
    sqLiteDatabase.execSQL(SQL_CREATE_MOVIE_POPULAR_TABLE);

    /*
     * Create a table to hold top rated movie data
     */
    final String SQL_CREATE_MOVIE_TOP_RATED_TABLE = "CREATE TABLE "
        + DatabaseContract.MovieTopRatedEntry.TABLE_NAME
        + " ("
        + DatabaseContract.MovieEntry._ID
        + " INTEGER PRIMARY KEY AUTOINCREMENT, "
        + DatabaseContract.MovieEntry.COLUMN_MOVIE_ID
        + " INTEGER NOT NULL, "
        + DatabaseContract.MovieEntry.COLUMN_MOVIE_TITLE
        + " TEXT, "
        + DatabaseContract.MovieEntry.COLUMN_MOVIE_IMAGE_URL
        + " TEXT, "
        + DatabaseContract.MovieEntry.COLUMN_MOVIE_POPULARITY
        + " REAL, "
        + DatabaseContract.MovieEntry.COLUMN_MOVIE_VOTE_AVERAGE
        + " REAL, "
        + DatabaseContract.MovieEntry.COLUMN_MOVIE_RELEASE_DATE
        + " TEXT, "
        + DatabaseContract.MovieEntry.COLUMN_MOVIE_OVERVIEW
        + " TEXT, "
        + DatabaseContract.MovieEntry.COLUMN_TIMESTAMP
        + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
        + " UNIQUE ("
        + DatabaseContract.MovieEntry.COLUMN_MOVIE_ID
        + ") ON CONFLICT REPLACE"
        + ");";

    sqLiteDatabase.execSQL(SQL_CREATE_MOVIE_TOP_RATED_TABLE);


    /*
     * Create a table to hold favorite movie data
     */
    final String SQL_CREATE_MOVIE_FAVORITE_TABLE = "CREATE TABLE "
        + DatabaseContract.MovieFavoriteEntry.TABLE_NAME
        + " ("
        + DatabaseContract.MovieEntry._ID
        + " INTEGER PRIMARY KEY AUTOINCREMENT, "
        + DatabaseContract.MovieEntry.COLUMN_MOVIE_ID
        + " INTEGER NOT NULL, "
        + DatabaseContract.MovieEntry.COLUMN_MOVIE_TITLE
        + " TEXT, "
        + DatabaseContract.MovieEntry.COLUMN_MOVIE_IMAGE_URL
        + " TEXT, "
        + DatabaseContract.MovieEntry.COLUMN_MOVIE_POPULARITY
        + " REAL, "
        + DatabaseContract.MovieEntry.COLUMN_MOVIE_VOTE_AVERAGE
        + " REAL, "
        + DatabaseContract.MovieEntry.COLUMN_MOVIE_RELEASE_DATE
        + " TEXT, "
        + DatabaseContract.MovieEntry.COLUMN_MOVIE_OVERVIEW
        + " TEXT, "
        + DatabaseContract.MovieEntry.COLUMN_TIMESTAMP
        + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
        + " UNIQUE ("
        + DatabaseContract.MovieEntry.COLUMN_MOVIE_ID
        + ") ON CONFLICT REPLACE"
        + ");";

    sqLiteDatabase.execSQL(SQL_CREATE_MOVIE_FAVORITE_TABLE);
  }

  /**
   * Note that this only fires if you change the version number for your database (in our case, DATABASE_VERSION).
   * It does NOT depend on the version number for your application found in your app/build.gradle file.
   *
   * @param sqLiteDatabase Database that is being upgraded
   * @param oldVersion The old database version
   * @param newVersion The new database version
   */
  @Override public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
    // For now simply drop the tables and create new ones. This means if you change the
    // DATABASE_VERSION the tables will be dropped.
    // TODO: In a production app, this method might be modified to ALTER the tables
    // instead of dropping them, so that existing data is not deleted.
    sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + DatabaseContract.MoviePopularEntry.TABLE_NAME);
    sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + DatabaseContract.MovieTopRatedEntry.TABLE_NAME);
    sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + DatabaseContract.MovieFavoriteEntry.TABLE_NAME);
    onCreate(sqLiteDatabase);
  }
}