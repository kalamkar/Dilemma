package com.teddytab.dilemma;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;

import com.teddytab.dilemma.model.Question;

public class Utils {
	private static final String TAG = "Utils";

	private static final String CRLF = "\r\n";
	private static final String HYPHENS = "--";
	private static final String BOUNDARY = "******This_is_my_Boundary" + Math.random();

	private static final Map<String, Integer> COLORS = new HashMap<String, Integer>();

	static {
		COLORS.put("arts", R.color.red);
		COLORS.put("business", R.color.green);
		COLORS.put("computers", R.color.blue);
		COLORS.put("games", R.color.purple);
		COLORS.put("health", R.color.red);
		COLORS.put("home", R.color.orange);
		COLORS.put("recreation", R.color.turquoise);
		COLORS.put("science", R.color.green);
		COLORS.put("society", R.color.blue);
		COLORS.put("sports", R.color.purple);
	}

	public static String toJson(Object obj) {
		return Config.GSON.toJson(obj);
	}

	public static <T>T fromJson(String json, Class<T> classOfT) {
		return Config.GSON.fromJson(json, classOfT);
	}

	public static String join(String[] array, String separator) {
		if (array == null) {
			return "";
		}
		StringBuilder response = new StringBuilder();
		for (String element : array) {
			response.append(element).append(separator);
		}
		return response.toString().replaceAll(separator + "$", "");
	}

	public static <T> void fillList(List<T> list, T[] array) {
		list.removeAll(list);
		if (array == null) {
			return;
		}
		for (T t : array) {
			if (t != null) {
				list.add(t);
			}
		}
	}

	public static String getText(Activity activity, int id, String defaultValue) {
		String text = ((TextView) activity.findViewById(id)).getText().toString();
		if (text == null || "".equals(text)) {
			text = defaultValue;
		}
		return text;
	}

	public static String getText(View view, int id, String defaultValue) {
		String text = ((TextView) view.findViewById(id)).getText().toString();
		if (text == null || "".equals(text)) {
			text = defaultValue;
		}
		return text;
	}

	public static void setText(View view, int id, String value) {
		((TextView) view.findViewById(id)).setText(value);
	}

	public static void setText(Activity activity, int id, String value) {
		((TextView) activity.findViewById(id)).setText(value);
	}

	@SafeVarargs
	public static Pair<Integer, String> uploadFile(String url, byte[] data, String contentType,
			Pair<String, String>... params) throws IOException {

		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setDoInput(true);
		conn.setDoOutput(true);
		conn.setUseCaches(false);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Connection", "Keep-Alive");
		conn.setRequestProperty("ENCTYPE", "multipart/form-data");
		conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + BOUNDARY);

		DataOutputStream out = new DataOutputStream(conn.getOutputStream());

		for (Pair<String, String> param : params) {
			out.writeBytes(HYPHENS + BOUNDARY + CRLF);
			out.writeBytes("Content-Disposition: form-data; name=\"" + param.first + "\"" + CRLF);
			out.writeBytes(CRLF);
			out.write(param.second.getBytes(), 0, param.second.length());
			out.writeBytes(CRLF);
		}

		out.writeBytes(HYPHENS + BOUNDARY + CRLF);
		out.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\"filename\"" + CRLF);
		out.writeBytes("Content-Type: " + contentType + CRLF);
		out.writeBytes(CRLF);
		out.write(data, 0, data.length);
		out.writeBytes(CRLF);

		out.writeBytes(HYPHENS + BOUNDARY + HYPHENS + CRLF);
		out.flush();
		out.close();

		int responseCode = conn.getResponseCode();
		StringBuilder response = new StringBuilder();

		try {
			String line;
			BufferedReader reader = new BufferedReader(
					new InputStreamReader((InputStream) conn.getContent()));
			while ((line = reader.readLine()) != null) {
				response.append(line);
			}
		} catch (IOException ex) {
			Log.w(TAG, ex);
			return Pair.create(responseCode, conn.getResponseMessage());
		} finally {
			conn.disconnect();
		}

		return Pair.create(responseCode, response.toString());
	}

	public static Pair<Integer, Integer> getWidthHeightForRatio(float ratio,
			int originalWidth, int originalHeight) {
		if (ratio == 0) {
			return Pair.create(originalWidth, originalHeight);
		}
		int width = originalWidth;
		int height = originalHeight;
		float heightBasedWidth = originalHeight * ratio;
		float widthBasedHeight = originalWidth * (1/ ratio);
		if (heightBasedWidth < originalWidth) {
			width = (int) heightBasedWidth;
			height = originalHeight;
		} else {
			width = originalWidth;
			height = (int) widthBasedHeight;
		}
		return Pair.create(width, height);
	}

	public static byte[] getUnzippedData(byte zippedData[]) {
		ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zippedData));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int count;
		try {
			zis.getNextEntry();
			while ((count = zis.read(buffer)) != -1) {
				baos.write(buffer, 0, count);
			}
			zis.close();
		} catch (IOException e) {
			Log.e(TAG, "Could not read from zipped data.", e);
			return null;
		}
		return baos.toByteArray();
	}

	public static String getTime(Context context, int seconds) {
		if (seconds / 86400 > 0) {
			return String.format("%d%s", seconds / 86400,
					context.getResources().getString(R.string.days_short));
		} else if (seconds / 3600 > 0) {
			return String.format("%d%s", seconds / 3600,
					context.getResources().getString(R.string.hours_short));
		} else {
			return String.format("%d%s", seconds / 60,
					context.getResources().getString(R.string.minutes_short));
		}
	}

	public static int getColorIdForCategory(String category) {
		if (category != null && COLORS.containsKey(category.toLowerCase())) {
			return COLORS.get(category.toLowerCase());
		}
		return R.color.action_bar;
	}

	public static boolean isTopSocialApp(String packageName) {
		if (packageName == null) {
			return false;
		}
		for (String topApp : Config.TOP_SOCIAL_APPS) {
			if (packageName.contains(topApp)) {
				return true;
			}
		}
		return false;
	}

	public static String getQuestionUrl(String actionId) {
		return actionId == null ? Config.SHORT_URL
				: String.format(Config.SHORT_URL + "/a/%s", actionId);
	}

	public static String getQuestionImageUrl(Question question) {
		return question == null ? Config.SHORT_URL
				: String.format(Config.SHORT_URL + "/i/%s", question.id);
	}

	public static String getQuestionText(Question question, String actionId, Context context) {
		if (question == null) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		if (question.choices != null && question.choices.length == 2
				&& question.choices[0].title != null && question.choices[1].title != null) {
			builder.append(question.choices[0].title);
			builder.append(" | ");
			builder.append(question.choices[1].title);
			builder.append(". ");
		}
		if (question.title != null) {
			builder.append(question.title);
			builder.append(". ");
		}
		builder.append(String.format(context.getResources().getString(R.string.pick_one),
				getQuestionUrl(actionId)));
		return builder.toString();
	}
}
