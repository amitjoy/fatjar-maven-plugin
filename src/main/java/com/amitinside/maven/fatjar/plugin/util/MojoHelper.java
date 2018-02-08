package com.amitinside.maven.fatjar.plugin.util;

import java.util.Map;
import java.util.Properties;

import org.apache.maven.project.MavenProject;

import com.google.common.collect.Maps;

public final class MojoHelper {

    private MojoHelper() {
        throw new IllegalAccessError("Cannot instantiate");
    }

    public static String getMavenEnvironmentVariable() {
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

    public static String getUserHome() {
        return System.getProperty("user.home");
    }

    public static String replaceVariable(final MavenProject project, String variable) {
        variable = variable.substring(2, variable.length() - 1);
        switch (variable) {
            case "${user.home}":
                return getUserHome();
            case "${project.basedir}":
                return project.getBasedir().getPath();
        }
        return null;
    }

    public static String getVersion(final Properties properties, final String property) {
        if (property.indexOf('$') == -1) {
            return property;
        }
        final Map<String, String> props = Maps.fromProperties(properties);
        final String parsedProperty = property.substring(2, property.length() - 1);
        return props.get(parsedProperty);
    }

}
