package com.ml.exceptions

class PaymentRequiredException extends MercadoLibreAPIException {
    def status = 402

    def PaymentRequiredException(message, error = "payment_required", cause = []) {
        super(message, error, cause)
    }

}
