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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.capabilities.CapabilitiesMetadata;
import org.gradle.api.capabilities.Capability;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.artifact.ArtifactSet;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.artifact.ArtifactSetFactory;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.artifact.FileDependencyArtifactSet;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.artifact.ResolvableArtifact;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.artifact.ResolvedVariant;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.excludes.specs.ExcludeSpec;
import org.gradle.api.internal.artifacts.type.ArtifactTypeRegistry;
import org.gradle.api.internal.attributes.ImmutableAttributes;
import org.gradle.internal.DisplayName;
import org.gradle.internal.component.external.model.ImmutableCapabilities;
import org.gradle.internal.component.external.model.ImmutableCapability;
import org.gradle.internal.component.local.model.LocalFileDependencyMetadata;
import org.gradle.internal.component.model.ComponentArtifactMetadata;
import org.gradle.internal.component.model.ComponentResolveMetadata;
import org.gradle.internal.component.model.ModuleSources;
import org.gradle.internal.component.model.VariantResolveMetadata;
import org.gradle.internal.model.CalculatedValueContainerFactory;
import org.gradle.util.internal.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultArtifactSelector implements ArtifactSelector {
    private final Map<ComponentArtifactIdentifier, ResolvableArtifact> allResolvedArtifacts = Maps.newHashMap();
    private final Map<VariantResolveMetadata.Identifier, ResolvedVariant> allResolvedVariants = Maps.newHashMap();
    private final List<OriginArtifactSelector> selectors;
    private final ArtifactTypeRegistry artifactTypeRegistry;
    private final ArtifactResolver artifactResolver;
    private final CalculatedValueContainerFactory calculatedValueContainerFactory;

    public DefaultArtifactSelector(List<OriginArtifactSelector> selectors, ArtifactResolver artifactResolver, ArtifactTypeRegistry artifactTypeRegistry, CalculatedValueContainerFactory calculatedValueContainerFactory) {
        this.selectors = selectors;
        this.artifactTypeRegistry = artifactTypeRegistry;
        this.artifactResolver = artifactResolver;
        this.calculatedValueContainerFactory = calculatedValueContainerFactory;
    }

    @Override
    public ArtifactSet resolveArtifacts(LocalFileDependencyMetadata fileDependencyMetadata) {
        return new FileDependencyArtifactSet(fileDependencyMetadata, artifactTypeRegistry, calculatedValueContainerFactory);
    }

    @Override
    public ArtifactSet resolveArtifacts(ComponentResolveMetadata component, Set<? extends VariantResolveMetadata> allVariants, Set<? extends VariantResolveMetadata> legacyVariants, ExcludeSpec exclusions, ImmutableAttributes overriddenAttributes) {

        ImmutableSet<ResolvedVariant> allResolvedVariants = buildResolvedVariants(component, allVariants, exclusions);
        ImmutableSet<ResolvedVariant> legacyResolvedVariants = buildResolvedVariants(component, legacyVariants, exclusions);

        ArtifactSet artifacts = null;
        for (OriginArtifactSelector selector : selectors) {
            artifacts = selector.resolveArtifacts(component, allResolvedVariants, legacyResolvedVariants, exclusions, overriddenAttributes);
            if (artifacts != null) {
                break;
            }
        }
        if (artifacts == null) {
            throw new IllegalStateException("No artifacts selected.");
        }
        return artifacts;
    }

    private ImmutableSet<ResolvedVariant> buildResolvedVariants(ComponentResolveMetadata component, Set<? extends VariantResolveMetadata> allVariants, ExcludeSpec exclusions) {
        ImmutableSet.Builder<ResolvedVariant> resolvedVariantBuilder = ImmutableSet.builder();
        for (VariantResolveMetadata variant : allVariants) {
            ResolvedVariant resolvedVariant = toResolvedVariant(variant.getIdentifier(), variant.asDescribable(), variant.getAttributes(), variant.getArtifacts(), variant.getCapabilities(), exclusions, component.getModuleVersionId(), component.getSources());
            resolvedVariantBuilder.add(resolvedVariant);
        }
        return resolvedVariantBuilder.build();
    }

    private ResolvedVariant toResolvedVariant(VariantResolveMetadata.Identifier identifier,
                                              DisplayName displayName,
                                              ImmutableAttributes variantAttributes,
                                              ImmutableList<? extends ComponentArtifactMetadata> artifacts,
                                              CapabilitiesMetadata capabilities,
                                              ExcludeSpec exclusions,
                                              ModuleVersionIdentifier ownerId,
                                              ModuleSources moduleSources) {
        // artifactsToResolve are those not excluded by their owning module
        List<? extends ComponentArtifactMetadata> artifactsToResolve = CollectionUtils.filter(artifacts,
                artifact -> !exclusions.excludesArtifact(ownerId.getModule(), artifact.getName())
        );

        boolean hasExcludedArtifact = artifactsToResolve.size() < artifacts.size();

        if (hasExcludedArtifact || identifier == null) {
            // An ad hoc variant, has no identifier
            return createResolvedVariant(displayName, variantAttributes, artifacts, capabilities, ownerId, moduleSources, artifactsToResolve, null);
        } else {
            return allResolvedVariants.computeIfAbsent(identifier, id -> createResolvedVariant(displayName, variantAttributes, artifacts, capabilities, ownerId, moduleSources, artifactsToResolve, identifier));
        }
    }

    private ResolvedVariant createResolvedVariant(DisplayName displayName, ImmutableAttributes variantAttributes, ImmutableList<? extends ComponentArtifactMetadata> artifacts, CapabilitiesMetadata capabilities, ModuleVersionIdentifier ownerId, ModuleSources moduleSources, List<? extends ComponentArtifactMetadata> artifactsToResolve, VariantResolveMetadata.Identifier resolvedIdentifier) {
        ImmutableAttributes attributes = artifactTypeRegistry.mapAttributesFor(variantAttributes, artifacts);
        return ArtifactSetFactory.toResolvedVariant(resolvedIdentifier, displayName, attributes, artifactsToResolve, withImplicitCapability(capabilities.getCapabilities(), ownerId), ownerId, moduleSources, allResolvedArtifacts, artifactResolver, calculatedValueContainerFactory);
    }

    private static ImmutableCapabilities withImplicitCapability(Collection<? extends Capability> capabilities, ModuleVersionIdentifier identifier) {
        // TODO: This doesn't seem right. We should know the capability of the variant before we get here instead of assuming that it's the same as the owner
        if (capabilities.isEmpty()) {
            return ImmutableCapabilities.of(ImmutableCapability.defaultCapabilityForComponent(identifier));
        } else {
            return ImmutableCapabilities.copyAsImmutable(capabilities);
        }
    }

    @Override
    public ArtifactSet resolveArtifacts(ComponentResolveMetadata component, Collection<? extends ComponentArtifactMetadata> artifacts, ImmutableAttributes overriddenAttributes) {
        ImmutableAttributes attributes = artifactTypeRegistry.mapAttributesFor(component.getAttributes(), artifacts);
        return ArtifactSetFactory.adHocVariant(component.getId(), component.getModuleVersionId(), artifacts, component.getSources(), component.getAttributesSchema(), artifactResolver, allResolvedArtifacts, attributes, overriddenAttributes, calculatedValueContainerFactory);
    }
}
