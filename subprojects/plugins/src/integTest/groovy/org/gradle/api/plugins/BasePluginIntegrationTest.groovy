/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.plugins

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.test.precondition.Requires
import org.gradle.test.preconditions.UnitTestPreconditions

import static org.hamcrest.CoreMatchers.containsString

class BasePluginIntegrationTest extends AbstractIntegrationSpec {

    @Requires(UnitTestPreconditions.MandatoryFileLockOnOpen)
    def "clean failure message indicates file"() {
        given:
        buildFile << """
            apply plugin: 'base'
        """

        and:
        def channel = new RandomAccessFile(file("build/newFile").createFile(), "rw").channel
        def lock = channel.lock()

        when:
        fails "clean"

        then:
        failure.assertThatCause(containsString("Unable to delete directory '${file('build')}'"))
        failure.assertThatCause(containsString("Failed to delete some children. This might happen because a process has files open or has its working directory set in the target directory."))
        failure.assertThatCause(containsString(file("build/newFile").absolutePath))

        cleanup:
        lock?.release()
        channel?.close()
    }

    def "cannot define 'build' and 'check' tasks when applying plugin"() {
        buildFile << """
            apply plugin: 'base'

            task $taskName {
                doLast {
                    println "CUSTOM"
                }
            }
"""
        when:
        fails "build"

        then:
        failure.assertHasCause "Cannot add task '$taskName' as a task with that name already exists."
        where:
        taskName << ['build', 'check']
    }

    def "can define 'default' and 'archives' configurations prior to applying plugin"() {
        buildFile << """
            configurations {
                create("default")
                archives
            }
            apply plugin: 'base'
        """

        expect:
        executer.expectDocumentedDeprecationWarning("The configuration default was created explicitly. This configuration name is reserved for creation by Gradle. This behavior has been deprecated. This behavior is scheduled to be removed in Gradle 9.0. Do not create a configuration with this name. Consult the upgrading guide for further information: https://docs.gradle.org/current/userguide/upgrading_version_8.html#configurations_allowed_usage")
        executer.expectDocumentedDeprecationWarning("The configuration archives was created explicitly. This configuration name is reserved for creation by Gradle. This behavior has been deprecated. This behavior is scheduled to be removed in Gradle 9.0. Do not create a configuration with this name. Consult the upgrading guide for further information: https://docs.gradle.org/current/userguide/upgrading_version_8.html#configurations_allowed_usage")
        succeeds "help"
    }

    def "can override archiveBaseName in custom Jar task"() {
        buildFile """
            apply plugin: 'base'
            class MyJar extends Jar {
                MyJar() {
                    super()
                    archiveBaseName.set("myjar")
                }
            }
            task myJar(type: MyJar) {
                doLast { task ->
                    assert task.archiveBaseName.get() == "myjar"
                }
            }
        """
        expect:
        succeeds("myJar")
    }
}
