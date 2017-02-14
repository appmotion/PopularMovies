package de.appmotion.popularmovies.utilities;

import android.os.AsyncTask;
import android.support.annotation.StringDef;
import de.appmotion.popularmovies.App;
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
public class CallApiTask extends AsyncTask<URL, Void, String> {

  // Define {@link ErrorType} Types
  public static final String NULL = "null";
  public static final String API_ERROR = "api_error";
  public static final String OFFLINE = "offline";
  // Listener for onPostExecute method
  private final OnPostExecuteListener mListener;

  public CallApiTask(OnPostExecuteListener listener) {
    mListener = listener;
  }

  @Override protected String doInBackground(URL... params) {
    URL url = params[0];
    try {
      Request request = new Request.Builder().url(url).get().build();

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

  @Override protected void onPostExecute(String results) {
    if (results == null) {
      mListener.showErrorMessage(NULL);
    } else {
      switch (results) {
        case API_ERROR:
          mListener.showErrorMessage(API_ERROR);
          break;
        case OFFLINE:
          mListener.showErrorMessage(OFFLINE);
          break;
        case "":
          break;
        default:
          mListener.parseAndShowJsonData(results);
          break;
      }
    }
  }

  public interface OnPostExecuteListener {
    void parseAndShowJsonData(String jsonData);

    void showErrorMessage(@CallApiTask.ErrorType String errorType);
  }

  @Retention(RetentionPolicy.CLASS) @StringDef({ NULL, API_ERROR, OFFLINE }) public @interface ErrorType {
  }
}
