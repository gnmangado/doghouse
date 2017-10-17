package com.ml.public2private.filters;

import com.mercadolibre.datadog.MetricCollector;
import com.ml.public2private.exception.AuthenticationException;
import com.ml.public2private.exception.AuthorizationException;
import com.ml.public2private.exception.OAuthException;
import com.ml.public2private.utils.ErrorMessage;
import com.ml.public2private.wrappers.Public2PrivateRequestWrapper;
import com.newrelic.api.agent.NewRelic;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.log4j.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class Public2PrivateFilter implements Filter {
    private Logger logger =  Logger.getLogger(this.getClass().getName());

    private String authDomain;
    private String headerSwitchName=null;
    private String headerSwitchValue=null;



    private String appName=null;
    private Boolean userValidate=true;
    private Boolean allowTestTokens = false;
    private Boolean ignoreTransaction=false;
    public void init(FilterConfig filterConfig) throws ServletException {
        setAuthDomain(filterConfig.getInitParameter("authDomain"));
        setHeaderSwitch(filterConfig.getInitParameter("headerSwitch"));
        setUserValidate(filterConfig.getInitParameter("userValidate"));
        setIgnoreTransaction(filterConfig.getInitParameter("ignoreTransaction"));
        setAppName(filterConfig.getInitParameter("appName"));
        setAllowTestTokens(Boolean.valueOf(filterConfig.getInitParameter("allowTestTokens")));

    }

    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest originalRequest = (HttpServletRequest) request;
        HttpServletResponse originalResponse = (HttpServletResponse) response;
        Boolean localUserValidate = this.userValidate,
                localAllowTestTokens = this.allowTestTokens;

        if (request.getParameter("userValidate")!= null)
            localUserValidate = Boolean.valueOf(request.getParameter("userValidate"));
        /*if (request.getParameter("allowTestTokens") != null) {
            localAllowTestTokens = Boolean.valueOf(request.getParameter("allowTestTokens"));
            System.out.println("chau " +request.getParameter("allowTestTokens"));
        } */
        try {
            if (isFilterActive(originalRequest, originalResponse)) {
                ServletRequest req;
                try {
                    req = Public2PrivateRequestWrapper.getInstance((HttpServletRequest) request, authDomain, localUserValidate, ignoreTransaction, appName, localAllowTestTokens);
                    NewRelic.incrementCounter("Custom/mlapi/api-call/auth/valid/count");
                    MetricCollector.recordFullMetric("Custom.mlapi.api_call.auth.valid.count", 1, "appName:"+appName);
                    chain.doFilter(req, response);
                }catch (OAuthException e) {
                    NewRelic.incrementCounter("Custom/mlapi/api-call/auth/OAuthException/count");
                    MetricCollector.recordFullMetric("Custom.mlapi.api_call.auth.OAuthException.count", 1, "appName:"+appName);

                    PrintWriter writer = response.getWriter();

                    HttpResponse oauthResp = e.getResponse();
                    for (Header header : oauthResp.getHeaders("Connection") ) {
                        oauthResp.removeHeader(header);
                    }

                    setHeaders(originalResponse, oauthResp);
                    originalResponse.setCharacterEncoding("UTF-8");
                    originalResponse.setContentType("application/json");
                    originalResponse.setStatus(oauthResp.getStatusLine().getStatusCode());

                    this.doIgnoreTransaction();

                    writer.print(e.getError());

                    writer.flush();
                }catch (AuthorizationException e) {
                    NewRelic.incrementCounter("Custom/mlapi/api-call/auth/AuthorizationException/count");
                    MetricCollector.recordFullMetric("Custom.mlapi.api_call.auth.AuthorizationException.count", 1, "appName:"+appName);

                    PrintWriter writer = response.getWriter();
                    originalResponse.setCharacterEncoding("UTF-8");
                    originalResponse.setContentType("application/json");
                    originalResponse.setStatus(403);
                    this.doIgnoreTransaction();
                    writer.print(new ErrorMessage(e, "Authorization Error", 403).json());
                    writer.flush();
                }catch (AuthenticationException e) {
                    NewRelic.incrementCounter("Custom/mlapi/api-call/auth/AuthenticationException/count");
                    MetricCollector.recordFullMetric("Custom.mlapi.api_call.auth.AuthenticationException.count", 1, "appName:"+appName);

                    PrintWriter writer = response.getWriter();
                    originalResponse.setCharacterEncoding("UTF-8");
                    originalResponse.setContentType("application/json");
                    originalResponse.setStatus(500);
                    this.doIgnoreTransaction();
                    writer.print(new ErrorMessage(e, e.getError(), 500).json());
                    writer.flush();
                } catch (RuntimeException e ) {
                    NewRelic.incrementCounter("Custom/mlapi/api-call/auth/OtherException/count");
                    MetricCollector.recordFullMetric("Custom.mlapi.api_call.auth.OtherException.count", 1, "appName:"+appName);

                    Throwable root = ExceptionUtils.getRootCause(e);
                    logger.error("P2P Filter error caught: " + e.getMessage() + " " + e.getClass().getName());
                    logger.error("P2P Filter error caught (root): " + (root != null ? root.getMessage() : "null"));
                    throw e;
                }
            }else {
                chain.doFilter(originalRequest, originalResponse);
            }
        } catch (RuntimeException e) {
            Throwable root = ExceptionUtils.getRootCause(e);
            logger.error("P2P Filter error caught: " + e.getMessage() + " " +  e.getClass().getName());
            logger.error("P2P Filter error caught (root): " + (root != null?root.getMessage():"null"));
            throw e;
        }

    }

    private void doIgnoreTransaction() {
        if (ignoreTransaction) NewRelic.ignoreTransaction();
    }

    private void setHeaders(HttpServletResponse target,
                            HttpResponse source) {
        Header[] headers = source.getAllHeaders();

        for (Header h : headers) {
            target.setHeader(h.getName(), h.getValue());
        }
    }

    private boolean isFilterActive(HttpServletRequest originalRequest,
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
    public void setUserValidate(String userValidate) {
        if (userValidate != null) {
            this.userValidate = Boolean.valueOf(userValidate);
        }
    }
    public void setIgnoreTransaction(String ignoreTransaction) {
        if (ignoreTransaction != null) {
            this.ignoreTransaction = Boolean.valueOf(ignoreTransaction);
        }
    }

    public void destroy() {

    }

    public void setAuthDomain(String authDomain) {
        this.authDomain = authDomain;
    }

    public String getAuthDomain() {
        return authDomain;
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

     public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void setAllowTestTokens(Boolean allowTestTokens) {
        this.allowTestTokens = allowTestTokens;
    }
}
