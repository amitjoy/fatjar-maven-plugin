package com.amitinside.maven.fatjar.plugin;

import static java.io.File.separator;

public final class Constants {

    private Constants() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static final String MAVEN_LOCAL_REPOSITORY = ".m2" + separator + "repository";
    public static final String BNDLIB = "biz.aQute.bnd.jar";

}
