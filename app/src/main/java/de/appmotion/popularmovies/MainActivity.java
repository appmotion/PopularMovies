package de.appmotion.popularmovies;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import de.appmotion.popularmovies.dto.Movie;
import de.appmotion.popularmovies.utilities.NetworkUtils;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

  private static final String STATE_MENU_STATE = "menu_state";

  private AlertDialog mAboutDialog;

  private RecyclerView mMoviesRecyclerView;
  private MoviesRecyclerViewAdapter mMoviesRecyclerViewAdapter;

  private int mCurrentMoviePage = 1;
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
        if (isLastItemDisplaying(recyclerView)) {
          downloadMovies(mMenuState, "de-DE", "US");
        }
      }
    });

    // LayoutManager
    RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false);
    mMoviesRecyclerView.setLayoutManager(layoutManager);

    // Set the adapter
    mMoviesRecyclerViewAdapter = new MoviesRecyclerViewAdapter(this);
    mMoviesRecyclerViewAdapter.setHasStableIds(true);
    mMoviesRecyclerView.setAdapter(mMoviesRecyclerViewAdapter);

    downloadMovies(mMenuState, "de-DE", "US");
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
    // Handle item selection
    switch (item.getItemId()) {
      case R.id.popular:
        mMoviesRecyclerViewAdapter.clearMovieList();
        mCurrentMoviePage = 1;
        downloadMovies(MenuState.POPULAR_MOVIES, "de-DE", "US");
        return true;
      case R.id.top:
        mMoviesRecyclerViewAdapter.clearMovieList();
        mCurrentMoviePage = 1;
        downloadMovies(MenuState.TOP_RATED_MOVIES, "de-DE", "US");
        return true;
      case R.id.about:
        showAboutDialog();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void downloadConfiguration() {
    URL configurationUrl = NetworkUtils.buildConfigurationUrl();
    new CallApiTask().execute(configurationUrl);
  }

  /**
   * Get Popular or Top Rated Movies from themoviedb.org
   */
  private void downloadMovies(MenuState menuState, String language, String region) {
    mMenuState = menuState;
    if (menuState.equals(MenuState.POPULAR_MOVIES)) {
      downloadPopularMovies(language, ++mCurrentMoviePage, region);
    } else if (menuState.equals(MenuState.TOP_RATED_MOVIES)) {
      downloadTopRatedMovies(language, ++mCurrentMoviePage, region);
    }
  }

  /**
   * Get Popular Movies from themoviedb.org
   */
  private void downloadPopularMovies(String language, int page, String region) {
    URL popularMoviesUrl = NetworkUtils.buildPopularMoviesUrl(language, String.valueOf(page), region);
    new CallApiTask().execute(popularMoviesUrl);
  }

  /**
   * Get Top Rated Movies from themoviedb.org
   */
  private void downloadTopRatedMovies(String language, int page, String region) {
    URL topRatedMoviesUrl = NetworkUtils.buildTopRatedMoviesUrl(language, String.valueOf(page), region);
    new CallApiTask().execute(topRatedMoviesUrl);
  }

  private void showAboutDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    mAboutDialog = builder.setCancelable(true)
        .setMessage(R.string.tmdb_notice)
        .setTitle(R.string.about)
        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
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
   * This method will make the View for the JSON data visible and
   * hide the error message.
   * <p>
   * Since it is okay to redundantly set the visibility of a View, we don't
   * need to check whether each view is currently visible or invisible.
   */
  private void showJsonDataView(String jsonData) {
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
    // First, make sure the error is invisible
    //mErrorMessageDisplay.setVisibility(View.INVISIBLE);
    // Then, make sure the JSON data is visible
    //mSearchResultsTextView.setVisibility(View.VISIBLE);
  }

  /**
   * This method will make the error message visible and hide the JSON
   * View.
   * <p>
   * Since it is okay to redundantly set the visibility of a View, we don't
   * need to check whether each view is currently visible or invisible.
   */
  private void showErrorMessage() {
    Toast.makeText(this, "Network Error occured!", Toast.LENGTH_LONG).show();
    // First, hide the currently visible data
    //mSearchResultsTextView.setVisibility(View.INVISIBLE);
    // Then, show the error
    //mErrorMessageDisplay.setVisibility(View.VISIBLE);
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

  public class CallApiTask extends AsyncTask<URL, Void, String> {

    @Override protected void onPreExecute() {
      super.onPreExecute();
      //mLoadingIndicator.setVisibility(View.VISIBLE);
    }

    @Override protected String doInBackground(URL... params) {
      URL url = params[0];
      try {
        Request request = new Request.Builder().url(url).get().build();

        Response response = App.getInstance().getOkHttpClient().newCall(request).execute();
        if (response.code() == 200) {
          Scanner scanner = new Scanner(response.body().byteStream());
          scanner.useDelimiter("\\A");
          boolean hasInput = scanner.hasNext();
          if (hasInput) {
            return scanner.next();
          } else {
            return "noDataAvailable";
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
      return null;
    }

    @Override protected void onPostExecute(String results) {
      //mLoadingIndicator.setVisibility(View.INVISIBLE);
      if (results != null && !results.equals("") && !results.equals("noDataAvailable")) {
        showJsonDataView(results);
      } else if (results == null || !results.equals("noDataAvailable")) {
        showErrorMessage();
      }
    }
  }
}
