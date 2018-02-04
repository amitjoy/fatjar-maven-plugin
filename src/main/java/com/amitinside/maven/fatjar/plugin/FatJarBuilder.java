/*******************************************************************************
 * Copyright (c) 2018 Amit Kumar Mondal
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package com.amitinside.maven.fatjar.plugin;

import static aQute.bnd.osgi.Constants.*;
import static com.amitinside.maven.fatjar.plugin.Configurer.Params.*;
import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkArgument;
import static java.io.File.separator;
import static java.util.stream.Collectors.joining;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.maven.project.MavenProject;

import com.amitinside.maven.fatjar.plugin.Configurer.Params;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

public final class FatJarBuilder {

    private final File sourceLocation;
    private final String bndFile;
    private final String bsn;
    private final String version;
    private final String[] extensionsToUnarchive;
    private final String targetLocation;
    private final MavenProject mavenProject;
    private String fileName;

    private FatJarBuilder(final MavenProject mavenProject) {
        bsn = Configurer.INSTANCE.getAsString(BUNDLE_SYMBOLIC_NAME);
        version = Configurer.INSTANCE.getAsString(Params.BUNDLE_VERSION);
        fileName = Configurer.INSTANCE.getAsString(TARGET_FILENAME);
        sourceLocation = (File) Configurer.INSTANCE.get(SOURCE_DIRECTORY);
        extensionsToUnarchive = (String[]) Configurer.INSTANCE.get(EXTENSION_TO_UNARCHIVE);
        targetLocation = Configurer.INSTANCE.getAsString(Params.TARGET_DIRECTORY);
        bndFile = sourceLocation + separator + "temp.bnd";
        this.mavenProject = mavenProject;

        checkArgument(!bsn.trim().isEmpty(), "Bundle Symbolic Name cannot be empty");
        checkArgument(!targetLocation.trim().isEmpty(), "Target Directory cannot be empty");
    }

    public static FatJarBuilder newInstance(final MavenProject mavenProject) {
        return new FatJarBuilder(mavenProject);
    }

    public void build() throws Exception {
        extractArchives();
        buildBndConfigFile();
        executeBnd();
        FileUtils.deleteDirectory(sourceLocation);
    }

    private void extractArchives() throws IOException {
        try (Stream<Path> paths = Files.walk(sourceLocation.toPath())) {
            //@formatter:off
            paths.filter(Files::isRegularFile)
                 .map(Path::toFile)
                 .filter(f -> Arrays.stream(extensionsToUnarchive)
                                    .anyMatch(e -> f.getName().endsWith(e)))
                 .forEach(this::extract);
           //@formatter:on
        }
    }

    private void extract(final File file) {
        try {
            final ZipFile zipFile = new ZipFile(file);
            zipFile.extractAll(sourceLocation.getPath());
        } catch (final ZipException e) {
            // suppress due to the usage in stream
        }
    }

    private void buildBndConfigFile() throws IOException {
        try (Stream<Path> paths = Files.walk(sourceLocation.toPath())) {
            //@formatter:off
            final String classpath = paths.filter(Files::isRegularFile)
                                    .map(Path::toFile)
                                    .filter(f -> f.getName().endsWith(".jar"))
                                    .map(File::getPath)
                                    .collect(joining( ", " ));
            //@formatter:on
            final StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append("Bundle-SymbolicName: ");
            contentBuilder.append(bsn);
            contentBuilder.append("\n\n");
            contentBuilder.append("ver: 1.0.0");
            contentBuilder.append("\n\n");
            contentBuilder.append("-classpath: ");
            contentBuilder.append(classpath);
            contentBuilder.append("\n\n");
            contentBuilder.append("Export-Package: *;version=${ver}");

            try (final Writer writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(bndFile), UTF_8))) {
                writer.write(contentBuilder.toString());
            }
        }
    }

    private void executeBnd() throws Exception {
        final Properties beanProperties = new Properties();
        try (Builder builder = new Builder(new Processor(beanProperties, false))) {
            loadProperties(builder, mavenProject);

            if (builder.getProperty(BUNDLE_SYMBOLICNAME) == null) {
                builder.setProperty(BUNDLE_SYMBOLICNAME, bsn);
            }
            if (builder.getProperty(BUNDLE_NAME) == null) {
                builder.setProperty(BUNDLE_NAME, bsn);
            }
            if (builder.getProperty(Constants.BUNDLE_VERSION) == null) {
                builder.setProperty(Constants.BUNDLE_VERSION, version);
            }
            if (fileName.isEmpty()) {
                fileName = bsn + "-" + version + ".jar";
            }
            final Jar bndJar = builder.build();
            bndJar.write(targetLocation + separator + fileName);
        }
    }

    private File loadProperties(final Builder builder, final MavenProject bndProject) throws IOException {
        final File baseDir = bndProject.getBasedir();
        if (baseDir != null) {
            final File bndfile = new File(bndFile);
            if (bndfile.isFile()) {
                builder.setProperties(bndfile.getParentFile(), builder.loadProperties(bndfile));
                return bndfile;
            }
        }
        return null;
    }

}
