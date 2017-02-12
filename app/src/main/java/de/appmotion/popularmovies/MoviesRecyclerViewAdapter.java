package de.appmotion.popularmovies;

import android.app.Activity;
import android.content.res.Configuration;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import de.appmotion.popularmovies.dto.Movie;
import de.appmotion.popularmovies.utilities.NetworkUtils;
import java.util.ArrayList;
import java.util.List;

class MoviesRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  private final Activity mActivity;
  private final NetworkUtils.ImageSize mRequiredImageSize;
  private List<Movie> mMovieList;

  MoviesRecyclerViewAdapter(Activity activity) {
    mActivity = activity;
    mMovieList = new ArrayList<>(0);
    // Get current device configuration
    Configuration configuration = activity.getResources().getConfiguration();
    int screenWidthDp = configuration.screenWidthDp;
    if (screenWidthDp <= 92) {
      mRequiredImageSize = NetworkUtils.ImageSize.WIDTH92;
    }
    else if (screenWidthDp <= 154) {
      mRequiredImageSize = NetworkUtils.ImageSize.WIDTH154;
    }
    else if (screenWidthDp <= 185) {
      mRequiredImageSize = NetworkUtils.ImageSize.WIDTH185;
    }
    else if (screenWidthDp <= 342) {
      mRequiredImageSize = NetworkUtils.ImageSize.WIDTH342;
    }
    else if (screenWidthDp <= 500) {
      mRequiredImageSize = NetworkUtils.ImageSize.WIDTH500;
    }
    else if (screenWidthDp <= 780) {
      mRequiredImageSize = NetworkUtils.ImageSize.WIDTH780;
    }
    else {
      mRequiredImageSize = NetworkUtils.ImageSize.ORIGINAL;
    }
  }

  @Override public int getItemViewType(int position) {
    return ViewType.DEFAULT.typeNumber;
  }

  @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View v;
    RecyclerView.ViewHolder vh = null;

     /* Movie View */
    if (ViewType.DEFAULT.typeNumber == viewType) {
      v = LayoutInflater.from(parent.getContext()).inflate(R.layout.movie_item, parent, false);
      vh = new ViewHolderMovieItem(v);
    }
    return vh;
  }

  // Replace the contents of a view (invoked by the layout manager)
  @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    /* Movie View */
    if (ViewType.DEFAULT.typeNumber == getItemViewType(position)) {
      final ViewHolderMovieItem viewHolderMovieItem = (ViewHolderMovieItem) holder;
      Movie movie = mMovieList.get(position);

      // Load Movie Image
      Picasso.with(mActivity)
          .load(NetworkUtils.buildMovieImageUri(mRequiredImageSize, movie.getImagePath()))
          .placeholder(android.R.drawable.screen_background_light_transparent)
          .error(R.drawable.movie_empty)
          .into(viewHolderMovieItem.movieImage, new Callback() {
            @Override public void onSuccess() {
            }

            @Override public void onError() {
            }
          });

      // Set ClickListener on itemView
      viewHolderMovieItem.itemView.setOnClickListener(new ViewClickListener(viewHolderMovieItem, movie));
    }
  }

  // Return the size of your dataset (invoked by the layout manager)
  @Override public int getItemCount() {
    if (mMovieList == null) {
      return 0;
    }
    return mMovieList.size();
  }

  @Override public long getItemId(int position) {
    if (mMovieList == null) {
      return 0L;
    }
    return mMovieList.get(position).getId();
  }

  void replaceMovieList(List<Movie> newList) {
    mMovieList = newList;
    notifyDataSetChanged();
  }

  void addMovieList(List<Movie> newList) {
    if (mMovieList == null) {
      mMovieList = new ArrayList<>(0);
    }
    mMovieList.addAll(newList);
    notifyDataSetChanged();
  }

  void clearMovieList() {
    if (mMovieList != null) {
      mMovieList.clear();
      notifyDataSetChanged();
    }
  }

  private enum ViewType {
    DEFAULT(0);

    public final int typeNumber;

    ViewType(final int typeNumber) {
      this.typeNumber = typeNumber;
    }
  }

  /**
   * ViewHolder MovieItem
   */
  private static class ViewHolderMovieItem extends RecyclerView.ViewHolder {
    View itemView;
    ImageView movieImage;

    ViewHolderMovieItem(View view) {
      super(view);
      itemView = view;
      movieImage = (ImageView) view.findViewById(R.id.iv_movie);
    }
  }

  private static class ViewClickListener implements View.OnClickListener {
    private final ViewHolderMovieItem viewHolderMovieItem;
    private final Movie movie;

    ViewClickListener(ViewHolderMovieItem viewHolderMovieItem, Movie movie) {
      this.viewHolderMovieItem = viewHolderMovieItem;
      this.movie = movie;
    }

    @Override public void onClick(View v) {
      // Show Movie Detail Screen
    }
  }
}
