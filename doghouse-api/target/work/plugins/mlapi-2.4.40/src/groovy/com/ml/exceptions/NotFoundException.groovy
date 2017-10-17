package com.ml.exceptions

class NotFoundException extends MercadoLibreAPIException {

    def NotFoundException(message, error = "not_found", cause = []) {
        super(message, error, cause)
    }

    def status = 404
}
