package de.appmotion.popularmovies;

import android.app.Application;
import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.picasso.Picasso;
import java.util.concurrent.TimeUnit;
import okhttp3.Cache;
import okhttp3.OkHttpClient;

public class App extends Application {

  private static App instance;
  private OkHttpClient okHttpClient;

  public App() {
    instance = this;
  }

  public static App getInstance() {
    return instance;
  }

  @Override public void onCreate() {
    super.onCreate();
    // LeakCanary
    if (LeakCanary.isInAnalyzerProcess(this)) {
      // This process is dedicated to LeakCanary for heap analysis.
      // You should not init your app in this process.
      return;
    }
    LeakCanary.install(this);

    // OkHttpClient
    int cacheSize = 50 * 1024 * 1024; // 50 MiB
    Cache cache = new Cache(getCacheDir(), cacheSize);
    okHttpClient = new OkHttpClient.Builder().cache(cache).connectTimeout(2L, TimeUnit.SECONDS).readTimeout(5L, TimeUnit.SECONDS).build();

    // Picasso with configured okHttpClient
    Picasso.Builder builder = new Picasso.Builder(this);
    builder.downloader(new OkHttp3Downloader(okHttpClient));
    Picasso picasso = builder.build();
    Picasso.setSingletonInstance(picasso);
  }

  public OkHttpClient getOkHttpClient() {
    return okHttpClient;
  }
}
