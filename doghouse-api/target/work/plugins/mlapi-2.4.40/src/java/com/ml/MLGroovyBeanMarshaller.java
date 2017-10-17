package com.ml;

import grails.converters.JSON;
import groovy.lang.GroovyObject;
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException;
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller;
import org.codehaus.groovy.grails.web.json.JSONWriter;
import org.springframework.beans.BeanUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class MLGroovyBeanMarshaller implements ObjectMarshaller<JSON> {
    public boolean splits = true;
    public boolean supports(Object object) {
        return object instanceof GroovyObject;
    }
    public MLGroovyBeanMarshaller() {

    }


    public MLGroovyBeanMarshaller(boolean splits) {
        this.splits = splits;
    }
    public void marshalObject(Object o, JSON json) throws ConverterException {
        JSONWriter writer = json.getWriter();
        try {
            writer.object();
            for (PropertyDescriptor property : BeanUtils.getPropertyDescriptors(o.getClass())) {
                String name = property.getName();
                Method readMethod = property.getReadMethod();
                if (readMethod != null && !(name.equals("metaClass")) && !(name.equals("class"))) {
                    Object value = readMethod.invoke(o, (Object[]) null);
                    String key = splits ? splitCamelCase(name) : name;
                    writer.key(key);
                    json.convertAnother(value);
                }
            }
            for (Field field : o.getClass().getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (Modifier.isPublic(modifiers) && !(Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers))) {
                    String key = splits ? splitCamelCase(field.getName()) : field.getName();
                    writer.key(key);
                    json.convertAnother(field.get(o));
                }
            }
            writer.endObject();
        }
        catch (ConverterException ce) {
            throw ce;
        }
        catch (Exception e) {
            throw new ConverterException("Error converting Bean with class " + o.getClass().getName(), e);
        }
    }
    public static String splitCamelCase(String s) {
       return s.replaceAll("([a-z\\d])([A-Z])", "$1_$2").toLowerCase();
    }
}
