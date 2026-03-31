package com.project.pure.template;

import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.processor.IProcessor;

import java.util.Set;

public class FieldDialect extends AbstractProcessorDialect {

    public FieldDialect() {
        super("Pure Field Dialect", "th", 1000);
    }

    @Override
    public Set<IProcessor> getProcessors(String dialectPrefix) {
        return Set.of(new FieldAttributeTagProcessor(dialectPrefix));
    }
}
