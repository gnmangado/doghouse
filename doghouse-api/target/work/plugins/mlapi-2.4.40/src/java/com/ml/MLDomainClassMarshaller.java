package com.ml;

import grails.converters.JSON;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.support.proxy.DefaultProxyHandler;
import org.codehaus.groovy.grails.support.proxy.ProxyHandler;
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException;
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller;
import org.codehaus.groovy.grails.web.json.JSONWriter;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.util.*;

public class MLDomainClassMarshaller implements ObjectMarshaller<JSON> {
    public boolean splits = true;
    private boolean includeVersion = false;
    private boolean includeClass = true;
    private ProxyHandler proxyHandler;

    public MLDomainClassMarshaller(boolean includeVersion) {
        this(includeVersion, false, new DefaultProxyHandler(), true);
    }

    public MLDomainClassMarshaller(boolean includeVersion, boolean includeClass) {
        this(includeVersion, includeClass, new DefaultProxyHandler(), true);
    }

    public MLDomainClassMarshaller(boolean includeVersion, boolean includeClass, boolean splits) {
        this(includeVersion, includeClass, new DefaultProxyHandler(), splits);
    }

    public MLDomainClassMarshaller(boolean includeVersion, boolean includeClass, ProxyHandler proxyHandler, boolean splits) {
        this.includeVersion = includeVersion;
        this.proxyHandler = proxyHandler;
        this.includeClass = includeClass;
        this.splits = splits;
    }

    public boolean isIncludeVersion() {
        return includeVersion;
    }

    public boolean isIncludeClass() {
        return includeClass;
    }

    public void setIncludeVersion(boolean includeVersion) {
        this.includeVersion = includeVersion;
    }

    public boolean supports(Object object) {
        //return ConverterUtil.isDomainClass(object.getClass());
        return DomainClassArtefactHandler.isDomainClass(object.getClass());
    }

    static String splitCamelCase(String s) {
       return s.replaceAll("([a-z\\d])([A-Z])", "$1_$2").toLowerCase();
    }

    private DomainClassArtefactHandler getDomainClassArtefactHandler(){
        return new DomainClassArtefactHandler();
    }

    @SuppressWarnings("unchecked")
    public void marshalObject(Object value, JSON json) throws ConverterException {
        JSONWriter writer = json.getWriter();
        value = proxyHandler.unwrapIfProxy(value);
        Class<?> clazz = value.getClass();
        //grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE)
        //Domainc
        //GrailsDomainClass domainClass = ConverterUtil.getDomainClass(clazz.getName());
        GrailsDomainClass domainClass = (GrailsDomainClass) getDomainClassArtefactHandler().newArtefactClass(clazz);
        BeanWrapper beanWrapper = new BeanWrapperImpl(value);

        writer.object();
        if(isIncludeClass()) {
           writer.key("class").value(domainClass.getClazz().getName());
        }

        GrailsDomainClassProperty id = domainClass.getIdentifier();
        Object idValue = extractValue(value, id);

        json.property("id", idValue);

        if (isIncludeVersion()) {
            GrailsDomainClassProperty versionProperty = domainClass.getVersion();
            Object version = extractValue(value, versionProperty);
            json.property("version", version);
        }

        GrailsDomainClassProperty[] properties = domainClass.getPersistentProperties();

        for (GrailsDomainClassProperty property : properties) {
            String skey = splits ?  splitCamelCase(property.getName()) : property.getName();
            writer.key(skey);
            if (!property.isAssociation()) {
                // Write non-relation property
                Object val = beanWrapper.getPropertyValue(property.getName());
                json.convertAnother(val);
            }
            else {
                Object referenceObject = beanWrapper.getPropertyValue(property.getName());
                if (isRenderDomainClassRelations()) {
                    if (referenceObject == null) {
                        writer.value(null);
                    }
                    else {
                        referenceObject = proxyHandler.unwrapIfProxy(referenceObject);
                        if (referenceObject instanceof SortedMap) {
                            referenceObject = new TreeMap((SortedMap) referenceObject);
                        }
                        else if (referenceObject instanceof SortedSet) {
                            referenceObject = new TreeSet((SortedSet) referenceObject);
                        }
                        else if (referenceObject instanceof Set) {
                            referenceObject = new HashSet((Set) referenceObject);
                        }
                        else if (referenceObject instanceof Map) {
                            referenceObject = new HashMap((Map) referenceObject);
                        }
                        else if (referenceObject instanceof Collection){
                            referenceObject = new ArrayList((Collection) referenceObject);
                        }
                        json.convertAnother(referenceObject);
                    }
                }
                else {
                    if (referenceObject == null) {
                        json.value(null);
                    }
                    else {
                        //GrailsDomainClass referencedDomainClass = property.getReferencedDomainClass();
                        GrailsDomainClass referencedDomainClass = (GrailsDomainClass) new DomainClassArtefactHandler().newArtefactClass(property.getReferencedPropertyType());
                        // Embedded are now always fully rendered
                        if(referencedDomainClass == null || property.isEmbedded() || GrailsClassUtils.isJdk5Enum(property.getType())) {
                            json.convertAnother(referenceObject);
                        }
                        else if (property.isOneToOne() || property.isManyToOne() || property.isEmbedded()) {
                            asShortObject(referenceObject, json, referencedDomainClass.getIdentifier(), referencedDomainClass);
                        }
                        else {
                            GrailsDomainClassProperty referencedIdProperty = referencedDomainClass.getIdentifier();
                            @SuppressWarnings("unused")
                            String refPropertyName = referencedDomainClass.getPropertyName();
                            if (referenceObject instanceof Collection) {
                                Collection o = (Collection) referenceObject;
                                writer.array();
                                for (Object el : o) {
                                    asShortObject(el, json, referencedIdProperty, referencedDomainClass);
                                }
                                writer.endArray();
                            }
                            else if (referenceObject instanceof Map) {
                                Map<Object, Object> map = (Map<Object, Object>) referenceObject;
                                for (Map.Entry<Object, Object> entry : map.entrySet()) {
                                    String key = String.valueOf(entry.getKey());
                                    Object o = entry.getValue();
                                    writer.object();
                                    writer.key(key);
                                    asShortObject(o, json, referencedIdProperty, referencedDomainClass);
                                    writer.endObject();
                                }
                            }
                        }
                    }
                }
            }
        }
        writer.endObject();
    }

    protected void asShortObject(Object refObj, JSON json, GrailsDomainClassProperty idProperty, GrailsDomainClass referencedDomainClass) throws ConverterException {
        JSONWriter writer = json.getWriter();
        writer.object();
        if(isIncludeClass()) {
            writer.key("class").value(referencedDomainClass.getName());
        }
        writer.key("id").value(extractValue(refObj, idProperty));
        writer.endObject();
    }

    protected Object extractValue(Object domainObject, GrailsDomainClassProperty property) {
        BeanWrapper beanWrapper = new BeanWrapperImpl(domainObject);
        return beanWrapper.getPropertyValue(property.getName());
    }

    protected boolean isRenderDomainClassRelations() {
        return false;
    }

}
