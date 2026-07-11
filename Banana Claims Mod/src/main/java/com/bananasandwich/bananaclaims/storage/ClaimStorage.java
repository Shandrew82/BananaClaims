package com.bananasandwich.bananaclaims.storage;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.claim.Claim;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Persistent claim storage with safe reload and atomic replacement semantics.
 */
public final class ClaimStorage {

    private static final String CLAIMS_FILE_NAME = "claims.json";

    private static final Gson GSON =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .disableHtmlEscaping()
                    .create();

    private final Path claimsFile;

    public ClaimStorage() {
        this.claimsFile =
                FabricLoader.getInstance()
                        .getConfigDir()
                        .resolve("bananaclaims")
                        .resolve(CLAIMS_FILE_NAME);
    }

    public List<Claim> loadClaims() {
        return tryLoadClaims()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Banana Claims could not load claim data from "
                                        + claimsFile
                                        + ". Startup was stopped to prevent an empty claim set from overwriting recoverable data."
                        )
                );
    }

    /**
     * Attempts to load claim data without replacing the caller's current
     * in-memory state when the file cannot be read or parsed.
     */
    public Optional<List<Claim>> tryLoadClaims() {
        if (!Files.exists(claimsFile)) {
            return Optional.of(new ArrayList<>());
        }

        try (BufferedReader reader =
                     Files.newBufferedReader(
                             claimsFile,
                             StandardCharsets.UTF_8
                     )) {
            ClaimData data =
                    GSON.fromJson(
                            reader,
                            ClaimData.class
                    );

            if (data == null || data.claims == null) {
                return Optional.of(new ArrayList<>());
            }

            return Optional.of(
                    new ArrayList<>(data.claims)
            );
        } catch (IOException | JsonParseException exception) {
            Bananaclaims.LOGGER.error(
                    "Failed to load Banana Claims data from '{}'. The file was not modified.",
                    claimsFile,
                    exception
            );

            return Optional.empty();
        }
    }

    public Path getClaimsFile() {
        return claimsFile;
    }

    public synchronized void saveClaims(
            List<Claim> claims
    ) {
        try {
            Files.createDirectories(
                    claimsFile.getParent()
            );

            ClaimData data = new ClaimData();
            data.claims =
                    claims == null
                            ? List.of()
                            : List.copyOf(claims);

            Path temporaryFile =
                    claimsFile.resolveSibling(
                            CLAIMS_FILE_NAME + ".tmp"
                    );

            try (BufferedWriter writer =
                         Files.newBufferedWriter(
                                 temporaryFile,
                                 StandardCharsets.UTF_8
                         )) {
                GSON.toJson(data, writer);
            }

            try {
                Files.move(
                        temporaryFile,
                        claimsFile,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (IOException atomicMoveFailure) {
                Files.move(
                        temporaryFile,
                        claimsFile,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
        } catch (IOException exception) {
            Bananaclaims.LOGGER.error(
                    "Failed to save Banana Claims data to '{}'.",
                    claimsFile,
                    exception
            );
        }
    }

    private static final class ClaimData {
        private List<Claim> claims = new ArrayList<>();
    }
}
