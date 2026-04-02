package aubymori.old2ditems;

import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Old2DItems implements ModInitializer {
	public static final String MOD_ID = "old-2d-items";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		MidnightConfig.init("old-2d-items", Old2DItemsConfig.class);
	}
}