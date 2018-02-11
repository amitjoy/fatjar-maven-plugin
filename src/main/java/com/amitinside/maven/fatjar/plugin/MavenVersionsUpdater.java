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
import static com.google.common.base.Preconditions.checkArgument;
import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

import java.io.File;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.twdata.maven.mojoexecutor.MojoExecutor.Element;

import com.google.common.collect.Lists;

public final class MavenVersionsUpdater {

    private static final String DEPENDENCY_PLUGIN_VERSION = "3.0.2";
    private static final String VERSIONS_PLUGIN_VERSION = "2.5";
    private final String mavenHome;
    private final boolean shouldUpdateVersions;
    private final MavenProject mavenProject;
    private final MavenSession mavenSession;
    private final BuildPluginManager pluginManager;
    private final List<String> extensionsToUnarchive;
    private final File sourceLocation;

    private MavenVersionsUpdater(final MavenProject mavenProject, final MavenSession mavenSession,
            final BuildPluginManager pluginManager, final List<String> extensionsToUnarchive) {
        mavenHome = Configurer.INSTANCE.getAsString(MAVEN_LOCATION);
        sourceLocation = (File) Configurer.INSTANCE.get(SOURCE_DIRECTORY);
        final String needUpdate = Configurer.INSTANCE.getAsString(UPDATE_VERSION);
        shouldUpdateVersions = Boolean.valueOf(needUpdate);
        this.mavenProject = mavenProject;
        this.mavenSession = mavenSession;
        this.pluginManager = pluginManager;
        this.extensionsToUnarchive = extensionsToUnarchive;

        checkArgument(!mavenHome.trim().isEmpty(), "Maven Home Location cannot be empty");
    }

    public static MavenVersionsUpdater newInstance(final MavenProject mavenProject, final MavenSession mavenSession,
            final BuildPluginManager pluginManager, final List<String> extensionsToUnarchive) {
        return new MavenVersionsUpdater(mavenProject, mavenSession, pluginManager, extensionsToUnarchive);
    }

    public void update() throws MojoExecutionException {
        if (shouldUpdateVersions) {
            executeVersionsMojo();
        }
        executeDependencyMojo();
    }

    private List<Dependency> getArtefactsToUnarchive() {
        final List<Dependency> deps = Lists.newArrayList();
        for (final Dependency dep : mavenProject.getModel().getDependencies()) {
            final String type = dep.getType();
            if (extensionsToUnarchive.contains(type)) {
                deps.add(dep);
            }
        }
        return deps;
    }

    private Element[] convertToElement(final Dependency dependency) {
        final String groupId = dependency.getGroupId();
        final String artifactid = dependency.getArtifactId();
        final String version = dependency.getVersion();
        final String type = dependency.getType();

        final Element groupIdElement = element(name("groupId"), groupId);
        final Element artifactidElement = element(name("artifactId"), artifactid);
        final Element versionElement = element(name("version"), version);
        final Element typeElement = element(name("type"), type);
        final Element overwriteElement = element(name("overWrite"), "true");
        final Element outputDirElement = element(name("outputDirectory"), sourceLocation.getPath());

        final List<Element> elements = Lists.newArrayList(groupIdElement, artifactidElement, versionElement,
                typeElement, overwriteElement, outputDirElement);

        return elements.toArray(new Element[elements.size()]);
    }

    private Element[] convertToArtefactItems() {
        final List<Element> artifactItems = Lists.newArrayList();
        for (final Dependency dep : getArtefactsToUnarchive()) {
            final Element artifactItem = element(name("artifactItem"), convertToElement(dep));
            artifactItems.add(artifactItem);
        }
        return artifactItems.toArray(new Element[artifactItems.size()]);
    }

    private void executeVersionsMojo() throws MojoExecutionException {
        //@formatter:off
        executeMojo(
                plugin(
                        groupId("org.codehaus.mojo"),
                        artifactId("versions-maven-plugin"),
                        version(VERSIONS_PLUGIN_VERSION)
                        ),
                goal("update-properties"),
                configuration(
                        element(name("generateBackupPoms"), "false")),
                executionEnvironment(
                        mavenProject,
                        mavenSession,
                        pluginManager
                        )
                );
        //@formatter:on
    }

    private void executeDependencyMojo() throws MojoExecutionException {
        //@formatter:off
        executeMojo(
                plugin(
                    groupId("org.apache.maven.plugins"),
                    artifactId("maven-dependency-plugin"),
                    version(DEPENDENCY_PLUGIN_VERSION)
                ),
                goal("copy"),
                configuration(
                    element(name("artifactItems"), convertToArtefactItems())
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
