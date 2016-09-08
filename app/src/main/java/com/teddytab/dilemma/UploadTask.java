package com.teddytab.dilemma;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import com.teddytab.dilemma.model.ApiResponse;

public class UploadTask extends AsyncTask<Pair<String, String>, Void, Pair<Integer, String>> {
	private static final String TAG = "UploadTask";

	private final String localUrl;
	private final InputStream localInputStream;
	private final String contentType;
	private final UploadListener listener;

	public interface UploadListener {
		public void onUploadSuccess(ApiResponse response);
		public void onUploadError();
	}

	public UploadTask(InputStream localInputStream, String contentType, UploadListener listener) {
		this.localInputStream = localInputStream;
		this.localUrl = null;
		this.contentType = contentType;
		this.listener = listener;
	}
	
	public UploadTask(String localUrl, String contentType, UploadListener listener) {
		this.localUrl = localUrl;
		this.localInputStream = null;
		this.contentType = contentType;
		this.listener = listener;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Pair<Integer, String> doInBackground(Pair<String, String>... params) {
		try {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			if (localUrl != null) {
				URL parsedUrl = new URL(localUrl);
				URLConnection connection = parsedUrl.openConnection();
				copy(new BufferedInputStream(connection.getInputStream()), output);
			} else {
				copy(new BufferedInputStream(localInputStream), output);
			}
			
			return Utils.uploadFile(Config.MEDIA_URL, output.toByteArray(), contentType, params);
		} catch (Throwable t) {
			Log.e(TAG, "", t);
			return null;
		}
	}

	@Override
	protected void onPostExecute(Pair<Integer, String> response) {
		if (response == null) {
			Log.w(TAG, "Got null response");
			return;
		}
		Log.d(TAG, String.format("Got response %d %s", response.first, response.second));
		if (response.first != 200) {
			listener.onUploadError();
		} else {
			listener.onUploadSuccess(Utils.fromJson(response.second, ApiResponse.class));
		}
	}
	
	public static void copy(InputStream input, OutputStream output) throws IOException {
		byte[] buffer = new byte[512 * 1024];
		int bytesRead = 0;
		while ((bytesRead = input.read(buffer)) != -1) {
			output.write(buffer, 0, bytesRead);
		}
		output.close();
		input.close();
	}
}