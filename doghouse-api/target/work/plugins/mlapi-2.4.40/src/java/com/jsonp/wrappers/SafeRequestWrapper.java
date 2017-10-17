package com.jsonp.wrappers;

import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

/**
 * Limpia de parámetros propios de JSONP ap request
 * 
 * @author nallegrotti
 * 
 */
public class SafeRequestWrapper extends MultipleReadRequestWrapper {

	@Override
	@SuppressWarnings("rawtypes")
	public Enumeration getHeaderNames() {
		@SuppressWarnings("unchecked")
		ArrayList<String> originales = Collections.list(super.getHeaderNames());
		originales.add("Content-Type");
        String removeHeader = null;
        for (String header : originales) {
            if ("x-http-method-override".equals(header.toLowerCase()))
                removeHeader = header;
        }
        if (removeHeader != null) originales.remove(removeHeader);
		return Collections.enumeration(originales);
	}

	protected SafeRequestWrapper(HttpServletRequest request) throws IOException {
		super(request);

    }

	public static HttpServletRequest getInstance(HttpServletRequest request) throws IOException {
		Logger hlog = Logger.getLogger("headerLogger");
		if (hlog.isDebugEnabled()) {
			Enumeration<?> headerNames = request.getHeaderNames();
			StringBuilder headers = new StringBuilder();
			while (headerNames.hasMoreElements()) {
				String name = (String) headerNames.nextElement();
				String value = request.getHeader(name);
				headers.append(name).append(":").append(value);
			}
			hlog.debug("HEADERS: " + headers);
		}
        if (isUnsafe(request)) {
			return new SafeRequestWrapper(request);
		} else if (request instanceof MultipleReadRequestWrapper) {
            return request;
        } else  {
            return new MultipleReadRequestWrapper(request);
        }
	}

	@Override
	public String[] getParameterValues(String name) {
		@SuppressWarnings("rawtypes")
		Map map = getParameterMap();
		return (String[]) map.get(name);
	}

	@Override
	public String getParameter(String name) {
        if (name.equalsIgnoreCase("_method"))
            return null;
        else
            return super.getParameter(name);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Map getParameterMap() {
        Map parameterMap = new HashMap(super.getParameterMap());
		if (parameterMap != null) {
			parameterMap.remove("_method");
		}
		return parameterMap;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Enumeration getParameterNames() {
		Set ks = getParameterMap().keySet();
		return Collections.enumeration(ks);
	}

	public static boolean isUnsafe(HttpServletRequest request) {
		return request.getMethod().equals("POST")
				&& (request.getParameter("_method") != null || request.getHeader("x-http-method-override") != null);
	}

	@Override
	public String getHeader(String name) {
		if (name.equalsIgnoreCase("Accept")) {
			return "application/json";
		} else if (name.equalsIgnoreCase("Content-Type")) {
            return "application/json";
        } else if (name.equalsIgnoreCase("x-http-method-override")) {
            return "";
		} else {
			return super.getHeader(name);
		}
	}

	@SuppressWarnings({ "rawtypes" })
	@Override
	public Enumeration getHeaders(String name) {
		if (name.equalsIgnoreCase("Accept")) {
			return new StringTokenizer("application/json");
		} else if (name.equalsIgnoreCase("Content-Type")) {
			return new StringTokenizer("application/json");
		}  else if (name.equalsIgnoreCase("x-http-method-override")) {
            return new StringTokenizer("");
		} else {
			return super.getHeaders(name);
		}
	}

	@Override
	public String getQueryString() {
		String originalQueryString = super.getQueryString();
		if (originalQueryString!=null) {
			StringBuilder queryString = new StringBuilder();
			String separador = "";
			for (String param : originalQueryString.split("&")) {
				if (!param.startsWith("callback=")
						&& !param.startsWith("_method=")
						&& !param.startsWith("_body=")) {
					queryString.append(separador);
					queryString.append(param);
					separador = "&";
				}
			}
			return queryString.toString();
		}else {
			return null;
		}
	}
}
