package com.teddytab.dilemma.model;

public class ApiResponse {
	public String code;
	public String message;
	public Media media[];
	public Question questions[];
	public Action action;

	public static class Action {
		public String id;
	}
}
