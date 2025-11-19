package net.cjcrispy.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.cjcrispy.BookOfE;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class SlimeNameConfig {
	private static final Identifier NAME_CONFIG_ID = Identifier.of("bookofe", "slime_names.json");
	
	private static List<String> commonerNames = new ArrayList<>();
	private static List<String> mageNames = new ArrayList<>();
	private static List<String> warriorNames = new ArrayList<>();
	
	private static boolean loaded = false;
	
	/**
	 * Loads the slime names from the JSON config file.
	 * Should be called during mod initialization.
	 */
	public static void load() {
		try {
			// Try to load from resources
			InputStream stream = SlimeNameConfig.class.getClassLoader()
				.getResourceAsStream("data/" + NAME_CONFIG_ID.getNamespace() + "/" + NAME_CONFIG_ID.getPath());
			
			if (stream == null) {
				BookOfE.LOGGER.warn("Could not find slime names config file, using defaults");
				loadDefaults();
				return;
			}
			
			JsonObject json = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();
			
			commonerNames.clear();
			mageNames.clear();
			warriorNames.clear();
			
			if (json.has("commoner") && json.get("commoner").isJsonArray()) {
				for (JsonElement element : json.getAsJsonArray("commoner")) {
					commonerNames.add(element.getAsString());
				}
			}
			
			if (json.has("mage") && json.get("mage").isJsonArray()) {
				for (JsonElement element : json.getAsJsonArray("mage")) {
					mageNames.add(element.getAsString());
				}
			}
			
			if (json.has("warrior") && json.get("warrior").isJsonArray()) {
				for (JsonElement element : json.getAsJsonArray("warrior")) {
					warriorNames.add(element.getAsString());
				}
			}
			
			stream.close();
			
			// Fallback to defaults if any list is empty
			if (commonerNames.isEmpty()) {
				BookOfE.LOGGER.warn("No commoner names found, using defaults");
				loadDefaults();
			} else if (mageNames.isEmpty()) {
				BookOfE.LOGGER.warn("No mage names found, using defaults");
				loadDefaults();
			} else if (warriorNames.isEmpty()) {
				BookOfE.LOGGER.warn("No warrior names found, using defaults");
				loadDefaults();
			} else {
				loaded = true;
				BookOfE.LOGGER.info("Loaded {} commoner, {} mage, and {} warrior names", 
					commonerNames.size(), mageNames.size(), warriorNames.size());
			}
			
		} catch (Exception e) {
			BookOfE.LOGGER.error("Failed to load slime names config", e);
			loadDefaults();
		}
	}
	
	private static void loadDefaults() {
		if (commonerNames.isEmpty()) {
			commonerNames.add("Commoner");
		}
		if (mageNames.isEmpty()) {
			mageNames.add("Mage");
		}
		if (warriorNames.isEmpty()) {
			warriorNames.add("Warrior");
		}
		loaded = true;
	}
	
	/**
	 * Gets a random name for a commoner slime.
	 */
	public static String getRandomCommonerName(Random random) {
		if (commonerNames.isEmpty()) {
			return "Commoner";
		}
		return commonerNames.get(random.nextInt(commonerNames.size()));
	}
	
	/**
	 * Gets a random name for a mage slime.
	 */
	public static String getRandomMageName(Random random) {
		if (mageNames.isEmpty()) {
			return "Mage";
		}
		return mageNames.get(random.nextInt(mageNames.size()));
	}
	
	/**
	 * Gets a random name for a warrior slime.
	 */
	public static String getRandomWarriorName(Random random) {
		if (warriorNames.isEmpty()) {
			return "Warrior";
		}
		return warriorNames.get(random.nextInt(warriorNames.size()));
	}
	
	/**
	 * Checks if the config has been loaded.
	 */
	public static boolean isLoaded() {
		return loaded;
	}
	
	private SlimeNameConfig() {}
}

