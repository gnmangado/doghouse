package com.ml.public2private.wrappers;

import com.jsonp.wrappers.MultipleReadRequestWrapper;
import com.mercadolibre.datadog.MetricCollector;
import com.mercadolibre.datadog.utils.Timer;
import com.ml.public2private.exception.AuthenticationException;
import com.ml.public2private.exception.AuthorizationException;
import com.ml.public2private.exception.OAuthException;
import com.newrelic.api.agent.NewRelic;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.codehaus.groovy.grails.web.json.JSONArray;
import org.codehaus.groovy.grails.web.json.JSONException;
import org.codehaus.groovy.grails.web.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

public class Public2PrivateRequestWrapper extends MultipleReadRequestWrapper {

    private static final int APACHE_POOL_SIZE = 150;
    private static final Object SO_TIMEOUT = 1000;
    private static final boolean STALE_CONNECTION_CHECK = true;
    private static final int CONNECTION_TIMEOUT = 1000;
    private Set<String> forbiddenParams = new HashSet<String>(Arrays.asList(
            "caller.id", "caller.scopes", "caller.status", "client.id",
            "admin.id"));
    private Set<String> forbiddenHeaders = new HashSet<String>(Arrays.asList(
            "x-caller-id", "x-caller-scopes", "x-caller-status", "x-client-id",
            "x-admin-id", "x-test-token"));

    private Logger logger;
    private HttpClient client;
    private JSONObject privateData;
    private Map<String, String> privateHeaders;
    private String authDomain = "http://localhost:8080";
    private Boolean userValidate = null;
    private Boolean ignoreTransaction = false;
    private Boolean allowTestTokens = false;
    private HashMap<String, String> privateParams = null;
    private String appName;

    private Public2PrivateRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        logger = Logger.getLogger(this.getClass().getName());
    }

    /**
     * Devuelve la instancia que corresponda
     *
     * @param request
     * @param authDomain
     * @return
     */
    public static HttpServletRequest getInstance(HttpServletRequest request, String authDomain, Boolean userValidate, Boolean ignoreTransaction, String appName,
                                                 Boolean allowTestTokens) throws IOException, AuthorizationException {
        Public2PrivateRequestWrapper public2PrivateRequestWrapper = new Public2PrivateRequestWrapper(
                request);
        public2PrivateRequestWrapper.appName = appName;
        public2PrivateRequestWrapper.setAuthDomain(authDomain);
        public2PrivateRequestWrapper.setUserValidate(userValidate);
        public2PrivateRequestWrapper.setAllowTestTokens(allowTestTokens);
        public2PrivateRequestWrapper.setIgnoreTransaction(ignoreTransaction);
        public2PrivateRequestWrapper.init();
        return public2PrivateRequestWrapper;
    }

    @Override
    public String getHeader(String name) {
        if (forbiddenHeaders.contains(name.toLowerCase())) {
            return getPrivateHeadersMap().get(name.toLowerCase());
        }
        return super.getHeader(name);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Enumeration getHeaders(String name) {
        if (forbiddenHeaders.contains(name.toLowerCase())) {
            String rta;
            rta = getPrivateHeadersMap().get(name.toLowerCase());
            if (rta != null) {
                return Collections.enumeration(Arrays.asList(rta));
            } else {
                return Collections.enumeration(new ArrayList());
            }
        }
        return super.getHeaders(name);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Enumeration getHeaderNames() {
        List<String> headerNames = Collections.list(super.getHeaderNames());
        List<String> toRemove = new ArrayList<String>(headerNames.size());
        for (String header : headerNames) {
            if (forbiddenHeaders.contains(header.toLowerCase())) {
                toRemove.add(header);
            }
        }
        headerNames.removeAll(toRemove);
        headerNames.addAll(getPrivateHeadersMap().keySet());
        return Collections.enumeration(headerNames);
    }

    @Override
    public String getParameter(String name) {
        if (forbiddenParams.contains(name.toLowerCase())) {
            return (String) getParameterMap().get(name);
        }
        return super.getParameter(name);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Map getParameterMap() {
        Map parameterMap = new HashMap(super.getParameterMap());
        for (String param : forbiddenParams) {
            parameterMap.remove(param);
        }
        parameterMap.putAll(privateParams);

        return parameterMap;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Enumeration getParameterNames() {
        return Collections.enumeration(getParameterMap().keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        if (forbiddenParams.contains(name.toLowerCase())) {
            String rta[] = {(String) getParameterMap().get(name)};
            return rta;
        }
        return super.getParameterValues(name);
    }

    /**
     * Obtiene los parámetros privados a partir del access_token.
     *
     * @throws AuthenticationException
     * @throws AuthorizationException
     *
     * @return JSONObject con los datos privados o null si es un llamado anónimo
     *         (sin access_token)
     */
    private JSONObject getPrivateData() throws AuthenticationException,
            AuthorizationException {
        if (privateData == null) {
            String accessToken = super.getParameter("access_token");
            if (accessToken == null || "".equals(accessToken.trim())) {
                return null;
            }
            String url = authDomain + "/auth/access_token/" + urlEncodeIt(accessToken);

            if (allowTestTokens)
                url += "?allowTestTokens=true";

            HttpGet httpget = new HttpGet( url );

            HttpResponse response = null;
            Timer t = new Timer();
            try {
                response = client.execute(httpget);
                NewRelic.incrementCounter("Custom/mlapi/api-call/auth/success/count");
                MetricCollector.recordFullMetric("Custom.mlapi.api_call.auth.success.count", 1, "appName:"+appName);
            } catch (Exception e) {
                //this.doIgnoreTransaction();
                NewRelic.noticeError(e);
                NewRelic.incrementCounter("Custom/mlapi/api-call/auth/fail/count");
                MetricCollector.recordFullMetric("Custom.mlapi.api_call.auth.fail.count", 1, "appName:"+appName);
                throw new AuthenticationException("OAuth connect error", "Error converting access token", e);
            } finally {
                MetricCollector.recordFullMetric("Custom.mlApi.auth", t.getEllapsedTimeInMillis(), "appName:" + appName, "responseStatus:" + (response != null ? response.getStatusLine().getStatusCode() : -1), "method:" + getMethod());
                NewRelic.recordResponseTimeMetric("Custom/mlapi/api-call/auth/time", t.getEllapsedTimeInMillis());
            }
            if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                try {
                    String jsonString = EntityUtils.toString(entity);
                    privateData = new JSONObject(jsonString);
                } catch (Exception e) {
                    this.doIgnoreTransaction();
                    throw new AuthenticationException("OAuth reading error", "Error converting access token", e);
                }
            }else {
                String error;
                try {
                    error = EntityUtils.toString(response.getEntity());
                    //If access_token is not found (status code: 404) it returns 401-Unauthorized
                    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                        JSONObject json = new JSONObject(error);
                        json.put("status", HttpStatus.SC_UNAUTHORIZED);
                        error = json.toString();
                        response.setStatusCode(HttpStatus.SC_UNAUTHORIZED);
                    }
                } catch (Exception e) {
                    this.doIgnoreTransaction();
                    throw new AuthenticationException("OAuth reading error", "Error converting access token", e);
                }
                throw new OAuthException(error, "Invalid Auth status response [" + response.getStatusLine() + "]", response);
            }
        }

        validate(privateData);

        return privateData;
    }

    /**
     * @param parameter Un parámetro de queryString
     * @return El mismo parámetro encodeado para URL
     */
    private String urlEncodeIt(String parameter) {
        try {
            return URLEncoder.encode(parameter, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            return parameter;
        }
    }


    private void doIgnoreTransaction() {
        if (ignoreTransaction) NewRelic.ignoreTransaction();
    }

    private void validate(JSONObject json) throws AuthenticationException, AuthorizationException {
        try {
            if (userValidate==true && json!=null && json.has("status") && !"active".equals(json.getString("status").toLowerCase())) {
                throw new AuthorizationException("User not active", null);
            }
            if (json.containsKey("is_test") && json.getBoolean("is_test") && !allowTestTokens) {
                throw new AuthorizationException("Test tokens are not allowed");
            }
        } catch (JSONException e) {
            throw new AuthenticationException("User status error", "Error on status Validation", e);
        } catch (NullPointerException e) {
            throw new AuthenticationException("User status error", "Error on status Validation", e);
        }
    }

    private void initHttpClient() {
        HttpParams params = new BasicHttpParams();
        ConnManagerParams.setMaxTotalConnections(params, APACHE_POOL_SIZE);
        params.setParameter(CoreConnectionPNames.SO_TIMEOUT, SO_TIMEOUT);
        params.setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, STALE_CONNECTION_CHECK);
        params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, CONNECTION_TIMEOUT);

        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
        client = new DefaultHttpClient(cm, params);
    }

    private String getScopes(JSONObject user) throws JSONException {
        JSONArray array = user.getJSONArray("scopes");
        StringBuilder scope = new StringBuilder();
        for (int i = 0; i < array.length(); i++) {
            scope.append(array.getString(i));
            scope.append(i != (array.length() - 1) ? "," : "");
        }
        logger.info("scopes: " + scope.toString());
        return scope.toString();
    }

    private Boolean isTestToken(JSONObject token) {
        return token.containsKey("is_test") && token.getBoolean("is_test");
    }

    private Map<String, String> getPrivateHeadersMap() {
        return privateHeaders;
    }

    public String getAuthDomain() {
        return authDomain;
    }

    public void setAuthDomain(String authDomain) {
        this.authDomain = authDomain;
    }
    public void setUserValidate(Boolean userValidate) {
        this.userValidate = userValidate;
    }
    public void setIgnoreTransaction(Boolean ignoreTransaction) {
        this.ignoreTransaction = ignoreTransaction;
    }

    private void initPrivateParams() throws AuthenticationException, AuthorizationException {
        privateParams = new HashMap<String, String>();
        try {
            JSONObject json = getPrivateData();
            if (json != null) {
                String callerId = json.getString("user_id");
                privateParams.put("caller.id", callerId);
                privateParams.put("caller.scopes", getScopes(json));
                privateParams.put("caller.status", "ACTIVE");
                if (json.has("client_id")) {
                    String clientId = json.get("client_id").toString();
                    privateParams.put("client.id", clientId);
                }
                if (json.has("admin_id")) {
                    String adminId = json.getString("admin_id");
                    privateParams.put("admin.id", adminId);
                }
                if (json.has("site_id")) {
                    String siteId = json.get("site_id").toString();
                    privateParams.put("caller.siteId", siteId);
                }
            }
        } catch (JSONException e) {
            throw new AuthenticationException(
                    "Auth response reading error", e.getMessage(), e);
        }
    }

    private void initPrivateHeaders() throws AuthenticationException, AuthorizationException {
        JSONObject json = getPrivateData();
        privateHeaders = new HashMap<String, String>();
        if (json != null) {
            try {
                String callerId = json.getString("user_id");
                privateHeaders.put("x-caller-id", callerId);
                privateHeaders.put("x-caller-scopes", getScopes(json));
                privateHeaders.put("x-caller-status", "ACTIVE");
                privateHeaders.put("x-test-token", isTestToken(json).toString());
                if (json.has("client_id")) {
                    String clientId = json.get("client_id").toString();
                    privateHeaders.put("x-client-id", clientId);
                }
                if (json.has("admin_id")) {
                    String adminId = json.getString("admin_id");
                    privateHeaders.put("x-admin-id", adminId);
                }
            } catch (JSONException e) {
                throw new AuthenticationException(
                        "Error reading private data");
            }
        }
    }

    private void init() throws AuthenticationException, AuthorizationException {
        initHttpClient();
        initPrivateParams();
        initPrivateHeaders();
    }

    public void setAllowTestTokens(Boolean allowTestTokens) {
        this.allowTestTokens = allowTestTokens;
    }
}
