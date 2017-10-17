import com.mercadolibre.opensource.frameworks.restclient.SimpleRestClient
import com.mercadolibre.opensource.frameworks.restclient.cache.local.SimpleLocalRestClientCacheProvider
import com.ml.DateConverter
import com.ml.exceptions.ForbiddenException
import com.ml.exceptions.NotModifiedException
import grails.plugins.i18n_gettext.T9nService
import grails.util.Holders
import org.codehaus.groovy.grails.orm.hibernate.support.ClosureEventTriggeringInterceptor as Events
import org.codehaus.groovy.grails.support.proxy.DefaultProxyHandler
import org.springframework.beans.BeanWrapperImpl
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver

class MlapiGrailsPlugin {
    def grailsApplication
    def version = "2.4.40"
    def grailsVersion = "2.3.0 > *"
    def dependsOn = [core:"2.3.0 > *", jodaTime:"1.4 > *"]
    def pluginExcludes = [
    'web-app/**/*',
    'grails-app/controllers/com/ml/test/**/*',
    'grails-app/domain/**/*',
    'grails-app/i18n/**/*',
    'grails-app/services/**/*',
    'grails-app/taglib/**/*',
    'grails-app/utils/**/*',
    'target/**/*',
    'test/**/*',
    'scripts/**/*',
    "grails-app/conf/CrowdinConf.groovy"
    ]
    def author = "Jonathan Leibiusky"
    def authorEmail = "ionathan@gmail.com"
    def title = "MercadoLibre API Grails Plugin"
    def description = ''
    def observe = ["controllers"]

    def doWithWebDescriptor = { xml ->
        def authConfig =  Holders.config.'publicApi'.'auth'

        def servletDescriptor = xml.servlet.find { node -> node.'servlet-name'.text() == "grails" && node.'servlet-class'.text() == "org.codehaus.groovy.grails.web.servlet.GrailsDispatcherServlet" }

        def optionsNode = servletDescriptor.appendNode("init-param")
        if (optionsNode) {
            optionsNode.appendNode("param-name","dispatchOptionsRequest")
            optionsNode.appendNode("param-value","true")

            def traceNode = servletDescriptor.appendNode("init-param")
            traceNode.appendNode("param-name","dispatchTraceRequest")
            traceNode.appendNode("param-value","true")
            } else {
                servletDescriptor.appendNode {
                    'init-param' {
                        'param-name'('dispatchOptionsRequest')
                        'param-value'('true')
                    }
                    'init-param' {
                        'param-name'('dispatchTraceRequest')
                        'param-value'('true')
                    }
                }
            }


        //servletDescriptor.'init-param'
        def filters = xml.'filter'
        filters[-1] + {
            'filter' {
                'filter-name'('jp2j')
                'filter-class'('com.jsonp.filters.JsonpFilter')
                'init-param' {
                    'param-name'('headerSwitch')
                    'param-value'('X-Public:true')
                }
            }
            'filter' {
                'filter-name'('public2private')
                'filter-class'('com.ml.public2private.filters.Public2PrivateFilter')
                'init-param' {
                    'param-name'('headerSwitch')
                    'param-value'('X-Public:true')
                }
                'init-param' {
                    'param-name'('authDomain')
                    'param-value'(authConfig.domain)
                }
                'init-param' {
                    'param-name'('userValidate')
                    'param-value'(Holders.config.userValidate)
                }
                'init-param' {
                    'param-name'('ignoreTransaction')
                    'param-value'(Holders.config.ignoreTransaction)
                }
                'init-param' {
                    'param-name'('appName')
                    'param-value'(Holders.grailsApplication.getMetadata().getApplicationName().toString())
                }
                'init-param' {
                    'param-name'('allowTestTokens')
                    'param-value'(Holders.config.allowTestTokens?:false)
                }

            }
        }
        def filterMappings = xml.'filter'
        filterMappings[-1] + {
            'filter-mapping' {
                'filter-name'('jp2j')
                'url-pattern'('/*')
            }
            'filter-mapping' {
                'filter-name'('public2private')
                'url-pattern'('/*')
            }
        }
    }

    def doWithSpring = { applicationContext ->
        beans = {

            localeResolver(AcceptHeaderLocaleResolver) {
                defaultLocale = new java.util.Locale('en');
            }

            t9nService(T9nService) {
                localeResolver = ref("localeResolver")
            }
        }

        //permissions http&cache beans
        String baseurl = Holders.config?.mlAPI?.permissionsHost

        mlapiRestClient(SimpleRestClient){ bean ->
            name= "mlapi Rest client"
            baseUrl=baseurl
            bean.initMethod= "init"
            bean.scope="singleton"
            soTimeout=2000
            caches = ["/permissions/.*": ref("mlapiPermissionsCache")]
        }

        mlapiPermissionsCache(SimpleLocalRestClientCacheProvider){ bean ->
            bean.initMethod= "init"
            bean.destroyMethod= "destroy"
            bean.scope="singleton"
            name="mlapiPermissionsCache"
            concurrencyLevel= 100
            initialCapacity= 10000
            purgeSleep= 100
            purgeBlockSize= 100
            loadFactor= 0.8
        }
    }

    def doWithDynamicMethods = { ctx ->
        application.controllerClasses.each controllerClosure
        application.domainClasses.each domainClosure
    }

    def doWithApplicationContext = { applicationContext ->
    }

    def onChange = { event ->
        application.controllerClasses.each controllerClosure
    }

    def onConfigChange = { event ->
    }
    def excludedProps = [Events.ONLOAD_EVENT,
    Events.BEFORE_DELETE_EVENT, Events.AFTER_DELETE_EVENT,
    Events.BEFORE_INSERT_EVENT, Events.AFTER_INSERT_EVENT,
    Events.BEFORE_UPDATE_EVENT, Events.AFTER_UPDATE_EVENT,

    "onLoad",
    "beforeDelete", "afterDelete",
    "beforeInsert", "afterInsert",
    "beforeUpdate", "afterUpdate"]

    def controllerClosure = { controllerClass ->
        controllerClass.metaClass.setLastModified = { d ->
            def lastModified = DateConverter.rfc1123format(d)
            response.setHeader("Last-Modified", lastModified)
            def modifiedSinceHeader = request.getHeader("If-Modified-Since")
            if(modifiedSinceHeader) {
                def modifiedSince = DateConverter.rfc1123parse(modifiedSinceHeader)
                if(d.before(modifiedSince) || lastModified == modifiedSinceHeader) {
                    throw new NotModifiedException()
                }
            }
        }
        controllerClass.metaClass.expiresOn = { d ->
            int seconds = (d.getTime()-(new Date()).getTime())/1000
            response.setHeader("Cache-Control", "max-age=${seconds}, stale-while-revalidate=${seconds/2}, stale-if-error=${seconds*2}")
            request.mlSetExpiration = true
        }
        controllerClass.metaClass.expiresIn = { seconds ->
            seconds = Long.parseLong(seconds.toString())
            response.setHeader("Cache-Control", "max-age=${seconds}, stale-while-revalidate=${seconds/2}, stale-if-error=${seconds*2}")
            request.mlSetExpiration = true
        }
        controllerClass.metaClass.getCaller = { ->
            def callerId = request.getHeader('X-Caller-Id')
            if(callerId == null) {
                callerId = params.'caller.id'
            }
            return callerId
        }
        controllerClass.metaClass.getCallerScopes = { ->
            def scopes = request.getHeader("X-Caller-Scopes")
            if(!scopes) {
                scopes = params?.caller?.scopes
            }
            return scopes?scopes.tokenize(","):[]
        }
        controllerClass.metaClass.getCallerStatus = { ->
            def callerStatus = request.getHeader('X-Caller-Status')
            if(callerStatus == null) {
                callerStatus = params.'caller.status'
            }
            return callerStatus
        }
        controllerClass.metaClass.assertCaller = { id ->
            if(isCallerAdmin()){ //JGYONZO Esto es para impersonar
                return
            }
            def callerId = getCaller()
            if(callerId == null || !callerId.toString().equals(id.toString())) {
                throw new ForbiddenException("The caller is not authorized to access this resource");
            }
        }
        controllerClass.metaClass.assertCallerActive = { ->
            def callerStatus = getCallerStatus()
            if(callerStatus == null || !callerStatus.toString().equals("ACTIVE")) {
                throw new ForbiddenException("The caller is not authorized to access this resource");
            }
        }
        controllerClass.metaClass.acceptCollectionParams = { ->
            request.'mlAcceptedParameters' << "ids".toString()
            request.'mlAcceptedParameters' << "attributes".toString()
        }
        controllerClass.metaClass.acceptAllParams = { ->
            request.'mlAcceptedParameters' = null
        }
        //checks if the admin-id has admin permissions
        controllerClass.metaClass.assertCallerAdmin = { ->
            if (!isCallerAdmin()){
                throw new ForbiddenException("The caller is not authorized to access this resource");
            }
        }
        //returns true if the admin-id has admin permissions
        controllerClass.metaClass.isCallerAdmin = { ->
            if ( request?.getHeader('X-Public')?.equalsIgnoreCase("true") ){
                return params?.get("caller.admin") //ESto no es un param real por lo que está bien tomarlo con .
                } else {
                    return request?.getHeader("X-Caller-Scopes")?.contains("admin") || params?.get("caller")?.get("scopes")?.contains("admin")
                }
            }
        //returns the admin-id
        controllerClass.metaClass.getAdminId = { ->
            def callerId = request.getHeader('X-Admin-Id')
            if ( callerId == null && isCallerAdmin() ){
                callerId = "internal"
            }
            return callerId
        }
        //checks if the admin-id is the owner of the resource
        controllerClass.metaClass.assertAdminOwner = { id ->
            def callerId = getAdminId()
            if(callerId == null || !callerId.toString().equals(id.toString())) {
                throw new ForbiddenException("The caller is not authorized to access this resource");
            }
        }
        //checks if the admin-id has admin permmissions or is owner of the resource
        controllerClass.metaClass.assertCallerAdminOrOwner = { id ->
            if(!isCallerAdmin()){
                assertAdminOwner(id)
            }
        }

        controllerClass.metaClass.getClientId = { ->
            try {
                (params?.get("client.id") ?: request.getHeader('X-Client-Id')) as Long
                } catch (e){
                    return null
                }
        }

        controllerClass.metaClass.isInternal = { ->
            if ( request?.getHeader('X-Public')?.equalsIgnoreCase("true") )
                return false
            else
                return true
        }
        controllerClass.metaClass.isTestToken = { ->
            if ( request?.getHeader('X-Test-Token')?.equalsIgnoreCase("true") )
                return true
            else
                return false
        }
        controllerClass.metaClass.errorResponse = { Exception e ->
            request.exception = e
            ////////if (controllerClass.metaClass.respondsTo())
            forward(controller: 'error', action: 'treatExceptions')
        }
    }

    def domainClosure = { domainClass ->
        def getTypeMap = { clazz ->
            def properties = clazz.getPersistentProperties();
            def typeMap = [:]
            properties.each {
                typeMap[it.name]=it.type
            }
            return typeMap
        }
        domainClass.metaClass.typeMap = getTypeMap(domainClass)
        domainClass.metaClass.fill = { parameters ->
            def newParams = [:]
            def params = [:]
            if(parameters) {
                params = parameters
                } else {
                    def request =  org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes().getNativeRequest()
                    if(request.accepted_media == 'json') {
                        params = request.JSON
                    }
                }
                params.each { prop, value ->
                    def chunks = prop.tokenize("_")
                    def field = ""
                    if(chunks.size()>1) {
                        field = chunks[0]
                        chunks[1..chunks.size()-1].each { word ->
                            field += word[0].toUpperCase()+word[1..word.size()-1]
                        }
                        } else {
                            field = prop
                        }
                        newParams[field] = value
                    }
                    delegate.properties = newParams
                }
                domainClass.metaClass.part= { m ->
                    def map= [:]
                    if(m.'include') {
                        m.'include'.each{
                            map[it]= delegate."${it}"
                        }
                        } else if(m.'except') {
                            m.'except'.addAll excludedProps
                            def props= domainClass.persistentProperties.findAll {
                                !(it.name in m.'except')
                            }
                            if(delegate.id) {
                                map['id']=delegate.id
                            }
                            def proxyHandler = new DefaultProxyHandler()
                            def value = proxyHandler.unwrapIfProxy(delegate)
                            def beanWrapper = new BeanWrapperImpl(value);
                            props.each {
                                Object val = beanWrapper.getPropertyValue(it.name);
                                map[it.name]= val
                            }
                        }
                        return map
                    }
                }
}
