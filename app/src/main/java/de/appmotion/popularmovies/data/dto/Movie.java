package de.appmotion.popularmovies.data.dto;

public class Movie {

  private final Long id;
  private final String imagePath;

  public Movie(Long id, String imagePath) {
    this.id = id;
    this.imagePath = imagePath;
  }

  public Long getId() {
    return id;
  }

  public String getImagePath() {
    return imagePath;
  }
}
