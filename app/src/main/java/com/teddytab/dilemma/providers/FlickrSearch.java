package com.teddytab.dilemma.providers;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.teddytab.dilemma.Utils;
import com.teddytab.dilemma.model.Media;
import com.teddytab.dilemma.model.Media.Attribution;

public abstract class FlickrSearch extends AsyncTask<String, Void, List<Media>> {
	private final static String TAG = "FlickrResponseTask";

	private static final String FLICKR_SEARCH_URL = "https://api.flickr.com/services/rest/?"
			+ "method=flickr.photos.search&api_key=9f0bb32217421e5c605dc46a87cf2688&"
			+ "license=4,7&per_page=20&format=json&nojsoncallback=1&sort=relevance&tag_mode=all&"
			+ "extras=owner_name";

	// Farm ID, Server ID, ID, Secret
	private static final String FLICKR_PHOTO_URL = "https://farm%s.staticflickr.com/%s/%s_%s.jpg";

	// User ID/Owner, ID
	private static final String FLICKR_PHOTO_PAGE_URL = "https://www.flickr.com/photos/%s/%s";

	static class Photo {
		String id;
		String owner;
		String ownername;
		String secret;
		String server;
		String farm;
		String title;
	}

	static class Photos {
		int page;
		int perpage;
		String pages;
		String total;
		Photo photo[];
	}

	static class Result {
		Photos photos;
		String stat;
	}

	protected final DefaultHttpClient httpClient = new DefaultHttpClient();

	public FlickrSearch() {
		HttpParams params = httpClient.getParams();
		HttpConnectionParams.setConnectionTimeout(params, 5000);
		HttpConnectionParams.setSoTimeout(params, 5000);
		httpClient.setParams(params);
	}

	@Override
	protected List<Media> doInBackground(String... query) {
		List<Media> mediaList = new ArrayList<Media>();
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			Uri.Builder uri = Uri.parse(FLICKR_SEARCH_URL).buildUpon();
			if (query.length > 0) {
				uri.appendQueryParameter("text", query[0]);
			}
			String requestUrl = uri.build().toString();
			Log.v(TAG, requestUrl);
			HttpResponse response = httpClient.execute(new HttpGet(requestUrl));
			if (response != null && response.getEntity() != null) {
				response.getEntity().writeTo(output);
			}
			output.close();
			Result result = Utils.fromJson(output.toString(), Result.class);
			if (result == null || !"ok".equalsIgnoreCase(result.stat)) {
				Log.v(TAG, output.toString());
				return mediaList;
			}
			for (Photo photo : result.photos.photo) {
				Media media = new Media();
				media.url = String.format(FLICKR_PHOTO_URL, photo.farm, photo.server, photo.id,
						photo.secret);
				media.tags = new String[] { query[0] };
				Attribution attr = new Attribution();
				attr.title = photo.ownername;
				attr.url = String.format(FLICKR_PHOTO_PAGE_URL, photo.owner, photo.id);
				attr.description = photo.title;
				media.attribution = attr;
				mediaList.add(media);
			}
		} catch (Throwable e) {
			Log.e(TAG, "", e);
		}
		return mediaList;
	}
}
