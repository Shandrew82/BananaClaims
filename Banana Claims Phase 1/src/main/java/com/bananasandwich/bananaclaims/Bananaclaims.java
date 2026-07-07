package com.bananasandwich.bananaclaims;

import com.bananasandwich.bananaclaims.claim.ClaimManager;
import com.bananasandwich.bananaclaims.commands.ClaimCommands;
import com.bananasandwich.bananaclaims.events.ClaimEnterLeaveEvents;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bananaclaims implements ModInitializer {
	public static final String MOD_ID = "bananaclaims";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ClaimManager.load();
		ClaimCommands.register();
		ClaimEnterLeaveEvents.register();
		LOGGER.info("Banana Claims loaded.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
