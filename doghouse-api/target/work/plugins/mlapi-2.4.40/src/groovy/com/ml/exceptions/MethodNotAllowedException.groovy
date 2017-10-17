package com.ml.exceptions

class MethodNotAllowedException extends MercadoLibreAPIException {
    def status = 405

    def MethodNotAllowedException(message, error = "method_not_allowed", cause = []) {
        super(message, error, cause)
    }

}
