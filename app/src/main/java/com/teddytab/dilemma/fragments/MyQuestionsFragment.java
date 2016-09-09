package com.teddytab.dilemma.fragments;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;

import com.teddytab.dilemma.App;
import com.teddytab.dilemma.Config;

import java.io.UnsupportedEncodingException;

import okhttp3.Request;

public class MyQuestionsFragment extends QuestionsFragment {

	protected App app;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		app = (App) activity.getApplication();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		new GetQuestionList().execute(Pair.create("idfa", app.idfa), Pair.create("type", "GOOGLE"));
	}

	@Override
	protected Request makeRequest(Pair<String, String>... params)
			throws UnsupportedEncodingException {
		Uri.Builder uri = Uri.parse(Config.QUESTION_URL).buildUpon();
		for (Pair<String, String> param : params) {
			uri.appendQueryParameter(param.first, param.second);
		}
		return new Request.Builder().url(uri.build().toString()).build();
	}
}
