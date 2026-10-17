package dev.ryanhcode.sable.fabric.mixin.compatibility.create.schematics;

import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.fabric.mixinterface.compatibility.create.schematics.StructureTemplateExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.util.SableNBTUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mixin(StructureTemplate.class)
public abstract class StructureTemplateMixin implements StructureTemplateExtension {

    @Unique
    private final List<SubLevelTemplate> sable$subLevelTemplates = new ObjectArrayList<>();


    @Inject(method = "load", at = @At("TAIL"))
    private void sable$load(final HolderGetter<Block> holderGetter, final CompoundTag tag, final CallbackInfo ci) {
        final ListTag subLevelTags = tag.getListOrEmpty("sub_levels");

        for (final Tag subLevelTag : subLevelTags) {
            if (!(subLevelTag instanceof final CompoundTag subLevelCompound)) continue;
            final StructureTemplate t = new StructureTemplate();

            t.load(holderGetter, subLevelCompound);

            final Optional<UUID> uuid = subLevelCompound.read("uuid", UUIDUtil.CODEC);
            if (uuid.isEmpty()) continue;
            final Vector3d position = SableNBTUtils.readVector3d(subLevelCompound.getCompoundOrEmpty("position"));
            final Quaterniond orientation = SableNBTUtils.readQuaternion(subLevelCompound.getCompoundOrEmpty("orientation"));

            this.sable$subLevelTemplates.add(new SubLevelTemplate(uuid.get(), position, orientation, t));
        }
    }

    @Inject(method = "fillEntityList", at = @At(value = "INVOKE", target = "Ljava/util/List;clear()V", remap = false))
    private void sable$fillEntityList(final Level level,
                                final BlockPos minPos,
                                final BlockPos maxPos,
                                final ProblemReporter problemReporter,
                                final CallbackInfo ci,
                                @Local final List<Entity> entities) {
        final ActiveSableCompanion helper = Sable.HELPER;
        final SubLevel schematicSubLevel = helper.getContaining(level, minPos);

        entities.removeIf(entity -> {
            final SubLevel entitySubLevel = helper.getContaining(entity);
            return !(entitySubLevel == schematicSubLevel || Sable.HELPER.getTrackingSubLevel(entity) == schematicSubLevel);
        });
    }

    @Override
    public List<SubLevelTemplate> sable$getSubLevels() {
        return this.sable$subLevelTemplates;
    }
}
