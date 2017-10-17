package com.jsonp.filters;

import com.jsonp.wrappers.JsonpRequestWrapper;
import com.jsonp.wrappers.JsonpResponseWrapper;
import com.jsonp.wrappers.MultipleReadRequestWrapper;
import com.jsonp.wrappers.SafeRequestWrapper;
import com.ml.exceptions.MethodNotAllowedException;
import com.ml.public2private.utils.ErrorMessage;
import com.newrelic.api.agent.NewRelic;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class JsonpFilter implements Filter {
    private Logger logger =  Logger.getLogger(this.getClass().getName());;


    private String headerSwitchName=null;
    private String headerSwitchValue=null;

    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain filterChain) throws IOException, ServletException {
        /*replace request with multiplReadWrapper*/
        MultipleReadRequestWrapper myRequestWrapper = new MultipleReadRequestWrapper((HttpServletRequest) request);
        HttpServletResponse originalResponse = (HttpServletResponse) response;
        try {
            if (isFilterActive(myRequestWrapper, originalResponse)) {
                //handle internal errors
                HttpServletRequest req;
                HttpServletResponse res;
                try {
                    req = JsonpRequestWrapper
                            .getInstance(myRequestWrapper);
                    res = JsonpResponseWrapper.getInstance(
                            originalResponse, myRequestWrapper);
                } catch (MethodNotAllowedException e) {
                    logger.error("Method is not allowed");
                    throw e;
                } catch (Exception e) {
                    Throwable root = ExceptionUtils.getRootCause(e);
                    logger.error("JSONP Filter error caught: " + e.getMessage() + " " + e.getClass().getName());
                    logger.error("JSONP Filter error caught (root): " + (root != null?root.getMessage():"null"));
                    filterChain.doFilter(myRequestWrapper, response);
                    return;
                }
                filterChain.doFilter(req, res);
                res.flushBuffer();
            } else {
                HttpServletRequest safeRequest;
                try {
                    safeRequest = SafeRequestWrapper.getInstance(myRequestWrapper);
                } catch (Exception e) {
                    Throwable root = ExceptionUtils.getRootCause(e);
                    logger.error("JSONP Filter error caught: " + e.getMessage() + " " + e.getClass().getName());
                    logger.error("JSONP Filter error caught (root): " + (root != null?root.getMessage():"null"));
                    filterChain.doFilter(myRequestWrapper, response);
                    return;
                }
                filterChain.doFilter(safeRequest, response);
            }
        } catch (MethodNotAllowedException e) {
            PrintWriter writer = response.getWriter();
            originalResponse.setCharacterEncoding("UTF-8");
            originalResponse.setContentType("application/json");
            originalResponse.setStatus(405);
            this.doIgnoreTransaction();
            writer.print(new ErrorMessage(e, "Method not allowed", 405).json());
            writer.flush();
        } catch (RuntimeException e) {
            Throwable root = ExceptionUtils.getRootCause(e);
            logger.error("JSONP Filter error caught: " + e.getMessage() + " " + e.getClass().getName()) ;
            logger.error("JSONP Filter error caught (root): " + (root != null?root.getMessage():"null"));
            throw e;
        }

    }

    public boolean isFilterActive(HttpServletRequest originalRequest,
                                  HttpServletResponse originalResponse) {
        String switchName = getHeaderSwitchName();
        if (switchName!=null) {
            String filterSwitch = originalRequest.getHeader(switchName);
            String switchValue = getHeaderSwitchValue();
            return (switchValue==null && filterSwitch!=null)
                    || (switchValue!=null && switchValue.equals(filterSwitch));
        }else {
            return true;
        }
    }

    public void setHeaderSwitch(String headerSwitch) {
        if (headerSwitch != null) {
            String[] headerSwitchPair = headerSwitch.split(":");
            headerSwitchName = headerSwitchPair.length >= 1 ? headerSwitchPair[0]
                    : null;
            headerSwitchValue = headerSwitchPair.length >= 2 ? headerSwitchPair[1]
                    : null;
        }
    }

    @Override
    public void init(FilterConfig conf) throws ServletException {
        setHeaderSwitch(conf.getInitParameter("headerSwitch"));
    }

    public void setHeaderSwitchName(String headerSwitchName) {
        this.headerSwitchName = headerSwitchName;
    }

    public String getHeaderSwitchName() {
        return headerSwitchName;
    }

    public void setHeaderSwitchValue(String headerSwitchValue) {
        this.headerSwitchValue = headerSwitchValue;
    }

    public String getHeaderSwitchValue() {
        return headerSwitchValue;
    }
    private void doIgnoreTransaction() {
        NewRelic.ignoreTransaction();
    }
}
