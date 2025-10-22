package com.comerzzia.bricodepot.api.omnichannel.api.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.comerzzia.core.util.config.AppInfo;

/**
 * Ensures that the Comerzzia home directory contains the reporting templates bundled
 * with the application so that the legacy services that expect them on the filesystem
 * can find them.
 */
public final class ComerzziaHomeInitializer {

    private static final Logger log = LoggerFactory.getLogger(ComerzziaHomeInitializer.class);

    private static final String COMERZZIA_HOME_PROPERTY = "COMERZZIA_HOME";
    private static final String INFORMES_DIRECTORY = "informes";
    private static final String CLASSPATH_PATTERN = "classpath*:/informes/**/*.*";
    private static final Map<String, String> TEMPLATE_ALIASES = createTemplateAliases();

    private static Path informesBasePath;

    private ComerzziaHomeInitializer() {
        // Utility class
    }

    /**
     * Copies the report resources to the Comerzzia home directory if required and stores
     * the location for later use.
     */
    public static synchronized void initialize() {
        if (informesBasePath != null) {
            return;
        }

        try {
            Path homePath = resolveComerzziaHome();
            informesBasePath = homePath.resolve(INFORMES_DIRECTORY);
            copyInformesResources(informesBasePath);
            ensureTemplateAliases(informesBasePath);
            applyRutaBase();
            log.info("Comerzzia report resources available at {}", informesBasePath.toAbsolutePath());
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to prepare Comerzzia report templates", ex);
        }
    }

    /**
     * Applies the computed report base path to the Comerzzia {@link AppInfo} configuration.
     * This method can be invoked safely multiple times.
     */
    public static synchronized void applyRutaBase() {
        if (informesBasePath == null) {
            // ensure initialization if not yet performed
            try {
                initialize();
            } catch (IllegalStateException ex) {
                log.warn("Unable to initialize Comerzzia report templates: {}", ex.getMessage());
                return;
            }
        }

        String base = informesBasePath.toAbsolutePath().toString();
        if (!base.endsWith(File.separator)) {
            base = base + File.separator;
        }

        try {
            Object informesInfo = AppInfo.getInformesInfo();
            if (informesInfo != null) {
                Method setter = informesInfo.getClass().getMethod("setRutaBase", String.class);
                setter.invoke(informesInfo, base);
                log.debug("Updated AppInfo informes base path to {}", base);
            }
        } catch (NoSuchMethodException e) {
            log.warn("Comerzzia InformesInfo does not provide a setRutaBase method");
        } catch (Exception e) {
            log.warn("Failed to update Comerzzia informes base path: {}", e.getMessage());
        }
    }

    private static Path resolveComerzziaHome() throws IOException {
        String home = System.getProperty(COMERZZIA_HOME_PROPERTY);
        if (StringUtils.isBlank(home)) {
            home = System.getenv(COMERZZIA_HOME_PROPERTY);
        }

        Path homePath;
        if (StringUtils.isBlank(home)) {
            homePath = Paths.get(System.getProperty("user.home"), ".comerzzia");
            System.setProperty(COMERZZIA_HOME_PROPERTY, homePath.toString());
        } else {
            homePath = Paths.get(home);
        }

        Files.createDirectories(homePath);
        return homePath;
    }

    private static void copyInformesResources(Path informesTarget) throws IOException {
        Files.createDirectories(informesTarget);
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(CLASSPATH_PATTERN);

        for (Resource resource : resources) {
            if (!resource.isReadable() || resource.getFilename() == null) {
                continue;
            }

            String relativePath = extractRelativePath(resource);
            if (relativePath == null) {
                continue;
            }

            Path destination = informesTarget.resolve(relativePath);
            Files.createDirectories(Objects.requireNonNull(destination.getParent()));
            try (InputStream inputStream = resource.getInputStream()) {
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static String extractRelativePath(Resource resource) throws IOException {
        String uri = resource.getURI().toString();
        int idx = uri.indexOf("/" + INFORMES_DIRECTORY + "/");
        if (idx < 0) {
            idx = uri.indexOf("!" + INFORMES_DIRECTORY + "/");
            if (idx >= 0) {
                idx += 1;
            }
        }
        if (idx < 0) {
            return null;
        }

        String relative = uri.substring(idx + ("/" + INFORMES_DIRECTORY + "/").length());
        relative = URLDecoder.decode(relative, StandardCharsets.UTF_8.name());
        relative = relative.replaceFirst("^/+", "");
        return relative.replace('/', File.separatorChar);
    }

    private static void ensureTemplateAliases(Path informesTarget) throws IOException {
        Path doctemplatesDir = informesTarget.resolve("doctemplates");
        Files.createDirectories(doctemplatesDir);

        for (Map.Entry<String, String> entry : TEMPLATE_ALIASES.entrySet()) {
            Path source = informesTarget.resolve(entry.getValue().replace('/', File.separatorChar));
            if (!Files.exists(source)) {
                continue;
            }
            Path destination = doctemplatesDir.resolve(entry.getKey());
            Files.createDirectories(Objects.requireNonNull(destination.getParent()));
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Map<String, String> createTemplateAliases() {
        Map<String, String> mapping = new LinkedHashMap<>();
        mapping.put("ventas.facturas.facturaA4.jasper", "ventas/facturas/facturaA4.jasper");
        mapping.put("ventas.facturas.facturaA4_es.jasper", "ventas/facturas/facturaA4.jasper");
        mapping.put("ventas.facturas.facturaA4_espanol.jasper", "ventas/facturas/facturaA4.jasper");
        mapping.put("ventas.facturas.facturaA4_spanish.jasper", "ventas/facturas/facturaA4.jasper");
        mapping.put("ventas.facturas.facturaA4_pt.jasper", "ventas/facturas/facturaA4_PT.jasper");
        mapping.put("ventas.facturas.facturaA4_PT.jasper", "ventas/facturas/facturaA4_PT.jasper");
        mapping.put("ventas.facturas.facturaA4_ca.jasper", "ventas/facturas/facturaA4_CA.jasper");
        mapping.put("ventas.facturas.facturaA4_CA.jasper", "ventas/facturas/facturaA4_CA.jasper");
        mapping.put("ventas.facturas.facturaA4_original.jasper", "ventas/facturas/facturaA4_Original.jasper");
        mapping.put("ventas.facturas.facturaA4_Original.jasper", "ventas/facturas/facturaA4_Original.jasper");
        return mapping;
    }
}
