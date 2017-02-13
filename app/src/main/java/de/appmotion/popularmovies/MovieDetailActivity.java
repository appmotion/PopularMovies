package de.appmotion.popularmovies;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import de.appmotion.popularmovies.utilities.CallApiTask;
import de.appmotion.popularmovies.utilities.NetworkUtils;
import java.net.URL;
import org.json.JSONException;
import org.json.JSONObject;

public class MovieDetailActivity extends BaseActivity {

  private TextView mMovieTitle;
  private ImageView mMovieImage;
  private TextView mMovieYear;
  private TextView mMovieDuration;
  private TextView mMovieRating;
  private TextView mMovieOverview;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setTitle(R.string.movie_detail);
    setContentView(R.layout.activity_movie_detail);

    // add back arrow to toolbar
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
      getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    // Views
    mMovieTitle = (TextView) findViewById(R.id.tv_movie_title);
    mMovieImage = (ImageView) findViewById(R.id.iv_movie_image);
    mMovieYear = (TextView) findViewById(R.id.tv_movie_year);
    mMovieDuration = (TextView) findViewById(R.id.tv_movie_duration);
    mMovieRating = (TextView) findViewById(R.id.tv_movie_rating);
    mMovieOverview = (TextView) findViewById(R.id.tv_movie_overview);

    if (getIntent() != null) {
      long movieId = getIntent().getLongExtra(MainActivity.EXTRA_MOVIE_ID, 0L);
      if (movieId == 0) {
        Toast.makeText(this, "Sorry, no Movie Details", Toast.LENGTH_LONG).show();
      } else {
        downloadMovieDetails(movieId, "en-US");
      }
    } else {
      Toast.makeText(this, "Sorry, no Movie Details", Toast.LENGTH_LONG).show();
    }
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      finish();
    }
    return super.onOptionsItemSelected(item);
  }

  /**
   * Get Detail of a Movie from themoviedb.org
   *
   * @param movieId The ID of Movie requested.
   * @param language The language requested.
   */
  private void downloadMovieDetails(long movieId, String language) {
    URL movieDetailUrl = NetworkUtils.buildMovieDetailUrl(movieId, language);
    new CallApiTask(this).execute(movieDetailUrl);
  }

  /**
   * Called from onPostExecute of {@link CallApiTask}.
   * Parse jsonData and show in Views.
   *
   * @param jsonData from onPostExecute of {@link CallApiTask}.
   */
  @Override public void parseAndShowJsonData(String jsonData) {
    try {
      JSONObject movieDetail = new JSONObject(jsonData);
      String title = movieDetail.getString("title");
      String year = movieDetail.getString("release_date");
      String duration = movieDetail.getString("runtime");
      String rating = movieDetail.getString("vote_average");
      String overview = movieDetail.getString("overview");
      String imagePath = movieDetail.getString("poster_path");

      // Ttile
      mMovieTitle.setText(title);
      // Year
      mMovieYear.setText(year);
      // Duration
      mMovieDuration.setText(duration);
      // Rating
      rating = rating + " / 10";
      mMovieRating.setText(rating);
      // Overview
      mMovieOverview.setText(overview);

      // Load Movie Image
      Picasso.with(this)
          .load(NetworkUtils.buildMovieImageUri(mRequiredImageSize, imagePath))
          .placeholder(android.R.drawable.screen_background_light_transparent)
          .error(R.drawable.movie_empty)
          .into(mMovieImage, new Callback() {
            @Override public void onSuccess() {
            }

            @Override public void onError() {
            }
          });
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }
}
