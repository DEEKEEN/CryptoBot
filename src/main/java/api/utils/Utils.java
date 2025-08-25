package api.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class Utils {
    public static ObjectMapper objectMapper = new ObjectMapper();

    public static <T> T convertOneModelToAnother(Object model, Class<T> clazz) {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper.convertValue(model, clazz);
    }

    public static <T> T convertStringToPOJO(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static JsonNode convertStringToJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static <T> T convertJsonToPOJO(JsonNode json, Class<T> clazz) {
        try {
            return objectMapper.treeToValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static <T> String convertPOJOToString(T clazz) {
        try {
            return objectMapper.writer().writeValueAsString(clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static <E> String convertPOJOListToString(List<E> array) {
        try {
            return objectMapper.writer().writeValueAsString(array);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static <T> T convertJsonStringToModel(String jsonString, TypeReference<T> typeRef) {
        try {
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return objectMapper.readValue(jsonString, typeRef);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert JSON string to model", e);
        }
    }
}
