package com.comerzzia.bricodepot.api.omnichannel.api.services.documentprint;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.comerzzia.api.core.service.exception.ApiException;
import com.comerzzia.bricodepot.backoffice.services.ventas.facturas.CargarFacturaA4Servicio;
import com.comerzzia.core.util.config.AppInfo;
import com.comerzzia.core.servicios.sesion.IDatosSesion;
import com.comerzzia.omnichannel.domain.dto.saledoc.PrintDocumentDTO;
import com.comerzzia.omnichannel.service.documentprint.jasper.JasperPrintServiceImpl;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

@Service
@Primary
public class BricodepotDocumentPrintService extends JasperPrintServiceImpl {

        private static final Logger LOGGER = LoggerFactory.getLogger(BricodepotDocumentPrintService.class);
        private static final Set<String> COMPILED_TEMPLATES = ConcurrentHashMap.newKeySet();
        private static final ConcurrentMap<String, File> TEMPLATE_RESOLUTION_CACHE = new ConcurrentHashMap<>();

        private final CargarFacturaA4Servicio cargarFacturaA4Servicio;

        @Autowired
        public BricodepotDocumentPrintService(CargarFacturaA4Servicio cargarFacturaA4Servicio) {
                this.cargarFacturaA4Servicio = cargarFacturaA4Servicio;
        }

        @Override
        protected Map<String, Object> generateDocParameters(IDatosSesion datosSesion, PrintDocumentDTO printRequest)
                        throws ApiException {
                enrichTicketPayments(printRequest);
                return super.generateDocParameters(datosSesion, printRequest);
        }

        private void enrichTicketPayments(PrintDocumentDTO printRequest) {
                if (cargarFacturaA4Servicio == null || printRequest == null) {
                        return;
                }

                Map<String, Object> customParams = printRequest.getCustomParams();
                if (customParams == null || customParams.isEmpty()) {
                        return;
                }

                Object ticket = customParams.get("ticket");
                if (ticket == null) {
                        return;
                }

                try {
                        cargarFacturaA4Servicio.generarMediosPago(ticket);
                }
                catch (Exception exception) {
                        LOGGER.warn("enrichTicketPayments() - Unable to enrich ticket payments", exception);
                }
        }

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
			JasperDesign design = loadPatchedDesign(jrxmlFile);
			JasperCompileManager.compileReportToFile(design, jasperFile.getAbsolutePath());
			COMPILED_TEMPLATES.add(absolutePath);
			compileSiblingTemplates(jrxmlFile.getParentFile(), jrxmlFile);
		}
		catch (JRException exception) {
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
                        String jasperPath = jasperFile.getAbsolutePath();
                        if (COMPILED_TEMPLATES.contains(jasperPath)) {
                                continue;
                        }

                        ensureParentDirectory(jasperFile);

                        boolean compiled = false;
                        try {
                                JasperDesign design = loadPatchedDesign(jrxmlFile);
                                JasperCompileManager.compileReportToFile(design, jasperPath);
                                compiled = true;
                        }
                        catch (JRException exception) {
                                LOGGER.warn("Failed to compile Jasper subreport '{}'", jrxmlFile.getAbsolutePath(), exception);
                        }
                        finally {
                                if (compiled) {
                                        COMPILED_TEMPLATES.add(jasperPath);
                                }
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
		}
		else {
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
		if (candidate.exists()) {
			return candidate;
		}

		if (template.contains("/")) {
			String normalizedTemplate = template.replace('/', File.separatorChar);
			File normalizedCandidate = buildTemplateCandidate(basePath + normalizedTemplate, localeId);
			if (normalizedCandidate.exists()) {
				return normalizedCandidate;
			}
		}
		else {
			File resolved = locateTemplateInReportsDirectory(basePath, template, localeId);
			if (resolved != null) {
				return resolved;
			}
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

	private File locateTemplateInReportsDirectory(String basePath, String template, String localeId) {
		if (StringUtils.isBlank(basePath) || StringUtils.isBlank(template)) {
			return null;
		}

		File baseDirectory = new File(basePath);
		if (!baseDirectory.isDirectory()) {
			return null;
		}

		for (String candidateName : buildCandidateNames(template, localeId)) {
			File cached = TEMPLATE_RESOLUTION_CACHE.get(candidateName);
			if (cached != null) {
				if (cached.exists()) {
					return cached;
				}
				TEMPLATE_RESOLUTION_CACHE.remove(candidateName);
			}

			File located = searchTemplateRecursively(baseDirectory, candidateName);
			if (located != null) {
				TEMPLATE_RESOLUTION_CACHE.put(candidateName, located);
				return located;
			}
		}

		return null;
	}

	private List<String> buildCandidateNames(String template, String localeId) {
		String extension = getTemplateExtension();
		List<String> candidates = new ArrayList<>(3);
		String locale = localeId != null ? localeId.toLowerCase(Locale.ROOT) : null;

		if (StringUtils.isNotBlank(locale)) {
			candidates.add(template + "_" + locale + extension);
			if (locale.length() >= 2) {
				candidates.add(template + "_" + StringUtils.left(locale, 2) + extension);
			}
		}

		candidates.add(template + extension);
		return candidates;
	}

	private File searchTemplateRecursively(File baseDirectory, String candidateName) {
		try (Stream<Path> paths = Files.walk(baseDirectory.toPath())) {
			return paths.filter(Files::isRegularFile).filter(path -> candidateName.equalsIgnoreCase(path.getFileName().toString())).findFirst().map(Path::toFile).orElse(null);
		}
		catch (IOException exception) {
			LOGGER.warn("locateTemplateInReportsDirectory() - Unable to resolve template '{}' under '{}'", candidateName, baseDirectory.getAbsolutePath(), exception);
			return null;
		}
	}

	private static final class JrxmlFilter implements FileFilter {

		@Override
		public boolean accept(File pathname) {
			return pathname.isFile() && StringUtils.endsWithIgnoreCase(pathname.getName(), ".jrxml");
		}
	}

	private JasperDesign loadPatchedDesign(File jrxmlFile) throws JRException {
		if (jrxmlFile == null) {
			throw new JRException("JRXML file is null");
		}

		try {
			byte[] bytes = Files.readAllBytes(jrxmlFile.toPath());
			String original = new String(bytes, StandardCharsets.UTF_8);
			String patched = applyTemplatePatches(jrxmlFile, original);
			try (InputStream inputStream = new ByteArrayInputStream(patched.getBytes(StandardCharsets.UTF_8))) {
				return JRXmlLoader.load(inputStream);
			}
		}
		catch (IOException exception) {
			LOGGER.warn("loadPatchedDesign() - Unable to read template '{}' while compiling", jrxmlFile.getAbsolutePath(), exception);
			return JRXmlLoader.load(jrxmlFile);
		}
	}

        private String applyTemplatePatches(File jrxmlFile, String content) {
                if (content == null) {
                        return "";
                }

                String result = content;
                result = normalizeTicketParameterType(result);
                result = patchTicketDateFormatting(result);
                result = patchDesgloseExpressions(result);
                
                if (isPortugueseTemplate(jrxmlFile)) {
                        result = patchAtcudExpressions(result);
                }

		if (requiresCodImpPatch(jrxmlFile)) {
			result = patchCodImpField(result);
		}

		return result;
	}

        private boolean isPortugueseTemplate(File jrxmlFile) {
                if (jrxmlFile == null) {
                        return false;
                }
                String name = jrxmlFile.getName();
                return "facturaA4_PT.jrxml".equalsIgnoreCase(name) || "facturaDevolucionA4_PT.jrxml".equalsIgnoreCase(name);
        }

        private boolean requiresCodImpPatch(File jrxmlFile) {
                if (jrxmlFile == null) {
                        return false;
                }
                return COD_IMP_PATCH_TEMPLATES.contains(jrxmlFile.getName());
        }

	private String normalizeTicketParameterType(String content) {
		if (StringUtils.isBlank(content)) {
			return content;
		}

		Matcher matcher = TICKET_PARAMETER_PATTERN.matcher(content);
		StringBuffer buffer = new StringBuffer(content.length());
		boolean updated = false;

                while (matcher.find()) {
                        String currentClass = matcher.group(2);
                        if (!NEW_TICKET_PARAMETER_CLASS.equals(currentClass)) {
                                matcher.appendReplacement(buffer, matcher.group(1)
                                        + Matcher.quoteReplacement(NEW_TICKET_PARAMETER_CLASS) + matcher.group(3));
                                updated = true;
                        }
                        else {
                                matcher.appendReplacement(buffer, matcher.group(0));
                        }
                }

                if (updated) {
                        matcher.appendTail(buffer);
                        return buffer.toString();
                }

                return content;
        }

	private String patchAtcudExpressions(String content) {
		if (StringUtils.isBlank(content)) {
			return content;
		}

		String result = content;
                if (!result.contains("name=\"fiscalData_ACTUD\"")) {
                        result = injectFiscalParameters(result);
                }
                else if (!result.contains("name=\"fiscalData_QR\"")) {
                        result = result.replace("name=\"fiscalData_ACTUD\" class=\"java.lang.String\"/>",
                                "name=\"fiscalData_ACTUD\" class=\"java.lang.String\"/>\n        <parameter name=\"fiscalData_QR\" class=\"java.lang.String\"/>");
                }

                result = PROPERTY_VALUE_PATTERN.matcher(result).replaceAll(FISCAL_PARAM_REPLACEMENT);
                result = PROPERTY_PATTERN.matcher(result).replaceAll(FISCAL_PARAM_REPLACEMENT);
                return result;
        }

	private String injectFiscalParameters(String content) {
		if (StringUtils.isBlank(content)) {
			return content;
		}

		String marker = "<parameter name=\"ticket\"";
                int markerIndex = content.indexOf(marker);
                if (markerIndex < 0) {
                        return content;
                }

                int insertIndex = content.indexOf('\n', markerIndex);
                if (insertIndex < 0) {
                        insertIndex = markerIndex + marker.length();
                }

                StringBuilder builder = new StringBuilder(content.length() + 160);
                builder.append(content, 0, insertIndex + 1);
                builder.append("        <parameter name=\"fiscalData_ACTUD\" class=\"java.lang.String\"/>\n");
                builder.append("        <parameter name=\"fiscalData_QR\" class=\"java.lang.String\"/>\n");
                builder.append(content.substring(insertIndex + 1));
                return builder.toString();
        }

        private String patchCodImpField(String content) {
                if (StringUtils.isBlank(content)) {
                        return content;
                }

                String patched = COD_IMP_FIELD_PATTERN.matcher(content).replaceAll("<![CDATA[codImpuesto]]>");
                patched = patched.replace("name=\"codImp\"", "name=\"codImpuesto\"");
                patched = patched.replace("$F{codImp", "$F{codImpuesto");
                return patched;
        }

        private String patchTicketDateFormatting(String content) {
                if (StringUtils.isBlank(content)) {
                        return content;
                }

                Matcher matcher = LEGACY_DATE_PATTERN.matcher(content);
                StringBuffer buffer = new StringBuffer(content.length());
                boolean updated = false;

                while (matcher.find()) {
                        String argument = matcher.group(1);
                        String replacement = "new java.text.SimpleDateFormat(\"dd/MM/yyyy\").format(" +
                                DATE_NORMALIZER_PREFIX + argument + "))";
                        matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
                        updated = true;
                }

                if (updated) {
                        matcher.appendTail(buffer);
                        return buffer.toString();
                }

                return content;
        }

        private String patchDesgloseExpressions(String content) {
                if (StringUtils.isBlank(content)) {
                        return content;
                }

                String result = DESGLOSE_PATTERN.matcher(content)
                        .replaceAll(Matcher.quoteReplacement(DESGLOSE_REPLACEMENT));
                result = DESGLOSE_ZERO_PATTERN.matcher(result)
                        .replaceAll("java.math.BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP)");
                return result;
        }

        private static final String PROPERTY_REGEX =
                "\\$P\\{ticket\\}\\.getCabecera\\(\\)\\.getFiscalData\\(\\)\\s*\\.getProperty\\(\"ATCUD\"\\)";
        private static final Pattern PROPERTY_PATTERN = Pattern.compile(PROPERTY_REGEX);
        private static final String PROPERTY_VALUE_REGEX =
                "\\$P\\{ticket\\}\\.getCabecera\\(\\)\\.getFiscalData\\(\\)\\s*\\.getProperty(?:Value)?\\(\"ATCUD\"\\)(?:\\s*\\.getValue\\(\\))?";
        private static final Pattern PROPERTY_VALUE_PATTERN = Pattern.compile(PROPERTY_VALUE_REGEX);
        private static final String FISCAL_PARAM_REPLACEMENT = Matcher.quoteReplacement("$P{fiscalData_ACTUD}");
        private static final Pattern TICKET_PARAMETER_PATTERN = Pattern.compile(
                "(<parameter\\s+name=\\\"ticket\\\"\\s+class=\\\")(.*?)(\\\"\\s*/>)"
        );
        private static final String NEW_TICKET_PARAMETER_CLASS =
                "com.comerzzia.omnichannel.model.documents.sales.FT_1_1_Document";
        private static final Pattern LEGACY_DATE_PATTERN = Pattern.compile(
                "new java\\.text\\.SimpleDateFormat\\(\"dd/MM/yyyy\"\\)\\.format\\(new java\\.text\\.SimpleDateFormat\\(\"yyyy-MM-dd'T'HH:mm:ss\"\\)\\.parse\\(\\((.+?)\\)\\)\\)",
                Pattern.DOTALL
        );
        private static final String DATE_NORMALIZER_PREFIX =
                "com.comerzzia.bricodepot.api.omnichannel.api.services.documentprint.BricodepotDocumentPrintService.normalizeDate(";
        private static final Pattern DESGLOSE_PATTERN = Pattern.compile(
                "new java\\.math\\.BigDecimal\\(\\$F\\{desglose2\\}\\.toString\\(\\)\\.replace\\(\",\", \\\".\\\"\\)\\)\\.setScale\\(2, java\\.math\\.RoundingMode\\.HALF_UP\\)"
        );
        private static final String DESGLOSE_REPLACEMENT =
                "com.comerzzia.bricodepot.api.omnichannel.api.services.documentprint.BricodepotDocumentPrintService.safeBigDecimal($F{desglose2}).setScale(2, java.math.RoundingMode.HALF_UP)";
        private static final Pattern DESGLOSE_ZERO_PATTERN = Pattern.compile(
                "new java\\.math\\.BigDecimal\\(\"0\"\\)\\.setScale\\(2, java\\.math\\.RoundingMode\\.HALF_UP\\)"
        );
        private static final Set<String> COD_IMP_PATCH_TEMPLATES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                "SubInfLineas.jrxml",
                "SubInfLineas_CA.jrxml",
                "SubInfLineas_PT.jrxml",
                "SubInfImportes.jrxml",
                "SubInfImportes_CA.jrxml",
                "SubInfImportes_PT.jrxml"
        )));
        private static final Pattern COD_IMP_FIELD_PATTERN = Pattern.compile("<!\\[CDATA\\[codImp\\]\\]>");

        public static Date normalizeDate(Object value) {
                if (value instanceof Date) {
                        return (Date) value;
                }
                if (value == null) {
                        return new Date();
                }

                String text = value.toString();
                for (String pattern : DATE_PATTERNS) {
                        try {
                                return new SimpleDateFormat(pattern).parse(text);
                        }
                        catch (ParseException exception) {
                                // try the next pattern
                        }
                }

                LOGGER.debug("normalizeDate() - Unable to parse date '{}' with supported patterns", text);
                return new Date();
        }

        public static BigDecimal safeBigDecimal(Object value) {
                if (value == null) {
                        return BigDecimal.ZERO;
                }
                if (value instanceof BigDecimal) {
                        return (BigDecimal) value;
                }

                String text = value.toString();
                if (StringUtils.isBlank(text)) {
                        return BigDecimal.ZERO;
                }

                String sanitized = text.trim().replace('*', '0').replace(',', '.');
                try {
                        return new BigDecimal(sanitized);
                }
                catch (NumberFormatException exception) {
                        LOGGER.debug("safeBigDecimal() - Unable to parse '{}', defaulting to zero", sanitized, exception);
                        return BigDecimal.ZERO;
                }
        }

        private static final List<String> DATE_PATTERNS = Arrays.asList(
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss",
                "dd/MM/yyyy"
        );
}
