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
        mavenRepo "http://git.ml.com:8081/nexus/content/repositories/MLGrailsPlugins"
	    mavenRepo "http://repo.grails.org/grails/plugins/"
        mavenRepo "http://repo.grails.org"
    }
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.
        compile ("com.mercadolibre.opensource.frameworks:restclient:1.1.4"){excludes 'groovy'}
        // runtime 'mysql:mysql-connector-java:5.1.5'
        test ("org.gebish:geb-spock:0.9.3")    {
            export = false
        }

        compile('org.codehaus.jackson:jackson-mapper-asl:1.9.13')
       
        compile ('com.newrelic.agent.java:newrelic-api:3.1.1') { 
            export = true 
        }
        compile ('org.springframework:spring-expression:4.0.5.RELEASE')
        compile ('org.springframework:spring-core:4.0.5.RELEASE') {
            export = true
        }
        compile ('org.springframework:spring-beans:4.0.5.RELEASE') {
            export = true
        }
        compile 'org.springframework:spring-aop:4.0.5.RELEASE'

        test 'org.codehaus.groovy:ast-transformation:0.1'

        compile 'net.sf.ezmorph:ezmorph:1.0.6'
        compile 'org.owasp.encoder:encoder:1.2'

    }

    plugins {
        test ":geb:0.9.3", {
            export = false
        }
        compile ":rest:0.8"

        runtime ':hibernate:3.6.10.13' ,{
            export = false
        }
        compile ':joda-time:1.5'
        compile ":i18n-gettext:0.114"
        build ':tomcat:7.0.52.1'    ,{
            export = false
        }
        compile ':ml-groovy-dog:0.2', {
            export =true
        }
        build ":release:3.0.1", {
            export=false
        }

        test ':code-coverage:2.0.3-2', {
            export=false
        }

    }
}

coverage {
    exclusions = ["**/test/**"]
    xml = true
}
