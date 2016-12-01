package com.teddytab.dilemma;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.teddytab.dilemma.fragments.OptionImageFragment;
import com.teddytab.dilemma.model.ApiResponse;
import com.teddytab.dilemma.model.Choice;
import com.teddytab.dilemma.model.Media;
import com.teddytab.dilemma.model.Question;
import com.teddytab.dilemma.providers.FlickrSearch;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.FormBody;
import okhttp3.Request;

public class CreateActivity extends FragmentActivity {
	private static final String TAG = "CreateActivity";

	public final List<Media> option1Images = new ArrayList<Media>();
	public final List<Media> option2Images = new ArrayList<Media>();

	private ViewPager pager1;
	private ViewPager pager2;

	private App app;
	private QuestionPoster request;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_create);

		app = (App) getApplication();

		((EditText) findViewById(R.id.option1)).addTextChangedListener(
				new OptionTextWatcher(R.id.option1, R.id.image1));
		((EditText) findViewById(R.id.option2)).addTextChangedListener(
				new OptionTextWatcher(R.id.option2, R.id.image2));

		option1Images.add(new Media());
		option1Images.add(new Media());
		option2Images.add(new Media());
		option2Images.add(new Media());

		pager1 = (ViewPager) findViewById(R.id.image1);
		pager1.setAdapter(new OptionImagePagerAdapter(
				option1Images, R.id.image1, getSupportFragmentManager()));

		pager2 = (ViewPager) findViewById(R.id.image2);
		pager2.setAdapter(new OptionImagePagerAdapter(
				option2Images, R.id.image2, getSupportFragmentManager()));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.create, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.action_post).setEnabled(isValid());
		return super.onPrepareOptionsMenu(menu);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.action_post:
			Media image1 = option1Images.get(pager1.getCurrentItem());
			Media image2 = option2Images.get(pager2.getCurrentItem());

			Choice choice1 = new Choice();
			choice1.title = Utils.getText(this, R.id.option1, null);
			if ((image1.url != null && image1.url.toLowerCase(Locale.US).startsWith("http"))
					|| image1.id != null) {
				choice1.media =  image1;
			}
			Choice choice2 = new Choice();
			choice2.title = Utils.getText(this, R.id.option2, null);
			if ((image2.url != null && image2.url.toLowerCase(Locale.US).startsWith("http"))
					|| image2.id != null) {
				choice2.media = image2;
			}
			Question question = new Question();
			question.title = Utils.getText(this, R.id.title, null);
			question.choices = new Choice[] { choice1, choice2 };
			// TODO(abhi): Remove leaking local media url
			request = new QuestionPoster(question);
			request.execute();
			break;
		}
		invalidateOptionsMenu();
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == Activity.RESULT_OK) {
			invalidateOptionsMenu();
		}
	}

	private boolean isValid() {
		if (request != null) {
			return false;
		}
		if (Utils.getText(this, R.id.option1, null) != null
				&& Utils.getText(this, R.id.option2, null) != null) {
			return true;
		}
		if (option1Images.size() <= pager1.getCurrentItem()
				|| option2Images.size() <= pager2.getCurrentItem()) {
			return false;
		}
		Media image1 = option1Images.get(pager1.getCurrentItem());
		Media image2 = option2Images.get(pager2.getCurrentItem());
		if (image1.id != null && image2.id != null) {
			return true;
		}
		if (((image1.url != null && image1.url.toLowerCase(Locale.US).startsWith("http"))
				&& (image2.url != null && image2.url.toLowerCase(Locale.US).startsWith("http")))) {
			return true;
		}
		return false;
	}

	private class OptionTextWatcher implements TextWatcher {
		private int optionId;
		private int imageId;
		private Timer searchTimer;

		OptionTextWatcher(int optionId, int imageId) {
			this.optionId = optionId;
			this.imageId = imageId;
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
		}

		@Override
		public void afterTextChanged(final Editable query) {
			invalidateOptionsMenu();
			if (searchTimer != null) {
				searchTimer.cancel();
			}
			searchTimer = new Timer();
			searchTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					VerticalViewPager pager = (VerticalViewPager) findViewById(imageId);
					if (pager.getPinned()) {
						return;
					}
					if (optionId == R.id.option1) {
						new ImageSearch(R.id.image1, option1Images).execute(query.toString());
					} else {
						new ImageSearch(R.id.image2, option2Images).execute(query.toString());
					}
				}
			}, Config.IMAGE_SEARCH_DELAY_MILLIS);
		}
	}

	private class QuestionPoster extends ApiResponseTask {
		private final Question question;

		QuestionPoster(Question question) {
			super();
			this.question = question;
		}

		@Override
		protected Request makeRequest(Pair<String, String>... params)
				throws UnsupportedEncodingException {
			FormBody.Builder builder = new FormBody.Builder()
					.add("idfa", app.idfa)
					.add("type", "GOOGLE");

			for (Pair<String, String> param : params) {
				builder.add(param.first, param.second);
			}
			builder.add("qstn", Utils.toJson(question));
			Log.v(TAG, Utils.toJson(question));
			return new Request.Builder().url(Config.QUESTION_URL).post(builder.build()).build();
		}

		@Override
		protected void onPostExecute(ApiResponse result) {
			super.onPostExecute(result);
			request = null;
			invalidateOptionsMenu();
			if (result == null) {
				return;
			}

			if ("OK".equalsIgnoreCase(result.code) && result.questions != null
					&& result.questions.length > 0) {
				// TODO(abhi): Add the new question to latest questions
				app.questions.put(result.questions[0].id, result.questions[0]);
				CreateActivity.this.setResult(Activity.RESULT_OK, getIntent()
						.putExtra(DetailsActivity.QUESTION_ID, result.questions[0].id));
				finish();
			}
			Toast.makeText(CreateActivity.this, result.message, Toast.LENGTH_LONG).show();
		}
	}

	private class ImageSearch extends FlickrSearch {
		private final int imageId;
		private final List<Media> optionImages;

		ImageSearch(int imageId, List<Media> optionImages) {
			this.imageId = imageId;
			this.optionImages = optionImages;
		}

		@Override
		protected void onPostExecute(List<Media> result) {
			super.onPostExecute(result);
			if (result == null || result.size() == 0) {
				return;
			}
			Media galleryMedia = optionImages.remove(1);
			Media cameraMedia = optionImages.remove(0);
			optionImages.removeAll(optionImages);
			optionImages.add(cameraMedia);
			optionImages.add(galleryMedia);
			optionImages.addAll(result);
			ViewPager pager = (ViewPager) findViewById(imageId);
			pager.getAdapter().notifyDataSetChanged();
			pager.invalidate();
			pager.setCurrentItem(2);
		}
	}

	private static class OptionImagePagerAdapter extends FragmentStatePagerAdapter {
		private final List<Media> mediaList;
		private final int pagerId;

        public OptionImagePagerAdapter(List<Media> mediaList, int pagerId, FragmentManager fm) {
            super(fm);
            this.mediaList = mediaList;
            this.pagerId = pagerId;
        }

        @Override
        public int getCount() {
            return mediaList.size();
        }

        @Override
        public Fragment getItem(int position) {
            return OptionImageFragment.newInstance(pagerId, position);
        }
    }
}
