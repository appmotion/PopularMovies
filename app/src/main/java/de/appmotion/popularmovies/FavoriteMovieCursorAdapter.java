package de.appmotion.popularmovies;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.IntDef;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import de.appmotion.popularmovies.data.FavoriteMovie;
import de.appmotion.popularmovies.data.source.local.MovieContract;
import de.appmotion.popularmovies.data.source.remote.NetworkUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * {@link RecyclerView.Adapter} that can display a {@link FavoriteMovie} from a {@link android.database.Cursor}.
 */
class FavoriteMovieCursorAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  // {@link ViewType} Type
  private static final int DEFAULT = 0;
  private final ListItemClickListener mOnClickListener;
  private final @NetworkUtils.ImageSize String mRequiredImageSize;
  private final Context mContext;
  // Holds on to the cursor to display the favoritelist
  private Cursor mCursor;

  // An ItemTouchHelper for swiping movie items
  private ItemTouchHelper mMovieItemTouchHelper;

  FavoriteMovieCursorAdapter(Context context, @NetworkUtils.ImageSize String requiredImageSize, ListItemClickListener listener) {
    mContext = context;
    mRequiredImageSize = requiredImageSize;
    mOnClickListener = listener;
    setHasStableIds(true);
  }

  @Override public void onAttachedToRecyclerView(RecyclerView recyclerView) {
    super.onAttachedToRecyclerView(recyclerView);
    // Create an item touch helper to handle swiping items off the list
    mMovieItemTouchHelper = new MovieItemTouchHelper();
    mMovieItemTouchHelper.attachToRecyclerView(recyclerView);
  }

  @Override public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
    mMovieItemTouchHelper.attachToRecyclerView(null);
    super.onDetachedFromRecyclerView(recyclerView);
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

  @Override public long getItemId(int position) {
    if (mCursor != null && mCursor.moveToPosition(position)) {
      return mCursor.getLong(mCursor.getColumnIndexOrThrow(MovieContract.FavoriteMovieEntry._ID));
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
    // Always close the previous mCursor first
    if (mCursor != null) {
      mCursor.close();
    }
    mCursor = newCursor;
    notifyDataSetChanged();
  }

  /**
   * The interface that receives onClick messages.
   */
  public interface ListItemClickListener {
    void onListItemClick(FavoriteMovie favoriteMovie);
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
      final FavoriteMovie favoriteMovie = FavoriteMovie.from(mCursor);

      // Load Movie Image
      Picasso.with(itemView.getContext())
          .load(NetworkUtils.buildMovieImageUri(mRequiredImageSize, favoriteMovie.getImageUrl()))
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
      mCursor.moveToPosition(clickedPosition);
      final FavoriteMovie favoriteMovie = FavoriteMovie.from(mCursor);
      mOnClickListener.onListItemClick(favoriteMovie);
    }
  }

  class MovieItemTouchHelper extends ItemTouchHelper {

    MovieItemTouchHelper() {
      super(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

        @Override public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
          //do nothing, we only care about swiping
          return false;
        }

        // Called when a user swipes left or right on a ViewHolder
        @Override public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
          // remove from DB
          deleteFavoriteMovie(viewHolder.getItemId());
        }
      });
    }
  }

  /**
   * Removes the record with the specified id
   *
   * @param id the DB id to be removed
   */
  private void deleteFavoriteMovie(long id) {
    // Build appropriate uri with String row id appended
    String stringId = String.valueOf(id);
    Uri uri = MovieContract.FavoriteMovieEntry.CONTENT_URI;
    uri = uri.buildUpon().appendPath(stringId).build();
    // Delete a single row of data using a ContentResolver
    mContext.getContentResolver().delete(uri, null, null);
  }
}
