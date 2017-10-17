package com.ml.exceptions

class UnauthorizedException extends MercadoLibreAPIException {
    
    def UnauthorizedException(message, error = "unauthorized", cause = []) {
        super(message, error, cause)
    }

    def status = 401
}
