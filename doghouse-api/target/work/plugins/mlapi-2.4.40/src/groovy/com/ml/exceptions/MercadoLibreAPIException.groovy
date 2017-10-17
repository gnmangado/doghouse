package com.ml.exceptions

class MercadoLibreAPIException extends RuntimeException {

    def status = 500
    def error
    def internalCause = []

    def MercadoLibreAPIException(message, error, cause) {
        super(message.toString(), (cause in Throwable) ? cause : null)
        this.error = error
        this.internalCause = cause
    }

    def MercadoLibreAPIException(message){
        super(message)
    }

    def MercadoLibreAPIException() {
        super("internal_error")
    }

}
