package com.teddytab.dilemma;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicNameValuePair;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.NetworkImageView;
import com.teddytab.dilemma.fragments.RelatedQuestionsFragment;
import com.teddytab.dilemma.model.ApiResponse;
import com.teddytab.dilemma.model.Question;

@SuppressWarnings("unchecked")
public class DetailsActivity extends FragmentActivity implements OnClickListener {
	private static final String TAG = "DetailsActivity";

	public static final String QUESTION_ID = "QUESTION_ID";

	private App app;
	private Question question;

	private List<ApiResponseTask> requests = new ArrayList<ApiResponseTask>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_details);

		app = (App) getApplication();

		question = app.questions.get(getIntent().getStringExtra(QUESTION_ID));
		if (question == null || question.choices == null || question.choices.length < 2) {
			return;
		}

		findViewById(R.id.flag).setOnClickListener(this);
		findViewById(R.id.image1).setOnClickListener(this);
		findViewById(R.id.image2).setOnClickListener(this);
		findViewById(R.id.option1).setOnClickListener(this);
		findViewById(R.id.option2).setOnClickListener(this);

		if (question.title == null) {
			findViewById(R.id.title).setVisibility(View.GONE);
		} else {
			Utils.setText(this, R.id.title, question.title);
		}

		if (question.choices[0].title == null && question.choices[1].title == null) {
			findViewById(R.id.option1).setVisibility(View.GONE);
			findViewById(R.id.option2).setVisibility(View.GONE);
		} else {
			Utils.setText(this, R.id.option1, question.choices[0].title);
			Utils.setText(this, R.id.option2, question.choices[1].title);
		}

		if (question.choices[0].media == null || question.choices[1].media == null) {
			findViewById(R.id.image1).setVisibility(View.GONE);
			findViewById(R.id.image2).setVisibility(View.GONE);
		} else {
			((NetworkImageView) findViewById(R.id.image1)).setImageUrl(
					question.choices[0].media.url, app.imageLoader);
			((NetworkImageView) findViewById(R.id.image2)).setImageUrl(
					question.choices[1].media.url, app.imageLoader);
		}

		if (app.answers.containsKey(question.id)) {
			int choice = app.answers.get(question.id);
			findViewById(choice == 0 ? R.id.check1 : R.id.check2)
				.setBackgroundResource(R.drawable.circle_colored);
		}

		Utils.setText(this, R.id.time, Utils.getTime(getApplicationContext(),
				question.secondsAgo));
		Utils.setText(this, R.id.place, question.location != null ? question.location :
				getResources().getString(R.string.someplace));
		Utils.setText(this, R.id.answers, String.format(getResources().getString(
				R.string.num_answers), question.numAnswers));

		if (question.category != null) {
			Utils.setText(this, R.id.category, question.category);
			findViewById(R.id.category).setBackgroundColor(getResources().getColor(
					Utils.getColorIdForCategory(question.category)));
		} else {
			findViewById(R.id.category).setVisibility(View.GONE);
		}

		addShareButtons();
		addStats();

		GetRelatedQuestions request = new GetRelatedQuestions();
		requests.add(request);
		request.execute(Pair.create("idfa", app.idfa), Pair.create("type", "GOOGLE"),
				Pair.create("qid", question.id));
	}

	@Override
	protected void onStop() {
		for (ApiResponseTask request : requests) {
			request.cancel(true);
		}
		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.details, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.action_share:
			new ShareActionPoster(question.id, null).execute();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.image1:
		case R.id.option1:
			findViewById(R.id.check1).setBackgroundResource(R.drawable.circle_colored);
			findViewById(R.id.check2).setBackgroundResource(R.drawable.circle);
			new AnswerPoster(question.id, 0).execute();
			break;
		case R.id.image2:
		case R.id.option2:
			findViewById(R.id.check2).setBackgroundResource(R.drawable.circle_colored);
			findViewById(R.id.check1).setBackgroundResource(R.drawable.circle);
			new AnswerPoster(question.id, 1).execute();
			break;
		case R.id.flag:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.are_you_sure);
			builder.setPositiveButton(R.string.flag,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							new FlagRequest(question.id).execute();
						}
					});
			builder.setNegativeButton(android.R.string.cancel,
					new DialogInterface.OnClickListener() {
			    		@Override
			    		public void onClick(DialogInterface dialog, int which) {
			    			dialog.cancel();
			    		}
					});
			builder.show();
			break;
		default:
			try {
				new ShareActionPoster(question.id, (ResolveInfo) v.getTag()).execute();
			} catch(Exception ex) {
				Log.w(TAG, ex);
			}
		}
	}

	private class AnswerPoster extends ApiResponseTask {
		private final String questionId;
		private final int choiceIndex;

		AnswerPoster(String questionId, int choiceIndex) {
			super();
			this.questionId = questionId;
			this.choiceIndex = choiceIndex;
		}

		@Override
		protected HttpRequestBase makeRequest(Pair<String, String>... params)
				throws UnsupportedEncodingException {
			List<NameValuePair> queryParams = new ArrayList<NameValuePair>();
			queryParams.add(new BasicNameValuePair("idfa", app.idfa));
			queryParams.add(new BasicNameValuePair("type", "GOOGLE"));
			for (Pair<String, String> param : params) {
				queryParams.add(new BasicNameValuePair(param.first, param.second));
			}
			queryParams.add(new BasicNameValuePair("qid", questionId));
			queryParams.add(new BasicNameValuePair("chix", Integer.toString(choiceIndex)));
			Log.v(TAG, String.format("Selected answer #%d for %s", choiceIndex, questionId));
			HttpPost request = new HttpPost(Config.ANSWER_URL);
			request.setEntity(new UrlEncodedFormEntity(queryParams));
			return request;
		}

		@Override
		protected void onPostExecute(ApiResponse result) {
			super.onPostExecute(result);
			if (result != null) {
				Toast.makeText(DetailsActivity.this, result.message, Toast.LENGTH_LONG).show();
			}
			if (result == null || !"OK".equalsIgnoreCase(result.code)) {
				findViewById(R.id.check1).setBackgroundResource(R.drawable.circle);
				findViewById(R.id.check2).setBackgroundResource(R.drawable.circle);
			} else {
				question = result.questions[0];
				app.answers.put(questionId, choiceIndex);
				app.stats.put(questionId, question.stats);
				addStats();
			}
		}
	}

	private class ShareActionPoster extends ApiResponseTask {
		private final String questionId;
		private final ResolveInfo resolveInfo;

		ShareActionPoster(String questionId, ResolveInfo resolveInfo) {
			super();
			this.questionId = questionId;
			this.resolveInfo = resolveInfo;
		}

		@Override
		protected HttpRequestBase makeRequest(Pair<String, String>... params)
				throws UnsupportedEncodingException {
			List<NameValuePair> queryParams = new ArrayList<NameValuePair>();
			queryParams.add(new BasicNameValuePair("idfa", app.idfa));
			queryParams.add(new BasicNameValuePair("type", "GOOGLE"));
			for (Pair<String, String> param : params) {
				queryParams.add(new BasicNameValuePair(param.first, param.second));
			}
			queryParams.add(new BasicNameValuePair("qid", questionId));
			queryParams.add(new BasicNameValuePair("actn", "SHARE"));
			HttpPost request = new HttpPost(Config.ACTION_URL);
			request.setEntity(new UrlEncodedFormEntity(queryParams));
			return request;
		}

		@Override
		protected void onPostExecute(ApiResponse result) {
			super.onPostExecute(result);
			if (result != null && "OK".equalsIgnoreCase(result.code) && result.action != null) {
				shareWith(result.action.id, resolveInfo);
			}
		}
	}

	private class FlagRequest extends ApiResponseTask {
		private final String questionId;

		FlagRequest(String questionId) {
			super();
			this.questionId = questionId;
		}

		@Override
		protected HttpRequestBase makeRequest(Pair<String, String>... params)
				throws UnsupportedEncodingException {
			List<NameValuePair> queryParams = new ArrayList<NameValuePair>();
			queryParams.add(new BasicNameValuePair("idfa", app.idfa));
			queryParams.add(new BasicNameValuePair("type", "GOOGLE"));
			for (Pair<String, String> param : params) {
				queryParams.add(new BasicNameValuePair(param.first, param.second));
			}
			queryParams.add(new BasicNameValuePair("qid", questionId));
			queryParams.add(new BasicNameValuePair("flag", "1"));
			HttpPut request = new HttpPut(Config.QUESTION_URL);
			request.setEntity(new UrlEncodedFormEntity(queryParams));
			return request;
		}

		@Override
		protected void onPostExecute(ApiResponse result) {
			super.onPostExecute(result);
			if (result != null && result.message != null) {
				Toast.makeText(DetailsActivity.this, result.message, Toast.LENGTH_SHORT).show();
			}
		}
	}

	protected class GetRelatedQuestions extends ApiResponseTask {
		@Override
		protected HttpRequestBase makeRequest(Pair<String, String>... params)
				throws UnsupportedEncodingException {
			Uri.Builder uri = Uri.parse(Config.RELATED_URL).buildUpon();
			for (Pair<String, String> param : params) {
				uri.appendQueryParameter(param.first, param.second);
			}
			return new HttpGet(uri.build().toString());
		}

		@Override
		protected void onPostExecute(ApiResponse result) {
			super.onPostExecute(result);
			if (result == null) {
				return;
			} else if (!"OK".equalsIgnoreCase(result.code)) {
				Toast.makeText(DetailsActivity.this, result.message, Toast.LENGTH_LONG).show();
				return;
			}
			String[] relatedQuestionIds = new String[result.questions.length];
			int i = 0;
			for (Question question : result.questions) {
				app.questions.put(question.id, question);
				relatedQuestionIds[i++] = question.id;
			}
			RelatedQuestionsFragment fragment = new RelatedQuestionsFragment();
			Bundle args = new Bundle();
			args.putStringArray(RelatedQuestionsFragment.QUESTION_IDS, relatedQuestionIds);
			fragment.setArguments(args);
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.related_container, fragment).commit();
		}
	}

	public void shareWith(String actionId, ResolveInfo resolveInfo) {
		Intent share = new Intent();
		if (resolveInfo != null && resolveInfo.activityInfo != null
				&& resolveInfo.activityInfo.applicationInfo != null) {
			share.setComponent(new ComponentName(
					resolveInfo.activityInfo.applicationInfo.packageName,
					resolveInfo.activityInfo.name));
		}
		share.setAction(Intent.ACTION_SEND);
		share.setType("text/plain");
		share.putExtra(Intent.EXTRA_TEXT, Utils.getQuestionText(question, actionId, this));
		share.putExtra(Intent.EXTRA_SUBJECT, question.getTitle());
		share.putExtra(Intent.EXTRA_TITLE, question.getTitle());
		startActivity(share);
	}

	private void addShareButtons() {
		LinearLayout buttons = (LinearLayout) findViewById(R.id.buttons);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.topMargin = params.bottomMargin = params.leftMargin =
				getResources().getDimensionPixelSize(R.dimen.medium_margin);
		for (ResolveInfo app : getPackageManager().queryIntentActivities(
				new Intent(Intent.ACTION_SEND).setType("text/plain"), 0)) {
			if (app != null && app.activityInfo != null) {
				if (Utils.isTopSocialApp(app.activityInfo.packageName)) {
					ImageView button = new ImageView(this);
					button.setImageDrawable(app.loadIcon(getPackageManager()));
					button.setTag(app);
					button.setOnClickListener(this);
					buttons.addView(button, params);
				}
			}
		}
	}

	private void addStats() {
		Map<String, Integer[]> stats = app.stats.get(question.id);
		if (stats == null) {
			return;
		}
		int green = getResources().getColor(R.color.green);
		int red = getResources().getColor(R.color.red);
		LinearLayout container = (LinearLayout) findViewById(R.id.stats_container);
		container.removeAllViews();
		for (String feature : stats.keySet()) {
			Integer options[] = stats.get(feature);
			View statRow = LayoutInflater.from(this).inflate(R.layout.stats, null);
			Utils.setText(statRow, R.id.feature, feature);
			TextView option1 = (TextView) statRow.findViewById(R.id.option1);
			TextView option2 = (TextView) statRow.findViewById(R.id.option2);
			option1.setText(Integer.toString(options[0]));
			option2.setText(Integer.toString(options[1]));
			option1.setBackgroundColor(app.answers.get(question.id) == 0 ? green : red);
			option2.setBackgroundColor(app.answers.get(question.id) == 0 ? red: green);
			if (options[0] != 0 && options[1] != 0) {
				((LinearLayout.LayoutParams) option1.getLayoutParams()).weight = options[0];
				((LinearLayout.LayoutParams) option2.getLayoutParams()).weight = options[1];
			} else if (options[0] == 0) {
				option1.getLayoutParams().width = LayoutParams.WRAP_CONTENT;
				option2.getLayoutParams().width = LayoutParams.MATCH_PARENT;
			} else if (options[1] == 0) {
				option1.getLayoutParams().width = LayoutParams.MATCH_PARENT;
				option2.getLayoutParams().width = LayoutParams.WRAP_CONTENT;
			}
			container.addView(statRow);
			Log.v(TAG, String.format("Adding feature %s with numbers %d %d", feature, options[0],
					options[1]));
		}
		container.requestLayout();
	}
}
