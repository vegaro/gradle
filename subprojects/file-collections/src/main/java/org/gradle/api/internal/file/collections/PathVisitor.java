/*
 * Copyright 2015 the original author or authors.
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

import org.gradle.api.GradleException;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.file.LinksStrategy;
import org.gradle.api.file.ReadOnlyFileTreeElement;
import org.gradle.api.file.RelativePath;
import org.gradle.api.file.SymbolicLinkDetails;
import org.gradle.api.internal.file.AttributeBasedFileVisitDetails;
import org.gradle.api.internal.file.DefaultFileVisitDetails;
import org.gradle.api.internal.file.DefaultSymbolicLinkDetails;
import org.gradle.api.internal.file.UnauthorizedFileVisitDetails;
import org.gradle.api.specs.Spec;
import org.gradle.internal.nativeintegration.filesystem.FileSystem;
import org.gradle.internal.os.OperatingSystem;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystemLoopException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicBoolean;


class PathVisitor implements java.nio.file.FileVisitor<Path> {
    private final Deque<FileVisitDetails> directoryDetailsHolder;
    private final Spec<? super ReadOnlyFileTreeElement> spec;
    private final boolean postfix;
    private final FileVisitor visitor;
    private final AtomicBoolean stopFlag;
    private final RelativePath rootPath;
    private final FileSystem fileSystem;
    private final LinksStrategy linksStrategy;

    public PathVisitor(Deque<FileVisitDetails> directoryDetailsHolder, Spec<? super ReadOnlyFileTreeElement> spec, boolean postfix, FileVisitor visitor, AtomicBoolean stopFlag, RelativePath rootPath, FileSystem fileSystem) {
        this.directoryDetailsHolder = directoryDetailsHolder;
        this.spec = spec;
        this.postfix = postfix;
        this.visitor = visitor;
        this.stopFlag = stopFlag;
        this.rootPath = rootPath;
        this.fileSystem = fileSystem;
        this.linksStrategy = visitor.getLinksStrategy() == null ? LinksStrategy.NONE : visitor.getLinksStrategy();
    }

    private boolean shouldVisit(ReadOnlyFileTreeElement element) {
        return spec.isSatisfiedBy(element);
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        FileVisitDetails details = getFileVisitDetails(dir, attrs, true);
        if (directoryDetailsHolder.size() == 0 || shouldVisit(details)) {
            if (details.isSymbolicLink()) {
                visitor.visitFile(details);
                return FileVisitResult.SKIP_SUBTREE;
            }
            directoryDetailsHolder.push(details);
            if (directoryDetailsHolder.size() > 1 && !postfix) {
                visitor.visitDir(details);
            }
            return checkStopFlag();
        } else {
            return FileVisitResult.SKIP_SUBTREE;
        }
    }

    private FileVisitResult checkStopFlag() {
        return stopFlag.get()
            ? FileVisitResult.TERMINATE
            : FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        FileVisitDetails details = getFileVisitDetails(file, attrs, false);
        if (shouldVisit(details)) {
            linksStrategy.maybeThrowOnBrokenLink(details.getSymbolicLinkDetails(), file.toString());
            visitor.visitFile(details);
        }
        return checkStopFlag();
    }

    private FileVisitDetails getFileVisitDetails(Path file, @Nullable BasicFileAttributes attrs, boolean isDirectory) {
        SymbolicLinkDetails linkDetails = getLinkDetails(file);
        boolean preserveLink = linksStrategy.shouldBePreserved(linkDetails);
        File child = file.toFile(); //TODO: use path here?
        FileVisitDetails dirDetails = directoryDetailsHolder.peek();
        RelativePath childPath = dirDetails != null ? dirDetails.getRelativePath().append(!isDirectory || preserveLink, child.getName()) : rootPath;
        if (attrs == null) {
            return new UnauthorizedFileVisitDetails(child, childPath);
        } else if (isDirectory && OperatingSystem.current() == OperatingSystem.WINDOWS) {
            // Workaround for https://github.com/gradle/gradle/issues/11577
            return new DefaultFileVisitDetails(child, childPath, stopFlag, fileSystem, linkDetails, preserveLink);
        } else {
            return new AttributeBasedFileVisitDetails(child, childPath, stopFlag, fileSystem, attrs, linkDetails, preserveLink);
        }
    }

    @Nullable
    private static SymbolicLinkDetails getLinkDetails(Path path) { //TODO: unauthorized case?
        if (!Files.isSymbolicLink(path)) {
            return null;
        }
        return new DefaultSymbolicLinkDetails(path);
    }

    private FileVisitDetails getUnauthorizedFileVisitDetails(Path file) {
        return getFileVisitDetails(file, null, false);
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        FileVisitDetails details = getUnauthorizedFileVisitDetails(file);
        if (isNotFileSystemLoopException(exc) && shouldVisit(details)) {
            throw new GradleException(String.format("Could not read path '%s'.", file), exc);
        }
        return checkStopFlag();
    }

    private static boolean isNotFileSystemLoopException(@Nullable IOException e) {
        return e != null && !(e instanceof FileSystemLoopException);
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, @Nullable IOException exc) {
        if (exc != null) {
            if (!(exc instanceof FileSystemLoopException)) {
                throw new GradleException(String.format("Could not read directory path '%s'.", dir), exc);
            }
        } else {
            if (postfix) {
                FileVisitDetails details = directoryDetailsHolder.peek();
                if (directoryDetailsHolder.size() > 1 && details != null) {
                    visitor.visitDir(details);
                }
            }
        }
        directoryDetailsHolder.pop();
        return checkStopFlag();
    }
}
