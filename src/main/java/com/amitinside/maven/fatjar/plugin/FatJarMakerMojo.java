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

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Mojo(name = "makefat")
public class FatJarMakerMojo extends AbstractMojo {

    @Component
    private MavenProject mavenProject;

    @Component
    private MavenSession mavenSession;

    @Component
    private BuildPluginManager pluginManager;

    @Parameter(required = false)
    private String mavenLocation;

    @Parameter(required = true)
    private String bundleSymbolicName;

    @Parameter(required = true)
    private String bundleVersion;

    @Parameter(required = false)
    private String targetFilename;

    @Parameter(required = false)
    private String[] extensionsToUnarchive;

    @Parameter(required = true)
    private String targetDirectory;

    @Parameter(required = false, defaultValue = "true")
    private String updateDependencyVersions;

    private File sourceDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        String mavenHome = getMavenEnvironmentVariable();
        if (mavenHome == null) {
            mavenHome = mavenLocation;
            if (mavenHome.trim().isEmpty()) {
                //@formatter:off
                getLog().error("No Maven Environment Variable Found. "
                             + "Please set environment variable or "
                             + "set it explicitly as a configuration "
                             + "parameter");
                //@formatter:on
                return;
            }
        }
        mavenLocation = mavenHome;
        try {
            resolveTargetLocation();
            resolveBundleSymbolicName();
            resolveBundleVersion();

            createSourceDirectory();
            storeConfugurationParameters();
            MavenVersionsUpdater
                    .newInstance(mavenProject, mavenSession, pluginManager, Lists.newArrayList(extensionsToUnarchive))
                    .update();
            LocalMavenRepositoryBrowser.newInstance().copyArtefact();
            FatJarBuilder.newInstance(mavenProject).build();
        } catch (final Exception e) {
            throw new MojoFailureException(e.getMessage());
        }
    }

    private void resolveTargetLocation() throws IOException {
        targetDirectory = resolveProperty(targetDirectory);
        File file = new File(targetDirectory);
        if (!file.isAbsolute()) {
            file = new File(mavenProject.getBasedir(), targetDirectory);
        }
        targetDirectory = file.getCanonicalPath();
    }

    private void resolveBundleSymbolicName() {
        bundleSymbolicName = resolveProperty(bundleSymbolicName);
    }

    private void resolveBundleVersion() {
        bundleVersion = resolveProperty(bundleVersion);
    }

    private String resolveProperty(final String instance) {
        checkArgument(!targetDirectory.trim().isEmpty(), "Target Directory cannot be empty");

        if (instance.indexOf('$') == -1) {
            return instance;
        }
        final Properties properties = mavenProject.getProperties();
        final Map<String, String> props = Maps.fromProperties(properties);
        final String parsedProperty = instance.substring(2, instance.length() - 1);
        return props.get(parsedProperty);
    }

    private void createSourceDirectory() {
        final String userDir = System.getProperty("user.dir");
        sourceDirectory = new File(userDir + File.separator + "fatjar_build");
    }

    private void storeConfugurationParameters() {
        final Configurer configurer = Configurer.INSTANCE;
        configurer.put(MAVEN_LOCATION, mavenLocation);
        configurer.put(POM_LOCATION, mavenProject.getFile().getPath());
        configurer.put(BUNDLE_SYMBOLIC_NAME, bundleSymbolicName);
        configurer.put(BUNDLE_VERSION, bundleVersion);
        configurer.put(TARGET_FILENAME, targetFilename);
        configurer.put(EXTENSION_TO_UNARCHIVE, extensionsToUnarchive);
        configurer.put(SOURCE_DIRECTORY, sourceDirectory);
        configurer.put(TARGET_DIRECTORY, targetDirectory);
        configurer.put(UPDATE_VERSION, updateDependencyVersions);
    }

    private static String getMavenEnvironmentVariable() {
        if (System.getenv("M2_HOME") != null) {
            return System.getenv("M2_HOME");
        } else if (System.getenv("MAVEN_HOME") != null) {
            return System.getenv("MAVEN_HOME");
        } else if (System.getenv("M3_HOME") != null) {
            return System.getenv("M3_HOME");
        } else if (System.getenv("MVN_HOME") != null) {
            return System.getenv("MVN_HOME");
        } else if (System.getProperty("maven.home") != null) {
            return System.getProperty("maven.home");
        } else {
            return null;
        }
    }
}
