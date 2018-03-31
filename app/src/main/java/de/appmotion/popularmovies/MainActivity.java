package de.appmotion.popularmovies;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import de.appmotion.popularmovies.data.Movie;
import de.appmotion.popularmovies.data.source.local.MovieContract;
import de.appmotion.popularmovies.data.source.remote.NetworkLoader;
import de.appmotion.popularmovies.data.source.remote.NetworkUtils;
import de.appmotion.popularmovies.databinding.ActivityMainBinding;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Display Movies via a grid of their corresponding movie poster thumbnails.
 */
public class MainActivity extends BaseActivity
    implements MoviePopularCursorAdapter.ListItemClickListener, MovieTopRatedCursorAdapter.ListItemClickListener,
    MovieFavoriteCursorAdapter.ListItemClickListener {

  // This number will uniquely identify a CursorLoader for loading data from 'movie_popular' DB table.
  // Use negative number because positive numbers are reserved for mMoviePageToDownload variable which is used by NetworkLoader.
  private static final int CURSOR_LOADER_MOVIE_POPULAR = -1;
  // This number will uniquely identify a CursorLoader for loading data from 'movie_top_rated' DB table.
  private static final int CURSOR_LOADER_MOVIE_TOP_RATED = -2;
  // This number will uniquely identify a CursorLoader for loading data from 'movie_favorite' DB table.
  private static final int CURSOR_LOADER_MOVIE_FAVORITE = -3;
  // Constant for logging
  private static final String TAG = MainActivity.class.getSimpleName();
  // Define {@link MenuState} Types
  private static final int MOVIE_POPULAR = 0;
  private static final int MOVIE_TOP_RATED = 1;
  private static final int MOVIE_FAVORITE = 2;
  // Save {@link MenuState} via onSaveInstanceState
  private static final String STATE_MENU_STATE = "menu_state";
  // Which page of a movie list from the server has to be downloaded. This number will uniquely identify corresponding NetworkLoader, too.
  private int mMoviePageToDownload = 1;
  // Callback for {@link NetworkLoader}
  private LoaderManager.LoaderCallbacks<String> mNetworkLoaderCallback;
  // Callback for {@link CursorLoader}
  private LoaderManager.LoaderCallbacks<Cursor> mCursorLoaderCallback;
  // The About Dialog
  private AlertDialog mAboutDialog;
  // RecyclerView.Adapter containing popular {@link Movie}s.
  private MoviePopularCursorAdapter mMoviePopularCursorAdapter;
  // RecyclerView.Adapter containing top rated {@link Movie}s.
  private MovieTopRatedCursorAdapter mMovieTopRatedCursorAdapter;
  // RecyclerView.Adapter containing {@link FavoriteMovie}s.
  private MovieFavoriteCursorAdapter mMovieFavoriteCursorAdapter;
  // Saves current selected {@link MenuState} from Options Menu
  private int mMenuState = MOVIE_POPULAR;

  private ActivityMainBinding mMainBinding;

  /**
   * Called when the activity is first created. This is where you should do all of your normal
   * static set up: create views, bind data to lists, etc.
   *
   * Always followed by onStart().
   *
   * @param savedInstanceState The Activity's previously frozen state, if there was one.
   */
  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

    updateValuesFromBundle(savedInstanceState);

    // Initiate Callbacks for the Loaders
    mNetworkLoaderCallback = initNetworkLoaderCallback();
    mCursorLoaderCallback = initCursorLoaderCallback();

    // RecyclerView
    // Use setHasFixedSize to improve performance if you know that changes in content do not
    // change the child layout size in the RecyclerView
    mMainBinding.rvMovieList.setHasFixedSize(true);
    mMainBinding.rvMovieList.addOnScrollListener(new RecyclerView.OnScrollListener() {
      @Override public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        if (dy > 0 && isLastItemDisplaying(recyclerView)) {
          if (mMenuState == MOVIE_POPULAR) {
            downloadAndShowPopularMovies(mDefaultLanguage, mDefaultCountry);
          } else if (mMenuState == MOVIE_TOP_RATED) {
            downloadAndShowTopRatedMovies(mDefaultLanguage, mDefaultCountry);
          }
        }
      }
    });

    // LayoutManager
    // This value should be true if you want to reverse your layout. Generally, this is only
    // true with horizontal lists that need to support a right-to-left layout.
    boolean shouldReverseLayout = false;
    RecyclerView.LayoutManager layoutManager =
        new GridLayoutManager(this, calculateNoOfColumns(), GridLayoutManager.VERTICAL, shouldReverseLayout);
    mMainBinding.rvMovieList.setLayoutManager(layoutManager);

    // Initiate the popular movie cursor adapter for RecyclerView
    mMoviePopularCursorAdapter = new MoviePopularCursorAdapter(this, mRequiredImageSize, this);
    // Initiate the top rated movie cursor adapter for RecyclerView
    mMovieTopRatedCursorAdapter = new MovieTopRatedCursorAdapter(this, mRequiredImageSize, this);
    // Initiate the favorite movie cursor adapter for RecyclerView
    mMovieFavoriteCursorAdapter = new MovieFavoriteCursorAdapter(this, mRequiredImageSize, this);

    /*
     * Ensures a loader is initialized and active. If the loader doesn't already exist, one is
     * created and (if the activity/fragment is currently started) starts the loader. Otherwise
     * the last created loader is re-used.
     */

    // Loader for Popular Movies
    getSupportLoaderManager().initLoader(CURSOR_LOADER_MOVIE_POPULAR, null, mCursorLoaderCallback);
    // Loader for Top Rated Movies
    getSupportLoaderManager().initLoader(CURSOR_LOADER_MOVIE_TOP_RATED, null, mCursorLoaderCallback);
    // Loader for Favorite Movies
    getSupportLoaderManager().initLoader(CURSOR_LOADER_MOVIE_FAVORITE, null, mCursorLoaderCallback);

    // Set title of this Activity depending on current {@link MenuState} and
    // get Movies depending on current {@link MenuState}
    if (mMenuState == MOVIE_POPULAR) {
      setTitle(R.string.action_popular);
      mMainBinding.rvMovieList.setAdapter(mMoviePopularCursorAdapter);
      downloadAndShowPopularMovies(mDefaultLanguage, mDefaultCountry);
    } else if (mMenuState == MOVIE_TOP_RATED) {
      setTitle(R.string.action_top);
      mMainBinding.rvMovieList.setAdapter(mMovieTopRatedCursorAdapter);
      downloadAndShowTopRatedMovies(mDefaultLanguage, mDefaultCountry);
    } else if (mMenuState == MOVIE_FAVORITE) {
      setTitle(R.string.action_favorite_show);
      mMainBinding.rvMovieList.setAdapter(mMovieFavoriteCursorAdapter);
    }
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt(STATE_MENU_STATE, mMenuState);
  }

  /**
   * Called when the activity is becoming visible to the user.
   *
   * Followed by onResume() if the activity comes to the foreground, or onStop() if it becomes
   * hidden.
   */
  @Override protected void onStart() {
    super.onStart();
  }

  /**
   * Called when the activity will start interacting with the user. At this point your activity
   * is at the top of the activity stack, with user input going to it.
   *
   * Always followed by onPause().
   */
  @Override protected void onResume() {
    super.onResume();
  }

  /**
   * Called when the system is about to start resuming a previous activity. This is typically
   * used to commit unsaved changes to persistent data, stop animations and other things that may
   * be consuming CPU, etc. Implementations of this method must be very quick because the next
   * activity will not be resumed until this method returns.
   *
   * Followed by either onResume() if the activity returns back to the front, or onStop() if it
   * becomes invisible to the user.
   */
  @Override protected void onPause() {
    super.onPause();
  }

  /**
   * Called when the activity is no longer visible to the user, because another activity has been
   * resumed and is covering this one. This may happen either because a new activity is being
   * started, an existing one is being brought in front of this one, or this one is being
   * destroyed.
   *
   * Followed by either onRestart() if this activity is coming back to interact with the user, or
   * onDestroy() if this activity is going away.
   */
  @Override protected void onStop() {
    super.onStop();
  }

  /**
   * Called after your activity has been stopped, prior to it being started again.
   *
   * Always followed by onStart()
   */
  @Override protected void onRestart() {
    super.onRestart();
  }

  /**
   * The final call you receive before your activity is destroyed. This can happen either because
   * the activity is finishing (someone called finish() on it, or because the system is
   * temporarily destroying this instance of the activity to save space. You can distinguish
   * between these two scenarios with the isFinishing() method.
   */
  @Override protected void onDestroy() {
    dismissDialog(mAboutDialog);
    mMainBinding.rvMovieList.clearOnScrollListeners();
    super.onDestroy();
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.main, menu);
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      // Load and show Popular Movies.
      case R.id.action_popular:
        mMenuState = MOVIE_POPULAR;
        setTitle(R.string.action_popular);
        mMoviePageToDownload = 1;
        mMainBinding.rvMovieList.setAdapter(mMoviePopularCursorAdapter);
        downloadAndShowPopularMovies(mDefaultLanguage, mDefaultCountry);
        return true;
      // Load and show To Rated Movies.
      case R.id.action_top:
        mMenuState = MOVIE_TOP_RATED;
        setTitle(R.string.action_top);
        mMoviePageToDownload = 1;
        mMainBinding.rvMovieList.setAdapter(mMovieTopRatedCursorAdapter);
        downloadAndShowTopRatedMovies(mDefaultLanguage, mDefaultCountry);
        return true;
      // Load local saved favorite Movies.
      case R.id.action_favorite_show:
        mMenuState = MOVIE_FAVORITE;
        setTitle(R.string.action_favorite_show);
        mMainBinding.rvMovieList.setAdapter(mMovieFavoriteCursorAdapter);
        return true;
      // Show About Dialog.
      case R.id.action_about:
        showAboutDialog();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void downloadConfiguration() {
    // Get URL for popular Configuration Download and build Bundle for {@link NetworkLoader}
    URL configurationUrl = NetworkUtils.buildConfigurationUrl();
    Bundle queryBundle = new Bundle();
    queryBundle.putSerializable(NetworkLoader.EXTRA_QUERY_URL, configurationUrl);

    // Call getSupportLoaderManager and store it in a LoaderManager variable
    LoaderManager loaderManager = getSupportLoaderManager();
    // Get our Loader by calling getLoader and passing the ID we specified
    Loader<String> networkLoader = loaderManager.getLoader(0);
    // If the Loader was null, initialize it. Else, restart it.
    if (networkLoader == null) {
      loaderManager.initLoader(0, queryBundle, mNetworkLoaderCallback);
    } else {
      loaderManager.restartLoader(0, queryBundle, mNetworkLoaderCallback);
    }
  }

  /**
   * Get Popular Movies from themoviedb.org
   *
   * @param language The language requested.
   * @param region The region requested.
   */
  private void downloadAndShowPopularMovies(String language, String region) {
    // Get URL for popular Movies Download and build Bundle for {@link NetworkLoader}
    URL popularMoviesUrl = NetworkUtils.buildPopularMoviesUrl(language, String.valueOf(mMoviePageToDownload), region);
    Bundle queryBundle = new Bundle();
    queryBundle.putSerializable(NetworkLoader.EXTRA_QUERY_URL, popularMoviesUrl);

    // Call getSupportLoaderManager and store it in a LoaderManager variable
    LoaderManager loaderManager = getSupportLoaderManager();
    // Get our Loader by calling getLoader and passing the ID we specified
    Loader<String> networkLoader = loaderManager.getLoader(mMoviePageToDownload);
    // If the Loader was null, initialize it. Else, restart it.
    if (networkLoader == null) {
      loaderManager.initLoader(mMoviePageToDownload, queryBundle, mNetworkLoaderCallback);
    } else {
      loaderManager.restartLoader(mMoviePageToDownload, queryBundle, mNetworkLoaderCallback);
    }
  }

  /**
   * Get Top Rated Movies from themoviedb.org
   *
   * @param language The language requested.
   * @param region The region requested.
   */
  private void downloadAndShowTopRatedMovies(String language, String region) {
    // Get URL for top rated Movies Download and build Bundle for {@link NetworkLoader}
    URL topRatedMoviesUrl = NetworkUtils.buildTopRatedMoviesUrl(language, String.valueOf(mMoviePageToDownload), region);
    Bundle queryBundle = new Bundle();
    queryBundle.putSerializable(NetworkLoader.EXTRA_QUERY_URL, topRatedMoviesUrl);

    // Call getSupportLoaderManager and store it in a LoaderManager variable
    LoaderManager loaderManager = getSupportLoaderManager();
    // Get our Loader by calling getLoader and passing the ID we specified
    Loader<String> networkLoader = loaderManager.getLoader(mMoviePageToDownload);
    // If the Loader was null, initialize it. Else, restart it.
    if (networkLoader == null) {
      loaderManager.initLoader(mMoviePageToDownload, queryBundle, mNetworkLoaderCallback);
    } else {
      loaderManager.restartLoader(mMoviePageToDownload, queryBundle, mNetworkLoaderCallback);
    }
  }

  private void showAboutDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    mAboutDialog = builder.setCancelable(true)
        .setMessage(R.string.tmdb_notice)
        .setTitle(R.string.action_about)
        .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
          }
        })
        .create();
    mAboutDialog.show();
  }

  private void dismissDialog(Dialog dialog) {
    if (dialog != null && dialog.isShowing()) {
      dialog.dismiss();
    }
  }

  /**
   * Called when Loader with ID:mMoviePageToDownload finished in onLoadFinished().
   * Parse jsonData and save it in DB.
   *
   * @param jsonData from onLoadFinished of {@link NetworkLoader}.
   */
  private void parseJson(String jsonData) {
    List<Movie> movieList = new ArrayList<>();
    try {
      JSONObject popular = new JSONObject(jsonData);
      JSONArray results = popular.getJSONArray("results");
      int i = 0;
      while (results != null && !results.isNull(i)) {
        JSONObject result = results.getJSONObject(i);
        if (result != null) {
          Long movieId = result.optLong("id", -1L);
          String title = result.optString("title");
          String imageUrl = result.optString("poster_path");
          double popularity = result.optDouble("popularity", 0);
          double voteAverage = result.optDouble("vote_average", 0);
          String releaseDate = result.optString("release_date");
          String overview = result.optString("overview");
          Movie movie = new Movie();
          movie.setMovieId(movieId);
          movie.setTitle(title);
          movie.setImageUrl(imageUrl);
          movie.setPopularity(popularity);
          movie.setVoteAverage(voteAverage);
          movie.setReleaseDate(releaseDate);
          movie.setOverview(overview);
          movieList.add(movie);
        }
        i++;
      }
      insertMovieList(movieList);
    } catch (JSONException e) {
      Log.e(TAG, "Parse Movie JSON list error: ", e);
    }
  }

  /**
   * Insert a movie list to {@link MovieContract.MoviePopularEntry} or {@link MovieContract.MovieTopRatedEntry}.
   *
   * @param movieList List of downloaded Movies
   * @return the number of newly created rows.
   */
  private int insertMovieList(@Nullable List<Movie> movieList) {
    if (movieList == null) {
      return 0;
    }
    ContentValues[] cvArray = new ContentValues[movieList.size()];

    for (int i = 0; i < movieList.size(); i++) {
      cvArray[i] = Movie.from(movieList.get(i));
    }

    if (mMenuState == MOVIE_POPULAR) {
      return getContentResolver().bulkInsert(MovieContract.MoviePopularEntry.CONTENT_URI, cvArray);
    } else if (mMenuState == MOVIE_TOP_RATED) {
      return getContentResolver().bulkInsert(MovieContract.MovieTopRatedEntry.CONTENT_URI, cvArray);
    } else {
      return 0;
    }
  }

  /**
   * Check whether the last item in RecyclerView is being displayed or not
   *
   * @param recyclerView which you would like to check
   * @return true if last position was Visible and false Otherwise
   */
  private boolean isLastItemDisplaying(RecyclerView recyclerView) {
    if (recyclerView.getAdapter().getItemCount() != 0) {
      int lastVisibleItemPosition = ((GridLayoutManager) recyclerView.getLayoutManager()).findLastVisibleItemPosition();
      return lastVisibleItemPosition != RecyclerView.NO_POSITION && lastVisibleItemPosition == recyclerView.getAdapter().getItemCount() - 1;
    }
    return false;
  }

  private void updateValuesFromBundle(Bundle savedInstanceState) {
    if (savedInstanceState != null) {
      if (savedInstanceState.keySet().contains(STATE_MENU_STATE)) {
        mMenuState = savedInstanceState.getInt(STATE_MENU_STATE, MOVIE_POPULAR);
      }
    }
  }

  /**
   * This is where we receive our callback from
   * {@link MoviePopularCursorAdapter.ListItemClickListener} or
   * {@link MovieTopRatedCursorAdapter.ListItemClickListener} or
   * {@link MovieFavoriteCursorAdapter.ListItemClickListener}
   *
   * This callback is invoked when you click on an item in the list.
   *
   * @param movie {@link Movie} in the list that was clicked.
   */
  @Override public void onListItemClick(Movie movie) {
    // Show Movie Detail Activity
    Intent intent = new Intent(this, MovieDetailActivity.class);
    intent.putExtra(MovieDetailActivity.EXTRA_MOVIE_OBJECT, movie);
    startActivity(intent);
  }

  /**
   * Below this point are {@link LoaderManager.LoaderCallbacks} methods
   **/

  private LoaderManager.LoaderCallbacks<String> initNetworkLoaderCallback() {
    return new LoaderManager.LoaderCallbacks<String>() {

      /**
       * This is called when a new Loader needs to be created.
       *
       * @param loaderId The ID whose loader is to be created.
       * @param args Any arguments supplied by the caller.
       * @return Return a new Loader instance that is ready to start loading.
       */
      @NonNull @Override public Loader<String> onCreateLoader(int loaderId, Bundle args) {
        // Extract the url query from the args using our constant
        URL queryUrl = (URL) args.getSerializable(NetworkLoader.EXTRA_QUERY_URL);
        return new NetworkLoader(MainActivity.this, queryUrl);
      }

      /**
       * Called when a previously created loader has finished its load.
       *
       * @param loader The Loader that has finished.
       * @param data The data generated by the Loader.
       */
      @Override public void onLoadFinished(@NonNull Loader<String> loader, String data) {
        // When we finish loading, we want to hide the loading indicator from the user.
        //mLoadingIndicator.setVisibility(View.INVISIBLE);

        // Allways destroy NetworkLoader instances so they wont start themselves again automatically when we enter the Activity.
        getSupportLoaderManager().destroyLoader(loader.getId());

        if (data == null) {
          Log.e(TAG, "The url was empty");
        } else {
          switch (data) {
            case NetworkLoader.API_ERROR:
              showErrorMessage(NetworkLoader.API_ERROR);
              break;
            case NetworkLoader.OFFLINE:
              showErrorMessage(NetworkLoader.OFFLINE);
              break;
            case NetworkLoader.EMPTY:
              showErrorMessage(NetworkLoader.EMPTY);
              break;
            default:
              // Here we succesfully get data from server. So next time we can download the following movie page by incrementing mMoviePageToDownload.
              parseJson(data);
              mMoviePageToDownload++;
              break;
          }
        }
      }

      /**
       * Called when a previously created loader is being reset, and thus
       * making its data unavailable. The application should at this point
       * remove any references it has to the Loader's data.
       *
       * @param loader The Loader that is being reset.
       */
      @Override public void onLoaderReset(@NonNull Loader<String> loader) {
        // do nothing
      }
    };
  }

  private LoaderManager.LoaderCallbacks<Cursor> initCursorLoaderCallback() {
    return new LoaderManager.LoaderCallbacks<Cursor>() {

      String sortOrder = null;

      @NonNull @Override public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {

        switch (loaderId) {
          case CURSOR_LOADER_MOVIE_POPULAR:
            Uri popularMovieQueryUri = MovieContract.MoviePopularEntry.CONTENT_URI;
            sortOrder = MovieContract.MovieEntry.COLUMN_MOVIE_POPULARITY + " DESC";
            return new CursorLoader(MainActivity.this, popularMovieQueryUri, null, null, null, sortOrder);
          case CURSOR_LOADER_MOVIE_TOP_RATED:
            Uri topRatedMovieQueryUri = MovieContract.MovieTopRatedEntry.CONTENT_URI;
            sortOrder = MovieContract.MovieEntry.COLUMN_MOVIE_VOTE_AVERAGE + " DESC";
            return new CursorLoader(MainActivity.this, topRatedMovieQueryUri, null, null, null, sortOrder);
          case CURSOR_LOADER_MOVIE_FAVORITE:
            Uri favoriteMovieQueryUri = MovieContract.MovieFavoriteEntry.CONTENT_URI;
            sortOrder = MovieContract.MovieEntry.COLUMN_TIMESTAMP + " DESC";
            return new CursorLoader(MainActivity.this, favoriteMovieQueryUri, null, null, null, sortOrder);
          default:
            throw new RuntimeException("Loader not Implemented: " + loaderId);
        }
      }

      @Override public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
          case CURSOR_LOADER_MOVIE_POPULAR:
            if (cursor != null) {
              // Data loaded
              if (cursor.moveToLast()) {
                // Show data from ContentProvider query
                // Update the cursor in the adapter to trigger UI to display the new list
                mMoviePopularCursorAdapter.swapCursor(cursor);
              }
              // Data empty
              else {
                mMoviePopularCursorAdapter.swapCursor(cursor);
              }
            }
            // Data not available
            else {
              mMoviePopularCursorAdapter.swapCursor(null);
            }
            break;
          case CURSOR_LOADER_MOVIE_TOP_RATED:
            if (cursor != null) {
              // Data loaded
              if (cursor.moveToLast()) {
                // Show data from ContentProvider query
                // Update the cursor in the adapter to trigger UI to display the new list
                mMovieTopRatedCursorAdapter.swapCursor(cursor);
              }
              // Data empty
              else {
                mMovieTopRatedCursorAdapter.swapCursor(cursor);
              }
            }
            // Data not available
            else {
              mMovieTopRatedCursorAdapter.swapCursor(null);
            }
            break;
          case CURSOR_LOADER_MOVIE_FAVORITE:
            if (cursor != null) {
              // Data loaded
              if (cursor.moveToLast()) {
                // Show data from ContentProvider query
                // Update the cursor in the adapter to trigger UI to display the new list
                mMovieFavoriteCursorAdapter.swapCursor(cursor);
              }
              // Data empty
              else {
                mMovieFavoriteCursorAdapter.swapCursor(cursor);
              }
            }
            // Data not available
            else {
              mMovieFavoriteCursorAdapter.swapCursor(null);
            }
            break;
        }
      }

      @Override public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        switch (loader.getId()) {
          case CURSOR_LOADER_MOVIE_POPULAR:
            // Since this Loader's data is now invalid, we need to clear the Adapter that is displaying the data.
            mMoviePopularCursorAdapter.swapCursor(null);
            break;
          case CURSOR_LOADER_MOVIE_TOP_RATED:
            // Since this Loader's data is now invalid, we need to clear the Adapter that is displaying the data.
            mMovieTopRatedCursorAdapter.swapCursor(null);
            break;
          case CURSOR_LOADER_MOVIE_FAVORITE:
            // Since this Loader's data is now invalid, we need to clear the Adapter that is displaying the data.
            mMovieFavoriteCursorAdapter.swapCursor(null);
            break;
        }
      }
    };
  }

  /**
   * Logical type which value must be one of explicitly named constants: MOVIE_POPULAR, MOVIE_TOP_RATED or MOVIE_FAVORITE
   **/
  @Retention(RetentionPolicy.CLASS) @IntDef({ MOVIE_POPULAR, MOVIE_TOP_RATED, MOVIE_FAVORITE }) public @interface MenuState {
  }
}
