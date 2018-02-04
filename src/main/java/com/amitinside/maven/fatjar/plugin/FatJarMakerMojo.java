package com.amitinside.maven.fatjar.plugin;

import static com.amitinside.maven.fatjar.plugin.Configurer.Params.*;
import static org.apache.commons.io.FileUtils.*;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "makefat")
public class FatJarMakerMojo extends AbstractMojo {

    @Parameter(property = "mavenLocation", required = false)
    private String mavenLocation;

    @Parameter(property = "pomLocation", required = true)
    private String pomLocation;

    @Parameter(property = "bundleSymbolicName", required = true)
    private String bundleSymbolicName;

    @Parameter(property = "fileName", required = false)
    private String fileName;

    @Parameter(property = "extensionsToUnarchive", required = false)
    private String[] extensionsToUnarchive;

    @Parameter(property = "sourceDirectory", required = true)
    private String sourceDirectory;

    @Parameter(property = "targetDirectory", required = true)
    private String targetDirectory;

    @Parameter(property = "updateDependencyVersions", required = false, defaultValue = "true")
    private String updateDependencyVersions;

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
        storeConfugurationParameters();
        try {
            MavenVersionsUpdater.newInstance().update();
            LocalMavenRepositoryBrowser.newInstance().copyArtefact();
            FatJarBuilder.newInstance().build();
            createSourceDirectory();
            deleteDirectory(new File(sourceDirectory));
        } catch (final Exception e) {
            throw new MojoFailureException(e.getMessage());
        }
    }

    private void createSourceDirectory() throws IOException {
        final File sourceDir = new File(sourceDirectory);
        forceMkdir(sourceDir);
    }

    private void storeConfugurationParameters() {
        final Configurer configurer = Configurer.INSTANCE;
        configurer.put(MAVEN_LOCATION, mavenLocation);
        configurer.put(POM_LOCATION, pomLocation);
        configurer.put(BUNDLE_SYMBOLIC_NAME, bundleSymbolicName);
        configurer.put(FILE_NAME, fileName);
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
