package com.ml.exceptions

class BadRequestException extends MercadoLibreAPIException {
    def status = 400

    def BadRequestException(message, error = "bad_request", cause = []) {
        super(message, error, cause)
    }

}
