package com.ml.exceptions

class UnsupportedMediaTypeException extends MercadoLibreAPIException {
    def status = 415

    def UnsupportedMediaTypeException(message, error = "server_error", cause = []) {
        super(message, error, cause)
    }

}
