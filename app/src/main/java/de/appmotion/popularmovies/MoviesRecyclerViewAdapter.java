package de.appmotion.popularmovies;

import android.content.Intent;
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

import static de.appmotion.popularmovies.MainActivity.EXTRA_MOVIE_ID;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Movie}.
 */
class MoviesRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  private final BaseActivity mActivity;
  private List<Movie> mMovieList;

  MoviesRecyclerViewAdapter(BaseActivity activity) {
    mActivity = activity;
    mMovieList = new ArrayList<>(0);
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
          .load(NetworkUtils.buildMovieImageUri(mActivity.mRequiredImageSize, movie.getImagePath()))
          .placeholder(android.R.drawable.screen_background_light_transparent)
          .error(R.drawable.movie_empty)
          .into(viewHolderMovieItem.movieImage, new Callback() {
            @Override public void onSuccess() {
            }

            @Override public void onError() {
            }
          });

      // Set ClickListener on itemView
      viewHolderMovieItem.itemView.setOnClickListener(new ViewClickListener(movie));
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
      movieImage = (ImageView) view.findViewById(R.id.iv_movie_image);
    }
  }

  private static class ViewClickListener implements View.OnClickListener {
    private final Movie movie;

    ViewClickListener(Movie movie) {
      this.movie = movie;
    }

    @Override public void onClick(View v) {
      // Show Movie Detail Screen
      Intent intent = new Intent(v.getContext(), MovieDetailActivity.class);
      intent.putExtra(EXTRA_MOVIE_ID, movie.getId());
      v.getContext().startActivity(intent);
    }
  }
}
