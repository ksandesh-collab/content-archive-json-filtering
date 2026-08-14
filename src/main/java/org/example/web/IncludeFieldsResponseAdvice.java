package org.example.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.controller.PublicationController;
import org.example.util.JsonFieldFilter;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.List;

/**
 * Projects any response body down to the fields requested via
 * includeOutputField, applied right before Jackson serializes the body.
 * Controllers stay typed (e.g. return List&lt;Publication&gt;) and stash the
 * requested fields as a request attribute; this advice is the single place
 * that converts the body to a JsonNode tree and runs it through
 * {@link JsonFieldFilter}, so the filtering behavior is not duplicated
 * across controllers.
 */
@ControllerAdvice
public class IncludeFieldsResponseAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    public IncludeFieldsResponseAdvice(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                   Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                   ServerHttpRequest request, ServerHttpResponse response) {

        if (body == null || !(request instanceof ServletServerHttpRequest servletRequest)) {
            return body;
        }
        Object fieldsAttribute = servletRequest.getServletRequest()
                .getAttribute(PublicationController.INCLUDE_OUTPUT_FIELD_ATTRIBUTE);
        if (!(fieldsAttribute instanceof List<?> fields) || fields.isEmpty()) {
            return body;
        }

        JsonNode tree = objectMapper.valueToTree(body);
        return JsonFieldFilter.filter(tree, (List<String>) fields);
    }
}
