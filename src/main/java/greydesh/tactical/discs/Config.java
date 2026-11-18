package greydesh.tactical.discs;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue MAX_BOUNCES_CONFIG = BUILDER
            .comment("Maximum number of block ricochets before returning to owner (default: 5)")
            .defineInRange("maxBounces", 5, 1, 30);

    private static final ModConfigSpec.DoubleValue MAX_SEGMENT_DISTANCE_CONFIG = BUILDER
            .comment("Maximum flight distance per straight segment in blocks before returning (default: 12.0)")
            .defineInRange("maxSegmentDistance", 12.0, 2.0, 64.0);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static int maxBounces = 5;
    public static double maxSegmentDistance = 12.0;

    public static void onLoad(final ModConfigEvent event) {
        maxBounces = MAX_BOUNCES_CONFIG.get();
        maxSegmentDistance = MAX_SEGMENT_DISTANCE_CONFIG.get();
    }
}
