package com.amitinside.maven.fatjar.plugin;

import static com.amitinside.maven.fatjar.plugin.Configurer.Params.*;
import static com.amitinside.maven.fatjar.plugin.Constants.BNDLIB;
import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkArgument;
import static java.io.File.separator;
import static java.util.stream.Collectors.*;
import static org.apache.commons.io.FileUtils.forceMkdir;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;

import com.amitinside.maven.fatjar.plugin.Configurer.Params;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

public final class FatJarBuilder {

    private final File sourceLocation;
    private final String fileName;
    private final String bndFile;
    private final String bsn;
    private final String[] extensionsToUnarchive;
    private final String targetLocation;

    private FatJarBuilder() {
        bsn = Configurer.INSTANCE.getAsString(BUNDLE_SYMBOLIC_NAME);
        fileName = Configurer.INSTANCE.getAsString(FILE_NAME);
        sourceLocation = (File) Configurer.INSTANCE.get(SOURCE_DIRECTORY);
        extensionsToUnarchive = (String[]) Configurer.INSTANCE.get(EXTENSION_TO_UNARCHIVE);
        targetLocation = Configurer.INSTANCE.getAsString(Params.TARGET_DIRECTORY);
        bndFile = sourceLocation + separator + "temp.bnd";

        checkArgument(!bsn.trim().isEmpty(), "Bundle Symbolic Name cannot be empty");
        checkArgument(!fileName.trim().isEmpty(), "File Name cannot be empty");
        checkArgument(!targetLocation.trim().isEmpty(), "Target Directory cannot be empty");
    }

    public static FatJarBuilder newInstance() {
        return new FatJarBuilder();
    }

    public void build() throws Exception {
        extractArchives();
        buildBndConfigFile();
        buildWithBnd();
        moveToTargetDirectory();
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
                                    .map(File::getName)
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

    private void buildWithBnd() throws Exception {
        final String path = exportResource(separator + BNDLIB);

        final Stream<String> params = Stream.of("java", "-jar", path, bndFile);
        final List<String> commandParams = params.collect(toList());
        final ProcessBuilder process = new ProcessBuilder(commandParams);
        process.start();
    }

    private static String exportResource(final String resourceName) throws Exception {
        String jarFolder;
        try (final InputStream stream = FatJarBuilder.class.getResourceAsStream(resourceName)) {
            if (stream == null) {
                throw new NullPointerException("Cannot get resource \"" + resourceName + "\" from JAR file.");
            }
            int readBytes;
            final byte[] buffer = new byte[4096];
            jarFolder = new File(
                    FatJarBuilder.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())
                            .getParentFile().getPath().replace('\\', '/');
            try (final OutputStream resStreamOut = new FileOutputStream(jarFolder + resourceName)) {
                while ((readBytes = stream.read(buffer)) > 0) {
                    resStreamOut.write(buffer, 0, readBytes);
                }
            }
        } catch (final Exception ex) {
            throw ex;
        }

        return jarFolder + resourceName;
    }

    private void moveToTargetDirectory() throws IOException {
        createTargetDirectory();
        final File oldFile = FileUtils.getFile(sourceLocation + separator + bsn + "-0.0.0.jar");
        final File newFile = FileUtils.getFile(sourceLocation + separator + fileName);
        final File destFile = FileUtils.getFile(targetLocation);
        if (oldFile.renameTo(newFile)) {
            FileUtils.copyFileToDirectory(newFile, destFile, true);
        }
    }

    private void createTargetDirectory() throws IOException {
        final File targetDir = new File(targetLocation);
        forceMkdir(targetDir);
    }

}
