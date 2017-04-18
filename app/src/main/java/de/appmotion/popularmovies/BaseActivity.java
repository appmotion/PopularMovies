package de.appmotion.popularmovies;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.widget.Toast;
import de.appmotion.popularmovies.utilities.CallApiTaskLoader;
import de.appmotion.popularmovies.utilities.NetworkUtils;
import java.util.Locale;

public abstract class BaseActivity extends AppCompatActivity {

  protected @NetworkUtils.ImageSize String mRequiredImageSize;

  protected Toast mToast;

  protected String mDefaultLanguage = Locale.getDefault().toString();
  protected String mDefaultCountry = Locale.getDefault().getCountry();

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Calculate mRequiredImageSize once for later usage.
    calculateImageSizeForApiCall();
  }

  @Override protected void onPause() {
    if (mToast != null) {
      mToast.cancel();
    }
    super.onPause();
  }

  /**
   * Calculate the Movie Image size we would like to get from API.
   */
  private void calculateImageSizeForApiCall() {
    int noOfColumns = calculateNoOfColumns();
    DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
    int screenWidthPx = displayMetrics.widthPixels / noOfColumns;
    if (screenWidthPx <= 92) {
      mRequiredImageSize = NetworkUtils.WIDTH92;
    } else if (screenWidthPx <= 154) {
      mRequiredImageSize = NetworkUtils.WIDTH154;
    } else if (screenWidthPx <= 185) {
      mRequiredImageSize = NetworkUtils.WIDTH185;
    } else if (screenWidthPx <= 342) {
      mRequiredImageSize = NetworkUtils.WIDTH342;
    } else if (screenWidthPx <= 500) {
      mRequiredImageSize = NetworkUtils.WIDTH500;
    } else if (screenWidthPx <= 780) {
      mRequiredImageSize = NetworkUtils.WIDTH780;
    } else {
      mRequiredImageSize = NetworkUtils.ORIGINAL;
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
   * Show a Toast Error Message.
   *
   * @param errorType the {@link CallApiTaskLoader.ErrorType}
   */
  protected void showErrorMessage(@CallApiTaskLoader.ErrorType String errorType) {
    String message;
    switch (errorType) {
      case CallApiTaskLoader.NULL:
        message = getString(R.string.error_loading_movies);
        break;
      case CallApiTaskLoader.API_ERROR:
        message = getString(R.string.error_loading_movies);
        break;
      case CallApiTaskLoader.OFFLINE:
        message = getString(R.string.error_connect_internet);
        break;
      default:
        message = errorType;
    }
    if (mToast != null) {
      mToast.cancel();
    }
    mToast = Toast.makeText(this, message, Toast.LENGTH_LONG);
    mToast.show();
  }

  /**
   * Show a Toast Message.
   *
   * @param message the Message to show
   */
  protected void showMessage(String message) {
    if (mToast != null) {
      mToast.cancel();
    }
    mToast = Toast.makeText(this, message, Toast.LENGTH_LONG);
    mToast.show();
  }
}
