package com.teddytab.dilemma;

import android.app.Application;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;
import android.util.Pair;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.ads.identifier.AdvertisingIdClient.Info;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.teddytab.dilemma.messaging.GCMUtils;
import com.teddytab.dilemma.model.ApiResponse;
import com.teddytab.dilemma.model.Question;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.FormBody;
import okhttp3.Request;

@SuppressWarnings("unchecked")
public class App extends Application {
	private static final String TAG = "App";

	public final Map<String, Question> questions = new HashMap<String, Question>();
	public final Map<String, Integer> answers = new HashMap<String, Integer>();
	public final Map<String, Map<String, Integer[]>> stats = new HashMap<String, Map<String, Integer[]>>();

	public RequestQueue requestQueue;
	public ImageLoader imageLoader;
	private GoogleCloudMessaging gcm;

	public String idfa;
	private String registrationId;

	private static final int CACHE_SIZE = 50 * 1024 * 1024;
	final LruCache<String, Bitmap> thumbnails = new LruCache<String, Bitmap>(CACHE_SIZE) {
		@Override
		protected int sizeOf(String key, Bitmap value) {
			return value.getByteCount();
	   }
	};

	final LruCache<String, Bitmap> photos = new LruCache<String, Bitmap>(CACHE_SIZE) {
		@Override
		protected int sizeOf(String key, Bitmap value) {
			return value.getByteCount();
	   }
	};

	@Override
	public void onCreate() {
		super.onCreate();
		registrationId = GCMUtils.getRegistrationId(this);
		requestAdvertisingId();
		requestQueue = Volley.newRequestQueue(this);
		imageLoader = new ImageLoader(requestQueue, new ImageLoader.ImageCache() {
		    private final LruCache<String, Bitmap> mCache = new LruCache<String, Bitmap>(100);
		    @Override
			public void putBitmap(String url, Bitmap bitmap) {
		        mCache.put(url, bitmap);
		    }
		    @Override
			public Bitmap getBitmap(String url) {
		        return mCache.get(url);
		    }
		});
	}

	private void requestAdvertisingId() {
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				try {
					Info info = AdvertisingIdClient.getAdvertisingIdInfo(App.this);
					return info.getId();
				} catch (IllegalStateException e) {
					Log.w(TAG, e);
				} catch (GooglePlayServicesRepairableException e) {
					Log.w(TAG, e);
				} catch (IOException e) {
					Log.w(TAG, e);
				} catch (GooglePlayServicesNotAvailableException e) {
					Log.w(TAG, e);
				}
				return null;
			}

            @Override
            protected void onPostExecute(String adId) {
                if (adId == null) {
                    return;
                }
                idfa = adId;
                if (registrationId != null) {
                    new RegisterDevice().execute();
                }
            }
        }.execute();
	}

	public void requestRegistrationId() {
		if (registrationId != null && idfa != null) {
			Log.i(TAG, "Device already registered, registration ID = " + registrationId);
			new RegisterDevice().execute();
			return;
		}
	    new AsyncTask<Void, Void, String>() {
			@Override
	        protected String doInBackground(Void... params) {
	            try {
	            	if (gcm == null) {
	                    gcm = GoogleCloudMessaging.getInstance(App.this);
	                }
	                return gcm.register(Config.GCM_SENDER_ID);
	            } catch (IOException ex) {
	                Log.w(TAG, ex);
	            }
	            return null;
	        }

            @Override
            protected void onPostExecute(String regId) {
                if (regId == null) {
                    return;
                }
                registrationId = regId;
                Log.i(TAG, "Device registered, registration ID = " + registrationId);
                GCMUtils.storeRegistrationId(App.this, registrationId);
                if (idfa != null) {
                    new RegisterDevice().execute();
                }
            }
	    }.execute(null, null, null);
	}

	private class RegisterDevice extends ApiResponseTask {
		@Override
		protected Request makeRequest(Pair<String, String>... params)
				throws UnsupportedEncodingException {
			FormBody body = new FormBody.Builder()
					.add("idfa", idfa)
					.add("type", "GOOGLE")
					.add("push_token", registrationId)
					.build();
			return new Request.Builder().url(Config.USER_URL).post(body).build();
		}

		@Override
		protected void onPostExecute(ApiResponse result) {
			new GetAnswersList().execute();
			super.onPostExecute(result);
		}
	}

	private class GetAnswersList extends ApiResponseTask {
		@Override
		protected Request makeRequest(Pair<String, String>... params)
				throws UnsupportedEncodingException {
			Uri.Builder uri = Uri.parse(Config.ANSWER_URL).buildUpon();
			uri.appendQueryParameter("idfa", idfa);
			uri.appendQueryParameter("type", "GOOGLE");
			for (Pair<String, String> param : params) {
				uri.appendQueryParameter(param.first, param.second);
			}
			return new Request.Builder().url(uri.build().toString()).build();
		}

		@Override
		protected void onPostExecute(ApiResponse result) {
			super.onPostExecute(result);
			if (result == null) {
				return;
			} else if (!"OK".equalsIgnoreCase(result.code)) {
				Toast.makeText(App.this, result.message, Toast.LENGTH_LONG).show();
				return;
			}
			answers.clear();
			for (Question question : result.questions) {
				if (question != null && question.answer != null) {
					answers.put(question.id, question.answer.choiceIndex);
					questions.put(question.id, question);
					stats.put(question.id, question.stats);
				}
			}
		}
	}
}
