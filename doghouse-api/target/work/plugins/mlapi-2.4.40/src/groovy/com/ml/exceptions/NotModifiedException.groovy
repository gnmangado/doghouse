package com.ml.exceptions

class NotModifiedException extends MercadoLibreAPIException {    

    def NotModifiedException(){
        super("Not modified exception")
    }

    def NotModifiedException(message, error = "not_modified", cause = []) {
        super(message, error, cause)
    }

    def status = 304
}
