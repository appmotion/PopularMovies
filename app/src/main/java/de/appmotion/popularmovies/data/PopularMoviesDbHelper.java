package de.appmotion.popularmovies.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Manages a local database for movies data.
 */
public class PopularMoviesDbHelper extends SQLiteOpenHelper {

  // The database name
  private static final String DATABASE_NAME = "popularmovies.db";

  // If you change the database schema, you must increment the database version or the onUpgrade method will not be called.
  private static final int DATABASE_VERSION = 1;

  // Constructor
  public PopularMoviesDbHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  /**
   * Called when the database is created for the first time. This is where the creation of
   * tables and the initial population of the tables should happen.
   *
   * @param sqLiteDatabase The database.
   */
  @Override public void onCreate(SQLiteDatabase sqLiteDatabase) {

    /* Create a table to hold Favoritelist data
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
    final String SQL_CREATE_WAITLIST_TABLE = "CREATE TABLE "
        + PopularMoviesContract.FavoritelistEntry.TABLE_NAME
        + " ("
        + PopularMoviesContract.FavoritelistEntry._ID
        + " INTEGER PRIMARY KEY AUTOINCREMENT, "
        + PopularMoviesContract.FavoritelistEntry.COLUMN_MOVIE_ID
        + " INTEGER NOT NULL UNIQUE, "
        + PopularMoviesContract.FavoritelistEntry.COLUMN_MOVIE_TITLE
        + " TEXT NOT NULL, "
        + PopularMoviesContract.FavoritelistEntry.COLUMN_MOVIE_IMAGE_URL
        + " TEXT, "
        + PopularMoviesContract.FavoritelistEntry.COLUMN_TIMESTAMP
        + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
        + ");";

    /*
     * After we've spelled out our SQLite table creation statement above, we actually execute
     * that SQL with the execSQL method of our SQLite database object.
     */
    sqLiteDatabase.execSQL(SQL_CREATE_WAITLIST_TABLE);
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
    // For now simply drop the table and create a new one. This means if you change the
    // DATABASE_VERSION the table will be dropped.
    // In a production app, this method might be modified to ALTER the table
    // instead of dropping it, so that existing data is not deleted.
    sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + PopularMoviesContract.FavoritelistEntry.TABLE_NAME);
    onCreate(sqLiteDatabase);
  }
}