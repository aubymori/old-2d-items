package aubymori.old2ditems;

import com.google.common.collect.Lists;
import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.resources.Identifier;

import java.util.List;

public class Old2DItemsConfig extends MidnightConfig {
    public static final String MAIN = "Old 2D Items Settings";

    @Entry(category = MAIN) public static Direction direction = Direction.SCREEN_HORZ;
    public enum Direction {
        SCREEN_HORZ, CAMERA_HORZ, SPIN, SCREEN
    }

    @Entry(category = MAIN) public static boolean flatModels = true;

    @Condition(requiredOption = "direction", requiredValue = "SCREEN_HORZ")
    @Condition(requiredOption = "flatModels")
    @Entry(category = MAIN) public static boolean renderBack = false;

    @Entry(category = MAIN) public static boolean affect3DModels = false;

    @Entry(category = MAIN, idMode = 0) public static List<Identifier> exceptions = Lists.newArrayList(Identifier.withDefaultNamespace("decorated_pot"));
}
