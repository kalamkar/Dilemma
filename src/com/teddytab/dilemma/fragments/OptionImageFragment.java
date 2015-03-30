package com.teddytab.dilemma.fragments;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.volley.toolbox.NetworkImageView;
import com.teddytab.dilemma.App;
import com.teddytab.dilemma.CreateActivity;
import com.teddytab.dilemma.R;
import com.teddytab.dilemma.UploadTask;
import com.teddytab.dilemma.UploadTask.UploadListener;
import com.teddytab.dilemma.VerticalViewPager;
import com.teddytab.dilemma.model.ApiResponse;
import com.teddytab.dilemma.model.Media;

public class OptionImageFragment extends Fragment implements OnClickListener, UploadListener {
	private static final String TAG = "OptionImageFragment";

	private static final int CAMERA_ACTIVITY = 1;
	private static final int GALLERY_ACTIVITY = 2;

	public static final String POSITION = "POSITION";
	public static final String PAGER_ID = "PAGER_ID";

	private final List<UploadTask> tasks = new ArrayList<UploadTask>();

	private String photoUrl;
	private Media media;
	private int position;

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
		return inflater.inflate(R.layout.fragment_option_image, null);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		media = getMedia();
		if (media != null && media.url != null) {
			((NetworkImageView) view.findViewById(R.id.image)).setImageUrl(
					media.url, app.imageLoader);
		}

		ImageView button = (ImageView) view.findViewById(R.id.button);
		ImageView pin = (ImageView) view.findViewById(R.id.pin);
		if (position == 0) {
			button.setOnClickListener(this);
			pin.setVisibility(View.GONE);
		} else if (position == 1) {
			button.setImageResource(R.drawable.ic_gallery);
			button.setOnClickListener(this);
			pin.setVisibility(View.GONE);
		} else {
			button.setVisibility(View.GONE);
			pin.setOnClickListener(this);
		}
	}

	public static OptionImageFragment newInstance(int pagerId, int position) {
		OptionImageFragment fragment = new OptionImageFragment();
		Bundle args = new Bundle();
		args.putInt(PAGER_ID, pagerId);
		args.putInt(POSITION, position);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.button) {
			if (position == 0) {
				startCameraActivity();
			} else if (position == 1) {
				startActivityForResult(Intent.createChooser(
						new Intent().setType("image/*").setAction(Intent.ACTION_GET_CONTENT),
						getResources().getString(R.string.choose_image)), GALLERY_ACTIVITY);
			}
		} else if (v.getId() == R.id.pin) {

			VerticalViewPager pager = (VerticalViewPager) getActivity().findViewById(getArguments().getInt(PAGER_ID));
			if (pager.getPinned()) {
				v.setBackgroundResource(R.drawable.circle);
				pager.setPinned(false);
			} else {
				v.setBackgroundResource(R.drawable.circle_colored);
				pager.setPinned(true);
			}

		}
	}

	private void startCameraActivity() {
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy-HH-mm-ss", Locale.ENGLISH);
	    Date date = Calendar.getInstance().getTime();
	    File photoFile;
	    try {
	    	photoFile = File.createTempFile(sdf.format(date), ".jpg", Environment
					.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES));
	    	photoUrl = Uri.fromFile(photoFile).toString();
		} catch (IOException ex) {
			Log.w(TAG, ex);
			return;
		}
	    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
	    if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) == null
	    		|| photoFile == null) {
	    	return;
	    }
		takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
		startActivityForResult(takePictureIntent, CAMERA_ACTIVITY);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == Activity.RESULT_OK && requestCode == CAMERA_ACTIVITY) {
			media.url = photoUrl;
			getView().findViewById(R.id.button).setVisibility(View.GONE);
			((NetworkImageView) getView().findViewById(R.id.image)).setImageUrl(
					media.url, app.imageLoader);
			uploadMedia(media.url);
		} else if (resultCode == Activity.RESULT_OK && requestCode == GALLERY_ACTIVITY) {
			media.url = data.getDataString();
			getView().findViewById(R.id.button).setVisibility(View.GONE);
			((ImageView) getView().findViewById(R.id.image)).setImageURI(data.getData());
			try {
				uploadMedia(getActivity().getContentResolver().openInputStream(data.getData()));
			} catch (FileNotFoundException e) {
				Log.w(TAG, e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void uploadMedia(String localUrl) {
		if (localUrl == null || localUrl.toLowerCase(Locale.US).startsWith("http")) {
			return;
		}
		UploadTask task = new UploadTask(localUrl, "image/jpeg", this);
		task.execute(Pair.create("idfa", "foo"), Pair.create("type", "GOOGLE"),
				Pair.create("tags", "test"));
		tasks.add(task);
	}

	@SuppressWarnings("unchecked")
	private void uploadMedia(InputStream inputStream) {
		UploadTask task = new UploadTask(inputStream, "image/jpeg", this);
		task.execute(Pair.create("idfa", "foo"), Pair.create("type", "GOOGLE"),
				Pair.create("tags", "test"));
		tasks.add(task);
	}

	@Override
	public void onUploadSuccess(ApiResponse response) {
		if (response == null || response.media == null || response.media.length == 0) {
			return;
		}
		media.id = response.media[0].id;
		getActivity().invalidateOptionsMenu();
	}

	@Override
	public void onUploadError() {
		Log.w(TAG, String.format("Upload failed for %s", media.url));
	}

	private Media getMedia() {
		position = getArguments().getInt(POSITION);
		if (position >= ((CreateActivity) getActivity()).option1Images.size()) {
			return null;
		}
		int pagerId = getArguments().getInt(PAGER_ID);
		if (pagerId == R.id.image1) {
			return ((CreateActivity) getActivity()).option1Images.get(position);
		}
		return ((CreateActivity) getActivity()).option2Images.get(position);
	}
}
