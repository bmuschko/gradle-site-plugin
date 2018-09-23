val functionalTest by sourceSets.creating


//sourceSets {
//    functionalTest {
//        groovy.srcDir file('src/functTest/groovy')
//        resources.srcDir file('src/functTest/resources')
//        compileClasspath += sourceSets.main.output + configurations.testRuntime
//        runtimeClasspath += output + compileClasspath
//    }
//}

//task functionalTest(type: Test) {
//    description = 'Runs the functional tests.'
//    group = 'verification'
//    testClassesDirs = sourceSets.functionalTest.output.classesDirs
//    classpath = sourceSets.functionalTest.runtimeClasspath
//    mustRunAfter test
//
//            reports {
//                html.destination = project.file("$html.destination/functional")
//                junitXml.destination = project.file("$junitXml.destination/functional")
//            }
//}
//
//check.dependsOn functionalTest
//
//gradlePlugin {
//    testSourceSets sourceSets.functionalTest
//}
//
//dependencies {
//    functionalTestCompile 'org.jsoup:jsoup:1.10.2'
//}
