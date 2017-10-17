package com.ml;

import grails.converters.JSON;
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException;
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller;
import org.codehaus.groovy.grails.web.json.JSONWriter;

import java.util.Map;

/**
 * @author Siegfried Puchbauer
 * @since 1.1
 */
@SuppressWarnings("unchecked")
public class MLMapMarshaller implements ObjectMarshaller<JSON> {
    public boolean splits = true;

    public MLMapMarshaller() {

    }

    public MLMapMarshaller(boolean splits) {
        this.splits = splits;
    }


    public boolean supports(Object object) {
        return object instanceof Map;
    }

    public void marshalObject(Object o, JSON converter) throws ConverterException {
        JSONWriter writer = converter.getWriter();
        writer.object();
        Map<Object,Object> map = (Map<Object,Object>) o;
        for (Map.Entry<Object,Object> entry : map.entrySet()) {
            Object key = entry.getKey();
            if (key != null) {
                Object value = entry.getValue();
                if (value instanceof groovy.lang.Closure) {
                    value = ((groovy.lang.Closure)value).call();
                }
                String skey = splits ? splitCamelCase(key.toString()) : key.toString();
                writer.key(skey);
                converter.convertAnother(value);
            }
        }
        writer.endObject();
    }
    
    static String splitCamelCase(String s) {
       return s.replaceAll("([a-z\\d])([A-Z])", "$1_$2").toLowerCase();
    }
}
