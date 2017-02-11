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

import android.net.Uri;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * These utilities will be used to communicate with the network.
 */
public class NetworkUtils {

  // themoviedb API Key
  private final static String key = "";

  // Urls
  private final static String MOVIE_DB_POPULAR_MOVIES = "https://api.themoviedb.org/3/movie/popular";
  private final static String MOVIE_DB_IMAGE_URL = "http://image.tmdb.org/t/p/";

  // Params
  private final static String API_KEY = "api_key";
  private final static String PARAM_LANGUAGE = "language";
  private final static String PARAM_PAGE = "page";
  private final static String PARAM_REGION = "region";

  private final static String sortBy = "stars";

  /**
   * Builds the URL used to query themoviedb for popular movies.
   *
   * @param language The language requested.
   * @param page The page requested.
   * @param region The region requested.
   * @return The URL to use to get popular movies.
   */
  public static URL buildPopularMoviesUrl(String language, String page, String region) {
    Uri builtUri = Uri.parse(MOVIE_DB_POPULAR_MOVIES)
        .buildUpon()
        .appendQueryParameter(API_KEY, key)
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
   * Builds the URL used to query themoviedb for a poster image of a movie.
   *
   * @param imageSize The size of the image.
   * @param imagePath The path of the image.
   * @return The Uri to use to get the image of a movie.
   */
  public static Uri buildMovieImageUri(ImageSize imageSize, String imagePath) {
    return Uri.parse(MOVIE_DB_IMAGE_URL).buildUpon().appendEncodedPath(imageSize.width).appendEncodedPath(imagePath).build();
  }

  public enum ImageSize {
    ORIGINAL("original"),
    WIDTH92("w92"),
    WIDTH154("w154"),
    WIDTH185("w185"),
    WIDTH342("w342"),
    WIDTH500("w500"),
    WIDTH780("w780");

    public final String width;

    ImageSize(final String width) {
      this.width = width;
    }
  }
}