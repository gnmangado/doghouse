package com.ml

class AcceptMedia {
    def supportedMedias = [application:[json:[value:"json", priority:1]]]
    def defaultMedia = "application"
    def defaultSubmedia = "json"
    def defaultType = "json"
    def process = { accept ->
        if(!accept) {
            return defaultType
        }
        def c = [ compare: { a,b-> 
                def xMult = supportedMedias."${a.type.media}"?."${a.type.submedia}"?.priority
                if(!xMult) {
                    xMult = 0.1
                }
                def yMult = supportedMedias."${b.type.media}"?."${b.type.submedia}"?.priority
                if(!yMult) {
                    yMult = 0.1
                }
                def x = a.qvalue*xMult
                def y = b.qvalue*yMult
                

                def res = x.equals(y)? 0: x>y? -1: 1 
                
                res
            }
        ] as Comparator

        def medias = []
        accept.tokenize(",").each {
            def mediaRange = it.tokenize("; q=")
            def media = mediaRange.get(0).trim().tokenize("/")
            def qvalue = 1
            if(mediaRange.size() > 1) {
                try {
                    qvalue = Double.parseDouble(mediaRange.get(1).trim())
                } catch(Throwable e) {
                    return null
                }
            }
            if(media.size<2) {
                //this means that sumedia is not present. treat this is a malformed header
                return null
            }
            medias << [qvalue:qvalue, type:[media:media.get(0).trim(), submedia:media.get(1).trim()]]
        }
        medias = medias.sort(false, c)
        def selectedMedia = medias.find { 
            if(it.type.media == "*") {
                if(it.type.submedia == "*") {
                    it.type.media = defaultMedia
                    it.type.submedia = defaultSubmedia
                    return true
                }
                def foundMedia = supportedMedias.find { media, submedias ->
                    if(submedias[it.type.submedia]) {
                        return true
                    }
                    return false
                }
                if(foundMedia) {
                    it.type.media = foundMedia.key
                    return true
                }
                return false
            } else if(supportedMedias[it.type.media]) {
                if(it.type.submedia == "*") {
                    it.type.submedia = supportedMedias[it.type.media].values().iterator().next().value
                    return true 
                }
                if(supportedMedias[it.type.media][it.type.submedia]) {
                    return true
                }
                return false
            } else {
                return false
            }
            if(!supportedMedias[it.type.media]) {
                false
            } else if(!supportedMedias[it.type.media][it.type.submedia]) {
                false
            } else {
                true
            }
        }
        if(selectedMedia) {
            return supportedMedias[selectedMedia.type.media][selectedMedia.type.submedia]?.value
        }
        return null
    } 
}
