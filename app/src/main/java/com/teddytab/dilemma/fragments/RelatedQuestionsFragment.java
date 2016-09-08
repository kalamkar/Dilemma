package com.teddytab.dilemma.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.android.volley.toolbox.NetworkImageView;
import com.teddytab.dilemma.App;
import com.teddytab.dilemma.DetailsActivity;
import com.teddytab.dilemma.R;
import com.teddytab.dilemma.model.Question;

public class RelatedQuestionsFragment extends Fragment implements OnClickListener {
	public static final String QUESTION_IDS = "QUESTION_IDS";

	protected App app;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		app = (App) activity.getApplication();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		return inflater.inflate(R.layout.fragment_related_questions, null);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		String[] questionIds = getArguments().getStringArray(QUESTION_IDS);
		if (questionIds == null) {
			return;
		}

		LinearLayout container = (LinearLayout) view.findViewById(R.id.related);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(container.getLayoutParams());
		params.rightMargin = app.getResources().getDimensionPixelSize(R.dimen.small_margin);
		for (String questionId : questionIds) {
			View questionView = getQuestionView(app.questions.get(questionId));
			if (questionView != null) {
				container.addView(questionView, params);
			}
		}
		view.requestLayout();
	}

	private View getQuestionView(Question question) {
		View view = getActivity().getLayoutInflater().inflate(R.layout.related_question_item, null);
		if (question.choices[0].media == null || question.choices[1].media == null) {
			view.findViewById(R.id.image1).setVisibility(View.GONE);
			view.findViewById(R.id.image2).setVisibility(View.GONE);
		} else {
			((NetworkImageView) view.findViewById(R.id.image1)).setImageUrl(
					question.choices[0].media.url, app.imageLoader);
			((NetworkImageView) view.findViewById(R.id.image2)).setImageUrl(
					question.choices[1].media.url, app.imageLoader);
		}
		view.setTag(question.id);
		view.setOnClickListener(this);
		return view;
	}

	@Override
	public void onClick(View v) {
		startActivity(new Intent(getActivity(), DetailsActivity.class)
				.putExtra(DetailsActivity.QUESTION_ID, (String) v.getTag()));
	}
}
