/*
 * Copyright 2013 the original author or authors.
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
package org.gradle.api.publish.maven.internal.dependencies;

import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.ExcludeRule;
import org.gradle.util.Path;

import javax.annotation.Nullable;
import java.util.Collection;

public interface MavenDependency {
    /**
     * The groupId value for this dependency.
     */
    String getGroupId();

    /**
     * The artifactId value for this dependency.
     */
    String getArtifactId();

    /**
     * The version value for this dependency.
     */
    @Nullable
    String getVersion();

    /**
     * The type value for this dependency.
     */
    @Nullable
    String getType();

    Collection<DependencyArtifact> getArtifacts();
    Collection<ExcludeRule> getExcludeRules();
    Path getProjectIdentityPath();
}
