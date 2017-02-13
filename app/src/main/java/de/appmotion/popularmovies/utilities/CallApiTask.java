package de.appmotion.popularmovies.utilities;

import android.os.AsyncTask;
import de.appmotion.popularmovies.App;
import java.io.IOException;
import java.net.URL;
import java.util.Scanner;
import okhttp3.Request;
import okhttp3.Response;

public class CallApiTask extends AsyncTask<URL, Void, String> {

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
            return "noDataAvailable";
          }
        default:
          return "apiCallError";
      }
    } catch (IOException e) {
      e.printStackTrace();
      return "offline";
    }
  }

  @Override protected void onPostExecute(String results) {
    if (results == null) {
      mListener.showErrorMessage(ErrorType.NULL);
    } else {
      if (results.equals("noData")) {
        // do nothing
      } else if (results.equals("apiError")) {
        mListener.showErrorMessage(ErrorType.API_ERROR);
      } else if (results.equals("offline")) {
        mListener.showErrorMessage(ErrorType.OFFLINE);
      } else if (results.equals("")) {
        // do nothing
      } else {
        mListener.parseAndShowJsonData(results);
      }
    }
  }

  public enum ErrorType {
    NULL,
    EPMTY,
    NO_DATA,
    API_ERROR,
    OFFLINE
  }

  public interface OnPostExecuteListener {
    void parseAndShowJsonData(String jsonData);

    void showErrorMessage(ErrorType errorType);
  }
}
