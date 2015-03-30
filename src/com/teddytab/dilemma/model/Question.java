package com.teddytab.dilemma.model;

import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class Question {
	public String id;
	public String title;
	public String category;
	public String location;
	public Map<String, Integer[]> stats;

	@SerializedName("seconds_ago")
	public int secondsAgo;

	public Choice choices[];

	public Answer answer;

	@SerializedName("num_answers")
	public int numAnswers;

	public String getTitle() {
		if (title != null) {
			return title;
		}
		if (choices != null && choices.length >= 2
				&& choices[0].title != null && choices[1].title != null) {
			return String.format("%s | %s", choices[0].title, choices[1].title);
		}
		return "dilemma";
	}

	public String getOptions() {
		if (choices != null && choices.length >= 2
				&& choices[0].title != null && choices[1].title != null) {
			return String.format("%s | %s", choices[0].title, choices[1].title);
		}
		if (title != null) {
			return title;
		}
		return "dilemma";
	}
}
