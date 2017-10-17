package com.ml.exceptions

class UnauthorizedScopesException extends MercadoLibreAPIException {
	
	def UnauthorizedScopesException(message, error = "unauthorized_scopes", cause = []) {
		super(message, error, cause)
	}

	def status = 401
}