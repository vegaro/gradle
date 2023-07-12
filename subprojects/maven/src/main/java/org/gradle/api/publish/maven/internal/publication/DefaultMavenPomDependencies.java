/*
 * Copyright 2023 the original author or authors.
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

package org.gradle.api.publish.maven.internal.publication;

import com.google.common.collect.ImmutableList;
import org.gradle.api.publish.maven.internal.dependencies.MavenDependency;

/**
 * Default implementation of {@link MavenPomDependencies}.
 */
public class DefaultMavenPomDependencies implements MavenPomDependencies {

    public static final DefaultMavenPomDependencies EMPTY = new DefaultMavenPomDependencies(
        ImmutableList.of(), ImmutableList.of(), ImmutableList.of(), ImmutableList.of(),
        ImmutableList.of(), ImmutableList.of(), ImmutableList.of()
    );

    private final ImmutableList<MavenDependency> runtimeDependencies;
    private final ImmutableList<MavenDependency> apiDependencies;
    private final ImmutableList<MavenDependency> optionalApiDependencies;
    private final ImmutableList<MavenDependency> optionalRuntimeDependencies;
    private final ImmutableList<MavenDependency> runtimeDependencyConstraints;
    private final ImmutableList<MavenDependency> apiDependencyConstraints;
    private final ImmutableList<MavenDependency> importDependencyConstraints;

    public DefaultMavenPomDependencies(
        ImmutableList<MavenDependency> runtimeDependencies,
        ImmutableList<MavenDependency> apiDependencies,
        ImmutableList<MavenDependency> optionalApiDependencies,
        ImmutableList<MavenDependency> optionalRuntimeDependencies,
        ImmutableList<MavenDependency> runtimeDependencyConstraints,
        ImmutableList<MavenDependency> apiDependencyConstraints,
        ImmutableList<MavenDependency> importDependencyConstraints
    ) {
        this.runtimeDependencies = runtimeDependencies;
        this.apiDependencies = apiDependencies;
        this.optionalApiDependencies = optionalApiDependencies;
        this.optionalRuntimeDependencies = optionalRuntimeDependencies;
        this.runtimeDependencyConstraints = runtimeDependencyConstraints;
        this.apiDependencyConstraints = apiDependencyConstraints;
        this.importDependencyConstraints = importDependencyConstraints;
    }

    @Override
    public ImmutableList<MavenDependency> getRuntimeDependencies() {
        return runtimeDependencies;
    }

    @Override
    public ImmutableList<MavenDependency> getApiDependencies() {
        return apiDependencies;
    }

    @Override
    public ImmutableList<MavenDependency> getOptionalApiDependencies() {
        return optionalApiDependencies;
    }

    @Override
    public ImmutableList<MavenDependency> getOptionalRuntimeDependencies() {
        return optionalRuntimeDependencies;
    }

    @Override
    public ImmutableList<MavenDependency> getRuntimeDependencyManagement() {
        return runtimeDependencyConstraints;
    }

    @Override
    public ImmutableList<MavenDependency> getApiDependencyManagement() {
        return apiDependencyConstraints;
    }

    @Override
    public ImmutableList<MavenDependency> getImportDependencyManagement() {
        return importDependencyConstraints;
    }
}
