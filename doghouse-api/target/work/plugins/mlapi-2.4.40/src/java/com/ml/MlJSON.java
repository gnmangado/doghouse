package com.ml;

import grails.converters.JSON;
import groovy.lang.Closure;
import org.codehaus.groovy.grails.web.converters.configuration.DefaultConverterConfiguration;
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException;
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller;

/**
 * Created by dguzik on 11/11/14.
 */
class MlJSON extends JSON {

    static String DEFAULT_MLAPI_CONFIG = "underscore";

    public static void registerObjectMarshaller(Class<?> clazz, Closure<?> callable) throws ConverterException {
        DefaultConverterConfiguration config = (DefaultConverterConfiguration) getNamedConfig(DEFAULT_MLAPI_CONFIG);
        config.registerObjectMarshaller(clazz, callable);

    }

    public
    static void registerObjectMarshaller(Class<?> clazz, int priority, Closure<?> callable) throws ConverterException {
        DefaultConverterConfiguration config = (DefaultConverterConfiguration) getNamedConfig(DEFAULT_MLAPI_CONFIG);
        config.registerObjectMarshaller(clazz, priority, callable);
    }

    public static void registerObjectMarshaller(ObjectMarshaller<JSON> om) throws ConverterException {
        DefaultConverterConfiguration config = (DefaultConverterConfiguration) getNamedConfig(DEFAULT_MLAPI_CONFIG);
        config.registerObjectMarshaller(om);
    }

    public static void registerObjectMarshaller(ObjectMarshaller<JSON> om, int priority) throws ConverterException {
        DefaultConverterConfiguration config = (DefaultConverterConfiguration) getNamedConfig(DEFAULT_MLAPI_CONFIG);
        config.registerObjectMarshaller(om, priority);

    }
}