package de.appmotion.popularmovies.utilities;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.StringDef;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;
import de.appmotion.popularmovies.App;
import de.appmotion.popularmovies.BuildConfig;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URL;
import java.util.Scanner;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Use OkHttp inside this AsyncTask to get data from themoviedb.org.
 */
public class CallApiTaskLoader extends AsyncTaskLoader<String> {

  /*
   * This number will uniquely identify our Loader and is chosen arbitrarily. You can change this
   * to any number you like, as long as you use the same variable name.
   */
  public static final int MOVIE_API_LOADER = 22;

  // Name of the URL sent via Bundle to {@link CallApiTaskLoader}
  public final static String EXTRA_QUERY_URL = BuildConfig.APPLICATION_ID + ".query_url";

  // Define {@link ErrorType} Types
  public static final String NULL = "null";
  public static final String API_ERROR = "api_error";
  public static final String OFFLINE = "offline";
  // Arguments for this AsyncTaskLoader
  private Bundle mArgs;

  public CallApiTaskLoader(Context context, Bundle args) {
    super(context);
    mArgs = args;
  }

  @Override protected void onStartLoading() {

    // If no arguments were passed, we don't have a query to perform. Simply return.
    if (mArgs == null) {
      return;
    }

    // Show the loading indicator
    //mLoadingIndicator.setVisibility(View.VISIBLE);

    // Force a load
    forceLoad();
  }

  @Override public String loadInBackground() {

    // Extract the url query from the args using our constant
    URL queryUrl = (URL) mArgs.getSerializable(EXTRA_QUERY_URL);

    // If the url is empty, there's nothing to search for
    if (queryUrl == null || TextUtils.isEmpty(queryUrl.toString())) {
      return null;
    }

    //
    try {
      Request request = new Request.Builder().url(queryUrl).get().build();

      Response response = App.getInstance().getOkHttpClient().newCall(request).execute();
      switch (response.code()) {
        case 200:
          Scanner scanner = new Scanner(response.body().byteStream());
          scanner.useDelimiter("\\A");
          boolean hasInput = scanner.hasNext();
          if (hasInput) {
            return scanner.next();
          } else {
            return null;
          }
          // response.code() is not 200
        default:
          return API_ERROR;
      }
    } catch (IOException e) {
      e.printStackTrace();
      return OFFLINE;
    }
  }

  @Retention(RetentionPolicy.CLASS) @StringDef({ NULL, API_ERROR, OFFLINE }) public @interface ErrorType {
  }
}
