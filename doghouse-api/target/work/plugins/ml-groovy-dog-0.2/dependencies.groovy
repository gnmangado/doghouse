grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"

grails.project.dependency.distribution = {
     remoteRepository(id:"MLGrailsPlugins", url:"http://git.ml.com:8081/nexus/content/repositories/MLGrailsPlugins") {
       authentication username: "mlGrailsPluginDeployer", password:"123456"
    }
}

grails.project.dependency.resolution = {
    inherits("global") { }
    log "warn"
    
    repositories {
        grailsPlugins()
        grailsHome()
        grailsCentral()

        mavenLocal()
        mavenCentral()

        mavenRepo "http://git.ml.com:8081/nexus/content/groups/Arquitectura"
    }
    
    dependencies {
        compile("javadog:javadog:0.0.10")
    }

    plugins {
        build(":tomcat:$grailsVersion",
              ":release:2.2.0",
              ":rest-client-builder:1.0.3") {
            export = false
        }
    }

}