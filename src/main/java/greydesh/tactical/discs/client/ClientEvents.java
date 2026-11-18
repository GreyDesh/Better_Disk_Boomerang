package greydesh.tactical.discs.client;

import greydesh.tactical.discs.client.renderer.DiscRenderer;
import greydesh.tactical.discs.init.ModEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public class ClientEvents {
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.DISC.get(), DiscRenderer::new);
    }
}
