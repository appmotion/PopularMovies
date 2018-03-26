package de.appmotion.popularmovies;

import android.content.ContentValues;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import de.appmotion.popularmovies.data.source.local.MovieContract;
import de.appmotion.popularmovies.data.source.remote.NetworkLoader;
import de.appmotion.popularmovies.data.source.remote.NetworkUtils;
import de.appmotion.popularmovies.databinding.ActivityMovieDetailBinding;
import java.net.URL;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Display the details for a movie.
 */
public class MovieDetailActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<String> {

  // Name of the 'Movie Id data' sent via Intent to this Activity
  public final static String EXTRA_MOVIE_ID = BuildConfig.APPLICATION_ID + ".movie_id";
  // Constant for logging
  private static final String TAG = MovieDetailActivity.class.getSimpleName();
  // Suggestions to Make Your Project Stand Out
  //TODO: Implement sharing functionality to allow the user to share the first trailer’s YouTube URL from the movie details screen.
  //TODO: Extend the favorites ContentProvider to store the movie poster, synopsis, user rating, and release date, and display them even when offline.
  private static final String TRAILER_SHARE_HASHTAG = " #PopularMovieApp";

  // This number will uniquely identify a NetworkLoader for loading movie detail data from themoviedb.org.
  private static final int NETWORK_LOADER_MOVIE_DETAIL = 1;

  // The Id of this movie
  private long mMovieId;
  // The tile of this movie
  private String mTitle;
  // The image url of this movie
  private String mImageUrl;

  private ActivityMovieDetailBinding mDetailBinding;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mDetailBinding = DataBindingUtil.setContentView(this, R.layout.activity_movie_detail);

    // add back arrow to toolbar
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
      getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    // Get Movie Id from Intent, then download Movie Details.
    if (getIntent() != null) {
      mMovieId = getIntent().getLongExtra(EXTRA_MOVIE_ID, 0L);
      if (mMovieId == 0) {
        showMessage(getString(R.string.error_loading_movie_detail));
      } else {
        downloadMovieDetails(mMovieId, mDefaultLanguage);
      }
    } else {
      showMessage(getString(R.string.error_loading_movie_detail));
    }
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.detail, menu);
    MenuItem menuItem = menu.findItem(R.id.action_share);
    menuItem.setIntent(createShareTrailerIntent());
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        finish();
        return true;
      // Add the currently shown Movie to favorite movie table in DB.
      case R.id.action_favorite_add:
        if (mMovieId != 0L && mTitle != null && mTitle.length() > 0) {
          Uri uri = addFavoriteMovie(mMovieId, mTitle, mImageUrl);
          if (uri != null) {
            // Movie successfuly added to table
            showMessage(getString(R.string.adding_movie_to_favoritelist));
          }
        }
        // Error: Movie data is empty and so it cannot be added to table
        else {
          showMessage(getString(R.string.error_adding_empty_movie_to_favoritelist));
        }
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  /**
   * Get Detail of a Movie from themoviedb.org
   *
   * @param movieId The ID of Movie requested.
   * @param language The language requested.
   */
  private void downloadMovieDetails(long movieId, String language) {
    // Get URL for Movie details Download and build Bundle for {@link NetworkLoader}
    URL movieDetailUrl = NetworkUtils.buildMovieDetailUrl(movieId, language);
    Bundle queryBundle = new Bundle();
    queryBundle.putSerializable(NetworkLoader.EXTRA_QUERY_URL, movieDetailUrl);

    // Call getSupportLoaderManager and store it in a LoaderManager variable
    LoaderManager loaderManager = getSupportLoaderManager();
    // Get our Loader by calling getLoader and passing the ID we specified
    Loader<String> callApiTaskLoader = loaderManager.getLoader(NETWORK_LOADER_MOVIE_DETAIL);
    // If the Loader was null, initialize it. Else, restart it.
    if (callApiTaskLoader == null) {
      loaderManager.initLoader(NETWORK_LOADER_MOVIE_DETAIL, queryBundle, this);
    } else {
      loaderManager.restartLoader(NETWORK_LOADER_MOVIE_DETAIL, queryBundle, this);
    }
  }

  /**
   * Called when MOVIE_DETAIL_LOADER finished in onLoadFinished().
   * Parse jsonData and show in Views.
   *
   * @param jsonData from onLoadFinished of {@link NetworkLoader}.
   */
  private void parseAndShowJsonData(String jsonData) {
    try {
      JSONObject movieDetail = new JSONObject(jsonData);
      mTitle = movieDetail.getString("title");
      String year = movieDetail.getString("release_date");
      String duration = movieDetail.getString("runtime");
      String rating = movieDetail.getString("vote_average");
      String overview = movieDetail.getString("overview");
      mImageUrl = movieDetail.getString("poster_path");

      // Ttile
      mDetailBinding.tvMovieTitle.setText(mTitle);
      // Year
      mDetailBinding.tvMovieYear.setText(year);
      // Duration
      duration = duration + "min";
      mDetailBinding.tvMovieDuration.setText(duration);
      // Rating
      rating = rating + " / 10";
      mDetailBinding.tvMovieRating.setText(rating);
      // Overview
      mDetailBinding.tvMovieOverview.setText(overview);

      // Load Movie Image
      Picasso.with(this)
          .load(NetworkUtils.buildMovieImageUri(mRequiredImageSize, mImageUrl))
          .placeholder(android.R.drawable.screen_background_light_transparent)
          .error(R.drawable.movie_empty)
          .into(mDetailBinding.ivMovieImage, new Callback() {
            @Override public void onSuccess() {
            }

            @Override public void onError() {
            }
          });
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  /**
   * Insert a movie to {@link MovieContract.FavoriteMovieEntry} with its id, title and imageUrl.
   *
   * @param movieId movies's id
   * @param title movie's title
   * @param imageUrl url to an image
   * @return {@link Uri} of new record added
   */
  private Uri addFavoriteMovie(long movieId, @NonNull String title, String imageUrl) {
    ContentValues cv = new ContentValues();
    cv.put(MovieContract.FavoriteMovieEntry.COLUMN_MOVIE_ID, movieId);
    cv.put(MovieContract.FavoriteMovieEntry.COLUMN_MOVIE_TITLE, title);
    cv.put(MovieContract.FavoriteMovieEntry.COLUMN_MOVIE_IMAGE_URL, imageUrl);

    // Insert the content values via a ContentResolver
    return getContentResolver().insert(MovieContract.FavoriteMovieEntry.CONTENT_URI, cv);
  }

  /**
   * Uses the ShareCompat Intent builder to create our Trailer intent for sharing. We set the
   * type of content that we are sharing (just regular text), the text itself, and we return the
   * newly created Intent.
   *
   * @return The Intent to use to start our share.
   */
  private Intent createShareTrailerIntent() {
    Intent shareIntent =
        ShareCompat.IntentBuilder.from(this).setType("text/plain").setText("mTrailerUrl" + TRAILER_SHARE_HASHTAG).getIntent();
    return shareIntent;
  }

  /**
   * Below this point are {@link LoaderManager.LoaderCallbacks} methods
   **/

  @NonNull @Override public Loader<String> onCreateLoader(int id, Bundle args) {
    return new NetworkLoader(this, args);
  }

  @Override public void onLoadFinished(@NonNull Loader<String> loader, String data) {
    // When we finish loading, we want to hide the loading indicator from the user.
    //mLoadingIndicator.setVisibility(View.INVISIBLE);

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
          parseAndShowJsonData(data);
          break;
      }
    }
  }

  // Override onLoaderReset as it is part of the interface we implement, but don't do anything in this method
  @Override public void onLoaderReset(@NonNull Loader<String> loader) {
  }
}
