package com.bananasandwich.bananaclaims.storage;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.claim.Claim;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ClaimStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type CLAIM_LIST_TYPE = new TypeToken<List<Claim>>() {}.getType();
    private static final Path CLAIMS_FILE = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("bananaclaims")
            .resolve("claims.json");

    private ClaimStorage() {}

    public static List<Claim> loadClaims() {
        try {
            if (!Files.exists(CLAIMS_FILE)) {
                return new ArrayList<>();
            }
            String json = Files.readString(CLAIMS_FILE, StandardCharsets.UTF_8);
            List<Claim> claims = GSON.fromJson(json, CLAIM_LIST_TYPE);
            return claims == null ? new ArrayList<>() : claims;
        } catch (Exception ex) {
            Bananaclaims.LOGGER.error("Failed to load Banana Claims data", ex);
            return new ArrayList<>();
        }
    }

    public static void saveClaims(List<Claim> claims) {
        try {
            Files.createDirectories(CLAIMS_FILE.getParent());
            Files.writeString(CLAIMS_FILE, GSON.toJson(claims, CLAIM_LIST_TYPE), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            Bananaclaims.LOGGER.error("Failed to save Banana Claims data", ex);
        }
    }
}
