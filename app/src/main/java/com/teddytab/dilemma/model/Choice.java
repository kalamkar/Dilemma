package com.teddytab.dilemma.model;

import com.google.gson.annotations.SerializedName;

public class Choice {
	public String title;
	public Media media;
	
	@SerializedName("num_answers")
	public int numAnswers;
}
