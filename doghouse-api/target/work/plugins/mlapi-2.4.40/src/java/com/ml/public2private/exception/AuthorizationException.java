package com.ml.public2private.exception;

public class AuthorizationException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 5906287469103641054L;

    public AuthorizationException(String msg) {
	super(msg);
    }

    public AuthorizationException(String msg, Throwable e) {
	super(msg, e);
    }

}
