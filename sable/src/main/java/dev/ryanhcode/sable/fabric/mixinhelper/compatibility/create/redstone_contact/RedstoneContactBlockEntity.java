package dev.ryanhcode.sable.fabric.mixinhelper.compatibility.create.redstone_contact;

import com.zurrtum.create.AllBlocks;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.content.contraptions.AbstractContraptionEntity;
import com.zurrtum.create.content.redstone.contact.RedstoneContactBlock;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.List;

/**
 * Block entity Sable adds to Create's redstone contact, so contacts also connect across sub-levels and contraptions.
 * Registered as {@code create:redstone_contact} from {@code AllBlockEntityTypesMixin} (during Create's own block entity
 * registration), see {@link #TYPE}.
 */
public class RedstoneContactBlockEntity extends SmartBlockEntity {

    public static final double CONTRAPTION_CHECK_BOUNDS = 1;

    /**
     * Set by {@code AllBlockEntityTypesMixin} when Create registers its block entity types.
     * Replaces upstream's Registrate {@code BlockEntityEntry} and its {@code RedstoneContactBlockEntityTypeGetter} duck.
     */
    public static BlockEntityType<RedstoneContactBlockEntity> TYPE;

    public RedstoneContactBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    public RedstoneContactBlockEntity(final BlockPos pos, final BlockState state) {
        this(TYPE, pos, state);
    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour<?>> behaviours) {

    }

    @Override
    public void tick() {
        super.tick();

        if (!this.isRemoved() && this.getLevel() != null) {
            final SubLevel parentSublevel = Sable.HELPER.getContaining(this);

            final Direction facing = this.getBlockState().getValue(RedstoneContactBlock.FACING);
            final Vector3d facingDir = JOMLConversion.atLowerCornerOf(facing.getUnitVec3i());
            if (parentSublevel != null) { // Comparison logic done in world space
                parentSublevel.logicalPose().transformNormal(facingDir);
            }

            final Vector3d frontWorldPosition = JOMLConversion.atCenterOf(this.getBlockPos().relative(facing));
            if (parentSublevel != null) {
                parentSublevel.logicalPose().transformPosition(frontWorldPosition);
            }

            final boolean found = this.checkForContactsInWorldOrSubLevel(frontWorldPosition, facing, parentSublevel, facingDir) ||
                    this.checkForContactsInContraption(frontWorldPosition, facingDir);

            if (found != this.getBlockState().getValue(RedstoneContactBlock.POWERED)) {
                if (found) {
                    this.getLevel().setBlockAndUpdate(this.getBlockPos(), this.getBlockState().setValue(RedstoneContactBlock.POWERED, true));
                } else {
                    this.getLevel().setBlockAndUpdate(this.getBlockPos(), this.getBlockState().setValue(RedstoneContactBlock.POWERED, false));
                }
            }
        }
    }

    private static boolean isContact(final BlockState state) {
        return state.is(AllBlocks.REDSTONE_CONTACT) || state.is(AllBlocks.ELEVATOR_CONTACT);
    }

    private boolean checkForContactsInContraption(final Vector3d frontWorldPosition, final Vector3d facingDir) {
        final Vec3 frontMoj = JOMLConversion.toMojang(frontWorldPosition);
        final Vec3 min = frontMoj.subtract(CONTRAPTION_CHECK_BOUNDS / 2, CONTRAPTION_CHECK_BOUNDS / 2, CONTRAPTION_CHECK_BOUNDS / 2);
        final Vec3 max = min.add(CONTRAPTION_CHECK_BOUNDS, CONTRAPTION_CHECK_BOUNDS, CONTRAPTION_CHECK_BOUNDS);

        final AABB searchBounds = new AABB(min, max);

        final List<AbstractContraptionEntity> contraptions = this.getLevel().getEntitiesOfClass(AbstractContraptionEntity.class, searchBounds);

        for (final AbstractContraptionEntity ace : contraptions) {
            final Vec3 contactLocalPos = ace.toLocalVector(frontMoj, 1f);
            final StructureTemplate.StructureBlockInfo candidateBlock = ace.getContraption().getBlocks().get(BlockPos.containing(contactLocalPos));

            if (candidateBlock == null) {
                continue;
            }

            final BlockState otherState = candidateBlock.state();
            if (!isContact(otherState)) {
                continue;
            }

            final Direction otherFacingDirection = otherState.getValue(RedstoneContactBlock.FACING);
            Vec3 otherFacingMoj = Vec3.atLowerCornerOf(otherFacingDirection.getUnitVec3i());
            otherFacingMoj = ace.applyRotation(otherFacingMoj, 1f);

            final Vector3d otherFacing = JOMLConversion.toJOML(otherFacingMoj);
            if (facingDir.dot(otherFacing) < -0.95) { // If facing towards each other
                return true;
            }
        }
        return false;
    }

    private boolean checkForContactsInWorldOrSubLevel(final Vector3d frontWorldPosition, final Direction facing, final SubLevel parentSublevel, final Vector3d facingDir) {
        return Sable.HELPER.findIncludingSubLevels(this.getLevel(), this.getBlockPos().getCenter().relative(facing, 1), true, parentSublevel, (subLevel, pos) -> {
            if (subLevel != null) {
                //Look for contraptions on this sublevel
                final Vector3d localFrontWorldPosition = subLevel.logicalPose().transformPositionInverse(frontWorldPosition, new Vector3d());
                final Vector3d localFacingDir = subLevel.logicalPose().transformNormalInverse(facingDir, new Vector3d());
                if (this.checkForContactsInContraption(localFrontWorldPosition, localFacingDir)) {
                    return true;
                }
            }

            final BlockState otherState = this.getLevel().getBlockState(pos);
            if (!isContact(otherState)) {
                return false;
            }

            final Direction otherFacing = otherState.getValue(RedstoneContactBlock.FACING);
            final Vector3d otherFacingDir = JOMLConversion.atLowerCornerOf(otherFacing.getUnitVec3i());
            if (subLevel != null) {
                subLevel.logicalPose().transformNormal(otherFacingDir);
            }

            return facingDir.dot(otherFacingDir) < -0.99; // If facing towards each other
        });
    }
}
