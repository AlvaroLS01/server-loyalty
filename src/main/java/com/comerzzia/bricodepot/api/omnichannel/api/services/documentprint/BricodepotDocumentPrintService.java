package com.comerzzia.bricodepot.api.omnichannel.api.services.documentprint;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.comerzzia.core.util.config.AppInfo;
import com.comerzzia.omnichannel.service.documentprint.DocumentPrintServiceImpl;

@Service
@Primary
public class BricodepotDocumentPrintService extends DocumentPrintServiceImpl {

    @Override
    protected File getTemplateLocaleFile(String template, String localeId) {
        File templateFile = super.getTemplateLocaleFile(template, localeId);
        if (templateFile.exists()) {
            return templateFile;
        }

        File alternative = resolveFromReportsDirectory(template, localeId);
        if (alternative.exists()) {
            return alternative;
        }

        return templateFile;
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
        String locale = localeId != null ? localeId.toLowerCase() : null;
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
}
