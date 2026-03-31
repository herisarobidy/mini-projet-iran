package com.project.pure.template;

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * Minimal support for Spring-style templates using th:field="*{...}" without thymeleaf-spring.
 *
 * Goal: ensure HTML forms have proper name/value/checked attributes so browsers submit fields.
 */
public class FieldAttributeTagProcessor extends AbstractAttributeTagProcessor {

    private static final String ATTR_NAME = "field";
    private static final int PRECEDENCE = 1000;

    public FieldAttributeTagProcessor(String dialectPrefix) {
        super(TemplateMode.HTML, dialectPrefix, null, false, ATTR_NAME, true, PRECEDENCE, true);
    }

    @Override
    protected void doProcess(ITemplateContext context,
                             IProcessableElementTag tag,
                             AttributeName attributeName,
                             String attributeValue,
                             IElementTagStructureHandler structureHandler) {

        String expr = attributeValue == null ? "" : attributeValue.trim();
        String fieldName = extractFieldName(expr);
        if (fieldName == null || fieldName.isBlank()) {
            structureHandler.removeAttribute(attributeName);
            return;
        }

        Object fieldValue = evaluateFieldValue(context, expr);

        String elementName = tag.getElementCompleteName();
        String type = tag.getAttributeValue("type");

        // Always ensure name exists
        structureHandler.setAttribute("name", fieldName);

        if ("input".equalsIgnoreCase(elementName) && type != null && type.equalsIgnoreCase("checkbox")) {
            boolean checked = false;
            if (fieldValue instanceof Boolean b) {
                checked = b;
            } else if (fieldValue instanceof String s) {
                checked = Boolean.parseBoolean(s);
            }
            if (checked) {
                structureHandler.setAttribute("checked", "checked");
            } else {
                structureHandler.removeAttribute("checked");
            }
            // If template didn't specify a value, browsers send "on" for checked.
        } else if ("input".equalsIgnoreCase(elementName)) {
            if (fieldValue != null) {
                structureHandler.setAttribute("value", String.valueOf(fieldValue));
            }
        } else if ("textarea".equalsIgnoreCase(elementName)) {
            // Basic: set body text. This overrides existing body.
            structureHandler.setBody(fieldValue == null ? "" : String.valueOf(fieldValue), false);
        } else if ("select".equalsIgnoreCase(elementName)) {
            // Only set name so the selected option is submitted.
            // We do not auto-select options here; templates usually rely on th:field for that.
        }

        structureHandler.removeAttribute(attributeName);
    }

    private static String extractFieldName(String expr) {
        // Accept *{name} or ${...} etc; we mainly support *{...}
        int open = expr.indexOf('{');
        int close = expr.lastIndexOf('}');
        if (open < 0 || close <= open) {
            return null;
        }
        String inside = expr.substring(open + 1, close).trim();
        if (inside.startsWith("*") || inside.startsWith("$") || inside.startsWith("#")) {
            // not expected, but leave as-is
        }
        return inside;
    }

    private static Object evaluateFieldValue(ITemplateContext context, String expr) {
        try {
            var parser = StandardExpressions.getExpressionParser(context.getConfiguration());
            var expression = parser.parseExpression(context, expr);
            return expression.execute(context);
        } catch (Exception ignored) {
            return null;
        }
    }
}
