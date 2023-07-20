/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.internal.resolve.resolver;

import com.google.common.collect.ImmutableSet;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.artifact.ArtifactSet;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.artifact.ResolvedVariant;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.excludes.specs.ExcludeSpec;
import org.gradle.api.internal.attributes.ImmutableAttributes;
import org.gradle.internal.component.local.model.LocalFileDependencyMetadata;
import org.gradle.internal.component.model.ComponentArtifactMetadata;
import org.gradle.internal.component.model.ComponentArtifactResolveMetadata;
import org.gradle.internal.component.model.VariantResolveMetadata;

import java.util.Collection;
import java.util.Set;

public interface ArtifactSelector {
    /**
     * Creates a set that will resolve the given artifacts of the given component.
     */
    ArtifactSet resolveComponentArtifacts(ComponentArtifactResolveMetadata component, Collection<? extends ComponentArtifactMetadata> artifacts, ImmutableAttributes overriddenAttributes);

    /**
     * Creates a set that will resolve the artifacts of the file dependency.
     */
    ArtifactSet resolveLocalArtifacts(LocalFileDependencyMetadata fileDependencyMetadata);

    /**
     * Resolves all provided {@code variants} from {@link VariantResolveMetadata} to {@link ResolvedVariant}s.
     */
    ImmutableSet<ResolvedVariant> resolveVariants(ComponentArtifactResolveMetadata component, Set<? extends VariantResolveMetadata> variants, ExcludeSpec exclusions);
}
