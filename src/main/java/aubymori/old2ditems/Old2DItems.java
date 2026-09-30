package aubymori.old2ditems;

import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ModInitializer;

public class Old2DItems implements ModInitializer {
	public static final String MOD_ID = "old-2d-items";

	@Override
	public void onInitialize() {
		MidnightConfig.init("old-2d-items", Old2DItemsConfig.class);
	}
}