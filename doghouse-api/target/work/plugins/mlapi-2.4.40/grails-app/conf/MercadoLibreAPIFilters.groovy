import com.jsonp.wrappers.MultipleReadRequestWrapper
import com.ml.AcceptCharset
import com.ml.AcceptMedia
import com.ml.AdminPermissionValidator
import com.ml.ObjectCloner
import com.ml.ScopesValidator
import com.ml.exceptions.UnauthorizedException
import com.ml.exceptions.UnauthorizedScopesException
import grails.converters.JSON
import grails.util.Holders
import org.apache.catalina.connector.RequestFacade
import org.apache.commons.lang.exception.ExceptionUtils
import org.apache.log4j.Logger
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.codehaus.jackson.map.ObjectMapper

import java.security.MessageDigest

class MercadoLibreAPIFilters {
    private final static safeParameters = ["caller.id".toString(), "caller.status".toString(), "caller.scopes".toString(), "client.id".toString(), "caller.admin".toString(),"admin.id".toString()]
    def groovyPagesTemplateEngine
    def apiTemplate
    def grailsApplication
    private Logger log =  Logger.getLogger(this.getClass().getSimpleName());;

    def md5(content) {
        def startTime = System.currentTimeMillis()
        def algorithm = MessageDigest.getInstance("MD5")
        algorithm.update(content.getBytes())
        def messageDigest = algorithm.digest()
        def sb = new StringBuilder()
        for (int i = 0; i < messageDigest.length; i++) {
            def hex = Integer.toHexString(0xff & messageDigest[i])
            if(hex.length()==1) sb.append('0')
            sb.append(hex)
        }
        def out = sb.toString()

        def diff = System.currentTimeMillis() - startTime
        log.debug "md5 was processed in $diff ms"

        return out
    }
    def toCamelCase(param) {
        def chunks = param.tokenize("_")
        def field = ""
        if(chunks.size()>1) {
            field = chunks[0]
            chunks[1..chunks.size()-1].each { word ->
                field += word[0].toUpperCase()+word[1..word.size()-1]
            }
        } else {
            field = param
        }
        return field
    }
    def useTemplate() {
        if(!apiTemplate) {
            def pluginPath = Holders.pluginManager.getPluginPath("mlapi")
            def viewsPath = pluginPath + '/' + GrailsResourceUtils.GRAILS_APP_DIR + "/views";
            def uri = viewsPath + '/apiHtml.gsp'
            def r = groovyPagesTemplateEngine.getResourceForUri(uri)
            apiTemplate = groovyPagesTemplateEngine.createTemplate(r, true)
        }
        return apiTemplate
    }

    def toJson(data) {
        try {
            return data as grails.converters.JSON
        } catch (ConcurrentModificationException e) {
            return ObjectCloner.instance.clone(data) as grails.converters.JSON
        }
    }

    def getAttrs (node, prefix=null) {
        def attrList = [];
        if (node instanceof Map && node.attributes instanceof Map) {
            node.attributes.each {key, nestedNode ->
                def keyPrefixed = prefix ? [prefix, key].join(".") : key;

                attrList += keyPrefixed;
                attrList += getAttrs(nestedNode, keyPrefixed);
            };
        }

        return attrList;
    }

    def attrsTree (attrs) {
        def map = [:]

        attrs.each { attr ->
            attr = attr.split ("\\.");

            map[attr[0]] = map[attr[0]] ? map[attr[0]] : [:];

            if (attr.size() > 1) {
                map[attr[0]] << attrsTree([attr[1..-1].join(".")]);
            } else {
                map[attr[0]] = null;
            }
        }

        return map;
    }

    def filterAttrs (attrs, data) {
        def originalData = data;

        if(data instanceof Map){
            data = new net.sf.json.JSONObject()

            attrs.keySet().each {attr ->

                if (originalData.containsKey(attr)) {
                    if (attrs[attr]) {
                        def nested = filterAttrs(attrs[attr], originalData[attr]);
                        
                        if (nested instanceof Collection) {
                            data[attr] = nested;
                        } else {
                            if (!data[attr]) {
                                data[attr] = new net.sf.json.JSONObject();
                            }
                            data[attr] << nested;
                        }
                    } else {
                        data[attr] = originalData[attr]
                    }
                } 
                else {
                    def realAttr = toCamelCase(attr)
                    if (originalData.containsKey(realAttr)) {
                        if (attrs[attr]) {
                            def nested = filterAttrs(attrs[attr], originalData[realAttr]);

                            if (nested instanceof Collection) {
                                data[attr] = nested;
                            } else {
                                if (!data[attr]) {
                                    data[attr] = new net.sf.json.JSONObject();
                                }
                                data[attr] << nested;
                            }
                        } else {
                            data[attr] = originalData[realAttr]
                        }
                    }
                }
            }
        }else if (data instanceof Collection) {
            data = new net.sf.json.JSONArray()
            originalData.each { item ->
                data.add(filterAttrs(attrs, item));
            }
        }

        return data;
    }

    def filters = {
        renderType(controller:'*', action:'*') {
            before = {
                response.setHeader('X-MLAPI-Version', grailsApplication.metadata.'plugins.mlapi')
                if(request.method == "TRACE") {
                    return false
                }
                request.mlStart = Calendar.getInstance().getTimeInMillis()

                def baseRequest = request
                while (baseRequest && !(baseRequest in MultipleReadRequestWrapper) && !(baseRequest in RequestFacade)) {
                    baseRequest = baseRequest.getRequest()
                }
                if (baseRequest in RequestFacade)
                    baseRequest = request

                //validate Accept-Charset
                def acs = new AcceptCharset()
                def accepted = acs.process(request.getHeader("Accept-Charset"))
                if(!accepted) {
                    render status:406, text:"Charset is not supported", contentType:"text/plain", encoding:"UTF-8"
                    return false
                }

                //validate payload if application/json
                def ct = request.getHeader("Content-Type")
                def tempStatus = -1
                try {
                    tempStatus = response?.status
                } catch (Exception e) {

                }
                if (
                        baseRequest in MultipleReadRequestWrapper && //I found my wrapper
                        controllerName != "error" && //not error controller
                        tempStatus < 400 && //no error status
                        !baseRequest?.isParsed()) { //not already parsed
                    if ( ct?.toLowerCase()?.contains("json")) {

                        try {
                            if (request.getInputStream().read() != -1) {
                                request.setParsed() //mark request as parsed to avoid multiple attempts

                                def jsonMap = [data: request.getJSON()]
                                //transform json to avoid JSONNull
                                JsonNullParser.convertJsonNulltoPrimitiveNull(jsonMap)
                                request.safeJSON = jsonMap.data
                            }
                        } catch (ConverterException e) {
                            request.exception = new com.ml.exceptions.BadRequestException("Bad JSON format -> ${e.message} (${ExceptionUtils.getRootCause(e).message})".toString())
                            forward(controller: 'error', action: 'treatExceptions')
                            return false
                        } catch (java.io.UnsupportedEncodingException e) {
                            log.error("Wrong encoding on message: ${request.headerNames.collect{["$it":request.getHeaders(it)]}} ${request.reader.text} ${request.queryString}")
                            request.safeJSON = null
                        }
                    }

                }

                //validate Accept-Encoding
                def enc = request.getHeader("Accept-Encoding")
                def valid = request.getHeader("Accept-Encoding")?.tokenize(",").find {
                    def encoding = it.tokenize(";").get(0).trim()
                    encoding=="gzip" || encoding=="*" || encoding=="deflate" || encoding=="identity"
                }
                if(enc!= null && !valid) {
                    render status:406, text:"Encoding is not supported", contentType:"text/plain", encoding:"UTF-8"
                    return false
                }

                //validate Accept
                def am = new AcceptMedia()
                def type = am.process(request.getHeader("Accept"))
                if(!type) {
                    render status:406, text:"Media is not supported", contentType:"text/plain", encoding:"UTF-8"
                    return false
                }
                if(!request.mlOriginController) {
                    request.mlOriginController = params.controller
                }
                if(!request.mlOriginAction) {
                    request.mlOriginAction = actionName
                }
                request.'accepted_media' = type
                //validate Scopes
                def scopes = request.getHeader("X-Caller-Scopes")
                if(!scopes) {
                    scopes = params?.caller?.scopes
                }

                //-->JYONZO: modifs para validacion de acceso de admins
                boolean isAdmin = AdminPermissionValidator.validateAdmin(scopes, request, grailsApplication?.config?.mlAPI?.resources, controllerName, actionName)
                params["caller.admin"] =  isAdmin
                params.caller?.admin = isAdmin

                //(borrar) Esto es mas que nada para apis jersey q usan capa grails y pasan el querystring de una
                try{
                    if (baseRequest in MultipleReadRequestWrapper) {
                        if (request.queryString && request.queryString != "" && request.queryString != "null") {
                            if (request.queryString.contains("caller.admin")) {
                                baseRequest.setQueryString(request.queryString.replaceAll("caller.admin=true", "caller.admin=" + params?.get("caller.admin")))
                            } else {
                                baseRequest.setQueryString(request.queryString + ("&caller.admin=" + params?.get("caller.admin")))
                            }
                        } else {
                            baseRequest.setQueryString("caller.admin=" + params?.get("caller.admin"))
                        }
                        baseRequest.setParameter("caller.admin", [params?.get("caller.admin").toString()] as String[]);
                    }
                }catch(Exception e){

                    log.error("Error setting querystring ${e.message}, the request is MR? ${request in MultipleReadRequestWrapper} ${request.class.getSimpleName()}")
                }
                //<--JYONZO: modifs para validacion de acceso de admins

                // Validate external request
                def xPublicValue = request.getHeader("X-Public")

                if(!ScopesValidator.validateExternalScopes(scopes, grailsApplication?.config?.mlAPI?.excludedResources, controllerName, actionName, xPublicValue, request.method, grailsApplication?.config)
                        && !request.exception){

                    request.exception = new UnauthorizedScopesException("Unauthorized external scopes","unauthorized_external_scopes")
                    forward(controller: 'error', action: 'treatExceptions')
                    return false
                }
                if(!ScopesValidator.validateScopes(scopes, grailsApplication?.config?.mlAPI?.scopes, controllerName, actionName)
                        && !request.exception){
                    request.exception = new UnauthorizedException("Unauthorized scopes","unauthorized_scopes")
                    forward(controller: 'error', action: 'treatExceptions')
                    return false
                }
                if(request.method != "OPTIONS"){
                    if(request.queryString) {
                        request.'mlParams' = request.queryString.split("&").collect { "${it.split("=")[0]}".toString() }
                    } else {
                        request.'mlParams' = []
                    }
                    request.'mlAcceptedParameters' = [] + safeParameters
                }

                /* > Get API Doc > */

                // Doc cache
                if (!servletContext.doc) {
                    servletContext.doc = [:];
                }

                def resourceInfo = servletContext.doc?."${request.mlOriginController}";

                if (resourceInfo == null) {
                    def docPath = servletContext.getRealPath("")+"/doc";

                    String docText = null;

                    File jsonOptionsFile = new File("${docPath}/${request.mlOriginController}.json");
                    if (jsonOptionsFile.exists()) {
                        docText = jsonOptionsFile.text;
                    }

                    if (!docText && grailsApplication.config.mlAPI."${request.mlOriginController}") {
                        String path = request.contextPath == "/" ? request.forwardURI : request.forwardURI - request.contextPath
                        def list = grailsApplication.config.mlapi?.evict ?: []
                        def evict = list.any { path?.matches("${it}.*") } && controllerName != "error"

                        JSON.use( evict ? 'noModify' : 'underscore') {
                            docText = (grailsApplication.config.mlAPI."${request.mlOriginController}" as grails.converters.JSON).toString();
                        }
                    }

                    if (docText) {
                        ObjectMapper mapper = new ObjectMapper();

                        docText = docText.replaceAll ("\"null\":", "\"null (null value)\":");

                        try {
                            def jsonOptionsConfig = mapper.readValue(docText, net.sf.json.JSONObject.class);
                            resourceInfo = jsonOptionsConfig;
                        } catch (org.codehaus.jackson.JsonParseException pe) {
                            log.error("Error parsing doc file for ${request.mlOriginController} - ${pe.message}", pe);
                        } catch (org.codehaus.jackson.map.JsonMappingException me) {
                            log.error("Error mapping doc file for ${request.mlOriginController} - ${me.message}", me);
                        } catch (Exception ue) {
                            log.error("Error unknown processing doc file for ${request.mlOriginController} - ${ue.message}", ue);
                        }
                    }

                    // No hay doc. Map vacio
                    if (!resourceInfo) {
                        resourceInfo = [:];
                    }

                    servletContext.doc."${request.mlOriginController}" = resourceInfo;
                }
                /* < Get API Doc < */

                request.mlAttributes = null
                if(params.attributes?.length() > 0) {
                    request.mlAttributes = params.attributes.split(',')*.trim();
                }
                if(request.mlAttributes?.size() > 0) {
                    if (!servletContext.allowedAttrs) {
                        servletContext.allowedAttrs = [:];
                    }
                    if (!servletContext.allowedAttrs."${request.mlOriginController}") {
                        servletContext.allowedAttrs."${request.mlOriginController}" = getAttrs(servletContext.doc."${request.mlOriginController}")
                    }

                    def allowedAttrs = servletContext.allowedAttrs."${request.mlOriginController}"

                    def searchable = grailsApplication.config.mlAPI.config?."${request.mlOriginController}"?."${request.mlOriginAction}"?.searchable;

                    request.mlAttributes.each {a ->
                        if(!allowedAttrs.contains (a) && (!searchable || !allowedAttrs.contains("results."+a))) {
                            // remove attributes since exception will be forwarded and parsed again
                            params.attributes = ""
                            request.exception = new com.ml.exceptions.BadRequestException("Attribute ${a} is not valid".toString())
                            forward(controller: 'error', action: 'treatExceptions')
                            return false
                        }
                        grailsApplication.config.mlAPI
                    }

                    if (searchable) {
                        request.mlAttributes = request.mlAttributes.collect {a ->
                            "results."+a;
                        }
                        request.mlAttributes << "paging";
                    }

                    request.mlAttributes = request.mlAttributes.sort();
                }
            }
            after = {


                // mlapi.evict is an optional config that allows the user to evict some URIs to be processed by the plugin
                // e.g.: mlapi.evict = ['/console','/config']
                String path = request.contextPath == "/" ? request.forwardURI : request.forwardURI - request.contextPath
                def list = grailsApplication.config.mlapi?.evict ?: []
                def evict = list.any { path?.matches("${it}.*") } && controllerName != "error"

                def renderers = [:]
                renderers["json"] = { data ->
                    if(data instanceof net.sf.json.JSON){
                        return [body:data, media:"application/json"]
                    }

                    def ret
                    JSON.use( evict ? 'noModify' : 'underscore') {
                        data = toJson(data)

                        ObjectMapper mapper = new ObjectMapper();

                        // Maintain root null values
                        data = "{\"data\": ${data.toString()}}"

                        data = mapper.readValue(data, net.sf.json.JSONObject.class).data;

                        ret = [body:data, media:"application/json"]
                    }

                    return ret
                }

                if(request.'accepted_media' && it != null && !evict && !path?.startsWith("/ping")) {
                    def resp = null
                    def e = request.exception?.cause?.class

                    if(request.method == "OPTIONS") {

                        def resourceInfo = servletContext.doc."${request.mlOriginController}"
                        def allowedMethods = []
                        if(resourceInfo.methods in Collection) {
                            allowedMethods += resourceInfo.methods.collectNested { it.method }
                        }
                        if(resourceInfo.actions in Collection) {
                            allowedMethods += resourceInfo.actions.collectNested { it.method }
                        }
                        allowedMethods << "HEAD"
                        allowedMethods << "OPTIONS"
                        response.setHeader('Allow',(allowedMethods as Set).join(", "))
                        response.setHeader('Cache-Control', "max-age=3600")

                        // CORS for desa
                        // response.setHeader("Access-Control-Allow-Origin", "*");
                        // response.setHeader("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS");

                        request.mlSetExpiration = true
                        it = [response:resourceInfo, status:200]
                    }
                    resp = it?.response

                    //somehow it breaks JSON converter
                    if (resp in org.codehaus.groovy.grails.web.sitemesh.GrailsContentBufferingResponse)
                        resp = null
                    def renderer = renderers[request.'accepted_media'](resp)
                    resp = renderer.body

                    /*if(request.mlAttributes?.size() > 0 && (resp in Collection || resp in Map)) {
                       resp = filterAttrs (attrsTree(request.mlAttributes), resp);
                   } */

                    if(request.mlAttributes?.size() > 0 && (resp in Collection || resp in Map) && controllerName != "error" && response.status < 400) {
                        try {
                            resp = filterAttrs(attrsTree(request.mlAttributes), resp);
                        } catch(Exception ee) {
                            request.mlAttributes = null
                            params.attributes  =null
                            if (request.exception)
                                throw  request.exception
                            else
                                throw ee
                        }
                    }

                    if(renderer) {
                        if(resp != null && request.method != "HEAD") {
                            def body = resp.toString()
                            def currentEtag = md5(body)
                            response.setHeader('Vary', 'Accept,Accept-Encoding')
                            if(!request.mlSetExpiration) {
                                response.setHeader('Cache-Control', "max-age=0")
                            }

                            if(it.status && it.status < 200 || it.status > 210) {
                                render text:body, contentType:renderer.'media', encoding:"UTF-8", status:it.status
                            } else {
                                response.setHeader('ETag', currentEtag)
                                def etag = request.getHeader("If-None-Match")
                                if(etag && etag == currentEtag) {
                                    render contentType:renderer.'media', encoding:"UTF-8", status:304
                                } else {
                                    render text:body, contentType:renderer.'media', encoding:"UTF-8", status:(it.status?it.status:200)
                                }
                            }
                        } else {
                            render contentType:renderer.'media', encoding:"UTF-8", status:(it.status?it.status:200)
                        }
                        return false
                    }
                }
                //TODO: add more medias
            }
        }
        ping(uri:'/ping') {
            before = {
                if(!grailsApplication?.config?.mlAPI?.newRelic?.allowPing){
                    request.setAttribute("com.newrelic.agent.IGNORE", true);
                }
            }
            after = {
                response.setHeader('Cache-Control', "max-age=0")
                if (request.exception)
                    render text: request.exception, status:500, contentType:'text/plain', encoding:'UTF-8'
                else
                    render text:"pong", status:200, contentType:'text/plain', encoding:'UTF-8'
                return false
            }
        }
    }

    class JsonNullParser {

        static void convertJsonNulltoPrimitiveNull(object) {
            if (object in Collection) {
                object.each {
                    convertJsonNulltoPrimitiveNull(it)
                }
            }

            if (object in Map) {
                for (tuple in object) {
                    if (tuple.value.getClass() == net.sf.json.JSONNull || net.sf.json.JSONNull.instance.equals(tuple.value) || org.codehaus.groovy.grails.web.json.JSONObject.NULL.equals(tuple.value) ) {
                        tuple.value = null
                    } else if(tuple.value in Map || tuple.value in Collection) {
                        convertJsonNulltoPrimitiveNull(tuple.value)
                    }
                }
            }


        }

    }

}
