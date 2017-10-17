class MlGroovyDogGrailsPlugin {
    def version = "0.2"
    def grailsVersion = "1.3 > *"
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    def title = "Ml Groovy Dog Plugin"
    def author = "mlabarinas"
    def authorEmail = ""
    def description = '''\
                    Ml Groovy Dog Plugin
                    '''

    def documentation = "http://grails.org/plugin/ml-groovy-dog"

    def doWithWebDescriptor = { xml -> }

    def doWithSpring = { }

    def doWithDynamicMethods = { ctx -> }

    def doWithApplicationContext = { applicationContext -> }

    def onChange = { event -> }

    def onConfigChange = { event -> }

    def onShutdown = { event -> }

}