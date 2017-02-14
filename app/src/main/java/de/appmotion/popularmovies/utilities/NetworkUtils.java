/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.appmotion.popularmovies.utilities;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.annotation.StringDef;
import de.appmotion.popularmovies.App;
import de.appmotion.popularmovies.BuildConfig;
import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * These utilities will be used to communicate with the network.
 */
public class NetworkUtils {

  // Define {@link ImageSize} Types
  public static final String ORIGINAL = "original";
  public static final String WIDTH92 = "w92";
  public static final String WIDTH154 = "w154";
  public static final String WIDTH185 = "w185";
  public static final String WIDTH342 = "w342";
  public static final String WIDTH500 = "w500";
  public static final String WIDTH780 = "w780";
  // themoviedb API Key
  private final static String KEY = BuildConfig.MOVIE_DB_API_KEY;
  // Urls
  private final static String MOVIE_DB_CONFIGURATION = "https://api.themoviedb.org/3/configuration";
  private final static String MOVIE_DB_POPULAR_MOVIES = "https://api.themoviedb.org/3/movie/popular";
  private final static String MOVIE_DB_TOP_RATED_MOVIES = "https://api.themoviedb.org/3/movie/top_rated";
  private final static String MOVIE_DB_MOVIE_DETAIL = "https://api.themoviedb.org/3/movie/";
  private final static String MOVIE_DB_IMAGE_URL = "http://image.tmdb.org/t/p/";
  // Params
  private final static String API_KEY = "api_key";
  private final static String PARAM_LANGUAGE = "language";
  private final static String PARAM_PAGE = "page";
  private final static String PARAM_REGION = "region";
  private final static String sortBy = "stars";

  /**
   * Builds the URL used to query themoviedb for API Configuration data.
   *
   * @return The URL to use to get configuration data for the API.
   */
  public static URL buildConfigurationUrl() {
    Uri builtUri = Uri.parse(MOVIE_DB_CONFIGURATION).buildUpon().appendQueryParameter(API_KEY, KEY).build();

    URL url = null;
    try {
      url = new URL(builtUri.toString());
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }

    return url;
  }

  /**
   * Builds the URL used to query themoviedb for Popular Movies.
   *
   * @param language The language requested.
   * @param page The page requested.
   * @param region The region requested.
   * @return The URL to use to get Popular Movies.
   */
  public static URL buildPopularMoviesUrl(String language, String page, String region) {
    Uri builtUri = Uri.parse(MOVIE_DB_POPULAR_MOVIES)
        .buildUpon()
        .appendQueryParameter(API_KEY, KEY)
        .appendQueryParameter(PARAM_LANGUAGE, language)
        .appendQueryParameter(PARAM_PAGE, page)
        .appendQueryParameter(PARAM_REGION, region)
        .build();

    URL url = null;
    try {
      url = new URL(builtUri.toString());
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }

    return url;
  }

  /**
   * Builds the URL used to query themoviedb for Top Rated Movies.
   *
   * @param language The language requested.
   * @param page The page requested.
   * @param region The region requested.
   * @return The URL to use to get Top Rated Movies.
   */
  public static URL buildTopRatedMoviesUrl(String language, String page, String region) {
    Uri builtUri = Uri.parse(MOVIE_DB_TOP_RATED_MOVIES)
        .buildUpon()
        .appendQueryParameter(API_KEY, KEY)
        .appendQueryParameter(PARAM_LANGUAGE, language)
        .appendQueryParameter(PARAM_PAGE, page)
        .appendQueryParameter(PARAM_REGION, region)
        .build();

    URL url = null;
    try {
      url = new URL(builtUri.toString());
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }

    return url;
  }

  /**
   * Builds the URL used to query themoviedb for Detail of a Movie.
   *
   * @param movieId The ID of Movie requested.
   * @param language The language requested.
   * @return The URL to use to get Top Rated Movies.
   */
  public static URL buildMovieDetailUrl(long movieId, String language) {
    Uri builtUri = Uri.parse(MOVIE_DB_MOVIE_DETAIL)
        .buildUpon()
        .appendEncodedPath(String.valueOf(movieId))
        .appendQueryParameter(API_KEY, KEY)
        .appendQueryParameter(PARAM_LANGUAGE, language)
        .build();

    URL url = null;
    try {
      url = new URL(builtUri.toString());
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }

    return url;
  }

  /**
   * Builds the URL used to query themoviedb for a poster image of a Movie.
   *
   * @param imageSize The size of the image.
   * @param imagePath The path of the image.
   * @return The Uri to use to get the image of a Movie.
   */
  public static Uri buildMovieImageUri(@ImageSize String imageSize, String imagePath) {
    return Uri.parse(MOVIE_DB_IMAGE_URL).buildUpon().appendEncodedPath(imageSize).appendEncodedPath(imagePath).build();
  }

  public static boolean isAnyNetworkOn() {
    ConnectivityManager connMgr = (ConnectivityManager) App.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
    return (networkInfo != null && networkInfo.isConnected());
  }

  public static void close(Closeable c) {
    if (c == null) {
      return;
    }
    try {
      c.close();
    } catch (IOException e) {
      //ignore
    }
  }

  @Retention(RetentionPolicy.CLASS) @StringDef({ ORIGINAL, WIDTH92, WIDTH154, WIDTH185, WIDTH342, WIDTH500, WIDTH780 })
  public @interface ImageSize {
  }
}