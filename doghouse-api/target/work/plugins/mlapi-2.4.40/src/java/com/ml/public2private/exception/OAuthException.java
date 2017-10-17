package com.ml.public2private.exception;

import org.apache.http.HttpResponse;


public class OAuthException extends AuthenticationException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5399969035100869446L;
	private HttpResponse response;

	public OAuthException(String msg) {
		super(msg);
	}

	public OAuthException(String msg, Throwable e) {
		super(msg, e);
	}

	public OAuthException(String error, String msg, Throwable e) {
		super(error, msg, e);
	}

	public OAuthException(String error, String msg, HttpResponse response) {
		super(error, msg, null);
		this.setResponse(response);
	}

	public void setResponse(HttpResponse response) {
		this.response = response;
	}

	public HttpResponse getResponse() {
		return response;
	}
}
