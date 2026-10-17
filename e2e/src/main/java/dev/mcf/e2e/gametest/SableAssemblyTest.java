package dev.mcf.e2e.gametest;

import dev.mcf.e2e.E2E;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestAssertPosException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.entity.EntityTypeTest;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static dev.mcf.e2e.gametest.SableTestHelper.*;

/**
 * Sub-level assembly. {@link #brittleBreaking} and {@link #allBlocks} are upstream Sable's
 * {@code neoforge/gametest/AssemblyTest} (NeoForge item capabilities mapped to Fabric's Transfer API); local Y
 * coordinates are one lower than upstream's (see {@link SableTestHelper}).
 */
public final class SableAssemblyTest {

    private static final Direction[] CAPABILITY_DIRECTIONS = {
            null,
            Direction.DOWN,
            Direction.UP,
            Direction.NORTH,
            Direction.SOUTH,
            Direction.WEST,
            Direction.EAST
    };

    /**
     * Upstream: assembling a 3x3x3 sandwich of redstone components and rails (blocks that pop off without support)
     * must move every block state into the sub-level unchanged.
     */
    @GameTest(structure = "sable:assemblytest.brittlebreak", maxTicks = 100)
    public void brittleBreaking(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final SubLevelPhysicsSystem physicsSystem = physics(helper);

        final BlockPos corner = helper.absolutePos(new BlockPos(0, 0, 0));
        final BlockPos farCorner = helper.absolutePos(new BlockPos(2, 2, 2));
        final BlockPos min = BlockPos.min(corner, farCorner);
        final BlockPos max = BlockPos.max(corner, farCorner);
        final BoundingBox3i bounds = new BoundingBox3i(
                min.getX(), min.getY(), min.getZ(),
                max.getX(), max.getY(), max.getZ()
        );

        final List<BlockState> expectedStates = new ArrayList<>(bounds.volume());
        for (final BlockPos pos : BlockPos.betweenClosed(min, max)) {
            expectedStates.add(level.getBlockState(pos));
        }

        final ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(level, min, BlockPos.betweenClosed(min, max), bounds);
        physicsSystem.getPipeline().teleport(subLevel,
                new Vector3d(min.getX() + (1 + max.getX() - min.getX()) / 2.0,
                        min.getY() + (1 + max.getY() - min.getY()) / 2.0,
                        min.getZ() + (1 + max.getZ() - min.getZ()) / 2.0),
                testOrientation(helper));
        helper.runAtTickTime(10, () -> {
            final Level plot = subLevel.getLevel();
            final BoundingBox3ic sublevelBounds = subLevel.getPlot().getBoundingBox();
            final Vector3ic actualSize = sublevelBounds.size(new Vector3i());
            final Vector3ic expectedSize = bounds.size(new Vector3i());
            if (actualSize.equals(expectedSize)) {
                int i = 0;
                for (final BlockPos pos : BlockPos.betweenClosed(sublevelBounds.minX(),
                        sublevelBounds.minY(),
                        sublevelBounds.minZ(),
                        sublevelBounds.maxX(),
                        sublevelBounds.maxY(),
                        sublevelBounds.maxZ())) {
                    final BlockState expected = expectedStates.get(i);
                    final BlockState actual = plot.getBlockState(pos);
                    if (!actual.equals(expected)) {
                        throw new GameTestAssertPosException(Component.literal("Expected %s, found %s".formatted(expected, actual)), pos.immutable(), pos.immutable(), (int) helper.getTick());
                    }
                    i++;
                }
                helper.succeed();
            } else {
                helper.fail("Expected %dx%dx%d region, got %dx%dx%d".formatted(
                        expectedSize.x(),
                        expectedSize.y(),
                        expectedSize.z(),
                        actualSize.x(),
                        actualSize.y(),
                        actualSize.z()));
            }
        });
    }

    /**
     * Upstream (optional, manual-only there; runs automatically here): places one state of every block that has a
     * block entity or an item inventory on a stone block, fills every slot, assembles the two blocks and checks that
     * the block survived, nothing dropped, and every slot kept its item.
     */
    @GameTest(structure = "sable:assemblytest.allblocks", required = false, maxTicks = 30_000_000)
    public void allBlocks(final GameTestHelper helper) {
        final boolean failOnFirstError = false;
        final boolean fastTest = true;
        final Set<Identifier> skip = Set.of(
                Identifier.fromNamespaceAndPath("copycats", "wrapped_copycat")
        );
        final Set<Identifier> illegalInventories = Set.of(
                Identifier.fromNamespaceAndPath("create_new_age", "reactor_fuel_acceptor"),
                Identifier.fromNamespaceAndPath("farmersdelight", "cooking_pot") // Unsure why this failed
        );

        final ServerLevel level = helper.getLevel();
        final ServerSubLevelContainer plotContainer = container(helper);
        final SubLevelPhysicsSystem physicsSystem = physics(helper);
        final ItemStack insertStack = new ItemStack(Items.OCELOT_SPAWN_EGG);

        final BlockPos localPos = new BlockPos(2, 2, 2);
        final BlockPos pos = helper.absolutePos(localPos);
        final BlockPos onPos = pos.below();
        final Set<Block> invalidBlocks = new HashSet<>();
        final Set<Block> failures = new HashSet<>();

        final TestProgressBar progressBar = new TestProgressBar(level.getServer().getPlayerList());
        final AtomicLong completedItems = new AtomicLong();

        long tick = 0;
        long tests = 0;
        for (final Map.Entry<ResourceKey<Block>, Block> entry : BuiltInRegistries.BLOCK.entrySet()) {
            final Identifier blockId = entry.getKey().identifier();
            if (skip.contains(blockId)) {
                continue;
            }

            final Block block = entry.getValue();
            for (final BlockState state : block.getStateDefinition().getPossibleStates()) {
                // Bug with lecterns. If the block state is set, but there isn't a book it will try to drop an air item
                if (state.is(Blocks.LECTERN) && state.getValue(BlockStateProperties.HAS_BOOK)) {
                    continue;
                }

                boolean hasInventory = false;
                for (@Nullable final Direction direction : CAPABILITY_DIRECTIONS) {
                    final Storage<ItemVariant> inventory = ItemStorage.SIDED.find(level, pos, state, level.getBlockEntity(pos), direction);
                    if (inventory != null) {
                        hasInventory = true;
                        break;
                    }
                }

                if (!hasInventory && !state.hasBlockEntity()) {
                    continue;
                }

                tests++;
                helper.runAtTickTime(tick += 2, () -> {
                    level.setBlock(onPos, Blocks.STONE.defaultBlockState(), 2);
                    level.setBlock(pos, state, 2);

                    // The block was unstable and can't be placed in this configuration
                    if (isInvalidState(level.getBlockState(pos))) {
                        helper.killAllEntities();
                        invalidBlocks.add(block);
                        return;
                    }

                    final List<Entity> startEntities = level.getEntities(EntityTypeTest.forClass(Entity.class), helper.getBounds(), Entity::isAlive);
                    @SuppressWarnings("unchecked") final List<ItemStack>[] startingInventory = new List[CAPABILITY_DIRECTIONS.length];

                    for (int i = 0; i < CAPABILITY_DIRECTIONS.length; i++) {
                        final Direction direction = CAPABILITY_DIRECTIONS[i];
                        final Storage<ItemVariant> inventory = inventory(level, pos, direction);

                        if (inventory != null) {
                            try {
                                insertIntoEverySlot(inventory, insertStack);
                                startingInventory[i] = contents(inventory);
                            } catch (final Throwable t) {
                                E2E.LOG.error("Inserting into {} failed", formatBlockState(state).getString(), t);
                                helper.fail(Component.literal(formatBlockState(state).getString() + " failed. Unable to insert items successfully for face " + direction), localPos);
                            }
                        }
                    }

                    helper.runAfterDelay(1, () -> {
                        final ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(level, pos, List.of(pos, onPos), new BoundingBox3i(
                                onPos.getX(),
                                onPos.getY(),
                                onPos.getZ(),
                                pos.getX() + 1,
                                pos.getY() + 1,
                                pos.getZ() + 1));
                        physicsSystem.getPipeline().teleport(subLevel,
                                new Vector3d(pos.getX() + 0.5,
                                        pos.getY() + 1.0,
                                        pos.getZ() + 0.5),
                                testOrientation(helper));

                        final BlockPos centerBlock = subLevel.getPlot().getCenterBlock();

                        if (isInvalidState(level.getBlockState(centerBlock))) {
                            invalidBlocks.add(block);
                            helper.killAllEntities();
                            removeSubLevel(plotContainer, subLevel);
                            progressBar.update(completedItems.incrementAndGet());
                            return;
                        }

                        final List<Entity> resultEntities = level.getEntities(EntityTypeTest.forClass(Entity.class), helper.getBounds(), Entity::isAlive);
                        if (startEntities.size() != resultEntities.size()) {
                            if (failOnFirstError) {
                                final List<Component> names = new ArrayList<>(resultEntities.size());
                                for (final Entity entity : resultEntities) {
                                    if (!startEntities.contains(entity)) {
                                        names.add(entity.getDisplayName());
                                    }
                                }
                                final String formattedEntities = ComponentUtils.formatList(names, Component.literal(", ")).getString();
                                helper.fail(formatBlockState(state).getString() + " failed. Expected " + startEntities.size() + " entities to exist, found " + formattedEntities);
                                return;
                            }
                            failures.add(block);
                            helper.killAllEntities();
                        }

                        if (!illegalInventories.contains(blockId)) {
                            for (int i = 0; i < CAPABILITY_DIRECTIONS.length; i++) {
                                final Direction direction = CAPABILITY_DIRECTIONS[i];
                                final Storage<ItemVariant> inventory = inventory(level, centerBlock, direction);
                                final List<ItemStack> starting = startingInventory[i];

                                if (inventory != null) {
                                    if (starting == null) {
                                        final String stateString = formatBlockState(state).getString();
                                        helper.fail(stateString + " failed. Expected no inventory for face " + direction + ", found items");
                                        return;
                                    }

                                    try {
                                        final List<ItemStack> found = contents(inventory);
                                        if (starting.size() != found.size()) {
                                            final String stateString = formatBlockState(state).getString();
                                            helper.fail(stateString + " failed. Expected " + starting.size() + " inventory slots for face " + direction + ", found " + found.size());
                                            return;
                                        }

                                        for (int slot = 0; slot < starting.size(); slot++) {
                                            if (!ItemStack.isSameItemSameComponents(starting.get(slot), found.get(slot))) {
                                                final String stateString = formatBlockState(state).getString();
                                                final String expectedStack = starting.get(slot).toString();
                                                final String foundStack = found.get(slot).toString();
                                                helper.fail(stateString + " failed. Expected slot " + slot + " for face " + direction + " to be " + expectedStack + " found " + foundStack);
                                                return;
                                            }
                                        }
                                    } catch (final GameTestAssertException e) {
                                        throw e;
                                    } catch (final Throwable t) {
                                        E2E.LOG.error("Reading {} failed", formatBlockState(state).getString(), t);
                                        helper.fail(formatBlockState(state).getString() + " failed. Unable to get items successfully for face " + direction);
                                    }
                                } else if (starting != null) {
                                    final String stateString = formatBlockState(state).getString();
                                    helper.fail(stateString + " failed. Expected inventory items for face " + direction + ", found none");
                                    return;
                                }
                            }
                        }

                        removeSubLevel(plotContainer, subLevel);
                        progressBar.update(completedItems.incrementAndGet());
                    });
                });

                if (fastTest) {
                    break;
                }
            }
        }

        progressBar.begin(tests);
        E2E.LOG.info("E2E: allBlocks assembles {} blocks", tests);

        helper.runAtTickTime(tick + 1, () -> {
            progressBar.end();

            if (!invalidBlocks.isEmpty()) {
                final List<String> names = new ArrayList<>(invalidBlocks.size());
                for (final Block block : invalidBlocks) {
                    names.add(String.valueOf(BuiltInRegistries.BLOCK.getKey(block)));
                }
                final String formattedLines = String.join("\n", names);
                E2E.LOG.info("Skipped blocks:\n{}", formattedLines);
            }

            if (!failures.isEmpty()) {
                final List<String> names = new ArrayList<>(failures.size());
                for (final Block block : failures) {
                    names.add(String.valueOf(BuiltInRegistries.BLOCK.getKey(block)));
                }
                final String formattedLines = String.join("\n", names);
                helper.fail(failures.size() + " blocks failed.\n" + formattedLines);
            }

            helper.succeed();
        });
    }

    /**
     * Assembling a floating 3x1x3 deck with a chest on it must move exactly those 10 blocks, and the chest's items,
     * into one sub-level and leave air behind; the new sub-level then falls onto the floor and comes to rest there.
     */
    @GameTest(structure = "sable:assemblytest.allblocks", maxTicks = 200)
    public void assembledBlocksSettle(final GameTestHelper helper) {
        // the structure is 5x4x5 with its floor at local y 0 (top face y = 1); the deck floats one block above it
        final BlockPos deckMin = new BlockPos(1, 2, 1);
        final BlockPos deckMax = new BlockPos(3, 2, 3);
        final BlockPos chestPos = new BlockPos(2, 3, 2);
        final BlockPos regionMax = new BlockPos(3, 3, 3);
        final ItemStack cargo = new ItemStack(Items.DIAMOND, 7);

        fill(helper, deckMin, deckMax, Blocks.OAK_PLANKS.defaultBlockState());
        helper.setBlock(chestPos, Blocks.CHEST);
        helper.getBlockEntity(chestPos, ChestBlockEntity.class).setItem(0, cargo.copy());
        final int placed = countWorldBlocks(helper, deckMin, regionMax);
        helper.assertValueEqual(10, placed, "blocks placed");

        final ServerSubLevel subLevel = assembleRegion(helper, deckMin, regionMax);
        helper.assertValueEqual(0, countWorldBlocks(helper, deckMin, regionMax), "world blocks left behind by the assembly");
        if (countBlocks(subLevel) != placed) {
            helper.fail("The assembled sub-level holds %d of the %d blocks: %s".formatted(countBlocks(subLevel), placed, describePlot(subLevel)));
        }
        final BoundingBox3ic plotBounds = subLevel.getPlot().getBoundingBox();
        helper.assertValueEqual(new Vector3i(3, 2, 3), new Vector3i(plotBounds.width(), plotBounds.height(), plotBounds.length()), "assembled plot size");
        assertCargo(helper, subLevel, cargo);

        final SubLevelProbe probe = new SubLevelProbe(helper, subLevel).sampleEveryTick();
        helper.assertTrue(Math.abs(probe.bottom() - 2.0) < 0.05, "assembled in place, bottom at y 2: " + probe);

        helper.startSequence()
                .thenExecuteAfter(5, () -> {
                    if (probe.sample().displacement().y() > -0.05) {
                        helper.fail("Assembled sub-level is not falling: " + probe);
                    }
                })
                .thenWaitUntil(() -> {
                    probe.sample();
                    probe.assertResting(10);
                })
                .thenExecute(() -> {
                    if (Math.abs(probe.bottom() - 1.0) > 0.1) {
                        helper.fail("Came to rest with its bottom at y %.3f instead of on the floor (y 1): %s".formatted(probe.bottom(), probe));
                    }
                    helper.assertValueEqual(placed, countBlocks(subLevel), "blocks in the sub-level after landing");
                    assertCargo(helper, subLevel, cargo);
                })
                .thenSucceed();
    }

    /**
     * Asserts that the sub-level holds exactly one chest, with {@code cargo} in its first slot.
     */
    private static void assertCargo(final GameTestHelper helper, final ServerSubLevel subLevel, final ItemStack cargo) {
        final BoundingBox3ic bounds = subLevel.getPlot().getBoundingBox();
        final List<ChestBlockEntity> chests = new ArrayList<>();
        for (final BlockPos pos : BlockPos.betweenClosed(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ())) {
            if (subLevel.getLevel().getBlockEntity(pos) instanceof final ChestBlockEntity chest) {
                chests.add(chest);
            }
        }

        helper.assertValueEqual(1, chests.size(), "chests in the sub-level");
        final ItemStack found = chests.getFirst().getItem(0);
        if (!ItemStack.matches(found, cargo)) {
            helper.fail("Chest in the sub-level holds %s instead of %s".formatted(found, cargo));
        }
    }

    private static @Nullable Storage<ItemVariant> inventory(final ServerLevel level, final BlockPos pos, @Nullable final Direction direction) {
        return ItemStorage.SIDED.find(level, pos, level.getBlockState(pos), level.getBlockEntity(pos), direction);
    }

    /**
     * Inserts {@code stack} into every slot of the inventory (or once, if the storage has no slots), like upstream's
     * {@code IItemHandler} loop.
     */
    private static void insertIntoEverySlot(final Storage<ItemVariant> inventory, final ItemStack stack) {
        final ItemVariant variant = ItemVariant.of(stack);
        try (final Transaction transaction = Transaction.openOuter()) {
            if (inventory instanceof final SlottedStorage<ItemVariant> slotted) {
                for (int slot = 0; slot < slotted.getSlotCount(); slot++) {
                    slotted.getSlot(slot).insert(variant, stack.getCount(), transaction);
                }
            } else {
                inventory.insert(variant, stack.getCount(), transaction);
            }
            transaction.commit();
        }
    }

    /**
     * @return the stack in every slot (or storage view) of the inventory, empty slots included
     */
    private static List<ItemStack> contents(final Storage<ItemVariant> inventory) {
        final List<ItemStack> stacks = new ArrayList<>();
        if (inventory instanceof final SlottedStorage<ItemVariant> slotted) {
            for (int slot = 0; slot < slotted.getSlotCount(); slot++) {
                final StorageView<ItemVariant> view = slotted.getSlot(slot);
                stacks.add(view.isResourceBlank() ? ItemStack.EMPTY : view.getResource().toStack((int) view.getAmount()));
            }
        } else {
            for (final StorageView<ItemVariant> view : inventory) {
                stacks.add(view.isResourceBlank() ? ItemStack.EMPTY : view.getResource().toStack((int) view.getAmount()));
            }
        }
        return stacks;
    }

    private static Component formatBlockState(final BlockState state) {
        final MutableComponent name = Component.literal(String.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock())));

        final Collection<Property<?>> properties = state.getProperties();
        if (!properties.isEmpty()) {
            final StringBuilder propertiesString = new StringBuilder("[");
            for (final Property<?> property : properties) {
                final Object value = state.getValue(property);
                propertiesString.append(property.getName()).append("=").append(value).append(",");
            }
            propertiesString.setCharAt(propertiesString.length() - 1, ']');
            name.append(propertiesString.toString());
        }

        return name;
    }

    private static boolean isInvalidState(final BlockState state) {
        return state.isAir() || state.getFluidState().createLegacyBlock() == state;
    }
}
