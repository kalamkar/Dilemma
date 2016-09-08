package com.teddytab.dilemma;

import java.lang.reflect.Modifier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Config {

	public static final String GCM_SENDER_ID = "209668780940";

	public static final String TOP_SOCIAL_APPS[] = new String[] {
		"com.facebook", "com.twitter", "com.linkedin", "com.google.android.apps.plus"};

	public static final Gson GSON_PRETTY = new GsonBuilder()
			.excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)
			.setPrettyPrinting().create();
	public static final Gson GSON = new GsonBuilder()
			.excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)
			.create();

	public static final String SHORT_URL = "http://dilemma.cc";
	public static final String API_URL = "https://dilemma-api.appspot.com";
	public static final String USER_URL = API_URL + "/user";
	public static final String MEDIA_URL = API_URL + "/media";
	public static final String QUESTION_URL = API_URL + "/question";
	public static final String SEARCH_URL = API_URL + "/question/search";
	public static final String RELATED_URL = API_URL + "/question/related";
	public static final String ANSWER_URL = API_URL + "/answer";
	public static final String ACTION_URL = API_URL + "/action";

	public static final int IMAGE_SEARCH_DELAY_MILLIS = 1500;
	public static final int BACK_BUTTON_DELAY_MILLIS = 1500;
}
