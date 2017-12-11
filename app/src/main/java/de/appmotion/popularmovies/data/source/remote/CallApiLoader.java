package de.appmotion.popularmovies.data.source.remote;

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
 * Use OkHttp inside this AsyncTaskLoader to get data from themoviedb.org.
 */
public class CallApiLoader extends AsyncTaskLoader<String> {

  // Name of the URL sent via Bundle to this Loader
  public final static String EXTRA_QUERY_URL = BuildConfig.APPLICATION_ID + ".query_url";

  // Define {@link ErrorType} Types
  public static final String NULL = "null";
  public static final String API_ERROR = "api_error";
  public static final String OFFLINE = "offline";
  // Arguments for this AsyncTaskLoader
  private Bundle mArgs;

  // Caching: This String will contain the raw JSON from the server results
  // We dont need this, because OkHttp is already doing caching in a smarter way.
  private String mJson;

  public CallApiLoader(Context context, Bundle args) {
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

    /*
     * If we already have cached results, just deliver them now. If we don't have any
     * cached results, force a load.
     */
    /*
    if (mJson != null) {
      deliverResult(mJson);
    } else {
      forceLoad();
    }
    */

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

  /**
   * Used for caching response from server to mJson
   *
   * @param json the response from server
   */
  @Override public void deliverResult(String json) {
    mJson = json;
    super.deliverResult(json);
  }

  @Retention(RetentionPolicy.CLASS) @StringDef({ NULL, API_ERROR, OFFLINE }) public @interface ErrorType {
  }
}
