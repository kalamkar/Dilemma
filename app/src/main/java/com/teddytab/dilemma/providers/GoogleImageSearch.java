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

public abstract class GoogleImageSearch extends AsyncTask<String, Void, List<Media>> {
	private final static String TAG = "GoogleImageSearch";

	private static final String SEARCH_URL =
			"https://ajax.googleapis.com/ajax/services/search/images?v=1.0&rsz=8";
	private static final String RIGHTS =
			"(cc_publicdomain|cc_attribute|cc_sharealike).-(cc_noncommercial|cc_nonderived)";

	static class Photo {
		String imageId;
		String title;
		String unescapedUrl;
		String originalContextUrl;
		String titleNoFormatting;
		String visibleUrl;
	}

	static class ResponseData {
		Photo results[];
	}

	static class Result {
		ResponseData responseData;
		int responseStatus;
	}

	protected final DefaultHttpClient httpClient = new DefaultHttpClient();

	public GoogleImageSearch() {
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
			Uri.Builder uri = Uri.parse(SEARCH_URL).buildUpon();
			uri.appendQueryParameter("as_rights", Uri.encode(RIGHTS));
			if (query.length > 0) {
				uri.appendQueryParameter("q", query[0]);
			}
			String requestUrl = uri.build().toString();
			Log.v(TAG, requestUrl);
			HttpResponse response = httpClient.execute(new HttpGet(requestUrl));
			if (response != null && response.getEntity() != null) {
				response.getEntity().writeTo(output);
			}
			output.close();
			Result result = Utils.fromJson(output.toString(), Result.class);
			if (result == null || result.responseStatus != 200) {
				Log.v(TAG, output.toString());
				return mediaList;
			}
			for (Photo photo : result.responseData.results) {
				Media media = new Media();
				media.url = photo.unescapedUrl;
				media.tags = new String[] { query[0] };
				Attribution attr = new Attribution();
				attr.title = photo.visibleUrl;
				attr.url = photo.originalContextUrl;
				attr.description = photo.titleNoFormatting;
				media.attribution = attr;
				mediaList.add(media);
			}
		} catch (Throwable e) {
			Log.e(TAG, "", e);
		}
		return mediaList;
	}
}
