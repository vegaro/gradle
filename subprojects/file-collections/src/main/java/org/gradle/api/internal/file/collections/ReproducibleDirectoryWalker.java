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

package org.gradle.api.internal.file.collections;

import org.apache.commons.io.comparator.PathFileComparator;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.file.LinksStrategy;
import org.gradle.api.file.ReadOnlyFileTreeElement;
import org.gradle.api.file.RelativePath;
import org.gradle.api.file.SymbolicLinkDetails;
import org.gradle.api.internal.file.DefaultFileVisitDetails;
import org.gradle.api.internal.file.DefaultSymbolicLinkDetails;
import org.gradle.api.specs.Spec;
import org.gradle.internal.nativeintegration.filesystem.FileSystem;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReproducibleDirectoryWalker implements DirectoryWalker {
    private final FileSystem fileSystem;

    public ReproducibleDirectoryWalker(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    @Nullable
    protected static File[] getChildren(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            Arrays.sort(children, PathFileComparator.PATH_COMPARATOR);
        }
        return children;
    }

    @Override
    public void walkDir(File file, RelativePath path, FileVisitor visitor, Spec<? super ReadOnlyFileTreeElement> spec, AtomicBoolean stopFlag, boolean postfix) {
        LinksStrategy linksStrategy = visitor.getLinksStrategy();
        linksStrategy = linksStrategy == null ? LinksStrategy.NONE : linksStrategy;
        walkDir(file, path, visitor, linksStrategy, spec, stopFlag, postfix);
    }

    //TODO: cover with tests
    private void walkDir(
        File file,
        RelativePath path,
        FileVisitor visitor,
        LinksStrategy linksStrategy,
        Spec<? super ReadOnlyFileTreeElement> spec,
        AtomicBoolean stopFlag,
        boolean postfix
    ) {
        File[] children = getChildren(file);
        if (children == null) {
            if (file.isDirectory() && !file.canRead()) {
                throw new GradleException(String.format("Could not list contents of directory '%s' as it is not readable.", file));
            }
            SymbolicLinkDetails linkDetails = getLinkDetails(file.toPath());
            if (linksStrategy.shouldBePreserved(linkDetails)) {
                //TODO: fix path
                FileVisitDetails details = new DefaultFileVisitDetails(file, path, stopFlag, fileSystem, linkDetails, true);
                if (DirectoryFileTree.isAllowed(details, spec)) {
                    linksStrategy.maybeThrowOnBrokenLink(details.getSymbolicLinkDetails(), file.toString());
                    visitor.visitFile(details);
                }
                return;
            }
            // else, might be a link which points to nothing, or has been removed while we're visiting, or ...
            throw new GradleException(String.format("Could not list contents of '%s'.", file));
        }
        List<FileVisitDetails> dirs = new ArrayList<FileVisitDetails>();
        for (int i = 0; !stopFlag.get() && i < children.length; i++) {
            File child = children[i];
            SymbolicLinkDetails linkDetails = getLinkDetails(file.toPath());
            boolean isFile = child.isFile(); //TODO: fix path and create a helper method
            RelativePath childPath = path.append(isFile, child.getName());
            boolean preserveLink = linksStrategy.shouldBePreserved(linkDetails);
            FileVisitDetails details = new DefaultFileVisitDetails(child, childPath, stopFlag, fileSystem, linkDetails, preserveLink);
            if (DirectoryFileTree.isAllowed(details, spec)) {
                if (isFile || preserveLink) {
                    visitor.visitFile(details);
                } else {
                    dirs.add(details);
                }
            }
        }

        // now handle dirs
        for (int i = 0; !stopFlag.get() && i < dirs.size(); i++) {
            FileVisitDetails dir = dirs.get(i);
            if (postfix) {
                walkDir(dir.getFile(), dir.getRelativePath(), visitor, spec, stopFlag, postfix);
                visitor.visitDir(dir);
            } else {
                visitor.visitDir(dir);
                walkDir(dir.getFile(), dir.getRelativePath(), visitor, spec, stopFlag, postfix);
            }
        }
    }

    @Nullable
    private static SymbolicLinkDetails getLinkDetails(Path path) { //TODO: unauthorized case?
        if (!Files.isSymbolicLink(path)) {
            return null;
        }
        return new DefaultSymbolicLinkDetails(path);
    }
}
