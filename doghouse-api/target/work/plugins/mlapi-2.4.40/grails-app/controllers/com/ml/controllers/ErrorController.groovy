package com.ml.controllers

import com.ml.exceptions.MercadoLibreAPIException
import org.apache.commons.lang.exception.ExceptionUtils
import org.codehaus.groovy.grails.web.errors.GrailsWrappedRuntimeException
import org.hibernate.StaleObjectStateException
import org.hibernate.exception.ConstraintViolationException
import org.springframework.orm.hibernate3.HibernateOptimisticLockingFailureException

import javax.servlet.http.HttpServletResponse

class ErrorController {

    def grailsApplication

    def treatExceptions = {
        try {

            log.debug("Catched exception", request.exception)

            def exception = request.exception

            // Unwrap exception
            if (exception in GrailsWrappedRuntimeException) {
                exception = exception.cause
            }

            def status = 500
            def cause = []
            def error = ""
            def message = exception.message

            if (exception.hasProperty("internalCause")/* && exception.cause && exception.cause != exception*/) {
                cause = exception.internalCause
            }

            if (exception.hasProperty("error")) {
                error = exception.error
            }

            if (exception.hasProperty("status")) {
                status = exception.status
            }

            // For non business exceptions show a 500
            if (!(exception in MercadoLibreAPIException)) {
                status = 500
                error = "internal_error"
                if (grailsApplication.config.mlapi?.conflictOnConstraint && isConstraintViolation(exception)){
                    status = HttpServletResponse.SC_CONFLICT
                    error = "conflict_error"
                }


                // Ofuscate real cause in prod env
                if ( grailsApplication.config.mlapi?.ofuscateErrors || !isInternal()) {
                    message = "Oops! Something went wrong..."
                    cause = []
                }
            }
            // Log 5xx errors
            if (status >= 500) {
                log.error "Internal Error", request.exception
            }

            def response = [:]
            response.message = message
            response.error = error
            response.status = status

            //always hide cause in external calls if throwable
            response.cause =
                    (
                            isInternal() ||
                            (
                                    !(cause in Throwable)&&
                                    !cause.any{
                                        it.class?.getSimpleName()?.contains("Exception") ||
                                        it.class?.getSimpleName()?.contains("Error")
                                    }
                            )
                    ) ? cause : null


            [response: response, status: status]

        } catch (e) {
            log.error "Error in ErrorAPIController", e
        }
    }

    private isConstraintViolation(Throwable exception) {
        if (ExceptionUtils.indexOfThrowable(exception, ConstraintViolationException) >= 0){
            return true
        }

        if (ExceptionUtils.indexOfThrowable(exception, HibernateOptimisticLockingFailureException) >= 0){
            return true
        }

        if (ExceptionUtils.indexOfThrowable(exception, StaleObjectStateException) >= 0 ){
            return true
        }

        return ExceptionUtils.getThrowables(exception).any { it.message?.contains("Unique index") || it.message?.contains("unique constraint") }
    }

    def notFound = {
        def resp = [:]
        resp.message = "Resource $request.forwardURI not found."
        resp.error = "not_found"
        resp.status = 404
        resp.cause = []

        [response:resp, status:resp.status]
    }
}
