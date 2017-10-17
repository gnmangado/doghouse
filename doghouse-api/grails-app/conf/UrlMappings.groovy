class UrlMappings {

	static mappings = {
        "/api/v1/states"(controller: 'state') {
            action = [GET: 'find']
        }

//        "/api/v1/states"(controller: 'state') {
//            action = [POST: 'create']
//        }
//
//        "/api/v1/states/$id"(controller:'state') {
//            action = [GET: 'get']
//        }
//
//        "/api/v1/states/$id"(controller:'state') {
//            action = [PUT: 'update']
//        }

//        "/$controller/$action?/$id?(.$format)?"{
//            constraints {
//                // apply constraints here
//            }
//        }
//
        "/"(view:"/index")
//        "500"(view:'/error')
	}
}
