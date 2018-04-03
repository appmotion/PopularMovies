package de.appmotion.popularmovies;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.AsyncQueryHandler;
import android.content.Intent;
import android.database.Cursor;
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
import android.view.View;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import de.appmotion.popularmovies.data.Movie;
import de.appmotion.popularmovies.data.source.local.MovieContract;
import de.appmotion.popularmovies.data.source.remote.NetworkLoader;
import de.appmotion.popularmovies.data.source.remote.NetworkUtils;
import de.appmotion.popularmovies.databinding.ActivityMovieDetailBinding;
import de.appmotion.popularmovies.databinding.MovieTrailerBinding;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.json.JSONArray;
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
  private static final String TRAILER_SHARE_HASHTAG = " #PopularMovieApp";

  // This number will uniquely identify a NetworkLoader for loading movie detail data from themoviedb.org.
  private static final int NETWORK_LOADER_MOVIE_DETAIL = 1;
  // This number will uniquely identify a NetworkLoader for loading movie trailer data from themoviedb.org.
  private static final int NETWORK_LOADER_MOVIE_TRAILER = 2;
  // This number will uniquely identify a NetworkLoader for loading movie review data from themoviedb.org.
  private static final int NETWORK_LOADER_MOVIE_REVIEW = 3;

  // Callback for {@link NetworkLoader}
  private LoaderManager.LoaderCallbacks<String> mNetworkLoaderCallback;

  // The Movie which will be shown
  private Movie mMovie;

  private ActivityMovieDetailBinding mDetailBinding;
  // Youtube-Key of the first Trailer Video
  private String mFirstTrailerKey;
  // Format release date of movie
  private SimpleDateFormat mReleaseDateFormat;
  private SimpleDateFormat mYearFormat;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mDetailBinding = DataBindingUtil.setContentView(this, R.layout.activity_movie_detail);

    mReleaseDateFormat = new SimpleDateFormat("yyyy-MM-dd", mLocale);
    mYearFormat = new SimpleDateFormat("yyyy", mLocale);

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

    showMovieDetails(mMovie, null);

    // Initiate Callbacks for the Loader
    mNetworkLoaderCallback = initNetworkLoaderCallback();

    // Loader for Movie Details
    getSupportLoaderManager().initLoader(NETWORK_LOADER_MOVIE_DETAIL, null, mNetworkLoaderCallback);
    // Loader for Movie Trailer
    getSupportLoaderManager().initLoader(NETWORK_LOADER_MOVIE_TRAILER, null, mNetworkLoaderCallback);
    // Loader for Movie Review
    getSupportLoaderManager().initLoader(NETWORK_LOADER_MOVIE_REVIEW, null, mNetworkLoaderCallback);
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.detail, menu);
    final MenuItem menuItemFavorite = menu.findItem(R.id.action_favorite_add_or_remove);
    menuItemFavorite.setIcon(R.drawable.ic_favorite_border_white_24dp);
    menuItemFavorite.setTitle(R.string.action_favorite_add);
    menuItemFavorite.setChecked(false);

    // Check if currently shown Movie is already in favorite table
    // Update function of mMenuItemShare accordingly
    Thread thread = new Thread(new Runnable() {
      @Override public void run() {
        if (mMovie == null) {
          return;
        }
        final Uri queryUri = MovieContract.MovieFavoriteEntry.CONTENT_URI;
        final String selection = MovieContract.MovieFavoriteEntry.COLUMN_MOVIE_ID + " = " + mMovie.getMovieId();
        final Cursor cursor = getContentResolver().query(queryUri, null, selection, null, null);
        if (cursor != null) {
          if (cursor.moveToNext()) {
            runOnUiThread(new Runnable() {
              @Override public void run() {
                menuItemFavorite.setIcon(R.drawable.ic_favorite_white_24dp);
                menuItemFavorite.setTitle(R.string.action_favorite_remove);
                menuItemFavorite.setChecked(true);
              }
            });
          }
          cursor.close();
        }
      }
    });
    thread.start();

    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        finish();
        return true;
      // Add or remove the currently shown Movie to or from favorite movie table in DB.
      case R.id.action_favorite_add_or_remove:
        if (!item.isChecked()) {
          if (mMovie != null) {
            addFavoriteMovie(mMovie, item);
          }
          // Error: Movie data is empty and so it cannot be added to table
          else {
            showMessage(getString(R.string.error_adding_empty_movie_to_favoritelist));
          }
        } else {
          if (mMovie != null) {
            removeFavoriteMovie(mMovie, item);
          }
        }
        return true;
      // Share the first trailer video of currently shown Movie.
      case R.id.action_share:
        if (mFirstTrailerKey != null && mFirstTrailerKey.length() > 0) {
          startShareTrailerIntent();
        } else {
          showMessage(getString(R.string.no_trailer_to_share));
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
  private void parseJsonMovieDetail(String jsonData) {
    try {
      JSONObject movieDetail = new JSONObject(jsonData);
      String runtime = movieDetail.getString("runtime");

      showMovieDetails(mMovie, runtime);
    } catch (JSONException e) {
      Log.e(TAG, "Parse Movie detail JSON error: ", e);
    }
  }

  /**
   * Called when NETWORK_LOADER_MOVIE_TRAILER finished in onLoadFinished().
   * Parse jsonData and show in Views.
   *
   * @param jsonData from onLoadFinished of {@link NetworkLoader}.
   */
  private void parseJsonMovieTrailer(String jsonData) {
    try {
      JSONObject trailerData = new JSONObject(jsonData);
      JSONArray results = trailerData.getJSONArray("results");

      mDetailBinding.llMovieTrailer.removeAllViews(); // Remove all old views from llMovieTrailer before adding new ones.

      for (int i = 0; i < results.length(); i++) {
        JSONObject trailer = results.getJSONObject(i);
        String trailerKey = trailer.getString("key");
        String trailerName = trailer.getString("name");
        if (i == 0) {
          mFirstTrailerKey = trailerKey;
        }
        showMovieTrailer(trailerKey, trailerName);
      }
    } catch (JSONException e) {
      Log.e(TAG, "Parse Movie trailer JSON error: ", e);
    }
  }

  /**
   * Called when NETWORK_LOADER_MOVIE_REVIEW finished in onLoadFinished().
   * Parse jsonData and show in Views.
   *
   * @param jsonData from onLoadFinished of {@link NetworkLoader}.
   */
  private void parseJsonMovieReview(String jsonData) {
    try {
      JSONObject reviewData = new JSONObject(jsonData);
      JSONArray results = reviewData.getJSONArray("results");

      for (int i = 0; i < results.length(); i++) {
        JSONObject review = results.getJSONObject(i);
        String content = review.getString("content");
        String author = review.getString("author");
        showMovieReview(content, author);
      }
    } catch (JSONException e) {
      Log.e(TAG, "Parse Movie review JSON error: ", e);
    }
  }

  private void showMovieDetails(Movie movie, @Nullable String runtime) {
    // Ttile
    mDetailBinding.tvMovieTitle.setText(movie.getTitle());
    // Year
    try {
      Date releaseDate = mReleaseDateFormat.parse(movie.getReleaseDate());
      mDetailBinding.tvMovieYear.setText(mYearFormat.format(releaseDate));
    } catch (ParseException e) {
      mDetailBinding.tvMovieYear.setText(movie.getReleaseDate());
    }
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

  private void showMovieTrailer(String trailerKey, String trailerName) {
    MovieTrailerBinding trailerBinding =
        DataBindingUtil.inflate(getLayoutInflater(), R.layout.movie_trailer, mDetailBinding.llMovieTrailer, true);
    trailerBinding.tvTrailerName.setText(trailerName);
    trailerBinding.getRoot().setTag(trailerKey);

    trailerBinding.getRoot().setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        watchYoutubeVideo((String) v.getTag());
      }
    });
  }

  private void showMovieReview(String content, String author) {
    String review = content + "\n\n" + getString(R.string.by) + " " + author;
    mDetailBinding.tvMovieReview.setText(review);
  }

  private void watchYoutubeVideo(String key) {
    Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + key));
    Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=" + key));
    try {
      startActivity(appIntent);
    } catch (ActivityNotFoundException ex) {
      startActivity(webIntent);
    }
  }

  /**
   * Insert a movie to {@link MovieContract.MovieFavoriteEntry}.
   *
   * @param movie The movie to add to favorite movie table
   * @param menuItem The MenuItem which changes on the basis of successfully insertion of movie
   */
  private void addFavoriteMovie(Movie movie, final MenuItem menuItem) {
    // Insert the content values via a AsyncQueryHandler
    @SuppressLint("HandlerLeak") AsyncQueryHandler asyncQueryHandler = new AsyncQueryHandler(getContentResolver()) {
      @Override protected void onInsertComplete(int token, Object cookie, Uri uri) {
        super.onInsertComplete(token, cookie, uri);
        if (uri != null) {
          // Movie successfuly added to table
          showMessage(getString(R.string.adding_movie_to_favoritelist));
          menuItem.setIcon(R.drawable.ic_favorite_white_24dp);
          menuItem.setTitle(R.string.action_favorite_remove);
          menuItem.setChecked(true);
        }
      }
    };
    asyncQueryHandler.startInsert(1, null, MovieContract.MovieFavoriteEntry.CONTENT_URI, Movie.from(movie));
  }

  /**
   * Removes the record with the specified movie
   *
   * @param movie the Movie to be removed
   * @param menuItem The MenuItem which changes on the basis of successfully remove of movie
   */
  private void removeFavoriteMovie(Movie movie, final MenuItem menuItem) {
    Uri uri = MovieContract.MovieFavoriteEntry.CONTENT_URI;
    String selection = MovieContract.MovieFavoriteEntry.COLUMN_MOVIE_ID  + " = ?";
    String[] selectionArgs = new String[] { String.valueOf(movie.getMovieId()) };

    // Delete a single row of data using a AsyncQueryHandler
    @SuppressLint("HandlerLeak") AsyncQueryHandler asyncQueryHandler = new AsyncQueryHandler(getContentResolver()) {
      @Override protected void onDeleteComplete(int token, Object cookie, int result) {
        super.onDeleteComplete(token, cookie, result);
        if (result > 0) {
          // Movie successfuly removed from table
          showMessage(getString(R.string.removing_movie_from_favoritelist));
          menuItem.setIcon(R.drawable.ic_favorite_border_white_24dp);
          menuItem.setTitle(R.string.action_favorite_add);
          menuItem.setChecked(false);
        }
      }
    };
    asyncQueryHandler.startDelete(1, null, uri, selection, selectionArgs);
  }

  /**
   * Uses the ShareCompat Intent builder to create our Trailer intent for sharing, then call
   * {@link android.content.Context#startActivity} with the given Intent.
   */
  private void startShareTrailerIntent() {
    String youtubeUrl = "http://www.youtube.com/watch?v=" + mFirstTrailerKey;
    Intent shareIntent =
        ShareCompat.IntentBuilder.from(this).setType("text/plain").setText(youtubeUrl + " " + TRAILER_SHARE_HASHTAG).getIntent();
    startActivity(shareIntent);
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
          case NETWORK_LOADER_MOVIE_TRAILER:
            // Get URL for Movie trailer Download
            URL movieTrailerUrl = NetworkUtils.buildMovieTrailerUrl(mMovie.getMovieId(), mDefaultLanguage);
            return new NetworkLoader(MovieDetailActivity.this, movieTrailerUrl);
          case NETWORK_LOADER_MOVIE_REVIEW:
            // Get URL for Movie review Download
            URL movieReviewUrl = NetworkUtils.buildMovieReviewUrl(mMovie.getMovieId(), mDefaultLanguage);
            return new NetworkLoader(MovieDetailActivity.this, movieReviewUrl);
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
                  parseJsonMovieDetail(data);
                  break;
              }
            }
            break;
          case NETWORK_LOADER_MOVIE_TRAILER:
            if (data == null) {
              Log.e(TAG, "Null response from Movie Trailer Loader");
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
                  parseJsonMovieTrailer(data);
                  break;
              }
            }
            break;
          case NETWORK_LOADER_MOVIE_REVIEW:
            if (data == null) {
              Log.e(TAG, "Null response from Movie Review Loader");
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
                  parseJsonMovieReview(data);
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
