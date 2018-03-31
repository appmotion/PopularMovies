package de.appmotion.popularmovies.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import de.appmotion.popularmovies.data.source.local.MovieContract;

/**
 * Immutable model class for a Movie.
 */
public final class Movie implements Parcelable {

  @SuppressWarnings("unused") public static final Parcelable.Creator<Movie> CREATOR = new Parcelable.Creator<Movie>() {
    @Override public Movie createFromParcel(Parcel in) {
      return new Movie(in);
    }

    @Override public Movie[] newArray(int size) {
      return new Movie[size];
    }
  };
  private long mId;
  private long mMovieId;
  private String mTitle;
  private String mImageUrl;
  private double mPopularity;
  private double mVoteAverage;
  private String mReleaseDate;
  private String mOverview;

  public Movie() {
  }

  protected Movie(Parcel in) {
    mId = in.readLong();
    mMovieId = in.readLong();
    mTitle = in.readString();
    mImageUrl = in.readString();
    mPopularity = in.readDouble();
    mVoteAverage = in.readDouble();
    mReleaseDate = in.readString();
    mOverview = in.readString();
  }

  /**
   * Use this constructor to return a Movie from a Cursor
   *
   * @return {@link Movie}
   */
  public static Movie from(Cursor cursor) {
    final Movie movie = new Movie();
    movie.setId(cursor.getLong(cursor.getColumnIndexOrThrow(MovieContract.MovieEntry._ID)));
    movie.setMovieId(cursor.getLong(cursor.getColumnIndexOrThrow(MovieContract.MovieEntry.COLUMN_MOVIE_ID)));
    movie.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(MovieContract.MovieEntry.COLUMN_MOVIE_TITLE)));
    movie.setImageUrl(cursor.getString(cursor.getColumnIndexOrThrow(MovieContract.MovieEntry.COLUMN_MOVIE_IMAGE_URL)));
    movie.setPopularity(cursor.getDouble(cursor.getColumnIndexOrThrow(MovieContract.MovieEntry.COLUMN_MOVIE_POPULARITY)));
    movie.setVoteAverage(cursor.getDouble(cursor.getColumnIndexOrThrow(MovieContract.MovieEntry.COLUMN_MOVIE_VOTE_AVERAGE)));
    movie.setReleaseDate(cursor.getString(cursor.getColumnIndexOrThrow(MovieContract.MovieEntry.COLUMN_MOVIE_RELEASE_DATE)));
    movie.setOverview(cursor.getString(cursor.getColumnIndexOrThrow(MovieContract.MovieEntry.COLUMN_MOVIE_OVERVIEW)));
    return movie;
  }

  /**
   * Use this constructor to return ContentValues from a Movie
   *
   * @return {@link ContentValues}
   */
  public static ContentValues from(Movie movie) {
    final ContentValues cv = new ContentValues();
    cv.put(MovieContract.MovieEntry.COLUMN_MOVIE_ID, movie.getMovieId());
    cv.put(MovieContract.MovieEntry.COLUMN_MOVIE_TITLE, movie.getTitle());
    cv.put(MovieContract.MovieEntry.COLUMN_MOVIE_IMAGE_URL, movie.getImageUrl());
    cv.put(MovieContract.MovieEntry.COLUMN_MOVIE_POPULARITY, movie.getPopularity());
    cv.put(MovieContract.MovieEntry.COLUMN_MOVIE_VOTE_AVERAGE, movie.getVoteAverage());
    cv.put(MovieContract.MovieEntry.COLUMN_MOVIE_RELEASE_DATE, movie.getReleaseDate());
    cv.put(MovieContract.MovieEntry.COLUMN_MOVIE_OVERVIEW, movie.getOverview());
    return cv;
  }

  public long getId() {
    return mId;
  }

  public void setId(long id) {
    mId = id;
  }

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

  public double getPopularity() {
    return mPopularity;
  }

  public void setPopularity(double popularity) {
    mPopularity = popularity;
  }

  public double getVoteAverage() {
    return mVoteAverage;
  }

  public void setVoteAverage(double voteAverage) {
    mVoteAverage = voteAverage;
  }

  public String getReleaseDate() {
    return mReleaseDate;
  }

  public void setReleaseDate(String releaseDate) {
    mReleaseDate = releaseDate;
  }

  public String getOverview() {
    return mOverview;
  }

  public void setOverview(String overview) {
    mOverview = overview;
  }

  @Override public int describeContents() {
    return 0;
  }

  @Override public void writeToParcel(Parcel dest, int flags) {
    dest.writeLong(mId);
    dest.writeLong(mMovieId);
    dest.writeString(mTitle);
    dest.writeString(mImageUrl);
    dest.writeDouble(mPopularity);
    dest.writeDouble(mVoteAverage);
    dest.writeString(mReleaseDate);
    dest.writeString(mOverview);
  }
}
