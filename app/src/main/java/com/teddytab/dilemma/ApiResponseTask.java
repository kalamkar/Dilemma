package com.teddytab.dilemma;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import com.google.gson.JsonSyntaxException;
import com.teddytab.dilemma.model.ApiResponse;

public abstract class ApiResponseTask extends AsyncTask<Pair<String, String>, Void, ApiResponse> {
	private final static String TAG = "ApiResponseTask";

	protected final DefaultHttpClient httpClient = new DefaultHttpClient();

	public ApiResponseTask() {
		HttpParams params = httpClient.getParams();
		HttpConnectionParams.setConnectionTimeout(params, 5000);
		HttpConnectionParams.setSoTimeout(params, 5000);
		httpClient.setParams(params);
	}

	abstract protected HttpRequestBase makeRequest(Pair<String, String>... params)
			throws UnsupportedEncodingException;

	@Override
	protected ApiResponse doInBackground(Pair<String, String>... params) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			HttpRequestBase request = makeRequest(params);
			Log.v(TAG, String.format("HTTP %s %s", request.getMethod(), request.getURI()));
			HttpResponse response = httpClient.execute(request);
			if (response != null && response.getEntity() != null) {
				response.getEntity().writeTo(output);
			}
			output.close();
			return Utils.fromJson(output.toString(), ApiResponse.class);
		} catch (JsonSyntaxException e) {
			Log.e(TAG, "", e);
			return null;
		} catch (IOException e) {
			Log.e(TAG, "", e);
			return null;
		}
	}
}
