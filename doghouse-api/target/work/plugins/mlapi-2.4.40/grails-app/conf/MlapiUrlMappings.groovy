class MlapiUrlMappings {
	static excludes = ["/plugins/*", "/js/*", "/css/*", "/images/*", "/restclient*"]
	static mappings = {
        "/ping"{
            controller = "apistatus"
            action = [GET:"ping"]
        }
        "/**"(controller:'error', action:'notFound') 
	}
}
