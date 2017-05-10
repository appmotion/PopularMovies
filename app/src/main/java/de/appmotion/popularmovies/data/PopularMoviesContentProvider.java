package de.appmotion.popularmovies.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class PopularMoviesContentProvider extends ContentProvider {

  // It's convention to use 100, 200, 300, etc for directories,
  // and related ints (101, 102, ..) for items in that directory.
  public static final int FAVORITE_MOVIES = 100;
  public static final int FAVORITE_MOVIES_WITH_ID = 101;

  private static final UriMatcher sUriMatcher = buildUriMatcher();
  // Member variable for a TaskDbHelper that's initialized in the onCreate() method
  private PopularMoviesDbHelper mDbHelper;

  public static UriMatcher buildUriMatcher() {

    // Initialize a UriMatcher with no matches by passing in NO_MATCH to the constructor
    UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    /*
      All paths added to the UriMatcher have a corresponding int.
      For each kind of uri you may want to access, add the corresponding match with addURI.
      The two calls below add matches for the favorite movies directory and a single item by ID.
     */
    uriMatcher.addURI(PopularMoviesContract.AUTHORITY, PopularMoviesContract.PATH_FAVORITE_MOVIES, FAVORITE_MOVIES);
    uriMatcher.addURI(PopularMoviesContract.AUTHORITY, PopularMoviesContract.PATH_FAVORITE_MOVIES + "/#", FAVORITE_MOVIES_WITH_ID);

    return uriMatcher;
  }

  /**
   * onCreate() is where you should initialize anything you’ll need to setup
   * your underlying data source.
   * In this case, you’re working with a SQLite database, so you’ll need to
   * initialize a DbHelper to gain access to it.
   */
  @Override public boolean onCreate() {

    mDbHelper = new PopularMoviesDbHelper(getContext());
    return true;
  }

  @Nullable @Override
  public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs,
      @Nullable String sortOrder) {
    return null;
  }

  @Nullable @Override public String getType(@NonNull Uri uri) {
    return null;
  }

  // Insert a single new row of data
  @Nullable @Override public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {

    // Get access to the database (to write new data to)
    final SQLiteDatabase db = mDbHelper.getWritableDatabase();

    // Write URI matching code to identify the match for the favorite movies directory
    int match = sUriMatcher.match(uri);
    Uri returnUri; // URI to be returned

    switch (match) {
      case FAVORITE_MOVIES:
        // Inserting values into favorite movies table
        long id = db.insert(PopularMoviesContract.FavoriteMovieEntry.TABLE_NAME, null, values);
        if (id > 0) {
          returnUri = ContentUris.withAppendedId(PopularMoviesContract.FavoriteMovieEntry.CONTENT_URI, id);
        } else {
          throw new android.database.SQLException("Failed to insert row into " + uri);
        }
        break;
      // Default case throws an UnsupportedOperationException
      default:
        throw new UnsupportedOperationException("Unknown uri: " + uri);
    }

    // Notify the resolver if the uri has been changed
    getContext().getContentResolver().notifyChange(uri, null);

    // Return constructed uri (this points to the newly inserted row of data)
    return returnUri;


    /*
    // Check if the id of the movie already exists in favorite movie table. If not the movie will be added to the table.
    String whereClause = PopularMoviesContract.FavoriteMovieEntry.COLUMN_MOVIE_ID + " = ?";
    String[] whereArgs = new String[] { String.valueOf(values.getAsLong(PopularMoviesContract.FavoriteMovieEntry.COLUMN_MOVIE_ID)) };
    Cursor cursor = db.query(PopularMoviesContract.FavoriteMovieEntry.TABLE_NAME, null, whereClause, whereArgs, null, null, null);
    if (!cursor.moveToFirst()) {
      // Close query cursor
      cursor.close();
      // Insert data
    } else {
      cursor.close();
      // Do not Insert data
    }
    */
  }

  @Override public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
    return 0;
  }

  @Override
  public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
    return 0;
  }
}