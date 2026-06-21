package com.shareway.infrastructure.adapter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ArrayToStringDeserializer extends JsonDeserializer<String> {
    @Override
    public String deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        if (p.currentToken() == JsonToken.START_ARRAY) {
            List<String> values = new ArrayList<>();
            while (p.nextToken() != JsonToken.END_ARRAY) {
                values.add(p.getText());
            }
            return String.join(",", values);
        }
        return p.getText();
    }
}