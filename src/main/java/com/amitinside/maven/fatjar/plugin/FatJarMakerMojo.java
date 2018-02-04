package com.amitinside.maven.fatjar.plugin;

import static com.amitinside.maven.fatjar.plugin.Configurer.Params.*;
import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
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

    @Parameter(property = "mavenLocation", required = false)
    private String mavenLocation;

    @Parameter(property = "bundleSymbolicName", required = true)
    private String bundleSymbolicName;

    @Parameter(property = "targetFilename", required = false)
    private String targetFilename;

    @Parameter(property = "extensionsToUnarchive", required = false)
    private String[] extensionsToUnarchive;

    @Parameter(property = "targetDirectory", required = true)
    private String targetDirectory;

    @Parameter(property = "updateDependencyVersions", required = false, defaultValue = "true")
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
            createSourceDirectory();
            storeConfugurationParameters();
            MavenVersionsUpdater
                    .newInstance(mavenProject, mavenSession, pluginManager, Lists.newArrayList(extensionsToUnarchive))
                    .update();
            LocalMavenRepositoryBrowser.newInstance().copyArtefact();
            FatJarBuilder.newInstance().build();
            FileUtils.deleteDirectory(sourceDirectory);
        } catch (final Exception e) {
            throw new MojoFailureException(e.getMessage());
        }
    }

    private void resolveTargetLocation() throws IOException {
        targetDirectory = resolveProperty();
        File file = new File(targetDirectory);
        if (!file.isAbsolute()) {
            file = new File(mavenProject.getBasedir(), targetDirectory);
        }
        targetDirectory = file.getCanonicalPath();
    }

    private String resolveProperty() {
        checkArgument(!targetDirectory.trim().isEmpty(), "Target Directory cannot be empty");

        if (targetDirectory.indexOf('$') == -1) {
            return targetDirectory;
        }
        final Properties properties = mavenProject.getProperties();
        final Map<String, String> props = Maps.fromProperties(properties);
        final String parsedProperty = targetDirectory.substring(2, targetDirectory.length() - 1);
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
