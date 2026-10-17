package dev.ryanhcode.sable.fabric.mixin.compatibility.create.schematics;

import com.zurrtum.create.catnip.levelWrappers.SchematicLevel;
import dev.ryanhcode.sable.fabric.mixinterface.compatibility.create.schematics.SchematicLevelExtension;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(SchematicLevel.class)
public class SchematicLevelMixin implements SchematicLevelExtension {

    @Unique
    private final List<SchematicLevelExtension.SchematicSubLevel> sable$subLevels = new ObjectArrayList<>();

    @Override
    public List<SchematicLevelExtension.SchematicSubLevel> sable$getSubLevels() {
        return this.sable$subLevels;
    }

}
