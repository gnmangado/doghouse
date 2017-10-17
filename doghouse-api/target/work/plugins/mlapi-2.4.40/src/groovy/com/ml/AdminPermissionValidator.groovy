package com.ml

import grails.util.Holders
import org.apache.log4j.Logger

class AdminPermissionValidator {
    private Logger log =  Logger.getLogger(this.getClass().getSimpleName());;

    static def validateAdmin = { scopes, request , config, controller, action->
		if(!scopes || !request || !controller || !action || controller == "error"){
			return false
		}
		def callerId = request.getHeader('X-Admin-Id')
		if(!callerId){
			return false
		}
		if(!((scopes.tokenize(',')).find {it == "admin"})){
			return false
		}
		
		def resourceConfig = config?."$controller"?."$action"?.resource
		def resource = (resourceConfig? resourceConfig : controller).toString()
		
		def isMocked = Holders?.config?.mlAPI?.isMockedPermissions
		if(isMocked){
			def hasPermission = Holders.config.mlAPI.mockedPermissions?."$callerId"?.contains(request.method+"_"+resource)
			return hasPermission?true:false //this is to return just true or false, not null
		}
		
		Long start = System.currentTimeMillis()
		def restClient = SpringUtils.getBean("mlapiRestClient")
		def respuesta
		Integer status
		restClient.get(
			uri:"/permissions/authorization_request?admin_id="+ callerId +"&resource=" + resource + "&http_method=" + request.method,
			success:{
				respuesta = it.data
				status =  it.status.getStatusCode()
			},
			failure: {
				def error
				if(it.exception){
					status = 503
					error = [code:503, message:"Service unavailable. ${it.exception.getMessage()}"]
				}else{
					status = it.status.getStatusCode()
					error = it.data.error
					if(it.data.stacktrace){
						log.error it.data.stacktrace
					}
				}
				respuesta = [error:error]
			},
		)
		if(!respuesta?.error){
			return respuesta.result == "OK"
		}else{
			return false
		}
	}
	
	static def filterGet = {request, apiResponse, config, controller, action ->
		def start = System.currentTimeMillis()
		def callerId = request.getHeader('X-Admin-Id')
		def restClient = SpringUtils.getBean("mlapiRestClient")
		def resourceConfig = config?."$controller"?."$action"?.resource
		def resource = (resourceConfig? resourceConfig : controller).toString()
		def respuesta
		def attrs = apiResponse.keySet() as List
		def body = [attrs:attrs]
		Integer status
		restClient.post(
			uri:"/permissions/authorization_get_filter?admin_id="+ callerId +"&resource=" + resource,
			data:body,
			success:{
				respuesta = it.data
				status =  it.status.getStatusCode()
			},
			failure: {
				def error
				if(it.exception){
					status = 503
					error = [code:503, message:"Service unavailable. ${it.exception.getMessage()}"]
				}else{
					status = it.status.getStatusCode()
					error = it.data.error
					if(it.data.stacktrace){
						log.error it.data.stacktrace
					}
				}
				respuesta = [error:error]
			}
		)
		if(status != 200){
			return [response:respuesta,status:status]
		}
		attrs.removeAll((respuesta.attrs as List))
		attrs.each{
			apiResponse.remove(it)
		}
		return [response:apiResponse,status:status]
	}
}
