package de.appmotion.popularmovies;

import android.content.Context;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import de.appmotion.popularmovies.data.Movie;
import de.appmotion.popularmovies.data.source.local.MovieContract;
import de.appmotion.popularmovies.data.source.remote.NetworkUtils;
import de.appmotion.popularmovies.databinding.MovieItemBinding;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Movie} from a {@link Cursor}.
 */
class MovieTopRatedCursorAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  // {@link ViewType} Type
  private static final int VIEW_TYPE_DEFAULT = 0;
  private final ListItemClickListener mOnClickListener;
  private final @NetworkUtils.ImageSize String mRequiredImageSize;
  private final Context mContext;
  // Holds on to the cursor to display the movie list
  private Cursor mCursor;

  MovieTopRatedCursorAdapter(Context context, @NetworkUtils.ImageSize String requiredImageSize, ListItemClickListener listener) {
    mContext = context;
    mRequiredImageSize = requiredImageSize;
    mOnClickListener = listener;
    setHasStableIds(true);
  }

  @Override public @ViewType int getItemViewType(int position) {
    return VIEW_TYPE_DEFAULT;
  }

  @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, @ViewType int viewType) {
    Context context = parent.getContext();
    LayoutInflater inflater = LayoutInflater.from(context);

    RecyclerView.ViewHolder viewHolder;

    switch (viewType) {
      case VIEW_TYPE_DEFAULT: {
        final View view = inflater.inflate(R.layout.movie_item, parent, false);
        viewHolder = new ViewHolderMovieItem(view);
        break;
      }
      default:
        throw new IllegalArgumentException("Invalid view type, value of " + viewType);
    }

    return viewHolder;
  }

  // Replace the contents of a view (invoked by the layout manager)
  @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    // Move the mCursor to the position of the item to be displayed
    if (!mCursor.moveToPosition(position)) {
      return; // bail if returned null
    }

    int viewType = getItemViewType(position);

    switch (viewType) {
      case VIEW_TYPE_DEFAULT: {
        final ViewHolderMovieItem viewHolder = (ViewHolderMovieItem) holder;
        viewHolder.bind();
        break;
      }
      default:
        throw new IllegalArgumentException("Invalid view type, value of " + viewType);
    }
  }

  @Override public int getItemCount() {
    if (mCursor == null) {
      return 0;
    }
    return mCursor.getCount();
  }

  @Override public long getItemId(int position) {
    if (mCursor != null && mCursor.moveToPosition(position)) {
      return mCursor.getLong(mCursor.getColumnIndexOrThrow(MovieContract.MovieEntry._ID));
    }
    return super.getItemId(position);
  }

  /**
   * Swaps the Cursor currently held in the adapter with a new one
   * and triggers a UI refresh
   *
   * @param newCursor the new cursor that will replace the existing one
   */
  public void swapCursor(Cursor newCursor) {
    mCursor = newCursor;
    notifyDataSetChanged();
  }

  /**
   * The interface that receives onClick messages.
   */
  public interface ListItemClickListener {
    void onListItemClick(Movie movie);
  }

  @Retention(RetentionPolicy.CLASS) @IntDef({ VIEW_TYPE_DEFAULT }) @interface ViewType {
  }

  /**
   * ViewHolder MovieItem
   */
  private class ViewHolderMovieItem extends RecyclerView.ViewHolder implements View.OnClickListener {
    MovieItemBinding mItemBinding;

    ViewHolderMovieItem(View itemView) {
      super(itemView);
      mItemBinding = DataBindingUtil.bind(itemView);
      itemView.setOnClickListener(this);
    }

    void bind() {
      final Movie movie = Movie.from(mCursor);

      // Load Movie Image
      Picasso.with(itemView.getContext())
          .load(NetworkUtils.buildMovieImageUri(mRequiredImageSize, movie.getImageUrl()))
          .placeholder(android.R.drawable.screen_background_light_transparent)
          .error(R.drawable.movie_empty)
          .into(mItemBinding.ivMovieImage, new Callback() {
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
      mCursor.moveToPosition(clickedPosition);
      final Movie movie = Movie.from(mCursor);
      mOnClickListener.onListItemClick(movie);
    }
  }
}
