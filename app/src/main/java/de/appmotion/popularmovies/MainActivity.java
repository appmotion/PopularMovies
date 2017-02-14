package de.appmotion.popularmovies;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import de.appmotion.popularmovies.dto.Movie;
import de.appmotion.popularmovies.utilities.CallApiTask;
import de.appmotion.popularmovies.utilities.NetworkUtils;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Display Movies via a grid of their corresponding movie poster thumbnails.
 */
public class MainActivity extends BaseActivity {

  // Name of the 'Movie Id data' sent via Intent to {@link MovieDetailActivity}
  public final static String EXTRA_MOVIE_ID = BuildConfig.APPLICATION_ID + ".movie_id";
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

  // Saves current selected entry from Options Menu
  private MenuState mMenuState = MenuState.POPULAR_MOVIES;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    updateValuesFromBundle(savedInstanceState);

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
    mMoviesRecyclerViewAdapter = new MoviesRecyclerViewAdapter(this);
    mMoviesRecyclerViewAdapter.setHasStableIds(true);
    mMoviesRecyclerView.setAdapter(mMoviesRecyclerViewAdapter);

    downloadMovies(mMenuState, "en-US", "US");
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putSerializable(STATE_MENU_STATE, mMenuState);
  }

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
        downloadMovies(MenuState.POPULAR_MOVIES, "en-US", "US");
        return true;
      // Load and show To Rated Movies.
      case R.id.top:
        setTitle(R.string.top_rated);
        mMoviesRecyclerViewAdapter.clearMovieList();
        mLastDownloadedMoviePage = 1;
        downloadMovies(MenuState.TOP_RATED_MOVIES, "en-US", "US");
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
    URL configurationUrl = NetworkUtils.buildConfigurationUrl();
    new CallApiTask(this).execute(configurationUrl);
  }

  /**
   * Get Popular or Top Rated Movies from themoviedb.org
   */
  private void downloadMovies(MenuState menuState, String language, String region) {
    mMenuState = menuState;
    if (menuState.equals(MenuState.POPULAR_MOVIES)) {
      downloadPopularMovies(language, ++mLastDownloadedMoviePage, region);
    } else if (menuState.equals(MenuState.TOP_RATED_MOVIES)) {
      downloadTopRatedMovies(language, ++mLastDownloadedMoviePage, region);
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
    URL popularMoviesUrl = NetworkUtils.buildPopularMoviesUrl(language, String.valueOf(page), region);
    new CallApiTask(this).execute(popularMoviesUrl);
  }

  /**
   * Get Top Rated Movies from themoviedb.org
   *
   * @param language The language requested.
   * @param page The page requested.
   * @param region The region requested.
   */
  private void downloadTopRatedMovies(String language, int page, String region) {
    URL topRatedMoviesUrl = NetworkUtils.buildTopRatedMoviesUrl(language, String.valueOf(page), region);
    new CallApiTask(this).execute(topRatedMoviesUrl);
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
   * Called from onPostExecute of {@link CallApiTask}.
   * Parse jsonData and show in Views.
   *
   * @param jsonData from onPostExecute of {@link CallApiTask}.
   */
  @Override public void parseAndShowJsonData(String jsonData) {
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
        mMenuState = (MenuState) savedInstanceState.getSerializable(STATE_MENU_STATE);
      }
    }
  }

  private enum MenuState {
    POPULAR_MOVIES,
    TOP_RATED_MOVIES
  }
}
