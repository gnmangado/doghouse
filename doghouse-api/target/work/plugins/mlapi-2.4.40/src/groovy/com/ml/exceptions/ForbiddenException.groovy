package com.ml.exceptions

class ForbiddenException extends MercadoLibreAPIException {    

    def ForbiddenException(message, error = "forbidden", cause = []) {
        super(message, error, cause)
    }

    def status = 403
}
