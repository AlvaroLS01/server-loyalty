package com.comerzzia.bricodepot.api.omnichannel.api.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.comerzzia.core.util.config.AppInfo;

/**
 * Resolves the location of the bundled report templates so that Comerzzia's
 * legacy components can find them on the filesystem. The initializer inspects
 * the classpath for the {@code informes} directory and, when required, copies
 * the resources to a temporary location that behaves like the expected
 * {@code COMERZZIA_HOME/informes} directory.
 */
public final class InformesResourceInitializer {

    private static final Logger log = LoggerFactory.getLogger(InformesResourceInitializer.class);

    private static final String COMERZZIA_HOME_PROPERTY = "COMERZZIA_HOME";
    private static final String INFORMES_DIRECTORY = "informes";
    private static final String CLASSPATH_PATTERN = "classpath*:/informes/**/*.*";

    private static volatile Path informesDirectory;

    private InformesResourceInitializer() {
        // Utility class
    }

    /**
     * Ensures that the report directory is available on the filesystem and that
     * {@link AppInfo} is aware of its location.
     */
    public static synchronized void initialize() {
        if (informesDirectory == null) {
            informesDirectory = resolveInformesDirectory();
            if (informesDirectory == null) {
                log.warn("Unable to resolve Comerzzia report directory from classpath resources");
                return;
            }
        }

        applyRutaBase();
    }

    /**
     * Applies the resolved directory to the Comerzzia configuration if possible.
     */
    public static synchronized void applyRutaBase() {
        if (informesDirectory == null) {
            return;
        }

        String base = informesDirectory.toAbsolutePath().toString();
        if (!base.endsWith(File.separator)) {
            base = base + File.separator;
        }

        try {
            Object informesInfo = AppInfo.getInformesInfo();
            if (informesInfo != null) {
                Method setter = informesInfo.getClass().getMethod("setRutaBase", String.class);
                setter.invoke(informesInfo, base);
                log.debug("Comerzzia report base directory set to {}", base);
            }
        } catch (NoSuchMethodException e) {
            log.warn("Comerzzia InformesInfo does not expose a setRutaBase method");
        } catch (Exception e) {
            log.warn("Unable to configure Comerzzia report directory: {}", e.getMessage());
        }
    }

    private static Path resolveInformesDirectory() {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL resourceUrl = classLoader.getResource(INFORMES_DIRECTORY);
            if (resourceUrl != null) {
                Path resolved = resolveFromUrl(resourceUrl);
                if (resolved != null) {
                    ensureComerzziaHome(resolved.getParent());
                    return resolved;
                }
            }
        } catch (Exception ex) {
            log.debug("Failed to resolve report directory from classpath: {}", ex.getMessage());
        }

        Path projectPath = Paths.get("src", "main", "resources", INFORMES_DIRECTORY);
        if (Files.exists(projectPath)) {
            Path absolute = projectPath.toAbsolutePath();
            ensureComerzziaHome(absolute.getParent());
            return absolute;
        }

        return null;
    }

    private static Path resolveFromUrl(URL resourceUrl) throws IOException, URISyntaxException {
        if (StringUtils.equals(resourceUrl.getProtocol(), "file")) {
            URI uri = resourceUrl.toURI();
            Path path = Paths.get(uri);
            if (Files.isDirectory(path)) {
                return path;
            }
        }

        if (StringUtils.equals(resourceUrl.getProtocol(), "jar")) {
            Path tempHome = Files.createTempDirectory("comerzzia-home");
            Path informesPath = tempHome.resolve(INFORMES_DIRECTORY);
            copyClasspathResources(informesPath);
            ensureComerzziaHome(tempHome);
            return informesPath;
        }

        // Attempt with Spring's ClassPathResource as a fallback
        ClassPathResource classPathResource = new ClassPathResource(INFORMES_DIRECTORY);
        if (classPathResource.exists() && classPathResource.isFile()) {
            try {
                Path path = classPathResource.getFile().toPath();
                if (Files.isDirectory(path)) {
                    return path;
                }
            } catch (IOException ex) {
                log.debug("Unable to access classpath resource as file: {}", ex.getMessage());
            }
        }

        return null;
    }

    private static void copyClasspathResources(Path targetDirectory) throws IOException {
        Files.createDirectories(targetDirectory);

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(CLASSPATH_PATTERN);

        String marker = "/" + INFORMES_DIRECTORY + "/";

        for (Resource resource : resources) {
            if (!resource.isReadable() || resource.getFilename() == null) {
                continue;
            }

            String resourcePath = resource.getURL().toString();
            int idx = resourcePath.indexOf(marker);
            if (idx < 0) {
                continue;
            }

            String relative = resourcePath.substring(idx + marker.length());
            relative = relative.replace('/', File.separatorChar);

            Path destination = targetDirectory.resolve(relative);
            Path parent = destination.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (InputStream inputStream = resource.getInputStream()) {
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void ensureComerzziaHome(Path homeDirectory) {
        if (homeDirectory == null) {
            return;
        }

        if (StringUtils.isNotBlank(System.getProperty(COMERZZIA_HOME_PROPERTY))
                || StringUtils.isNotBlank(System.getenv(COMERZZIA_HOME_PROPERTY))) {
            return;
        }

        System.setProperty(COMERZZIA_HOME_PROPERTY, homeDirectory.toAbsolutePath().toString());
    }
}
