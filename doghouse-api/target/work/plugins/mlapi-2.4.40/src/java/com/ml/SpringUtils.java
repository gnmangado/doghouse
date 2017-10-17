package com.ml;

import grails.util.Holders;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.springframework.context.ApplicationContext;

import javax.servlet.ServletContext;

public class SpringUtils {

   public static ApplicationContext getCtx() {
       return getApplicationContext();
   }

   public static ApplicationContext getApplicationContext() {
      ServletContext servletContext = (ServletContext) Holders.getServletContext();
      return (ApplicationContext) servletContext.getAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT);
   }

   @SuppressWarnings("unchecked")
   public static <T> T getBean(String beanName) {
               return (T) getApplicationContext().getBean(beanName);
   }
   
}