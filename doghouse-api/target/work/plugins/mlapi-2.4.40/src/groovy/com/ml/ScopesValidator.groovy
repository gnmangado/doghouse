package com.ml

import com.newrelic.api.agent.NewRelic

class ScopesValidator {
	
	//static def defaultExcludedResources =  [api:[ echo:[], echoPost:[], echoPut:[], echoDelete:[], echoHeaders:[], echoParams:[], queryString:[] ]]
	
	static def isExcludedFromScopeValidation = { excludedResources, controller, action ->
		def cobject = excludedResources?."$controller"
        return cobject in List && cobject.contains(action)
	}

	static def validateScopes = { paramScopes, config, controller, action ->
		def configScopes = config?."$controller"?."$action"?.scopes
		if(!configScopes || configScopes.size() == 0) return true
		def requestScopes = paramScopes?paramScopes.tokenize(","):null
		if(!requestScopes || requestScopes.size() == 0) return true
		for (def scope in configScopes) {
            if(requestScopes.contains(scope)) {
                return true
            }
        }
        return false
    }

	static def validateExternalScopes = { paramScopes, excludedResources, controller, action, xPublicValue, requestMethod, config ->
		if (requestMethod == "OPTIONS") return true

        def ret = true
        //check if action is excluded
		if( !ScopesValidator.isExcludedFromScopeValidation(excludedResources, controller, action) &&
			// Check for scopes
			(xPublicValue != null && xPublicValue == "true") ){

            //is a public request
            def requestScopes = paramScopes ? paramScopes.tokenize(",") : null
            //if no scopes , then return false
            if(!requestScopes || requestScopes.size() == 0) {
                ret = false
            } else {
                if (requestMethod in ["POST", "PUT", "DELETE"]) {
                    //request write or admin scope for writting
                    ret = requestScopes.any { scope -> scope == "admin" || scope.contains("write") }
                } else {
                    //request admin or read scope for reading
                    ret = requestScopes.any { scope -> scope == "admin" || scope.contains("read") }
                }
            }
        }

        if (!ret) NewRelic.incrementCounter("mlAPI/externalScopesFail/${grails.util.Metadata.current.'app.name'}-${controller}-${action}")
		if (config.mlapi.validateExternalScopes)
            return ret
        else
            return true;
	}
}
