package com.ml.exceptions

/**
 * User: mwaisgold
 * Date: 22/09/2010
 * Time: 11:02:46
 */
class ValidationException extends BadRequestException{

    ValidationException(message, errorString = "bad_request", causes = []){
        super(message, errorString, causes)
    }

}
