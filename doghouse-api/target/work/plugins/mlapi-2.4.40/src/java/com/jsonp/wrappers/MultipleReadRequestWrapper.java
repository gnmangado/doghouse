package com.jsonp.wrappers;

import grails.converters.JSON;
import net.sf.json.JSONNull;
import org.apache.commons.io.IOUtils;

import com.ml.exceptions.BadRequestException;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.net.URLDecoder;
import java.util.*;

/**
 * Limpia de parámetros propios de JSONP ap request
 *
 * @author nallegrotti
 *
 */
public class MultipleReadRequestWrapper extends HttpServletRequestWrapper {


    private Map<String, String[]> parameterMap = null;
    private ByteArrayOutputStream cachedBytes;
    private String myQueryString = null;
    private Object safeJSON = null;
    private boolean parsed = false;

    public MultipleReadRequestWrapper(HttpServletRequest request) throws IOException {

        super(request);
        cacheInputStream();
    }
    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (cachedBytes == null)
            cacheInputStream();

        return new CachedServletInputStream();
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream(), "UTF-8"));
    }

    private void cacheInputStream() throws IOException {
        /* Cache the inputstream in order to read it multiple times. For
         * convenience, I use apache.commons IOUtils
        */
        cachedBytes = new ByteArrayOutputStream();
        IOUtils.copy(checkForUtf8BOMAndDiscardIfAny(super.getInputStream()), cachedBytes);
    }


    private static InputStream checkForUtf8BOMAndDiscardIfAny(InputStream inputStream) throws IOException {
        PushbackInputStream pushbackInputStream = new PushbackInputStream(new BufferedInputStream(inputStream), 3);
        byte[] bom = new byte[3];
        if (pushbackInputStream.read(bom) != -1) {
            if (!(bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF)) {
                pushbackInputStream.unread(bom);
            }

        }
        return pushbackInputStream;
    }
    /* An inputstream which reads the cached request body */
    public class CachedServletInputStream extends ServletInputStream {
        private ByteArrayInputStream input;

        public CachedServletInputStream() {
            /* create a new input stream from the cached request body */
            input = new ByteArrayInputStream(cachedBytes.toByteArray());
        }

        @Override
        public int read() throws IOException {
            return input.read();
        }
    }


    @Override
    public String getQueryString() {
        if (myQueryString == null) {
            myQueryString = super.getQueryString();
        }
        return myQueryString;
    }
    public void setQueryString(String qs) {
        myQueryString = qs;
    }


    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Map<String, String[]> getParameterMap() {
        if (parameterMap  == null) {
            parameterMap = new HashMap(super.getParameterMap());
            parameterMap.remove("caller");

            String contentType = getContentType();
            if (contentType != null && contentType.contains("application/x-www-form-urlencoded") ){
                try {
                    addFormData();
                } catch (IllegalArgumentException e) {
                    throw new BadRequestException("Request body is not properly encoded as application/x-www-form-urlencoded type. Root cause: " + e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            fixMultipleParameter();
        }
        return Collections.unmodifiableMap(parameterMap);
    }

    private void fixMultipleParameter() {
        if (parameterMap != null) {
            for (String key: parameterMap.keySet()) {
                if (parameterMap.get(key).length > 1) {
                    String[] aux = {parameterMap.get(key)[0]};
                    parameterMap.put(key, aux);
                }
            }
        }

    }

    public void setParameter(String key, String[] value) {
        getParameterMap();
        if (parameterMap != null) {
            parameterMap.put(key, value);
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
        String[] strings = getParameterMap().get(name);
        if (strings != null)
        {
            return strings[0];
        }
        return super.getParameter(name);
    }


    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Enumeration getParameterNames() {
        Set ks = getParameterMap().keySet();
        return Collections.enumeration(ks);
    }

    public Object getSafeJSON() {
        if (safeJSON == null) {
            try {
                //try to parse it
                if (getInputStream().read() != -1) {
                    Object json = JSON.parse(this);
                    //transform json to avoid JSONNull
                    convertJsonNulltoPrimitiveNull(json);
                    safeJSON = json;
                }

            } catch (IOException e ) {

            }
        }
        return safeJSON;
    }




    public void setSafeJSON(Object safeJSON) {
        this.safeJSON = safeJSON;
    }

    static void convertJsonNulltoPrimitiveNull(Object object) {
        if (object instanceof Collection) {
            for (Object it: (Collection)object) {
                convertJsonNulltoPrimitiveNull(it);
            }

            if (object instanceof Map) {
                for (Map.Entry<Object, Object> tuple : ((Map<Object, Object>)(object)).entrySet())
                    if (tuple.getValue().getClass() == net.sf.json.JSONNull.class || JSONNull.getInstance().equals(tuple.getValue()) || org.codehaus.groovy.grails.web.json.JSONObject.NULL.equals(tuple.getValue())) {
                        tuple.setValue(null);
                    } else if (tuple.getValue() instanceof Map || tuple.getValue() instanceof Collection) {
                        convertJsonNulltoPrimitiveNull(tuple.getValue());
                    }
            }
        }


    }


    private void addFormData() throws IOException {
        BufferedReader reader = getReader() ;

        String line;
        while ((line = reader.readLine()) != null) {
            for (String pair : line.split("\\&")) {
                String[] kv = pair.split("=");
                String key = URLDecoder.decode(kv[0], "UTF-8");
                List<String> a = new ArrayList<String>();;
                if (parameterMap.containsKey(key))
                    a.addAll(Arrays.asList((String[]) (parameterMap.get(key))));
                String value = kv.length == 2 ? URLDecoder.decode(kv[1],"UTF-8"): "";
                a.add(value);
                String[] str = new String[a.size()];
                parameterMap.put(key, a.toArray(str));
            }
        }
    }

    public boolean isParsed() {
        return parsed;
    }

    public void setParsed() {
        this.parsed = true;
    }
}

