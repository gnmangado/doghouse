package com.ml.controllers

import com.ml.exceptions.MercadoLibreAPIException

/**
 * Created by dguzik on 6/4/14.
 */
abstract class MercadoLibreAPIController {
    def MercadoLibreAPIExceptionHandler(MercadoLibreAPIException e) {
        throw e
    }
}
