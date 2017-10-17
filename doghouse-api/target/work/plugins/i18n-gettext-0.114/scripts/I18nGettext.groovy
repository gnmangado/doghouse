//
//   Copyright 2008,2009,2010 Rainer Brang
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//
import grails.util.GrailsUtil 


i18nDir = "./grails-app/i18n"
i18nOutputDir = "${i18nGettextPluginDir}/lib"
fileNameToCreate = null
bundle = null
config = null
baseFolderToScan = "."

includeTargets << grailsScript("_GrailsCompile")
includeTargets << grailsScript("_GrailsInit")
includeTargets << grailsScript("_GrailsPackage")
includeTargets << gant.targets.Clean
includeTool << gant.tools.Execute

includeTargets << new File("${i18nGettextPluginDir}/scripts/_I18nGettext.groovy")

target( main: "Scan all .groovy and .gsp files for tr() trn() and merge with all .po files in "+i18nDir ) {
	
		compile() 
		def classLoader = Thread.currentThread().contextClassLoader
		classLoader.addURL(new File(classesDirPath).toURL()) 
		config = new ConfigSlurper(GrailsUtil.environment).parse(classLoader.loadClass('Config')) 
		
		parameters = []

        if( args ){
        	parameters = args.split("\n")
        }

        switch( parameters[0] ){
        case 'init':
        	if( parameters.size()>1 ){
        		fileNameToCreate = parameters[1]
        	} else {
        		fileNameToCreate = "Messages"
        	}
            touchpo()
        
        break
        case 'clobber':
        	clobber()
        break
        case 'clean':
        	clean()
        break
        case 'makemo':
        	if( parameters.size()>1 ){
            	bundle = parameters[1]
        	} else {
        		bundle = null
        	}
        	makemo()
        break
        case 'merge':
        	mergepo()
        break
        case 'touchpo':
        	if( parameters.size()>1 ){
        		fileNameToCreate = parameters[1]
        	} else {
        		fileNameToCreate = "Messages"
        	}
            touchpo()
        
        break
        case 'scan':
        default:
            if( parameters.size()>1 ){
                baseFolderToScan = parameters[1]
            } else {
                baseFolderToScan = "."
            }
            scan()
    }
}

setDefaultTarget(main)

