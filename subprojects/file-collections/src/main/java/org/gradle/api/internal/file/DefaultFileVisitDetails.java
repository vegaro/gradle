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

package org.gradle.api.internal.file;

import org.apache.commons.io.IOUtils;
import org.gradle.api.UncheckedIOException;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.RelativePath;
import org.gradle.internal.file.Stat;
import org.gradle.util.internal.GFileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultFileVisitDetails extends DefaultFileTreeElement implements FileVisitDetails {
    private final AtomicBoolean stop;
    private final boolean preserveLink;

    public DefaultFileVisitDetails(File file, RelativePath relativePath, AtomicBoolean stop, Stat stat, boolean preserveLink) {
        super(file, relativePath, stat);
        this.stop = stop;
        this.preserveLink = preserveLink;
    }

    @Override
    public File getFile() {
        return super.file;
    }

    @Override
    public void copyTo(OutputStream output) {
        try (InputStream inputStream = GFileUtils.openInputStream(file)) {
            IOUtils.copyLarge(inputStream, output);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void stopVisiting() {
        stop.set(true);
    }

    @Override
    public boolean isSymbolicLink() {
        return preserveLink && super.isSymbolicLink();
    }
}
