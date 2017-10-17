package com.ml.public2private.exception;

public class AuthenticationException extends RuntimeException {
	
	private String error = "Unknown Error";

	public AuthenticationException(String msg) {
		super(msg);
	}

	public AuthenticationException(String msg, Throwable e) {
		super(msg, e);
	}
	
	public AuthenticationException(String error, String msg, Throwable e) {
		super(msg, e);
		this.error = error;
	}
	
	public void setError(String error) {
		this.error = error;
	}

	public String getError() {
		return error;
	}

	/**
     * 
     */
	private static final long serialVersionUID = -2356795209699489618L;

}
