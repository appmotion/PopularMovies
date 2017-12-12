package de.appmotion.popularmovies.data;

/**
 * Model class for a Movie.
 */
public class Movie {

  private long mMovieId;
  private String mTitle;
  private String mImageUrl;

  public long getMovieId() {
    return mMovieId;
  }

  public void setMovieId(long movieId) {
    mMovieId = movieId;
  }

  public String getTitle() {
    return mTitle;
  }

  public void setTitle(String title) {
    mTitle = title;
  }

  public String getImageUrl() {
    return mImageUrl;
  }

  public void setImageUrl(String imageUrl) {
    mImageUrl = imageUrl;
  }
}
