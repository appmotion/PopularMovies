package de.appmotion.popularmovies.data.dto;

public class Movie {

  private final Long id;
  private final String imageUrl;

  public Movie(Long id, String imageUrl) {
    this.id = id;
    this.imageUrl = imageUrl;
  }

  public Long getId() {
    return id;
  }

  public String getImageUrl() {
    return imageUrl;
  }
}
