import com.ml.MLDomainClassMarshaller
import com.ml.MLGroovyBeanMarshaller
import com.ml.MLMapMarshaller
import com.ml.t9n
import grails.converters.JSON
import grails.util.Environment
import org.joda.time.DateTime
import org.springframework.validation.Errors
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError

import static com.ml.MLGroovyBeanMarshaller.splitCamelCase

class MLBootStrap {
    def grailsApplication

    def excludedProps = ["onLoad",
        "beforeDelete", "afterDelete",
        "beforeInsert", "afterInsert",
        "beforeUpdate", "afterUpdate"]

    def init = { servletContext ->

        //set default value
        if (Environment.getCurrentEnvironment() == Environment.PRODUCTION && grailsApplication.config.mlapi.ofuscateErrors == null)
            grailsApplication.config.mlapi.ofuscateErrors = true
        if (Environment.getCurrentEnvironment() == Environment.PRODUCTION && !grailsApplication.config.mlapi.validateExternalScopes in [true, false])
            grailsApplication.config.mlapi.validateExternalScopes = false
        if (Environment.getCurrentEnvironment() == Environment.TEST && !grailsApplication.config.mlapi.validateExternalScopes  in [true, false] && grails.util.Metadata.current.'app.name' == "mlapi")
            grailsApplication.config.mlapi.validateExternalScopes = true





        JSON.createNamedConfig("noModify") {
            it.registerObjectMarshaller(new MLGroovyBeanMarshaller(false))
            it.registerObjectMarshaller(new MLDomainClassMarshaller(false, false, false))
            it.registerObjectMarshaller(new MLMapMarshaller(false))
            it.registerObjectMarshaller(Errors, 3) { Errors errors ->
                //map.errors.descriptions = errors.allErrors
                errors.allErrors
            }
            it.registerObjectMarshaller(ObjectError, 3) { ObjectError err ->
                String msg = grailsApplication.mainContext.getMessage(it, Locale.getDefault())
                if(err in FieldError) {
                    msg = msg.replace(it.field, splitCamelCase(err.field))
                }

                [code: err.code, message: msg]
            }
            it.registerObjectMarshaller(Date) { date ->
                return new DateTime(date).toString()
            }
        }
        JSON.createNamedConfig("underscore") {
            it.registerObjectMarshaller(new MLGroovyBeanMarshaller())
            it.registerObjectMarshaller(new MLDomainClassMarshaller(false, false))
            it.registerObjectMarshaller(new MLMapMarshaller())
            it.registerObjectMarshaller(Errors, 3) { Errors errors ->
                //map.errors.descriptions = errors.allErrors
                errors.allErrors
            }
            it.registerObjectMarshaller(ObjectError, 3) { ObjectError err ->
                String msg = grailsApplication.mainContext.getMessage(err, Locale.getDefault())
                if(err in FieldError) {
                    msg = msg.replace(err.field, splitCamelCase(err.field))
                }

                [code: err.code, message: msg]
            }
            it.registerObjectMarshaller(Date) {date ->
                return new DateTime(date).toString()
            }


        }

        t9n.grailsApplication = grailsApplication;
        grailsApplication.config.mlAPI.excludedResources << [error: ["treatExceptions", "notFound"]]
    }
    def destroy = {
    } 
}
