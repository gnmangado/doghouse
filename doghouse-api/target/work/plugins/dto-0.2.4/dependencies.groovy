grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir	= "target/test-reports"
grails.project.dependency.resolution = {
    inherits "global" // inherit Grails' default dependencies
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'

    repositories {
        grailsHome()
        grailsCentral()
        mavenLocal()
        mavenCentral()
    }
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.
        // runtime 'com.mysql:mysql-connector-java:5.1.5'
        compile "net.sf.dozer:dozer:5.1", {
            // Because I'm including Dozer as a compile dependency (as opposed
            // to a runtime one), I have to exclude the 'provided' transitive
            // dependencies. I also exclude commons-logging.
            excludes "commons-logging", "spring", "xmlbeans", "xmlbeans-xpath", "junit"
        }

        test "org.gmock:gmock:0.8.0", {
            excludes "junit"
        }
    }

}
