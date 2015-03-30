package com.teddytab.dilemma.fragments;

import android.os.Bundle;
import android.util.Pair;
import android.view.View;

public class PopularFragment extends QuestionsFragment {

	@SuppressWarnings("unchecked")
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		new GetQuestionList().execute(Pair.create("sort", "popular"));
	}
}
