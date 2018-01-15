package de.appmotion.popularmovies.data.source;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import de.appmotion.popularmovies.data.source.local.MovieContract;
import de.appmotion.popularmovies.data.source.local.MovieDbHelper;

public class MovieContentProvider extends ContentProvider {

  // It's convention to use 100, 200, 300, etc for directories,
  // and related ints (101, 102, ..) for items in that directory.
  public static final int CODE_FAVORITE_MOVIE = 100;
  public static final int CODE_FAVORITE_MOVIE_WITH_ID = 101;

  private static final UriMatcher sUriMatcher = buildUriMatcher();
  // Member variable for a MovieDbHelper that's initialized in the onCreate() method
  private MovieDbHelper mDbHelper;

  /**
   * Creates the UriMatcher that will match each URI to the CODE_FAVORITE_MOVIE and
   * CODE_FAVORITE_MOVIE_WITH_ID constants defined above.
   *
   * @return A UriMatcher that correctly matches the constants for CODE_FAVORITE_MOVIE and CODE_FAVORITE_MOVIE_WITH_ID
   */
  public static UriMatcher buildUriMatcher() {

    /*
     * All paths added to the UriMatcher have a corresponding code to return when a match is
     * found. The code passed into the constructor of UriMatcher here represents the code to
     * return for the root URI. It's common to use NO_MATCH as the code for this case.
     */
    UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    /*
      All paths added to the UriMatcher have a corresponding int.
      For each kind of uri you may want to access, add the corresponding match with addURI.
      The two calls below add matches for the favorite movie directory and a single item by ID.
     */
    uriMatcher.addURI(MovieContract.AUTHORITY, MovieContract.PATH_FAVORITE_MOVIE, CODE_FAVORITE_MOVIE);
    // The "/#" signifies to the UriMatcher that if PATH_FAVORITE_MOVIE is followed by ANY number, that it should return the CODE_FAVORITE_MOVIE_WITH_ID code
    uriMatcher.addURI(MovieContract.AUTHORITY, MovieContract.PATH_FAVORITE_MOVIE + "/#", CODE_FAVORITE_MOVIE_WITH_ID);

    return uriMatcher;
  }

  /**
   * In onCreate, we initialize our content provider on startup. This method is called for all
   * registered content providers on the application main thread at application launch time.
   * It must not perform lengthy operations, or application startup will be delayed.
   *
   * Nontrivial initialization (such as opening, upgrading, and scanning
   * databases) should be deferred until the content provider is used (via {@link #query},
   * {@link #bulkInsert(Uri, ContentValues[])}, etc).
   *
   * Deferred initialization keeps application startup fast, avoids unnecessary work if the
   * provider turns out not to be needed, and stops database errors (such as a full disk) from
   * halting application launch.
   *
   * @return true if the provider was successfully loaded, false otherwise
   */
  @Override public boolean onCreate() {
    /*
     * As noted in the comment above, onCreate is run on the main thread, so performing any
     * lengthy operations will cause lag in your app. Since WeatherDbHelper's constructor is
     * very lightweight, we are safe to perform that initialization here.
     */
    mDbHelper = new MovieDbHelper(getContext());
    return true;
  }

  /**
   * Handle requests for data by URI
   */
  @Nullable @Override public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
      @Nullable String[] selectionArgs, @Nullable String sortOrder) {
    // Get access to underlying database (read-only for query)
    final SQLiteDatabase db = mDbHelper.getReadableDatabase();

    // Write URI matching code and set a variable to return a Cursor
    int match = sUriMatcher.match(uri);
    Cursor returnCursor;

    // Query Params
    String mSelection;
    String[] mSelectionArgs;

    switch (match) {
      // Query for the favorite movie directory
      case CODE_FAVORITE_MOVIE:
        returnCursor = db.query(MovieContract.FavoriteMovieEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
        break;
      case CODE_FAVORITE_MOVIE_WITH_ID:
        // using selection and selectionArgs
        // URI: content://<authority>/favorite_movie/#
        String id = uri.getPathSegments().get(1);
        // Selection is the _ID column = ?, and the Selection args = the row ID from the URI
        mSelection = MovieContract.FavoriteMovieEntry._ID + " = ?";
        mSelectionArgs = new String[] { id };
        returnCursor = db.query(MovieContract.FavoriteMovieEntry.TABLE_NAME, projection, mSelection, mSelectionArgs, null, null, sortOrder);
        break;
      default:
        throw new UnsupportedOperationException("Unknown uri: " + uri);
    }

    // Set a notification URI on the Cursor and return that Cursor
    returnCursor.setNotificationUri(getContext().getContentResolver(), uri);

    // Return the desired Cursor
    return returnCursor;
  }

  @Nullable @Override public String getType(@NonNull Uri uri) {
    return null;
  }

  /**
   * Insert a single new row of data
   */
  @Nullable @Override public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
    // Get access to the database (to write new data to)
    final SQLiteDatabase db = mDbHelper.getWritableDatabase();

    // Write URI matching code to identify the match for the favorite movie directory
    int match = sUriMatcher.match(uri);
    Uri returnUri; // URI to be returned

    switch (match) {
      case CODE_FAVORITE_MOVIE:
        // Inserting values into favorite movie table
        long id = db.insert(MovieContract.FavoriteMovieEntry.TABLE_NAME, null, values);
        if (id > 0) {
          returnUri = ContentUris.withAppendedId(MovieContract.FavoriteMovieEntry.CONTENT_URI, id);
        } else {
          throw new android.database.SQLException("Failed to insert row into " + uri);
        }
        break;
      default:
        throw new UnsupportedOperationException("Unknown uri: " + uri);
    }

    // Notify the resolver if the uri has been changed
    getContext().getContentResolver().notifyChange(uri, null);

    // Return constructed uri (this points to the newly inserted row of data)
    return returnUri;


    /*
    // Check if the id of the movie already exists in favorite movie table. If not the movie will be added to the table.
    String whereClause = MovieContract.FavoriteMovieEntry.COLUMN_MOVIE_ID + " = ?";
    String[] whereArgs = new String[] { String.valueOf(values.getAsLong(MovieContract.FavoriteMovieEntry.COLUMN_MOVIE_ID)) };
    Cursor cursor = db.query(MovieContract.FavoriteMovieEntry.TABLE_NAME, null, whereClause, whereArgs, null, null, null);
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

  /**
   * Handles requests to insert a set of new rows.
   *
   * @return The number of values that were inserted.
   */
  @Override public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
    // Get access to the database
    final SQLiteDatabase db = mDbHelper.getWritableDatabase();

    int match = sUriMatcher.match(uri);

    switch (match) {

      case CODE_FAVORITE_MOVIE:
        db.beginTransaction();
        int rowsInserted = 0;
        try {
          for (ContentValues value : values) {
            long _id = db.insert(MovieContract.FavoriteMovieEntry.TABLE_NAME, null, value);
            if (_id != -1) {
              rowsInserted++;
            }
          }
          db.setTransactionSuccessful();
        } finally {
          db.endTransaction();
        }

        if (rowsInserted > 0) {
          getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows inserted
        return rowsInserted;

      // If the URI does not match match CODE_FAVORITE_MOVIE, return the super implementation of bulkInsert
      default:
        return super.bulkInsert(uri, values);
    }
  }

  @Override public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
    // Get access to the database
    final SQLiteDatabase db = mDbHelper.getWritableDatabase();

    int match = sUriMatcher.match(uri);
    int moviesDeleted;  // Keep track of the number of deleted movies

    /*
     * If we pass null as the selection to SQLiteDatabase#delete, our entire table will be
     * deleted. However, if we do pass null and delete all of the rows in the table, we won't
     * know how many rows were deleted. According to the documentation for SQLiteDatabase,
     * passing "1" for the selection will delete all rows and return the number of rows
     * deleted, which is what the caller of this method expects.
      */
    if (null == selection) selection = "1";

    switch (match) {
      // Delete ALL rows in the table
      case CODE_FAVORITE_MOVIE:
        moviesDeleted = db.delete(MovieContract.FavoriteMovieEntry.TABLE_NAME, selection, selectionArgs);

        break;
      // Handle the single item case, recognized by the ID included in the URI path
      case CODE_FAVORITE_MOVIE_WITH_ID:
        // using selection and selectionArgs
        // URI: content://<authority>/favorite_movie/#
        String id = uri.getPathSegments().get(1);
        // Selection is the _ID column = ?, and the Selection args = the row ID from the URI
        String mSelection = "_id=?";
        String[] mSelectionArgs = new String[] { id };
        moviesDeleted = db.delete(MovieContract.FavoriteMovieEntry.TABLE_NAME, mSelection, mSelectionArgs);
        break;
      default:
        throw new UnsupportedOperationException("Unknown uri: " + uri);
    }

    // Notify the resolver of a change and return the number of items deleted
    if (moviesDeleted != 0) {
      // A movie was deleted, set notification
      getContext().getContentResolver().notifyChange(uri, null);
    }
    // Return the number of movies deleted
    return moviesDeleted;
  }

  @Override
  public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
    // Get access to the database
    final SQLiteDatabase db = mDbHelper.getWritableDatabase();

    int match = sUriMatcher.match(uri);
    int moviesUpdated;  // Keep track of the number of updated movies

    switch (match) {
      case CODE_FAVORITE_MOVIE_WITH_ID:
        // using selection and selectionArgs
        // URI: content://<authority>/favorite_movie/#
        String id = uri.getPathSegments().get(1);
        // Selection is the _ID column = ?, and the Selection args = the row ID from the URI
        String mSelection = "_id=?";
        String[] mSelectionArgs = new String[] { id };
        moviesUpdated = db.update(MovieContract.FavoriteMovieEntry.TABLE_NAME, values, mSelection, mSelectionArgs);
        break;
      default:
        throw new UnsupportedOperationException("Unknown uri: " + uri);
    }

    // Notify the resolver of a change and return the number of items updated
    if (moviesUpdated != 0) {
      // A movie was updated, set notification
      getContext().getContentResolver().notifyChange(uri, null);
    }

    // Return the number of movies updated
    return moviesUpdated;
  }
}
