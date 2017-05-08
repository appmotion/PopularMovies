package de.appmotion.popularmovies;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.IntDef;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import de.appmotion.popularmovies.data.PopularMoviesContract;
import de.appmotion.popularmovies.data.dto.Movie;
import de.appmotion.popularmovies.utilities.NetworkUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Movie}.
 */
class FavoriteMovieListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  // {@link ViewType} Type
  private static final int DEFAULT = 0;
  private final ListItemClickListener mOnClickListener;
  private final @NetworkUtils.ImageSize String mRequiredImageSize;
  // Holds on to the cursor to display the favoritelist
  private Cursor mCursor;

  FavoriteMovieListAdapter(@NetworkUtils.ImageSize String requiredImageSize, ListItemClickListener listener) {
    mRequiredImageSize = requiredImageSize;
    mOnClickListener = listener;
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
    // Move the mCursor to the position of the item to be displayed
    if (!mCursor.moveToPosition(position)) {
      return; // bail if returned null
    }

    /* Movie View */
    if (DEFAULT == getItemViewType(position)) {
      final ViewHolderMovieItem viewHolder = (ViewHolderMovieItem) holder;
      viewHolder.bind();
    }
  }

  @Override public int getItemCount() {
    if (mCursor == null) {
      return 0;
    }
    return mCursor.getCount();
  }

  /**
   * Swaps the Cursor currently held in the adapter with a new one
   * and triggers a UI refresh
   *
   * @param newCursor the new cursor that will replace the existing one
   */
  public void swapCursor(Cursor newCursor) {
    // Always close the previous mCursor first
    if (mCursor != null) {
      mCursor.close();
    }
    mCursor = newCursor;
    if (newCursor != null) {
      // Force the RecyclerView to refresh
      notifyDataSetChanged();
    }
  }

  /**
   * The interface that receives onClick messages.
   */
  public interface ListItemClickListener {
    void onListItemClick(long movieId);
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

    void bind() {
      final long rowId = mCursor.getLong(mCursor.getColumnIndexOrThrow(PopularMoviesContract.FavoritelistEntry._ID));
      final String movieTitle =
          mCursor.getString(mCursor.getColumnIndexOrThrow(PopularMoviesContract.FavoritelistEntry.COLUMN_MOVIE_TITLE));
      final String movieImageUrl =
          mCursor.getString(mCursor.getColumnIndexOrThrow(PopularMoviesContract.FavoritelistEntry.COLUMN_MOVIE_IMAGE_URL));

      // Load Movie Image
      Picasso.with(itemView.getContext())
          .load(NetworkUtils.buildMovieImageUri(mRequiredImageSize, movieImageUrl))
          .placeholder(android.R.drawable.screen_background_light_transparent)
          .error(R.drawable.movie_empty)
          .into(movieImage, new Callback() {
            @Override public void onSuccess() {
            }

            @Override public void onError() {
            }
          });

      // Set rowId as a tag of an itemView, so we can always get the rowId from a ViewHolderMovieItem
      itemView.setTag(rowId);
    }

    /**
     * Called whenever a user clicks on an item in the list.
     *
     * @param v The View that was clicked
     */
    @Override public void onClick(View v) {
      mOnClickListener.onListItemClick((long) v.getTag());
    }
  }
}
