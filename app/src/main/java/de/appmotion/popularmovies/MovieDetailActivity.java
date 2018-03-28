package de.appmotion.popularmovies;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import de.appmotion.popularmovies.data.Movie;
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
public class MovieDetailActivity extends BaseActivity {

  // Name of the 'Movie Id data' sent via Intent to this Activity
  public final static String EXTRA_MOVIE_OBJECT = BuildConfig.APPLICATION_ID + ".movie_object";
  // Constant for logging
  private static final String TAG = MovieDetailActivity.class.getSimpleName();
  // Suggestions to Make Your Project Stand Out
  //TODO: Implement sharing functionality to allow the user to share the first trailer’s YouTube URL from the movie details screen.
  //TODO: Extend the favorites ContentProvider to store the movie poster, synopsis, user rating, and release date, and display them even when offline.
  private static final String TRAILER_SHARE_HASHTAG = " #PopularMovieApp";

  // This number will uniquely identify a NetworkLoader for loading movie detail data from themoviedb.org.
  private static final int NETWORK_LOADER_MOVIE_DETAIL = 1;

  // Callback for {@link NetworkLoader}
  private LoaderManager.LoaderCallbacks<String> mNetworkLoaderCallback;

  // The Movie which will be shown
  private Movie mMovie;

  private ActivityMovieDetailBinding mDetailBinding;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mDetailBinding = DataBindingUtil.setContentView(this, R.layout.activity_movie_detail);

    // add back arrow to toolbar
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
      getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    // Get Movie from Intent
    if (getIntent() != null) {
      mMovie = getIntent().getParcelableExtra(EXTRA_MOVIE_OBJECT);
      if (mMovie == null) {
        showMessage(getString(R.string.error_showing_movie_detail));
      }
    } else {
      showMessage(getString(R.string.error_showing_movie_detail));
    }

    showMovieData(mMovie, null);

    // Initiate Callbacks for the Loader
    mNetworkLoaderCallback = initNetworkLoaderCallback();

    // Loader for Movie Details
    getSupportLoaderManager().initLoader(NETWORK_LOADER_MOVIE_DETAIL, null, mNetworkLoaderCallback);
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
        if (mMovie != null) {
          Uri uri = addFavoriteMovie(mMovie);
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
   * Called when NETWORK_LOADER_MOVIE_DETAIL finished in onLoadFinished().
   * Parse jsonData and show in Views.
   *
   * @param jsonData from onLoadFinished of {@link NetworkLoader}.
   */
  private void parseJson(String jsonData) {
    try {
      JSONObject movieDetail = new JSONObject(jsonData);
      String runtime = movieDetail.getString("runtime");

      showMovieData(mMovie, runtime);
    } catch (JSONException e) {
      Log.e(TAG, "Parse Movie detail JSON error: ", e);
    }
  }

  private void showMovieData(Movie movie, @Nullable String runtime) {
    // Ttile
    mDetailBinding.tvMovieTitle.setText(movie.getTitle());
    // Year
    mDetailBinding.tvMovieYear.setText(movie.getReleaseDate());
    // Runtime
    if (runtime != null) {
      runtime = runtime + "min";
      mDetailBinding.tvMovieRuntime.setText(runtime);
    } else {
      mDetailBinding.tvMovieRuntime.setText("");
    }
    // Rating
    String voteAverage = movie.getVoteAverage() + " / 10";
    mDetailBinding.tvMovieRating.setText(voteAverage);
    // Overview
    mDetailBinding.tvMovieOverview.setText(movie.getOverview());

    // Load Movie Image
    Picasso.with(this)
        .load(NetworkUtils.buildMovieImageUri(mRequiredImageSize, movie.getImageUrl()))
        .placeholder(android.R.drawable.screen_background_light_transparent)
        .error(R.drawable.movie_empty)
        .into(mDetailBinding.ivMovieImage, new Callback() {
          @Override public void onSuccess() {
          }

          @Override public void onError() {
          }
        });
  }

  /**
   * Insert a movie to {@link MovieContract.MovieFavoriteEntry}.
   *
   * @param movie The movie to add to favorite movie table
   * @return {@link Uri} of new record added
   */
  private Uri addFavoriteMovie(Movie movie) {
    // Insert the content values via a ContentResolver
    return getContentResolver().insert(MovieContract.MovieFavoriteEntry.CONTENT_URI, Movie.from(movie));
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

  private LoaderManager.LoaderCallbacks<String> initNetworkLoaderCallback() {
    return new LoaderManager.LoaderCallbacks<String>() {

      @NonNull @Override public Loader<String> onCreateLoader(int loaderId, Bundle args) {
        switch (loaderId) {
          case NETWORK_LOADER_MOVIE_DETAIL:
            // Get URL for Movie details Download
            URL movieDetailUrl = NetworkUtils.buildMovieDetailUrl(mMovie.getMovieId(), mDefaultLanguage);
            return new NetworkLoader(MovieDetailActivity.this, movieDetailUrl);
          default:
            throw new RuntimeException("Loader not Implemented: " + loaderId);
        }
      }

      @Override public void onLoadFinished(@NonNull Loader<String> loader, String data) {
        switch (loader.getId()) {
          case NETWORK_LOADER_MOVIE_DETAIL:
            if (data == null) {
              Log.e(TAG, "Null response from Movie Detail Loader");
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
                  parseJson(data);
                  break;
              }
            }
            break;
        }
      }

      // Override onLoaderReset as it is part of the interface we implement, but don't do anything in this method
      @Override public void onLoaderReset(@NonNull Loader<String> loader) {
      }
    };
  }
}
