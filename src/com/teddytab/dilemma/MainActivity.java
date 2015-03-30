package com.teddytab.dilemma;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.teddytab.dilemma.fragments.LatestFragment;
import com.teddytab.dilemma.fragments.MyQuestionsFragment;
import com.teddytab.dilemma.fragments.NearbyFragment;
import com.teddytab.dilemma.fragments.PopularFragment;
import com.teddytab.dilemma.fragments.SearchFragment;
import com.teddytab.dilemma.messaging.GCMUtils;

public class MainActivity extends FragmentActivity {
	private static final String TAG = "MainActivity";

	private static final int CREATE_ACTIVITY = 1;

	private App app;
	private PagerAdapter adapter;

	private long lastBackTime = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		app = (App) getApplication();

		if (GCMUtils.checkPlayServices(this)) {
			app.requestRegistrationId();
		}

		adapter = new PagerAdapter(this.getSupportFragmentManager());
		((ViewPager) findViewById(R.id.pager)).setAdapter(adapter);
	}

	@Override
	protected void onResume() {
	    super.onResume();
	    GCMUtils.checkPlayServices(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.action_search:
			((ViewPager) findViewById(R.id.pager)).setCurrentItem(4);
			break;
		case R.id.action_post:
			startActivityForResult(new Intent(this, CreateActivity.class), CREATE_ACTIVITY);
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == CREATE_ACTIVITY && data != null && resultCode == Activity.RESULT_OK) {
			startActivity(new Intent(this, DetailsActivity.class).putExtra(
					DetailsActivity.QUESTION_ID, data.getStringExtra(DetailsActivity.QUESTION_ID)));
		}
	}

	@Override
	public void onBackPressed() {
		long currentTime = System.currentTimeMillis();
		if (currentTime - lastBackTime < Config.BACK_BUTTON_DELAY_MILLIS) {
			super.onBackPressed();
		} else {
			lastBackTime = currentTime;
			Toast.makeText(this, getResources().getString(R.string.tap_back_two_times),
					Toast.LENGTH_SHORT).show();
		}
	}

	private class PagerAdapter extends FragmentStatePagerAdapter {
		private final Fragment fragments[] = {
				new PopularFragment(), new LatestFragment(), new NearbyFragment(),
				new MyQuestionsFragment(), new SearchFragment()
		};

		private final int titleIds[] = { R.string.popular, R.string.latest, R.string.nearby,
				R.string.my_dilemmas, R.string.search };

		public PagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			return fragments[position];
		}

		@Override
	    public CharSequence getPageTitle(int position) {
	        return getResources().getString(titleIds[position]);
	    }

		@Override
		public int getCount() {
			return fragments.length;
		}
	}
}
