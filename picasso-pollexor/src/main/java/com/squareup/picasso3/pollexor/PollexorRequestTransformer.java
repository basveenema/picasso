package com.squareup.picasso3.pollexor;

import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;
import com.squareup.picasso3.Request;
import com.squareup.pollexor.Thumbor;
import com.squareup.pollexor.ThumborUrlBuilder;
import com.squareup.pollexor.ThumborUrlBuilder.ImageFormat;

import static com.squareup.picasso3.Picasso.RequestTransformer;
import static com.squareup.pollexor.ThumborUrlBuilder.format;

/**
 * A {@link RequestTransformer} that changes requests to use {@link Thumbor} for some remote
 * transformations.
 * By default images are only transformed with Thumbor if they have a size set.
 */
public class PollexorRequestTransformer implements RequestTransformer {

  private static final Callback NONE = new Callback() {
    @Override
    public void configure(ThumborUrlBuilder ignored) { }
  };
  private final Thumbor thumbor;
  private final boolean alwaysTransform;
  private final Callback callback;

  /** Create a transformer for the specified {@link Thumbor}. */
  public PollexorRequestTransformer(@NonNull Thumbor thumbor) {
    this(thumbor, false, NONE);
  }

  /**
   * Create a transformer for the specified {@link Thumbor} which always transforms images using
   * Thumbor even when resize is not set.
   */
  public PollexorRequestTransformer(@NonNull Thumbor thumbor, boolean alwaysTransform) {
    this(thumbor, alwaysTransform, NONE);
  }

  /**
   * Create a transformer for the specified {@link Thumbor} which allows to configure a
   * {@link ThumborUrlBuilder} using a {@link Callback}
   */
  public PollexorRequestTransformer(@NonNull Thumbor thumbor, @NonNull Callback callback) {
    this(thumbor, false, callback);
  }

  /**
   * Create a transformer for the specified {@link Thumbor} which always transforms images using
   * Thumbor even when resize is not set and allows to configure a {@link ThumborUrlBuilder} using
   * a {@link Callback}
   */
  public PollexorRequestTransformer(@NonNull Thumbor thumbor, boolean alwaysTransform,
                                    @NonNull Callback callback) {
    this.thumbor = thumbor;
    this.alwaysTransform = alwaysTransform;
    this.callback = callback;
  }

  @NonNull @Override public Request transformRequest(@NonNull Request request) {
    if (request.resourceId != 0) {
      return request; // Don't transform resource requests.
    }
    Uri uri = request.uri;
    if (uri == null) {
      throw new IllegalArgumentException("Null uri passed to " + getClass().getCanonicalName());
    }
    String scheme = uri.getScheme();
    if (!"https".equals(scheme) && !"http".equals(scheme)) {
      return request; // Thumbor only supports remote images.
    }
    // Only transform requests that have resizes unless `alwaysTransform` is set.
    if (!request.hasSize() && !alwaysTransform) {
      return request;
    }

    // Start building a new request for us to mutate.
    Request.Builder newRequest = request.newBuilder();

    // Create the url builder to use.
    ThumborUrlBuilder urlBuilder = thumbor.buildImage(uri.toString());
    callback.configure(urlBuilder);

    // Resize the image to the target size if it has a size.
    if (request.hasSize()) {
      urlBuilder.resize(request.targetWidth, request.targetHeight);
      newRequest.clearResize();
    }

    // If the center inside flag is set, perform that with Thumbor as well.
    if (request.centerInside) {
      urlBuilder.fitIn();
      newRequest.clearCenterInside();
    }

    // If the Android version is modern enough use WebP for downloading.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      urlBuilder.filter(format(ImageFormat.WEBP));
    }

    // Update the request with the completed Thumbor URL.
    newRequest.setUri(Uri.parse(urlBuilder.toUrl()));

    return newRequest.build();
  }

  interface Callback {
    void configure(ThumborUrlBuilder builder);
  }
}
