package com.ml.exceptions

class NotImplementedException extends MercadoLibreAPIException {
    def status = 501

    def NotImplementedException(message, error = "not_implemented", cause = []) {
        super(message, error, cause)
    }
}
