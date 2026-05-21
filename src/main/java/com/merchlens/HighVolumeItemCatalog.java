package com.merchlens;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class HighVolumeItemCatalog
{
	private static final String CATEGORY_MISC = "Misc";
	private static final List<String> CATEGORIES = Collections.unmodifiableList(Arrays.asList(
		"All",
		"Runes",
		"Ammo",
		"Food",
		"Potions",
		"Herbs",
		"Teleports",
		"PvM Supplies",
		"Ores/Bars",
		"Logs/Planks",
		"Crafting",
		"Prayer",
		"Farming",
		CATEGORY_MISC
	));
	private static final Map<String, String> CATALOG = new LinkedHashMap<>();

	static
	{
		add("Runes",
			"Fire rune", "Air rune", "Water rune", "Earth rune", "Mind rune", "Body rune",
			"Chaos rune", "Death rune", "Blood rune", "Nature rune", "Law rune", "Cosmic rune",
			"Astral rune", "Soul rune", "Wrath rune", "Dust rune", "Lava rune", "Smoke rune",
			"Steam rune", "Mist rune", "Mud rune", "Pure essence", "Rune essence", "Lizardman fang"
		);

		add("Ammo",
			"Cannonball", "Feather", "Arrow shaft", "Headless arrow", "Broad arrowheads",
			"Bronze arrow", "Iron arrow", "Steel arrow", "Mithril arrow", "Adamant arrow",
			"Rune arrow", "Amethyst arrow", "Dragon arrow", "Bronze arrowtips", "Iron arrowtips",
			"Steel arrowtips", "Mithril arrowtips", "Adamant arrowtips", "Rune arrowtips",
			"Amethyst arrowtips", "Dragon arrowtips", "Bronze dart", "Iron dart", "Steel dart",
			"Mithril dart", "Adamant dart", "Rune dart", "Amethyst dart", "Dragon dart",
			"Bronze dart tip", "Iron dart tip", "Steel dart tip", "Mithril dart tip",
			"Adamant dart tip", "Rune dart tip", "Amethyst dart tip", "Dragon dart tip",
			"Broad bolts", "Bone bolts", "Bronze bolts", "Iron bolts", "Steel bolts",
			"Mithril bolts", "Adamant bolts", "Runite bolts", "Opal bolts", "Opal bolts (e)",
			"Sapphire bolts", "Sapphire bolts (e)", "Emerald bolts", "Emerald bolts (e)",
			"Ruby bolts", "Ruby bolts (e)", "Diamond bolts", "Diamond bolts (e)",
			"Dragonstone bolts", "Dragonstone bolts (e)", "Onyx bolts", "Onyx bolts (e)",
			"Dragon bolts", "Dragon bolts (unf)", "Opal dragon bolts", "Opal dragon bolts (e)",
			"Sapphire dragon bolts", "Sapphire dragon bolts (e)", "Emerald dragon bolts",
			"Emerald dragon bolts (e)", "Ruby dragon bolts", "Ruby dragon bolts (e)",
			"Diamond dragon bolts", "Diamond dragon bolts (e)", "Dragonstone dragon bolts",
			"Dragonstone dragon bolts (e)", "Onyx dragon bolts", "Onyx dragon bolts (e)",
			"Bolt racks", "Rune knife", "Dragon knife", "Red chinchompa", "Black chinchompa"
		);

		add("Food",
			"Shark", "Raw shark", "Monkfish", "Raw monkfish", "Manta ray", "Raw manta ray",
			"Dark crab", "Raw dark crab", "Anglerfish", "Raw anglerfish", "Cooked karambwan",
			"Raw karambwan", "Karambwanji", "Tuna potato", "Jug of wine", "Grapes",
			"Lobster", "Raw lobster", "Swordfish", "Raw swordfish", "Tuna", "Raw tuna",
			"Sea turtle", "Raw sea turtle", "Pineapple pizza", "Plain pizza", "Anchovy pizza",
			"Cake", "Chocolate cake", "Summer pie", "Wild pie", "Admiral pie", "Garden pie",
			"Botanical pie", "Fish pie"
		);

		addDosed("Potions",
			"Prayer potion", "Super restore", "Saradomin brew", "Stamina potion",
			"Ranging potion", "Magic potion", "Super combat potion", "Divine super combat potion",
			"Bastion potion", "Battlemage potion", "Antifire potion", "Extended antifire",
			"Super antifire potion", "Extended super antifire", "Antidote++", "Anti-venom",
			"Anti-venom+", "Sanfew serum", "Energy potion", "Super energy", "Combat potion",
			"Super attack", "Super strength", "Super defence", "Divine ranging potion",
			"Divine magic potion", "Divine bastion potion", "Divine battlemage potion",
			"Weapon poison", "Weapon poison+", "Weapon poison++", "Guthix rest",
			"Zamorak brew", "Forgotten brew", "Ancient brew", "Menaphite remedy"
		);

		add("Herbs",
			"Vial", "Vial of water", "Grimy guam leaf", "Guam leaf", "Grimy marrentill",
			"Marrentill", "Grimy tarromin", "Tarromin", "Grimy harralander", "Harralander",
			"Grimy ranarr weed", "Ranarr weed", "Grimy irit leaf", "Irit leaf",
			"Grimy avantoe", "Avantoe", "Grimy kwuarm", "Kwuarm", "Grimy snapdragon",
			"Snapdragon", "Grimy cadantine", "Cadantine", "Grimy lantadyme", "Lantadyme",
			"Grimy dwarf weed", "Dwarf weed", "Grimy torstol", "Torstol", "Grimy toadflax",
			"Toadflax", "Guam potion (unf)", "Marrentill potion (unf)",
			"Tarromin potion (unf)", "Harralander potion (unf)", "Ranarr potion (unf)",
			"Irit potion (unf)", "Avantoe potion (unf)", "Kwuarm potion (unf)",
			"Snapdragon potion (unf)", "Cadantine potion (unf)", "Lantadyme potion (unf)",
			"Dwarf weed potion (unf)", "Torstol potion (unf)", "Toadflax potion (unf)",
			"Snape grass", "Limpwurt root", "Red spiders' eggs", "White berries",
			"Mort myre fungus", "Potato cactus", "Wine of zamorak", "Crushed nest",
			"Blue dragon scale", "Dragon scale dust", "Unicorn horn", "Unicorn horn dust",
			"Chocolate bar", "Chocolate dust", "Amylase crystal", "Jangerberries",
			"Magic roots", "Coconut milk", "Poison ivy berries", "Marrentill tar",
			"Tarromin tar", "Harralander tar", "Guam tar", "Swamp tar"
		);

		add("Teleports",
			"Teleport to house", "Varrock teleport", "Lumbridge teleport", "Falador teleport",
			"Camelot teleport", "Ardougne teleport", "Watchtower teleport", "Trollheim teleport",
			"Barrows teleport", "Revenant cave teleport", "Zul-andra teleport",
			"Fenkenstrain's castle teleport", "West ardougne teleport", "Rimmington teleport",
			"Taverley teleport", "Pollnivneach teleport", "Rellekka teleport",
			"Brimhaven teleport", "Yanille teleport", "Battlefront teleport",
			"Salve graveyard teleport", "Mind altar teleport", "Draynor manor teleport",
			"Lunar isle teleport", "Cemetery teleport", "Kharyrll teleport", "Lassar teleport",
			"Dareeyak teleport", "Carrallanger teleport", "Annakarl teleport", "Ghorrock teleport",
			"Ring of dueling(8)", "Games necklace(8)", "Skills necklace(6)",
			"Combat bracelet(6)", "Amulet of glory(6)", "Necklace of passage(5)",
			"Burning amulet(5)", "Ring of wealth (5)", "Slayer ring(8)"
		);

		add("PvM Supplies",
			"Zulrah's scales", "Revenant ether", "Numulite", "Sacred eel", "Bracelet of ethereum",
			"Blighted manta ray", "Blighted anglerfish", "Blighted karambwan",
			"Blighted super restore(4)", "Blighted ancient ice sack", "Blighted bind sack",
			"Blighted entangle sack", "Blighted snare sack", "Blighted vengeance sack",
			"Blighted teleport spell sack", "Dark fishing bait", "Cave nightshade"
		);

		add("Ores/Bars",
			"Coal", "Copper ore", "Tin ore", "Iron ore", "Silver ore", "Gold ore",
			"Mithril ore", "Adamantite ore", "Runite ore", "Lovakite ore", "Amethyst",
			"Bronze bar", "Iron bar", "Steel bar", "Silver bar", "Gold bar", "Mithril bar",
			"Adamantite bar", "Runite bar", "Lovakite bar"
		);

		add("Logs/Planks",
			"Logs", "Oak logs", "Willow logs", "Maple logs", "Yew logs", "Magic logs",
			"Redwood logs", "Teak logs", "Mahogany logs", "Achey logs", "Plank",
			"Oak plank", "Teak plank", "Mahogany plank", "Bow string", "Flax",
			"Oak longbow (u)", "Willow longbow (u)", "Maple longbow (u)", "Yew longbow (u)",
			"Magic longbow (u)", "Maple longbow", "Yew longbow", "Magic longbow"
		);

		add("Crafting",
			"Bucket of sand", "Soda ash", "Seaweed", "Giant seaweed", "Molten glass",
			"Unpowered orb", "Air orb", "Water orb", "Earth orb", "Fire orb", "Battlestaff",
			"Air battlestaff", "Water battlestaff", "Earth battlestaff", "Fire battlestaff",
			"Green dragonhide", "Blue dragonhide", "Red dragonhide", "Black dragonhide",
			"Green dragon leather", "Blue dragon leather", "Red dragon leather",
			"Black dragon leather", "Cowhide", "Soft leather", "Hard leather", "Thread",
			"Needle", "Uncut sapphire", "Uncut emerald", "Uncut ruby", "Uncut diamond",
			"Uncut dragonstone", "Sapphire", "Emerald", "Ruby", "Diamond", "Dragonstone",
			"Opal", "Jade", "Red topaz", "Opal bolt tips", "Sapphire bolt tips",
			"Emerald bolt tips", "Ruby bolt tips", "Diamond bolt tips", "Dragonstone bolt tips",
			"Onyx bolt tips", "Ring of recoil", "Amulet of chemistry"
		);

		add("Prayer",
			"Dragon bones", "Superior dragon bones", "Wyvern bones", "Dagannoth bones",
			"Big bones", "Baby dragon bones", "Wyrm bones", "Drake bones", "Hydra bones",
			"Lava dragon bones", "Blessed bone shards", "Vile ashes", "Fiendish ashes",
			"Malicious ashes", "Abyssal ashes", "Infernal ashes", "Ensouled goblin head",
			"Ensouled dragon head", "Ensouled bloodveld head", "Ensouled abyssal head",
			"Ensouled aviansie head", "Ensouled demon head", "Ensouled horror head",
			"Ensouled giant head"
		);

		add("Farming",
			"Ranarr seed", "Snapdragon seed", "Toadflax seed", "Avantoe seed", "Kwuarm seed",
			"Cadantine seed", "Lantadyme seed", "Dwarf weed seed", "Torstol seed",
			"Watermelon seed", "Strawberry seed", "Sweetcorn seed", "Limpwurt seed",
			"Whiteberry seed", "Snape grass seed", "Potato cactus seed", "Poison ivy seed",
			"Papaya tree seed", "Palm tree seed", "Yew seed", "Magic seed", "Celastrus seed",
			"Redwood tree seed", "Watermelon", "Strawberry", "Sweetcorn", "Cactus spine",
			"Coconut", "Papaya fruit"
		);

		add(CATEGORY_MISC,
			"Jug", "Jug of water", "Bucket", "Soft clay", "Clay", "Limestone brick",
			"Gold leaf", "Bolt of cloth", "Fishing bait"
		);
	}

	private HighVolumeItemCatalog()
	{
	}

	public static boolean isCandidate(String itemName)
	{
		return CATALOG.containsKey(normalize(itemName));
	}

	public static String category(String itemName)
	{
		return CATALOG.getOrDefault(normalize(itemName), CATEGORY_MISC);
	}

	public static String[] categories()
	{
		return CATEGORIES.toArray(new String[0]);
	}

	private static void addDosed(String category, String... bases)
	{
		for (String base : bases)
		{
			for (int dose = 1; dose <= 4; dose++)
			{
				add(category, base + "(" + dose + ")");
			}
		}
	}

	private static void add(String category, String... names)
	{
		for (String name : names)
		{
			CATALOG.putIfAbsent(normalize(name), category);
		}
	}

	private static String normalize(String itemName)
	{
		return itemName == null ? "" : itemName.trim().replaceAll("\\s+", " ").toLowerCase(Locale.US);
	}
}
