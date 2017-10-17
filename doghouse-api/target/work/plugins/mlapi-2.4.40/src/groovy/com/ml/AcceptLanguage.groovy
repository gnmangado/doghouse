package com.ml

class AcceptLanguage {
    def supportedLocales = ["en", "es", "pt"]
    def defaultLocale = "en"
    def process = { accept ->
        if(!accept) {
            return new Locale(defaultLocale)
        }
        def c = [ compare: { x,y-> 
                def res = x.qvalue.equals(y.qvalue)? 0: x.qvalue>y.qvalue? -1: 1 
                
                res
            }
        ] as Comparator

        def locales = []
        accept.tokenize(",").each {
            def locale = it.tokenize("; q=")
            def qvalue = 1
            def generic = locale.get(0).tokenize("-")
            if(locale.size() > 1) {
                try {
                    qvalue = Double.parseDouble(locale.get(1).trim())
                } catch(Throwable e) {
                    return null
                }
            }
            locales << [qvalue:qvalue, name:generic.get(0)]
        }
        locales = locales.sort(c)
        def selectedLocale = locales.find { 
            if (it.name == "en" || it.name == "es" || it.name == "pt") {
                return true;
            }
            if(it.name == "*") {
                it.name = defaultLocale
                return true;
            }
        }
        if(selectedLocale) {
            return new Locale(selectedLocale.name)
        }
        return null
    } 
}
