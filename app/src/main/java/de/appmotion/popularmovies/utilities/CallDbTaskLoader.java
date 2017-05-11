package de.appmotion.popularmovies.utilities;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;
import android.util.Log;
import de.appmotion.popularmovies.BuildConfig;
import de.appmotion.popularmovies.data.PopularMoviesContract;

/**
 * This loader will return database data as a Cursor or null if an error occurs.
 */
public class CallDbTaskLoader extends AsyncTaskLoader<Cursor> {

  // Name of the Uri sent via Bundle to this Loader
  public final static String EXTRA_QUERY_URI = BuildConfig.APPLICATION_ID + ".query_uri";
  private static final String TAG = CallDbTaskLoader.class.getSimpleName();
  // Arguments for this AsyncTaskLoader
  private Bundle mArgs;

  // Initialize a Cursor
  private Cursor mCursor = null;

  public CallDbTaskLoader(Context context, Bundle args) {
    super(context);
    mArgs = args;
  }

  @Override protected void onStartLoading() {
    // If no arguments were passed, we don't have a query to perform. Simply return.
    if (mArgs == null) {
      return;
    }

    // Show the loading indicator
    //mLoadingIndicator.setVisibility(View.VISIBLE);

    if (mCursor != null) {
      // Delivers any previously loaded data immediately
      deliverResult(mCursor);
    } else {
      // Force a new load
      forceLoad();
    }
  }

  @Override public Cursor loadInBackground() {
    // Extract the uri query from the args using our constant
    Uri queryUri = mArgs.getParcelable(EXTRA_QUERY_URI);

    // If the uri is empty, there's nothing to search for
    if (queryUri == null || TextUtils.isEmpty(queryUri.toString())) {
      return null;
    }

    // Query and load all data in the background; sort by timestamp
    try {
      return getContext().getContentResolver()
          .query(queryUri, null, null, null, PopularMoviesContract.FavoriteMovieEntry.COLUMN_TIMESTAMP + " DESC");
    } catch (Exception e) {
      Log.e(TAG, "Failed to asynchronously load data.");
      e.printStackTrace();
      return null;
    }
  }

  /**
   * deliverResult sends the result of the load, a Cursor, to the registered listener
   */
  @Override public void deliverResult(Cursor cursor) {
    mCursor = cursor;
    super.deliverResult(cursor);
  }
}
