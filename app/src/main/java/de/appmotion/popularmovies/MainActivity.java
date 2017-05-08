package de.appmotion.popularmovies;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import butterknife.BindView;
import butterknife.ButterKnife;
import de.appmotion.popularmovies.data.PopularMoviesContract;
import de.appmotion.popularmovies.data.PopularMoviesDbHelper;
import de.appmotion.popularmovies.data.dto.Movie;
import de.appmotion.popularmovies.utilities.CallApiTaskLoader;
import de.appmotion.popularmovies.utilities.NetworkUtils;
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
    implements MovieListAdapter.ListItemClickListener, FavoriteMovieListAdapter.ListItemClickListener,
    LoaderManager.LoaderCallbacks<String> {

  private static final String TAG = MainActivity.class.getSimpleName();

  // Name of the 'Movie Id data' sent via Intent to {@link MovieDetailActivity}
  public final static String EXTRA_MOVIE_ID = BuildConfig.APPLICATION_ID + ".movie_id";
  // Define {@link MenuState} Types
  private static final int POPULAR_MOVIES = 0;
  private static final int TOP_RATED_MOVIES = 1;
  private static final int FAVORITE_MOVIES = 2;
  // Save {@link MenuState} via onSaveInstanceState
  private static final String STATE_MENU_STATE = "menu_state";
  // Views
  // RecyclerView which shows Movies
  @BindView(android.R.id.list) RecyclerView mMoviesRecyclerView;
  // The About Dialog
  private AlertDialog mAboutDialog;
  // RecyclerView.Adapter containing popular and top rated {@link Movie}s.
  private MovieListAdapter mMovieListAdapter;
  // RecyclerView.Adapter containing favorite {@link Movie}s.
  private FavoriteMovieListAdapter mFavoriteMovieListAdapter;
  // Which page of a movie list from the server has to be downloaded
  private int mMoviePageToDownload = 1;
  // Saves current selected {@link MenuState} from Options Menu
  private int mMenuState = POPULAR_MOVIES;

  private SQLiteDatabase mDb;
  // An ItemTouchHelper for swiping movie items while in FAVORITE_MOVIES {@link MenuState}
  private ItemTouchHelper mMovieItemTouchHelper;

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
    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);

    updateValuesFromBundle(savedInstanceState);

    // Create a DB helper (this will create the DB if run for the first time)
    PopularMoviesDbHelper dbHelper = new PopularMoviesDbHelper(this);
    // Keep a reference to the mDb until paused or killed.
    mDb = dbHelper.getReadableDatabase();

    // RecyclerView
    // Use setHasFixedSize to improve performance if you know that changes in content do not
    // change the child layout size in the RecyclerView
    mMoviesRecyclerView.setHasFixedSize(true);
    mMoviesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
      @Override public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        if (dy > 0 && isLastItemDisplaying(recyclerView)) {
          if (mMenuState == POPULAR_MOVIES) {
            downloadAndShowPopularMovies(mDefaultLanguage, mDefaultCountry);
          } else if (mMenuState == TOP_RATED_MOVIES) {
            downloadAndShowTopRatedMovies(mDefaultLanguage, mDefaultCountry);
          }
        }
      }
    });

    // Create an item touch helper to handle swiping items off the list
    mMovieItemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

      @Override public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        //do nothing, we only care about swiping
        return false;
      }

      @Override public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipevDir) {
        //get the id of the item being swiped
        long id = (long) viewHolder.itemView.getTag();
        //remove from DB
        removeFavoriteMovie(id);
        //update the list
        loadAndShowFavoriteMovies();
      }
    });
    mMovieItemTouchHelper.attachToRecyclerView(null);

    // LayoutManager
    // This value should be true if you want to reverse your layout. Generally, this is only
    // true with horizontal lists that need to support a right-to-left layout.
    boolean shouldReverseLayout = false;
    RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, calculateNoOfColumns(), GridLayoutManager.VERTICAL, shouldReverseLayout);
    mMoviesRecyclerView.setLayoutManager(layoutManager);

    // Initiate the popular and top rated movielist adapter for RecyclerView
    mMovieListAdapter = new MovieListAdapter(new ArrayList<Movie>(0), mRequiredImageSize, this);
    mMovieListAdapter.setHasStableIds(true);

    // Initiate the favorite movielist adapter for RecyclerView
    mFavoriteMovieListAdapter = new FavoriteMovieListAdapter(mRequiredImageSize, this);
    //mFavoriteMovieListAdapter.setHasStableIds(true); //TODO: Can we make the Ids stable?

    // Initialize the loader with mMoviePageToDownload as the ID, null for the bundle, and this for the context
    getSupportLoaderManager().initLoader(mMoviePageToDownload, null, this);

    // Set title of this Activity depending on current {@link MenuState} and
    // get Movies depending on current {@link MenuState}
    if (mMenuState == POPULAR_MOVIES) {
      setTitle(R.string.action_popular);
      mMoviesRecyclerView.setAdapter(mMovieListAdapter);
      downloadAndShowPopularMovies(mDefaultLanguage, mDefaultCountry);
    } else if (mMenuState == TOP_RATED_MOVIES) {
      setTitle(R.string.action_top);
      mMoviesRecyclerView.setAdapter(mMovieListAdapter);
      downloadAndShowTopRatedMovies(mDefaultLanguage, mDefaultCountry);
    } else if (mMenuState == FAVORITE_MOVIES) {
      setTitle(R.string.action_favorite_show);
      mMoviesRecyclerView.setAdapter(mFavoriteMovieListAdapter);
      mMovieItemTouchHelper.attachToRecyclerView(mMoviesRecyclerView);
      loadAndShowFavoriteMovies();
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
    mMoviesRecyclerView.clearOnScrollListeners();
    mDb.close();
    super.onDestroy();
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.main, menu);
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    mMovieItemTouchHelper.attachToRecyclerView(null);
    switch (item.getItemId()) {
      // Load and show Popular Movies.
      case R.id.action_popular:
        mMenuState = POPULAR_MOVIES;
        setTitle(R.string.action_popular);
        mMovieListAdapter.clearMovieList();
        mMoviePageToDownload = 1;
        mMoviesRecyclerView.setAdapter(mMovieListAdapter);
        downloadAndShowPopularMovies(mDefaultLanguage, mDefaultCountry);
        return true;
      // Load and show To Rated Movies.
      case R.id.action_top:
        mMenuState = TOP_RATED_MOVIES;
        setTitle(R.string.action_top);
        mMovieListAdapter.clearMovieList();
        mMoviePageToDownload = 1;
        mMoviesRecyclerView.setAdapter(mMovieListAdapter);
        downloadAndShowTopRatedMovies(mDefaultLanguage, mDefaultCountry);
        return true;
      // Load local saved favorite Movies.
      case R.id.action_favorite_show:
        mMenuState = FAVORITE_MOVIES;
        setTitle(R.string.action_favorite_show);
        mMoviesRecyclerView.setAdapter(mFavoriteMovieListAdapter);
        mMovieItemTouchHelper.attachToRecyclerView(mMoviesRecyclerView);
        loadAndShowFavoriteMovies();
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
    // Get URL for popular Configuration Download and build Bundle for {@link CallApiTaskLoader}
    URL configurationUrl = NetworkUtils.buildConfigurationUrl();
    Bundle queryBundle = new Bundle();
    queryBundle.putSerializable(CallApiTaskLoader.EXTRA_QUERY_URL, configurationUrl);

    // Call getSupportLoaderManager and store it in a LoaderManager variable
    LoaderManager loaderManager = getSupportLoaderManager();
    // Get our Loader by calling getLoader and passing the ID we specified
    Loader<String> callApiTaskLoader = loaderManager.getLoader(0);
    // If the Loader was null, initialize it. Else, restart it.
    if (callApiTaskLoader == null) {
      loaderManager.initLoader(0, queryBundle, this);
    } else {
      loaderManager.restartLoader(0, queryBundle, this);
    }
  }

  /**
   * Get Popular Movies from themoviedb.org
   *
   * @param language The language requested.
   * @param region The region requested.
   */
  private void downloadAndShowPopularMovies(String language, String region) {
    // Get URL for popular Movies Download and build Bundle for {@link CallApiTaskLoader}
    URL popularMoviesUrl = NetworkUtils.buildPopularMoviesUrl(language, String.valueOf(mMoviePageToDownload), region);
    Bundle queryBundle = new Bundle();
    queryBundle.putSerializable(CallApiTaskLoader.EXTRA_QUERY_URL, popularMoviesUrl);

    // Call getSupportLoaderManager and store it in a LoaderManager variable
    LoaderManager loaderManager = getSupportLoaderManager();
    // Get our Loader by calling getLoader and passing the ID we specified
    Loader<String> callApiTaskLoader = loaderManager.getLoader(mMoviePageToDownload);
    // If the Loader was null, initialize it. Else, restart it.
    if (callApiTaskLoader == null) {
      loaderManager.initLoader(mMoviePageToDownload, queryBundle, this);
    } else {
      loaderManager.restartLoader(mMoviePageToDownload, queryBundle, this);
    }
  }

  /**
   * Get Top Rated Movies from themoviedb.org
   *
   * @param language The language requested.
   * @param region The region requested.
   */
  private void downloadAndShowTopRatedMovies(String language, String region) {
    // Get URL for top rated Movies Download and build Bundle for {@link CallApiTaskLoader}
    URL topRatedMoviesUrl = NetworkUtils.buildTopRatedMoviesUrl(language, String.valueOf(mMoviePageToDownload), region);
    Bundle queryBundle = new Bundle();
    queryBundle.putSerializable(CallApiTaskLoader.EXTRA_QUERY_URL, topRatedMoviesUrl);

    // Call getSupportLoaderManager and store it in a LoaderManager variable
    LoaderManager loaderManager = getSupportLoaderManager();
    // Get our Loader by calling getLoader and passing the ID we specified
    Loader<String> callApiTaskLoader = loaderManager.getLoader(mMoviePageToDownload);
    // If the Loader was null, initialize it. Else, restart it.
    if (callApiTaskLoader == null) {
      loaderManager.initLoader(mMoviePageToDownload, queryBundle, this);
    } else {
      loaderManager.restartLoader(mMoviePageToDownload, queryBundle, this);
    }
  }

  /**
   * Get Favorite Movies from local database
   */
  private void loadAndShowFavoriteMovies() {
    // Run the getAllFavoriteMovies function and store the result in a Cursor variable
    Cursor cursor = getAllFavoriteMovies();
    // Update the cursor in the adapter to trigger UI to display the new list
    mFavoriteMovieListAdapter.swapCursor(cursor);
  }

  /**
   * Query the mDb and get all favorite movies from the favoritelist table
   *
   * @return Cursor containing the list of favorite movies
   */
  private Cursor getAllFavoriteMovies() {
    // Call query on mDb passing in the table name and projection String [] order by COLUMN_TIMESTAMP
    return mDb.query(PopularMoviesContract.FavoritelistEntry.TABLE_NAME, null, null, null, null, null,
        PopularMoviesContract.FavoritelistEntry.COLUMN_TIMESTAMP);
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
   * Parse jsonData and show in Views.
   *
   * @param jsonData from onLoadFinished of {@link CallApiTaskLoader}.
   */
  private void parseAndShowJsonData(String jsonData) {
    List<Movie> movieList = new ArrayList<>();
    try {
      JSONObject popular = new JSONObject(jsonData);
      JSONArray results = popular.getJSONArray("results");
      int i = 0;
      while (results != null && !results.isNull(i)) {
        JSONObject result = results.getJSONObject(i);
        if (result != null) {
          Long movieId = result.getLong("id");
          String imageUrl = result.getString("poster_path");
          Movie movie = new Movie(movieId, imageUrl);
          movieList.add(movie);
        }
        i++;
      }
      if (mMoviesRecyclerView != null) {
        // Add new downloaded Movies to adapter
        mMovieListAdapter.addMovieList(movieList);
      }
    } catch (JSONException e) {
      e.printStackTrace();
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
      if (lastVisibleItemPosition != RecyclerView.NO_POSITION && lastVisibleItemPosition == recyclerView.getAdapter().getItemCount() - 1) {
        return true;
      }
    }
    return false;
  }

  private void updateValuesFromBundle(Bundle savedInstanceState) {
    if (savedInstanceState != null) {
      if (savedInstanceState.keySet().contains(STATE_MENU_STATE)) {
        mMenuState = savedInstanceState.getInt(STATE_MENU_STATE, POPULAR_MOVIES);
      }
    }
  }

  /**
   * Removes the record with the specified id
   *
   * @param id the DB id to be removed
   * @return True: if removed successfully, False: if failed
   */
  private boolean removeFavoriteMovie(long id) {
    return mDb.delete(PopularMoviesContract.FavoritelistEntry.TABLE_NAME, PopularMoviesContract.FavoritelistEntry._ID + "=" + id, null) > 0;
  }

  /**
   * This is where we receive our callback from
   * {@link MovieListAdapter.ListItemClickListener}
   *
   * This callback is invoked when you click on an item in the list.
   *
   * @param movie {@link Movie} in the list that was clicked.
   */
  @Override public void onListItemClick(Movie movie) {
    // Show Movie Detail Activity
    Intent intent = new Intent(this, MovieDetailActivity.class);
    intent.putExtra(EXTRA_MOVIE_ID, movie.getId());
    startActivity(intent);
  }

  /**
   * This is where we receive our callback from
   * {@link FavoriteMovieListAdapter.ListItemClickListener}
   *
   * This callback is invoked when you click on an item in the list.
   *
   * @param id of movie in the list that was clicked.
   */
  @Override public void onListItemClick(long id) {
    showMessage("Movie DB Id: " + String.valueOf(id));
  }

  /**
   * Below this point are the three {@link LoaderManager.LoaderCallbacks} methods
   **/

  /**
   * This is called when a new Loader needs to be created.
   *
   * @param id The ID whose loader is to be created.
   * @param args Any arguments supplied by the caller.
   * @return Return a new Loader instance that is ready to start loading.
   */
  @Override public Loader<String> onCreateLoader(int id, Bundle args) {
    return new CallApiTaskLoader(this, args);
  }

  @Override public void onLoadFinished(Loader<String> loader, String data) {
    // When we finish loading, we want to hide the loading indicator from the user.
    //mLoadingIndicator.setVisibility(View.INVISIBLE);

    // If the results are null, we assume an error has occurred.
    if (data == null) {
      showErrorMessage(CallApiTaskLoader.NULL);
    } else {
      switch (data) {
        case CallApiTaskLoader.API_ERROR:
          showErrorMessage(CallApiTaskLoader.API_ERROR);
          break;
        case CallApiTaskLoader.OFFLINE:
          showErrorMessage(CallApiTaskLoader.OFFLINE);
          break;
        case "":
          Log.d(TAG, "Empty response from Server");
          break;
        default:
          // Here we succesfully get data from server. So next time we can download the following movie page by incrementing mMoviePageToDownload.
          parseAndShowJsonData(data);
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
  @Override public void onLoaderReset(Loader<String> loader) {
  }

  /**
   * Logical type which value must be one of explicitly named constants: POPULAR_MOVIES, TOP_RATED_MOVIES or FAVORITE_MOVIES
   **/
  @Retention(RetentionPolicy.CLASS) @IntDef({ POPULAR_MOVIES, TOP_RATED_MOVIES, FAVORITE_MOVIES }) public @interface MenuState {
  }
}
