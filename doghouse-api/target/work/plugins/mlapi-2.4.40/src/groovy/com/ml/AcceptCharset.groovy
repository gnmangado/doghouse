package com.ml

class AcceptCharset {
    def supportedCharset = "utf-8"
    def process = { charsets ->
        if(!charsets) {
            return true
        }
        def charset = []
        try {
            charsets.tokenize(",").each {
                def csRange = it.tokenize("; q=")
                def cs = csRange.get(0).trim()
                def qvalue = 1
                if(csRange.size() > 1) {
                    qvalue = Double.parseDouble(csRange.get(1).trim())
                }
                charset << [type:cs, qvalue:qvalue]
            }
        } catch(Exception ex) {
            return false
        }
        def c = charset.find { it.type=="*" || it.type.toLowerCase()==supportedCharset }
        return c!=null;
    } 
}
