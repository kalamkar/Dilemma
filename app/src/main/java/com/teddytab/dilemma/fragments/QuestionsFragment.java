package com.teddytab.dilemma.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.toolbox.NetworkImageView;
import com.teddytab.dilemma.ApiResponseTask;
import com.teddytab.dilemma.App;
import com.teddytab.dilemma.Config;
import com.teddytab.dilemma.DetailsActivity;
import com.teddytab.dilemma.R;
import com.teddytab.dilemma.Utils;
import com.teddytab.dilemma.model.ApiResponse;
import com.teddytab.dilemma.model.Question;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Request;

public abstract class QuestionsFragment extends Fragment {
	public static final String TAG = "QuestionsFragment";

	protected final List<String> questionIds = new ArrayList<String>();
	protected App app;
	protected QuestionGridAdapter adapter;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		app = (App) activity.getApplication();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		return inflater.inflate(R.layout.fragment_questions, null);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		adapter = new QuestionGridAdapter();
		((AbsListView) view.findViewById(R.id.questions)).setAdapter(adapter);
	}

	protected Request makeRequest(Pair<String, String>... params)
			throws UnsupportedEncodingException {
		Uri.Builder uri = Uri.parse(Config.SEARCH_URL).buildUpon();
		for (Pair<String, String> param : params) {
			uri.appendQueryParameter(param.first, param.second);
		}
		return new Request.Builder().url(uri.build().toString()).build();
	}

	protected class GetQuestionList extends ApiResponseTask {
		@Override
		protected Request makeRequest(Pair<String, String>... params)
				throws UnsupportedEncodingException {
			return QuestionsFragment.this.makeRequest(params);
		}

		@Override
		protected void onPostExecute(ApiResponse result) {
			super.onPostExecute(result);
			if (result == null) {
				return;
			} else if (!"OK".equalsIgnoreCase(result.code)) {
				Toast.makeText(getActivity(), result.message, Toast.LENGTH_LONG).show();
				return;
			}
			questionIds.clear();
			for (Question question : result.questions) {
				app.questions.put(question.id, question);
				questionIds.add(question.id);
			}
			adapter.notifyDataSetChanged();
		}
	}

	private class QuestionGridAdapter extends BaseAdapter implements OnClickListener {

		@Override
		public int getCount() {
			return questionIds.size();
		}

		@Override
		public Question getItem(int position) {
			return app.questions.get(questionIds.get(position));
		}

		@Override
		public long getItemId(int position) {
			Question question = getItem(position);
			return question == null ? 0 : question.hashCode();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			if (convertView != null && convertView instanceof ImageView) {
				view = convertView;
			} else {
				view = getActivity().getLayoutInflater().inflate(R.layout.question_item, null);
			}
			Question question = getItem(position);
			if (question == null || question.choices == null || question.choices.length < 2) {
				return view;
			}

			view.setTag(question.id);
			view.setOnClickListener(this);
			Utils.setText(view, R.id.title, question.getTitle());

			if (question.choices[0].title == null || question.choices[1].title == null ||
					question.title == null) {
				view.findViewById(R.id.option1).setVisibility(View.GONE);
				view.findViewById(R.id.option2).setVisibility(View.GONE);
			} else if (question.title != null) {
				Utils.setText(view, R.id.option1, question.choices[0].title);
				Utils.setText(view, R.id.option2, question.choices[1].title);
			}

			if (question.choices[0].media == null || question.choices[1].media == null) {
				view.findViewById(R.id.image1).setVisibility(View.GONE);
				view.findViewById(R.id.image2).setVisibility(View.GONE);
			} else {
				((NetworkImageView) view.findViewById(R.id.image1)).setImageUrl(
						question.choices[0].media.url, app.imageLoader);
				((NetworkImageView) view.findViewById(R.id.image2)).setImageUrl(
						question.choices[1].media.url, app.imageLoader);
			}

			if (question.category != null) {
				Utils.setText(view, R.id.category, question.category);
				view.findViewById(R.id.category).setBackgroundColor(getResources().getColor(
						Utils.getColorIdForCategory(question.category)));
			} else {
				view.findViewById(R.id.category).setVisibility(View.INVISIBLE);
			}


			return view;
		}

		@Override
		public void onClick(View v) {
			startActivity(new Intent(getActivity(), DetailsActivity.class)
					.putExtra(DetailsActivity.QUESTION_ID, (String) v.getTag()));
		}
	}
}
