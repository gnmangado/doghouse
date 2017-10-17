package com.ml.public2private.utils;

import org.codehaus.groovy.grails.web.json.JSONWriter;

import java.io.StringWriter;
import java.util.List;


public class ErrorMessage {

	String message;
	String error;
	int status;
	List<cause> cause;

	public ErrorMessage(Throwable e, String error, int status) {
		super();
		message = e.getMessage();
		this.error = error;
		this.status = status;
	}

	class cause {
		public cause(String code, String message) {
			this.code = code;
			this.message = message;
		}

		String code;
		String message;
	}

	public String json() {
		try {
			StringWriter sw = new StringWriter();
			new JSONWriter(sw).object().key("error").value(error).key("message").value(message).key("status")
					.value(status).endObject();
			return sw.toString();
		} catch (Exception e) {
			// no puede pasar
			return "Unkown Error";
		}
	}

}
