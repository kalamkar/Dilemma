package com.teddytab.dilemma.providers;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.teddytab.dilemma.Utils;
import com.teddytab.dilemma.model.Media;
import com.teddytab.dilemma.model.Media.Attribution;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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

	protected final OkHttpClient httpClient = new OkHttpClient.Builder()
			.connectTimeout(5000, TimeUnit.MILLISECONDS)
			.readTimeout(5000, TimeUnit.MILLISECONDS)
			.build();

	@Override
	protected List<Media> doInBackground(String... query) {
		List<Media> mediaList = new ArrayList<Media>();
		try {
			Uri.Builder uri = Uri.parse(SEARCH_URL).buildUpon();
			uri.appendQueryParameter("as_rights", Uri.encode(RIGHTS));
			if (query.length > 0) {
				uri.appendQueryParameter("q", query[0]);
			}
			String requestUrl = uri.build().toString();
			Log.v(TAG, requestUrl);
			Response response =
					httpClient.newCall(new Request.Builder().url(requestUrl).build()).execute();
			if (response == null || response.isSuccessful()) {
				return mediaList;
			}
			Result result = Utils.fromJson(response.body().string(), Result.class);
			if (result == null || result.responseStatus != 200) {
				Log.v(TAG, response.toString());
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
