package com.ml


class t9n {
	public static grailsApplication = null;
	public static t9nService = null;
    public static tr(String s) {
        return {->
        	if (!t9nService && grailsApplication?.mainContext?.containsBean("t9nService"))
        		t9nService = grailsApplication?.mainContext?.getBean("t9nService")
        	if (t9nService) {
        		def locale = getRequestParameter("Accept-language", "lang")
        		if (!locale) locale = "en"
        		t9nService.tr(s:s,locale:locale).toString();
        	} else
        	   	s.toString()
        };
    }
    public static String getRequestParameter(String header, String paramName = null) {
        def request = org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes()
        def value = request.getHeader(header)

        if (paramName && value == null) {
            value = request.params?.get(paramName)
        }

        return value
    }
}
