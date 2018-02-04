package com.amitinside.maven.fatjar.plugin;

import static com.amitinside.maven.fatjar.plugin.Configurer.Params.*;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.singletonList;

import java.io.File;
import java.util.Collections;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

public final class MavenVersionsUpdater {

    private final String mavenHome;
    private final String pomLocation;
    private final boolean shouldUpdateVersions;

    private MavenVersionsUpdater() {
        mavenHome = Configurer.INSTANCE.getAsString(MAVEN_LOCATION);
        pomLocation = Configurer.INSTANCE.getAsString(POM_LOCATION);
        final String needUpdate = Configurer.INSTANCE.getAsString(UPDATE_VERSION);
        shouldUpdateVersions = Boolean.valueOf(needUpdate);

        checkArgument(!mavenHome.trim().isEmpty(), "Maven Home Location cannot be empty");
        checkArgument(!pomLocation.trim().isEmpty(), "POM Location cannot be empty");
    }

    public static MavenVersionsUpdater newInstance() {
        return new MavenVersionsUpdater();
    }

    public void update() throws MavenInvocationException {
        final Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome(new File(mavenHome));

        // updates all the versions in the POM if there are newer version available
        if (shouldUpdateVersions) {
            final InvocationRequest versionBumpRequest = new DefaultInvocationRequest();
            versionBumpRequest.setPomFile(new File(pomLocation));
            versionBumpRequest.setGoals(singletonList("versions:update-properties"));

            invoker.execute(versionBumpRequest);
        }

        // builds the updated POM to have the dependencies available in local maven repo
        final InvocationRequest buildRequest = new DefaultInvocationRequest();
        buildRequest.setPomFile(new File(pomLocation));
        buildRequest.setGoals(Collections.singletonList("package"));
        buildRequest.setUpdateSnapshots(true);

        invoker.execute(buildRequest);
    }

}
