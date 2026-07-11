package com.bananasandwich.bananaclaims.localization;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Server-resolved language messages for Banana Claims.
 *
 * <p>Banana Claims does not require a client-side installation, so custom
 * translation keys cannot safely be sent to vanilla clients. This service
 * resolves bundled language entries on the server and sends ordinary literal
 * components, preserving server-side-only compatibility.</p>
 */
public final class BananaClaimsMessages {

    public static final String DEFAULT_LANGUAGE = "en_us";

    private static final String LANGUAGE_RESOURCE =
            "assets/bananaclaims/lang/" + DEFAULT_LANGUAGE + ".json";

    private static final Gson GSON = new Gson();

    private static final Type MESSAGE_MAP_TYPE =
            new TypeToken<Map<String, String>>() {
            }.getType();

    private static final Map<String, String> MESSAGES = loadMessages();

    private BananaClaimsMessages() {
    }

    public static Component text(
            String key,
            Object... arguments
    ) {
        return Component.literal(
                string(key, arguments)
        );
    }

    public static String string(
            String key,
            Object... arguments
    ) {
        String template = MESSAGES.get(key);

        if (template == null) {
            Bananaclaims.LOGGER.warn(
                    "Missing Banana Claims language key '{}'.",
                    key
            );
            template = key;
        }

        if (arguments == null || arguments.length == 0) {
            return template;
        }

        Object[] normalizedArguments =
                new Object[arguments.length];

        for (int index = 0;
             index < arguments.length;
             index++) {
            Object argument = arguments[index];

            normalizedArguments[index] =
                    argument instanceof Component component
                            ? component.getString()
                            : argument;
        }

        try {
            return String.format(
                    Locale.ROOT,
                    template,
                    normalizedArguments
            );
        } catch (IllegalArgumentException exception) {
            Bananaclaims.LOGGER.error(
                    "Invalid Banana Claims language format for key '{}'.",
                    key,
                    exception
            );

            return template;
        }
    }

    public static boolean contains(String key) {
        return key != null && MESSAGES.containsKey(key);
    }

    public static int size() {
        return MESSAGES.size();
    }

    private static Map<String, String> loadMessages() {
        ClassLoader classLoader =
                BananaClaimsMessages.class.getClassLoader();

        try (InputStream stream =
                     classLoader.getResourceAsStream(
                             LANGUAGE_RESOURCE
                     )) {
            if (stream == null) {
                Bananaclaims.LOGGER.error(
                        "Banana Claims language resource '{}' was not found.",
                        LANGUAGE_RESOURCE
                );
                return Map.of();
            }

            try (InputStreamReader reader =
                         new InputStreamReader(
                                 stream,
                                 StandardCharsets.UTF_8
                         )) {
                Map<String, String> loaded =
                        GSON.fromJson(
                                reader,
                                MESSAGE_MAP_TYPE
                        );

                if (loaded == null || loaded.isEmpty()) {
                    Bananaclaims.LOGGER.error(
                            "Banana Claims language resource '{}' was empty.",
                            LANGUAGE_RESOURCE
                    );
                    return Map.of();
                }

                Map<String, String> sanitized =
                        new LinkedHashMap<>();

                for (Map.Entry<String, String> entry
                        : loaded.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();

                    if (key == null
                            || key.isBlank()
                            || value == null) {
                        continue;
                    }

                    sanitized.put(key, value);
                }

                return Collections.unmodifiableMap(sanitized);
            }
        } catch (IOException | JsonParseException exception) {
            Bananaclaims.LOGGER.error(
                    "Failed to load Banana Claims language resource '{}'.",
                    LANGUAGE_RESOURCE,
                    exception
            );

            return Map.of();
        }
    }
}
