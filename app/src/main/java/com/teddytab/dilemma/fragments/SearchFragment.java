package com.teddytab.dilemma.fragments;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.teddytab.dilemma.App;
import com.teddytab.dilemma.Config;
import com.teddytab.dilemma.R;

public class SearchFragment extends Fragment {
	protected App app;
	private Timer searchTimer;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		app = (App) activity.getApplication();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		return inflater.inflate(R.layout.fragment_search, null);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		EditText query = (EditText) view.findViewById(R.id.query);
		query.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(final Editable text) {
				if (searchTimer != null) {
					searchTimer.cancel();
				}
				searchTimer = new Timer();
				searchTimer.schedule(new TimerTask() {
					@Override
					public void run() {
						if (!text.toString().isEmpty() && getFragmentManager() != null) {
							getFragmentManager().beginTransaction().replace(R.id.results,
									new SearchResultsFragment(text.toString())).commit();
						}
						((InputMethodManager) getActivity().getSystemService(
								Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(
										getView().findViewById(R.id.query).getWindowToken(), 0);
					}
				}, Config.IMAGE_SEARCH_DELAY_MILLIS);
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
			}

			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
			}
		});
	}

	private static class SearchResultsFragment extends QuestionsFragment {
		private final String query;

		SearchResultsFragment(String query) {
			this.query = query;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void onViewCreated(View view, Bundle savedInstanceState) {
			super.onViewCreated(view, savedInstanceState);
			new GetQuestionList().execute(Pair.create("qry", query));
		}
	}
}
