package com.teddytab.dilemma;

import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import com.google.gson.JsonSyntaxException;
import com.teddytab.dilemma.model.ApiResponse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public abstract class ApiResponseTask extends AsyncTask<Pair<String, String>, Void, ApiResponse> {
	private final static String TAG = "ApiResponseTask";

	protected final OkHttpClient httpClient = new OkHttpClient.Builder()
			.connectTimeout(5000, TimeUnit.MILLISECONDS)
			.readTimeout(5000, TimeUnit.MILLISECONDS)
			.build();

	abstract protected Request makeRequest(Pair<String, String>... params)
			throws UnsupportedEncodingException;

	@Override
	protected ApiResponse doInBackground(Pair<String, String>... params) {
		try {
			Request request = makeRequest(params);
			Log.v(TAG, String.format("HTTP %s %s", request.method(), request.url()));
			Response response = httpClient.newCall(request).execute();
			if (response != null && response.isSuccessful()) {
				return Utils.fromJson(response.body().string(), ApiResponse.class);
			}
			return null;
		} catch (JsonSyntaxException e) {
			Log.e(TAG, "", e);
			return null;
		} catch (IOException e) {
			Log.e(TAG, "", e);
			return null;
		}
	}
}
