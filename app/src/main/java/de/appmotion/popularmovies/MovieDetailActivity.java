package de.appmotion.popularmovies;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.Loader;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import de.appmotion.popularmovies.data.PopularMoviesContract;
import de.appmotion.popularmovies.data.PopularMoviesDbHelper;
import de.appmotion.popularmovies.utilities.CallApiTaskLoader;
import de.appmotion.popularmovies.utilities.NetworkUtils;
import java.net.URL;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Display the details for a movie.
 */
public class MovieDetailActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<String> {

  // Suggestions to Make Your Project Stand Out
  //TODO: Implement sharing functionality to allow the user to share the first trailer’s YouTube URL from the movie details screen.
  //TODO: Extend the favorites ContentProvider to store the movie poster, synopsis, user rating, and release date, and display them even when offline.
  private static final String TRAILER_SHARE_HASHTAG = " #PopularMovieApp";

  /*
   * This number will uniquely identify our Loader and is chosen arbitrarily. You can change this
   * to any number you like, as long as you use the same variable name.
   */
  private static int MOVIE_DETAIL_LOADER = 1;

  // Views
  @BindView(R.id.tv_movie_title) TextView mMovieTitle;
  @BindView(R.id.iv_movie_image) ImageView mMovieImage;
  @BindView(R.id.tv_movie_year) TextView mMovieYear;
  @BindView(R.id.tv_movie_duration) TextView mMovieDuration;
  @BindView(R.id.tv_movie_rating) TextView mMovieRating;
  @BindView(R.id.tv_movie_overview) TextView mMovieOverview;

  private SQLiteDatabase mDb;
  // The Id of this movie
  private long mMovieId;
  // The tile of this movie
  private String mTitle;
  // The image url of this movie
  private String mImageUrl;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_movie_detail);
    ButterKnife.bind(this);

    // add back arrow to toolbar
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
      getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    // Create a DB helper (this will create the DB if run for the first time)
    PopularMoviesDbHelper dbHelper = new PopularMoviesDbHelper(this);
    // Keep a reference to the mDb until paused or killed. Get a writable database
    // because we will be adding favorite movies
    mDb = dbHelper.getWritableDatabase();

    // Initialize the loader with MOVIE_DETAIL_LOADER as the ID, null for the bundle, and this for the context
    getSupportLoaderManager().initLoader(MOVIE_DETAIL_LOADER, null, this);

    // Get Movie Id from Intent, then download Movie Details.
    if (getIntent() != null) {
      mMovieId = getIntent().getLongExtra(MainActivity.EXTRA_MOVIE_ID, 0L);
      if (mMovieId == 0) {
        showMessage(getString(R.string.error_loading_movie_detail));
      } else {
        downloadMovieDetails(mMovieId, mDefaultLanguage);
      }
    } else {
      showMessage(getString(R.string.error_loading_movie_detail));
    }
  }

  @Override protected void onDestroy() {
    mDb.close();
    super.onDestroy();
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
      // Add the currently shown Movie to favoritelist table in DB.
      case R.id.action_favorite_add:
        if (mMovieId != 0L && mTitle != null && mTitle.length() > 0) {
          long rowId = addFavoriteMovie(mMovieId, mTitle, mImageUrl);
          // Error: Movie can not be added to favoritelist
          if (rowId == -1L) {
            showMessage(getString(R.string.error_adding_movie_to_favoritelist));
          }
          // Error: Movie was already added to favoritelist
          else if (rowId == -2L) {
            showMessage(getString(R.string.error_adding_duplicate_movie_to_favoritelist));
          }
          // Movie successfuly added to favoritelist
          else {
            showMessage(getString(R.string.adding_movie_to_favoritelist));
          }
        }
        // Error: Movie data is empty and so it cannot be added to favoritelist
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
    // Get URL for Movie details Download and build Bundle for {@link CallApiTaskLoader}
    URL movieDetailUrl = NetworkUtils.buildMovieDetailUrl(movieId, language);
    Bundle queryBundle = new Bundle();
    queryBundle.putSerializable(CallApiTaskLoader.EXTRA_QUERY_URL, movieDetailUrl);

    // Call getSupportLoaderManager and store it in a LoaderManager variable
    LoaderManager loaderManager = getSupportLoaderManager();
    // Get our Loader by calling getLoader and passing the ID we specified
    Loader<String> callApiTaskLoader = loaderManager.getLoader(MOVIE_DETAIL_LOADER);
    // If the Loader was null, initialize it. Else, restart it.
    if (callApiTaskLoader == null) {
      loaderManager.initLoader(MOVIE_DETAIL_LOADER, queryBundle, this);
    } else {
      loaderManager.restartLoader(MOVIE_DETAIL_LOADER, queryBundle, this);
    }
  }

  /**
   * Called when MOVIE_DETAIL_LOADER finished in onLoadFinished().
   * Parse jsonData and show in Views.
   *
   * @param jsonData from onLoadFinished of {@link CallApiTaskLoader}.
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
      mMovieTitle.setText(mTitle);
      // Year
      mMovieYear.setText(year);
      // Duration
      duration = duration + "min";
      mMovieDuration.setText(duration);
      // Rating
      rating = rating + " / 10";
      mMovieRating.setText(rating);
      // Overview
      mMovieOverview.setText(overview);

      // Load Movie Image
      Picasso.with(this)
          .load(NetworkUtils.buildMovieImageUri(mRequiredImageSize, mImageUrl))
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

  /**
   * Adds a movie to the mDb favoritelist with its id, title, imageUrl and the current timestamp.
   * This method checks if the id of the movie already exists in favoritelist. If not,
   * the movie will be added to favoritelist.
   *
   * @param id movies's id
   * @param title movie's title
   * @param imageUrl url to an image
   * @return id of new record added
   */
  private long addFavoriteMovie(long id, @NonNull String title, String imageUrl) {
    String whereClause = PopularMoviesContract.FavoritelistEntry.COLUMN_MOVIE_ID + " = ?";
    String[] whereArgs = new String[] { String.valueOf(id) };
    Cursor cursor = mDb.query(PopularMoviesContract.FavoritelistEntry.TABLE_NAME, null, whereClause, whereArgs, null, null, null);
    if (!cursor.moveToFirst()) {
      cursor.close();
      ContentValues cv = new ContentValues();
      cv.put(PopularMoviesContract.FavoritelistEntry.COLUMN_MOVIE_ID, id);
      cv.put(PopularMoviesContract.FavoritelistEntry.COLUMN_MOVIE_TITLE, title);
      cv.put(PopularMoviesContract.FavoritelistEntry.COLUMN_MOVIE_IMAGE_URL, imageUrl);
      return mDb.insert(PopularMoviesContract.FavoritelistEntry.TABLE_NAME, null, cv);
    } else {
      cursor.close();
      return -2L;
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
   * Below this point are the three {@link LoaderManager.LoaderCallbacks} methods
   **/

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
}
