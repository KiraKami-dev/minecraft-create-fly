package dev.ryanhcode.offroad.index;

import com.zurrtum.create.catnip.registry.RegisteredObjectsHelper;
import com.zurrtum.create.client.ponder.api.registration.PonderSceneRegistrationHelper;
import dev.ryanhcode.offroad.content.ponder.scenes.BoreheadBearingScenes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ItemLike;

public class OffroadPonderScenes {

    public static void register(final PonderSceneRegistrationHelper<Identifier> registry) {
        final PonderSceneRegistrationHelper<ItemLike> helper = registry.withKeyFunction(RegisteredObjectsHelper::getKeyOrThrow);

        helper.forComponents(OffroadBlocks.BOREHEAD_BEARING_BLOCK, OffroadBlocks.ROCK_CUTTER_BLOCK)
                .addStoryBoard("borehead_bearing/intro", BoreheadBearingScenes::boreheadIntro)
                .addStoryBoard("borehead_bearing/excavating", BoreheadBearingScenes::boreheadExcavating)
                .addStoryBoard("borehead_bearing/efficiency", BoreheadBearingScenes::boreheadEfficiency);
    }
}
