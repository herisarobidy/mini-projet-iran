package com.project.pure.template;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.thymeleaf.standard.serializer.IStandardJavaScriptSerializer;

import java.io.Writer;

public class JacksonJavaScriptSerializer implements IStandardJavaScriptSerializer {

    private final ObjectMapper mapper;

    public JacksonJavaScriptSerializer() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void serializeValue(Object object, Writer writer) {
        try {
            writer.write(mapper.writeValueAsString(object));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
