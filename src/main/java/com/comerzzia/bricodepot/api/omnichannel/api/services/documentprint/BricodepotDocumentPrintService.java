package com.comerzzia.bricodepot.api.omnichannel.api.services.documentprint;

import java.io.File;
import java.io.FileFilter;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.comerzzia.core.util.config.AppInfo;
import com.comerzzia.omnichannel.service.documentprint.jasper.JasperPrintServiceImpl;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;

@Service
@Primary
public class BricodepotDocumentPrintService extends JasperPrintServiceImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(BricodepotDocumentPrintService.class);
    private static final Set<String> COMPILED_TEMPLATES = ConcurrentHashMap.newKeySet();

    @Override
    protected File getTemplateLocaleFile(String template, String localeId) {
        File templateFile = super.getTemplateLocaleFile(template, localeId);
        compileTemplateIfNecessary(templateFile);
        if (templateFile.exists()) {
            return templateFile;
        }

        File alternative = resolveFromReportsDirectory(template, localeId);
        compileTemplateIfNecessary(alternative);
        if (alternative.exists()) {
            return alternative;
        }

        return templateFile;
    }

    private void compileTemplateIfNecessary(File jasperFile) {
        if (jasperFile == null) {
            return;
        }

        String absolutePath = jasperFile.getAbsolutePath();
        if (COMPILED_TEMPLATES.contains(absolutePath)) {
            return;
        }

        File jrxmlFile = toJrxmlFile(jasperFile);
        if (jrxmlFile == null || !jrxmlFile.exists()) {
            COMPILED_TEMPLATES.add(absolutePath);
            return;
        }

        ensureParentDirectory(jasperFile);

        try {
            JasperCompileManager.compileReportToFile(jrxmlFile.getAbsolutePath(), jasperFile.getAbsolutePath());
            COMPILED_TEMPLATES.add(absolutePath);
            compileSiblingTemplates(jrxmlFile.getParentFile(), jrxmlFile);
        } catch (JRException exception) {
            LOGGER.warn("Failed to compile Jasper template '{}' from '{}'", absolutePath, jrxmlFile.getAbsolutePath(), exception);
        }
    }

    private void compileSiblingTemplates(File directory, File primaryJrxml) {
        if (directory == null || !directory.isDirectory()) {
            return;
        }

        File[] jrxmlFiles = directory.listFiles(new JrxmlFilter());
        if (jrxmlFiles == null) {
            return;
        }

        for (File jrxmlFile : jrxmlFiles) {
            if (primaryJrxml != null && primaryJrxml.equals(jrxmlFile)) {
                continue;
            }
            File jasperFile = toJasperFile(jrxmlFile);
            if (jasperFile == null) {
                continue;
            }
            ensureParentDirectory(jasperFile);

            try {
                if (!jasperFile.exists() || jrxmlFile.lastModified() >= jasperFile.lastModified()) {
                    JasperCompileManager.compileReportToFile(jrxmlFile.getAbsolutePath(), jasperFile.getAbsolutePath());
                }
                COMPILED_TEMPLATES.add(jasperFile.getAbsolutePath());
            } catch (JRException exception) {
                LOGGER.warn("Failed to compile Jasper subreport '{}'", jrxmlFile.getAbsolutePath(), exception);
            }
        }
    }

    private void ensureParentDirectory(File jasperFile) {
        File parent = jasperFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }

    private File toJrxmlFile(File jasperFile) {
        String absolutePath = jasperFile != null ? jasperFile.getAbsolutePath() : null;
        if (StringUtils.isBlank(absolutePath)) {
            return null;
        }

        return new File(StringUtils.substringBeforeLast(absolutePath, ".") + ".jrxml");
    }

    private File toJasperFile(File jrxmlFile) {
        String absolutePath = jrxmlFile != null ? jrxmlFile.getAbsolutePath() : null;
        if (StringUtils.isBlank(absolutePath)) {
            return null;
        }

        String jasperPath;
        if (StringUtils.endsWithIgnoreCase(absolutePath, ".jrxml")) {
            jasperPath = StringUtils.substringBeforeLast(absolutePath, ".") + getTemplateExtension();
        } else {
            jasperPath = absolutePath + getTemplateExtension();
        }

        return new File(jasperPath);
    }

    private File resolveFromReportsDirectory(String template, String localeId) {
        String basePath = AppInfo.getInformesInfo().getRutaBase();
        if (StringUtils.isBlank(basePath)) {
            basePath = "";
        }

        if (!basePath.endsWith(File.separator)) {
            basePath = basePath + File.separator;
        }

        File candidate = buildTemplateCandidate(basePath + template, localeId);
        if (!candidate.exists() && template.contains("/")) {
            String normalizedTemplate = template.replace('/', File.separatorChar);
            candidate = buildTemplateCandidate(basePath + normalizedTemplate, localeId);
        }

        return candidate;
    }

    private File buildTemplateCandidate(String partialFileName, String localeId) {
        String locale = localeId != null ? localeId.toLowerCase(Locale.ROOT) : null;
        String extension = getTemplateExtension();
        File result = null;

        if (StringUtils.isNotBlank(locale)) {
            result = new File(partialFileName + "_" + locale + extension);
            if (!result.exists() && locale.length() >= 2) {
                result = new File(partialFileName + "_" + StringUtils.left(locale, 2) + extension);
            }
        }

        if (result == null || !result.exists()) {
            result = new File(partialFileName + extension);
        }

        return result;
    }

    private static final class JrxmlFilter implements FileFilter {
        @Override
        public boolean accept(File pathname) {
            return pathname.isFile() && StringUtils.endsWithIgnoreCase(pathname.getName(), ".jrxml");
        }
    }
}
