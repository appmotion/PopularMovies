package de.appmotion.popularmovies;

import android.content.Context;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import de.appmotion.popularmovies.data.dto.Movie;
import de.appmotion.popularmovies.utilities.NetworkUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Movie} from a {@link List<Movie>}.
 */
class MovieListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  // {@link ViewType} Type
  private static final int DEFAULT = 0;
  private final ListItemClickListener mOnClickListener;
  private final @NetworkUtils.ImageSize String mRequiredImageSize;
  private List<Movie> mMovieList;

  MovieListAdapter(@Nullable List<Movie> movieList, @NetworkUtils.ImageSize String requiredImageSize, ListItemClickListener listener) {
    mMovieList = movieList;
    mRequiredImageSize = requiredImageSize;
    mOnClickListener = listener;
    mMovieList = new ArrayList<>(0);
  }

  @Override public @ViewType int getItemViewType(int position) {
    return DEFAULT;
  }

  @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, @ViewType int viewType) {
    Context context = parent.getContext();
    int layoutIdForListItem = R.layout.movie_item;
    LayoutInflater inflater = LayoutInflater.from(context);

    View view;
    RecyclerView.ViewHolder viewHolder = null;

     /* Movie View */
    if (DEFAULT == viewType) {
      view = inflater.inflate(layoutIdForListItem, parent, false);
      viewHolder = new ViewHolderMovieItem(view);
    }
    return viewHolder;
  }

  // Replace the contents of a view (invoked by the layout manager)
  @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    /* Movie View */
    if (DEFAULT == getItemViewType(position)) {
      final ViewHolderMovieItem viewHolder = (ViewHolderMovieItem) holder;
      viewHolder.bind(position);
    }
  }

  @Override public int getItemCount() {
    if (mMovieList == null) {
      return 0;
    }
    return mMovieList.size();
  }

  @Override public long getItemId(int position) {
    if (mMovieList == null) {
      return RecyclerView.NO_ID;
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

  /**
   * The interface that receives onClick messages.
   */
  public interface ListItemClickListener {
    void onListItemClick(Movie movie);
  }

  @Retention(RetentionPolicy.CLASS) @IntDef({ DEFAULT }) @interface ViewType {
  }

  /**
   * ViewHolder MovieItem
   */
  class ViewHolderMovieItem extends RecyclerView.ViewHolder implements View.OnClickListener {
    @BindView(R.id.iv_movie_image) ImageView movieImage;

    ViewHolderMovieItem(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
      itemView.setOnClickListener(this);
    }

    void bind(int listIndex) {
      final Movie movie = mMovieList.get(listIndex);

      // Load Movie Image
      Picasso.with(itemView.getContext())
          .load(NetworkUtils.buildMovieImageUri(mRequiredImageSize, movie.getImageUrl()))
          .placeholder(android.R.drawable.screen_background_light_transparent)
          .error(R.drawable.movie_empty)
          .into(movieImage, new Callback() {
            @Override public void onSuccess() {
            }

            @Override public void onError() {
            }
          });
    }

    /**
     * Called whenever a user clicks on an item in the list.
     *
     * @param v The View that was clicked
     */
    @Override public void onClick(View v) {
      int clickedPosition = getAdapterPosition();
      mOnClickListener.onListItemClick(mMovieList.get(clickedPosition));
    }
  }
}
