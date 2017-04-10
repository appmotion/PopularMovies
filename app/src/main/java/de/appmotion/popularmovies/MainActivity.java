package de.appmotion.popularmovies;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import de.appmotion.popularmovies.dto.Movie;
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
    implements MoviesRecyclerViewAdapter.ListItemClickListener, LoaderManager.LoaderCallbacks<String> {

  // Name of the 'Movie Id data' sent via Intent to {@link MovieDetailActivity}
  public final static String EXTRA_MOVIE_ID = BuildConfig.APPLICATION_ID + ".movie_id";

  // Define {@link MenuState} Types
  private static final int POPULAR_MOVIES = 0;
  private static final int TOP_RATED_MOVIES = 1;
  // Save {@link MenuState} via onSaveInstanceState
  private static final String STATE_MENU_STATE = "menu_state";
  // The About Dialog
  private AlertDialog mAboutDialog;
  // RecyclerView which shows Movies
  private RecyclerView mMoviesRecyclerView;
  // RecyclerView.Adapter containing {@link Movie}s.
  private MoviesRecyclerViewAdapter mMoviesRecyclerViewAdapter;
  // Saves last downloaded movie page
  private int mLastDownloadedMoviePage = 1;
  // Saves current selected {@link MenuState} from Options Menu
  private int mMenuState = POPULAR_MOVIES;

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

    updateValuesFromBundle(savedInstanceState);

    // Set title of this Activity depending on current {@link MenuState}
    if (mMenuState == POPULAR_MOVIES) {
      setTitle(R.string.popular_movies);
    } else if (mMenuState == TOP_RATED_MOVIES) {
      setTitle(R.string.top_rated);
    }

    // RecyclerView
    mMoviesRecyclerView = (RecyclerView) findViewById(android.R.id.list);
    mMoviesRecyclerView.setHasFixedSize(true);
    mMoviesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
      @Override public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        if (dy > 0 && isLastItemDisplaying(recyclerView)) {
          downloadMovies(mMenuState, "en-US", "US");
        }
      }
    });

    // LayoutManager
    RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, calculateNoOfColumns(), GridLayoutManager.VERTICAL, false);
    mMoviesRecyclerView.setLayoutManager(layoutManager);

    // Set the adapter for RecyclerView
    mMoviesRecyclerViewAdapter = new MoviesRecyclerViewAdapter(new ArrayList<Movie>(0), mRequiredImageSize, this);
    mMoviesRecyclerViewAdapter.setHasStableIds(true);
    mMoviesRecyclerView.setAdapter(mMoviesRecyclerViewAdapter);

    // Initialize the loader with CallApiTaskLoader.MOVIE_API_LOADER as the ID, null for the bundle, and this for the context
    getSupportLoaderManager().initLoader(CallApiTaskLoader.MOVIE_API_LOADER, null, this);

    downloadMovies(mMenuState, "en-US", "US");
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
    super.onDestroy();
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu, menu);
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      // Load and show Popular Movies.
      case R.id.popular:
        setTitle(R.string.popular_movies);
        mMoviesRecyclerViewAdapter.clearMovieList();
        mLastDownloadedMoviePage = 1;
        downloadMovies(POPULAR_MOVIES, "en-US", "US");
        return true;
      // Load and show To Rated Movies.
      case R.id.top:
        setTitle(R.string.top_rated);
        mMoviesRecyclerViewAdapter.clearMovieList();
        mLastDownloadedMoviePage = 1;
        downloadMovies(TOP_RATED_MOVIES, "en-US", "US");
        return true;
      // Show About Dialog.
      case R.id.about:
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
    Loader<String> callApiTaskLoader = loaderManager.getLoader(CallApiTaskLoader.MOVIE_API_LOADER);
    // If the Loader was null, initialize it. Else, restart it.
    if (callApiTaskLoader == null) {
      loaderManager.initLoader(CallApiTaskLoader.MOVIE_API_LOADER, queryBundle, this);
    } else {
      loaderManager.restartLoader(CallApiTaskLoader.MOVIE_API_LOADER, queryBundle, this);
    }
  }

  /**
   * Get Popular or Top Rated Movies from themoviedb.org
   */
  private void downloadMovies(@MenuState int menuState, String language, String region) {
    mMenuState = menuState;
    if (menuState == POPULAR_MOVIES) {
      downloadPopularMovies(language, mLastDownloadedMoviePage++, region);
    } else if (menuState == TOP_RATED_MOVIES) {
      downloadTopRatedMovies(language, mLastDownloadedMoviePage++, region);
    }
  }

  /**
   * Get Popular Movies from themoviedb.org
   *
   * @param language The language requested.
   * @param page The page requested.
   * @param region The region requested.
   */
  private void downloadPopularMovies(String language, int page, String region) {
    // Get URL for popular Movies Download and build Bundle for {@link CallApiTaskLoader}
    URL popularMoviesUrl = NetworkUtils.buildPopularMoviesUrl(language, String.valueOf(page), region);
    Bundle queryBundle = new Bundle();
    queryBundle.putSerializable(CallApiTaskLoader.EXTRA_QUERY_URL, popularMoviesUrl);

    // Call getSupportLoaderManager and store it in a LoaderManager variable
    LoaderManager loaderManager = getSupportLoaderManager();
    // Get our Loader by calling getLoader and passing the ID we specified
    Loader<String> callApiTaskLoader = loaderManager.getLoader(CallApiTaskLoader.MOVIE_API_LOADER);
    // If the Loader was null, initialize it. Else, restart it.
    if (callApiTaskLoader == null) {
      loaderManager.initLoader(CallApiTaskLoader.MOVIE_API_LOADER, queryBundle, this);
    } else {
      loaderManager.restartLoader(CallApiTaskLoader.MOVIE_API_LOADER, queryBundle, this);
    }
  }

  /**
   * Get Top Rated Movies from themoviedb.org
   *
   * @param language The language requested.
   * @param page The page requested.
   * @param region The region requested.
   */
  private void downloadTopRatedMovies(String language, int page, String region) {
    // Get URL for top rated Movies Download and build Bundle for {@link CallApiTaskLoader}
    URL topRatedMoviesUrl = NetworkUtils.buildTopRatedMoviesUrl(language, String.valueOf(page), region);
    Bundle queryBundle = new Bundle();
    queryBundle.putSerializable(CallApiTaskLoader.EXTRA_QUERY_URL, topRatedMoviesUrl);

    // Call getSupportLoaderManager and store it in a LoaderManager variable
    LoaderManager loaderManager = getSupportLoaderManager();
    // Get our Loader by calling getLoader and passing the ID we specified
    Loader<String> callApiTaskLoader = loaderManager.getLoader(CallApiTaskLoader.MOVIE_API_LOADER);
    // If the Loader was null, initialize it. Else, restart it.
    if (callApiTaskLoader == null) {
      loaderManager.initLoader(CallApiTaskLoader.MOVIE_API_LOADER, queryBundle, this);
    } else {
      loaderManager.restartLoader(CallApiTaskLoader.MOVIE_API_LOADER, queryBundle, this);
    }
  }

  private void showAboutDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    mAboutDialog = builder.setCancelable(true)
        .setMessage(R.string.tmdb_notice)
        .setTitle(R.string.about)
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
   * Called when CallApiTaskLoader.MOVIE_API_LOADER finished in onLoadFinished().
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
          String imagePath = result.getString("poster_path");
          Movie movie = new Movie(movieId, imagePath);
          movieList.add(movie);
        }
        i++;
      }
      if (mMoviesRecyclerView != null) {
        // Add new downloaded Movies to adapter
        mMoviesRecyclerViewAdapter.addMovieList(movieList);
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
   * This is where we receive our callback from
   * {@link MoviesRecyclerViewAdapter.ListItemClickListener}
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
          break;
        default:
          parseAndShowJsonData(data);
          break;
      }
    }
  }

  // Override onLoaderReset as it is part of the interface we implement, but don't do anything in this method
  @Override public void onLoaderReset(Loader<String> loader) {
  }

  @Retention(RetentionPolicy.CLASS) @IntDef({ POPULAR_MOVIES, TOP_RATED_MOVIES }) public @interface MenuState {
  }
}
