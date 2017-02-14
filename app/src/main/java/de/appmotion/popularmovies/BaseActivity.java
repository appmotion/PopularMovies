package de.appmotion.popularmovies;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.widget.Toast;
import de.appmotion.popularmovies.utilities.CallApiTask;
import de.appmotion.popularmovies.utilities.NetworkUtils;

public abstract class BaseActivity extends AppCompatActivity implements CallApiTask.OnPostExecuteListener {

  protected NetworkUtils.ImageSize mRequiredImageSize;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Calculate mRequiredImageSize once for later usage.
    calculateImageSizeForApiCall();
  }

  /**
   * Calculate the Movie Image size we would like to get from API.
   */
  protected void calculateImageSizeForApiCall() {
    int noOfColumns = calculateNoOfColumns();
    DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
    int screenWidthPx = displayMetrics.widthPixels / noOfColumns;
    if (screenWidthPx <= 92) {
      mRequiredImageSize = NetworkUtils.ImageSize.WIDTH92;
    } else if (screenWidthPx <= 154) {
      mRequiredImageSize = NetworkUtils.ImageSize.WIDTH154;
    } else if (screenWidthPx <= 185) {
      mRequiredImageSize = NetworkUtils.ImageSize.WIDTH185;
    } else if (screenWidthPx <= 342) {
      mRequiredImageSize = NetworkUtils.ImageSize.WIDTH342;
    } else if (screenWidthPx <= 500) {
      mRequiredImageSize = NetworkUtils.ImageSize.WIDTH500;
    } else if (screenWidthPx <= 780) {
      mRequiredImageSize = NetworkUtils.ImageSize.WIDTH780;
    } else {
      mRequiredImageSize = NetworkUtils.ImageSize.ORIGINAL;
    }
  }

  /**
   * Calculate the number of columns for a LayoutManager of a RecyclerView.
   * Each column should be at minimum 180dp wide.
   *
   * @return number of columns
   */
  protected int calculateNoOfColumns() {
    DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
    float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
    int noOfColumns = (int) (dpWidth / 180);
    return noOfColumns;
  }

  /**
   * Called from onPostExecute of {@link CallApiTask}.
   * Show a Toast Error Message.
   *
   * @param errorType The errorType.
   */
  @Override public void showErrorMessage(CallApiTask.ErrorType errorType) {
    String message;
    switch (errorType) {
      case NULL:
        message = getString(R.string.error_loading_movies);
        break;
      case API_ERROR:
        message = getString(R.string.error_loading_movies);
        break;
      case OFFLINE:
        message = getString(R.string.error_connect_internet);
        break;
      default:
        message = errorType.name();
    }
    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
  }
}
