package de.appmotion.popularmovies;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import de.appmotion.popularmovies.utilities.NetworkUtils;

public abstract class BaseActivity extends AppCompatActivity {

  protected NetworkUtils.ImageSize mRequiredImageSize;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Get current device configuration
    Configuration configuration = getResources().getConfiguration();
    int screenWidthDp = configuration.screenWidthDp;
    if (screenWidthDp <= 92) {
      mRequiredImageSize = NetworkUtils.ImageSize.WIDTH92;
    } else if (screenWidthDp <= 154) {
      mRequiredImageSize = NetworkUtils.ImageSize.WIDTH154;
    } else if (screenWidthDp <= 185) {
      mRequiredImageSize = NetworkUtils.ImageSize.WIDTH185;
    } else if (screenWidthDp <= 342) {
      mRequiredImageSize = NetworkUtils.ImageSize.WIDTH342;
    } else if (screenWidthDp <= 500) {
      mRequiredImageSize = NetworkUtils.ImageSize.WIDTH500;
    } else if (screenWidthDp <= 780) {
      mRequiredImageSize = NetworkUtils.ImageSize.WIDTH780;
    } else {
      mRequiredImageSize = NetworkUtils.ImageSize.ORIGINAL;
    }
  }
}
