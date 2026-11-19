package com.knowledgepixels.nanodash.template;

import org.nanopub.Nanopub;
import org.nanopub.templates.ComplianceIssue;
import org.nanopub.templates.ComplianceReport;
import org.nanopub.templates.NanopubResolver;
import org.nanopub.templates.TemplateComplianceChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks the nanopublications the publishing form makes against their templates, with the
 * checker of nanopub-java, and logs what it finds.
 * <p>
 * The form fills a template with its own code, and nanopub-java has code of its own for what
 * a template means. As long as there are these two, they can come to disagree. This check
 * makes such a disagreement visible in the log. It decides nothing: a nanopublication is
 * made and published whatever is found, and a failure of the check itself is only logged.
 */
public class TemplateComplianceGuard {

    private static final Logger logger = LoggerFactory.getLogger(TemplateComplianceGuard.class);

    private TemplateComplianceGuard() {
    }

    /**
     * Checks a nanopublication the form has made, and logs the findings.
     *
     * @param nanopub the nanopublication, signed or not
     * @return the findings, or null if the check itself failed
     */
    public static ComplianceReport check(Nanopub nanopub) {
        return check(nanopub, TemplateData.RESOLVER);
    }

    static ComplianceReport check(Nanopub nanopub, NanopubResolver resolver) {
        try {
            long start = System.currentTimeMillis();
            ComplianceReport report = new TemplateComplianceChecker(resolver).check(nanopub);
            for (ComplianceIssue issue : report.getIssues()) {
                if (issue.isError()) {
                    logger.warn("Template compliance: {} made by the form does not comply with its templates: {}", nanopub.getUri(), issue.message());
                } else {
                    logger.info("Template compliance: note on {}: {}", nanopub.getUri(), issue.message());
                }
            }
            // One line for each nanopublication, so that the log also shows that the check ran.
            logger.info("Template compliance: checked {} in {} ms: {} error(s), {} note(s)", nanopub.getUri(), System.currentTimeMillis() - start,
                    report.getErrors().size(), report.getWarnings().size());
            return report;
        } catch (Exception | LinkageError ex) {
            logger.warn("Template compliance: could not check {}: {}", nanopub == null ? null : nanopub.getUri(), ex.toString());
            return null;
        }
    }

}
