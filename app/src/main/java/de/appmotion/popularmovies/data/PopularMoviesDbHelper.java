package de.appmotion.popularmovies.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PopularMoviesDbHelper extends SQLiteOpenHelper {

  // The database name
  private static final String DATABASE_NAME = "popularmovies.db";

  // If you change the database schema, you must increment the database version
  private static final int DATABASE_VERSION = 1;

  // Constructor
  public PopularMoviesDbHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override public void onCreate(SQLiteDatabase sqLiteDatabase) {

    // Create a table to hold Favoritelist data
    final String SQL_CREATE_WAITLIST_TABLE = "CREATE TABLE "
        + PopularMoviesContract.FavoritelistEntry.TABLE_NAME
        + " ("
        + PopularMoviesContract.FavoritelistEntry._ID
        + " INTEGER PRIMARY KEY AUTOINCREMENT,"
        + PopularMoviesContract.FavoritelistEntry.COLUMN_MOVIE_ID
        + " LONG NOT NULL UNIQUE, "
        + PopularMoviesContract.FavoritelistEntry.COLUMN_MOVIE_NAME
        + " TEXT NOT NULL, "
        + PopularMoviesContract.FavoritelistEntry.COLUMN_TIMESTAMP
        + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
        + "); ";

    sqLiteDatabase.execSQL(SQL_CREATE_WAITLIST_TABLE);
  }

  @Override public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
    // For now simply drop the table and create a new one. This means if you change the
    // DATABASE_VERSION the table will be dropped.
    // In a production app, this method might be modified to ALTER the table
    // instead of dropping it, so that existing data is not deleted.
    sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + PopularMoviesContract.FavoritelistEntry.TABLE_NAME);
    onCreate(sqLiteDatabase);
  }
}