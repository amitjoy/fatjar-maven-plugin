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

import static com.amitinside.maven.fatjar.plugin.Configurer.Params.*;
import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkArgument;
import static java.io.File.separator;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import com.amitinside.maven.fatjar.plugin.Configurer.Params;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

public final class FatJarBuilder {

    private final File sourceLocation;
    private final String targetClassesLocation;
    private final String bndFile;
    private final String bsn;
    private final String[] extensionsToUnarchive;
    private final String targetLocation;
    private final MavenProject mavenProject;
    private final MavenSession mavenSession;
    private final BuildPluginManager pluginManager;
    private String fileName;

    private FatJarBuilder(final MavenProject mavenProject, final MavenSession mavenSession,
            final BuildPluginManager pluginManager) {
        bsn = Configurer.INSTANCE.getAsString(BUNDLE_SYMBOLIC_NAME);
        fileName = Configurer.INSTANCE.getAsString(TARGET_FILENAME);
        sourceLocation = (File) Configurer.INSTANCE.get(SOURCE_DIRECTORY);
        extensionsToUnarchive = (String[]) Configurer.INSTANCE.get(EXTENSION_TO_UNARCHIVE);
        targetLocation = Configurer.INSTANCE.getAsString(Params.TARGET_DIRECTORY);
        bndFile = sourceLocation + separator + "temp.bnd";
        this.mavenProject = mavenProject;
        this.mavenSession = mavenSession;
        this.pluginManager = pluginManager;
        targetClassesLocation = sourceLocation.getParent() + separator + "target/";

        checkArgument(!bsn.trim().isEmpty(), "Bundle Symbolic Name cannot be empty");
        checkArgument(!targetLocation.trim().isEmpty(), "Target Directory cannot be empty");
    }

    public static FatJarBuilder newInstance(final MavenProject mavenProject, final MavenSession mavenSession,
            final BuildPluginManager pluginManager) {
        return new FatJarBuilder(mavenProject, mavenSession, pluginManager);
    }

    public void build() throws Exception {
        extractArchives();
        buildBndConfigFile();
        final String path = sourceLocation.getPath();
        executeBndMojo(path, path);
        final String manifestLoc = targetClassesLocation + "classes/META-INF/MANIFEST.MF";
        executeJarMojo(manifestLoc);
        moveToTargetDirectory();
        // deleteTempDirectories();
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

    private void moveToTargetDirectory() throws IOException {
        createTargetDirectory();

        final String artifactId = mavenProject.getArtifactId();
        final String version = mavenProject.getVersion();

        final String name = artifactId + "-" + version + ".jar";

        if (fileName.isEmpty()) {
            fileName = name;
        }
        final File oldFile = FileUtils.getFile(targetClassesLocation + separator + name);
        final File newFile = FileUtils.getFile(targetLocation + separator + fileName);
        Files.move(Paths.get(oldFile.toURI()), Paths.get(newFile.toURI()), ATOMIC_MOVE);
    }

    private void deleteTempDirectories() throws IOException {
        FileUtils.deleteDirectory(new File(targetClassesLocation));
        FileUtils.deleteDirectory(sourceLocation);
    }

    private void createTargetDirectory() throws IOException {
        final File targetDir = new File(targetLocation);
        forceMkdir(targetDir);
    }

    private void executeBndMojo(final String sourceDir, final String targetDir) throws MojoExecutionException {
        //@formatter:off
        executeMojo(
                plugin(
                        groupId("biz.aQute.bnd"),
                        artifactId("bnd-maven-plugin"),
                        version("3.5.0")
                        ),
                goal("bnd-process"),
                configuration(
                        element(name("bndfile"), bndFile),
                        element(name("sourceDir"), sourceDir),
                        element(name("targetDir"), targetDir)
                        ),
                executionEnvironment(
                        mavenProject,
                        mavenSession,
                        pluginManager
                        )
                );
        //@formatter:on
    }

    private void executeJarMojo(final String manifestLoc) throws MojoExecutionException {
        //@formatter:off
        executeMojo(
                plugin(
                        groupId("org.apache.maven.plugins"),
                        artifactId("maven-jar-plugin"),
                        version("3.0.2")
                        ),
                goal("jar"),
                configuration(
                        element(name("archive"),
                        element(name("manifestFile"), manifestLoc))
                            ),
                executionEnvironment(
                        mavenProject,
                        mavenSession,
                        pluginManager
                        )
                );
        //@formatter:on
    }

}
