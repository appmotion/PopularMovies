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
import android.widget.GridView;
import android.widget.Toast;
import de.appmotion.popularmovies.dto.Movie;
import de.appmotion.popularmovies.utilities.NetworkUtils;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

  private AlertDialog mAboutDialog;

  private RecyclerView mMoviesRecyclerView;
  private PopularMoviesAdapter mPopularMoviesAdapter;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // RecyclerView
    mMoviesRecyclerView = (RecyclerView) findViewById(android.R.id.list);
    mMoviesRecyclerView.setHasFixedSize(true);

    // LayoutManager
    RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false);
    mMoviesRecyclerView.setLayoutManager(layoutManager);

    // Set the adapter
    mPopularMoviesAdapter = new PopularMoviesAdapter(this);
    mPopularMoviesAdapter.setHasStableIds(true);
    mMoviesRecyclerView.setAdapter(mPopularMoviesAdapter);

    downloadPopularMovies();
  }

  @Override protected void onDestroy() {
    dismissDialog(mAboutDialog);
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
      case R.id.about:
        showAboutDialog();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  /**
   * Get Popular Movies from themoviedb.org
   */
  private void downloadPopularMovies() {
    URL popularMoviesUrl = NetworkUtils.buildPopularMoviesUrl("de-DE", "1", "US");
    new GetPopularMoviesTask().execute(popularMoviesUrl);
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
      mPopularMoviesAdapter.replaceMovieList(movieList);
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


  public class GetPopularMoviesTask extends AsyncTask<URL, Void, String> {

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      //mLoadingIndicator.setVisibility(View.VISIBLE);
    }

    @Override
    protected String doInBackground(URL... params) {
      URL popularMoviesUrl = params[0];
      String results = null;
      try {
        MediaType mediaType = MediaType.parse("application/octet-stream");
        RequestBody body = RequestBody.create(mediaType, "{}");
        Request request =
            new Request.Builder().url(popularMoviesUrl)
                .tag("getPopularMovies")
                .get()
                .build();

        Response response = App.getInstance().getOkHttpClient().newCall(request).execute();
        if (response.code() == 200) {
          Scanner scanner = new Scanner(response.body().byteStream());
          scanner.useDelimiter("\\A");
          boolean hasInput = scanner.hasNext();
          if (hasInput) {
            return scanner.next();
          } else {
            return null;
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
      return results;
    }

    @Override
    protected void onPostExecute(String results) {
      //mLoadingIndicator.setVisibility(View.INVISIBLE);
      if (results != null && !results.equals("")) {
        showJsonDataView(results);
      } else {
        showErrorMessage();
      }
    }
  }
}
