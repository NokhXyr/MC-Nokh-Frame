package NokhXyr.NokhFrame;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/** Shows nearby living pets that expose Minecraft's standard owner relationship. */
public final class OwnedPetPreviewRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(OwnedPetPreviewRenderer.class);
    private static final Set<Class<?>> FAILED_TYPES = new HashSet<>();

    private OwnedPetPreviewRenderer() {
    }

    public static void render(GuiGraphics graphics, LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        Vec3 origin = player.position();
        int rendered = 0;
        dispatcher.setRenderShadow(false);
        try {
            for (Entity entity : minecraft.level.entitiesForRendering()) {
                if (!(entity instanceof LivingEntity)
                        || !(entity instanceof OwnableEntity owned)
                        || !player.getUUID().equals(owned.getOwnerUUID())
                        || entity.distanceToSqr(player) > 16.0
                        || FAILED_TYPES.contains(entity.getClass())) continue;
                try {
                    dispatcher.render(entity, entity.getX() - origin.x, entity.getY() - origin.y,
                            entity.getZ() - origin.z, entity.getYRot(), 1.0F,
                            graphics.pose(), graphics.bufferSource(), 15728880);
                    if (++rendered >= 8) break;
                } catch (RuntimeException | LinkageError exception) {
                    FAILED_TYPES.add(entity.getClass());
                    LOGGER.warn("A pet renderer cannot run in the studio preview: {}",
                            entity.getClass().getName(), exception);
                }
            }
            graphics.flush();
        } finally {
            dispatcher.setRenderShadow(true);
        }
    }
}
