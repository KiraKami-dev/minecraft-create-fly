package dev.simulated_team.simulated.registrate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.zurrtum.create.api.behaviour.display.DisplaySource;
import com.zurrtum.create.api.behaviour.display.DisplayTarget;
import com.zurrtum.create.api.registry.CreateRegistries;
import com.zurrtum.create.foundation.gui.menu.MenuType;
import dev.simulated_team.simulated.client.BlockPropertiesTooltip;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import dev.simulated_team.simulated.index.SimDataComponents;
import dev.simulated_team.simulated.index.SimRegistries;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Per-mod registration helper (replaces the Registrate based registrate of upstream). Everything is registered
 * immediately into the vanilla / Create Fly registries, Create Fly style; this class only adds the bookkeeping the
 * Simulated creative tab needs (sections and tab order) and a few Simulated specific registries.
 */
public class SimulatedRegistrate {

    public static final Set<String> MODS = ConcurrentHashMap.newKeySet();
    public static final List<Supplier<Item>> TAB_ITEMS = Collections.synchronizedList(new ArrayList<>());
    public static final Map<Identifier, Identifier> ITEM_TO_SECTION = new ConcurrentHashMap<>();

    private static final Map<Identifier, Supplier<ItemLike>> NAVIGATION_TARGET_ITEMS = new ConcurrentHashMap<>();

    private final String modId;
    private Identifier currentSection;
    private final List<Item> items = new ArrayList<>();
    private final Map<ResourceKey<?>, Map<Identifier, Object>> simpleEntries = new ConcurrentHashMap<>();

    public SimulatedRegistrate(final Identifier initialSection, final String modId) {
        this.modId = modId;
        this.currentSection = initialSection;
        MODS.add(modId);
    }

    public String getModid() {
        return this.modId;
    }

    public SimulatedRegistrate inSection(final Identifier section) {
        this.currentSection = section;
        return this;
    }

    public Identifier id(final String name) {
        return Identifier.fromNamespaceAndPath(this.modId, name);
    }

    // Blocks & items

    public <T extends Block> T block(final String name, final Function<BlockBehaviour.Properties, T> factory, final BlockBehaviour.Properties properties) {
        final ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, this.id(name));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
    }

    /**
     * Registers an item and adds it to the Simulated creative tab, in the current section.
     */
    public <T extends Item> T item(final String name, final Function<Item.Properties, T> factory, final Item.Properties properties) {
        final ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, this.id(name));
        final T item = factory.apply(properties.setId(key));
        if (item instanceof final BlockItem blockItem) {
            blockItem.registerBlocks(Item.BY_BLOCK, item);
        }
        Registry.register(BuiltInRegistries.ITEM, key, item);

        this.items.add(item);
        TAB_ITEMS.add(() -> item);
        ITEM_TO_SECTION.put(key.identifier(), this.currentSection);
        return item;
    }

    public <T extends Item> T item(final String name, final Function<Item.Properties, T> factory) {
        return this.item(name, factory, new Item.Properties());
    }

    /**
     * Registers the item of a block, under the block's id (like Registrate's {@code BlockBuilder.item()}).
     */
    public <B extends Block, T extends Item> T blockItem(final B block, final BiFunction<B, Item.Properties, T> factory, final Item.Properties properties) {
        final Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        return this.item(id.getPath(), p -> factory.apply(block, p), properties.useBlockDescriptionPrefix());
    }

    public <B extends Block, T extends Item> T blockItem(final B block, final BiFunction<B, Item.Properties, T> factory) {
        return this.blockItem(block, factory, new Item.Properties());
    }

    public BlockItem blockItem(final Block block) {
        return this.blockItem(block, BlockItem::new);
    }

    /**
     * Adds an item that is not registered by this mod to the creative tab, in the current section.
     */
    public void addExtraItem(final Identifier item) {
        TAB_ITEMS.add(() -> BuiltInRegistries.ITEM.getValue(item));
        ITEM_TO_SECTION.put(item, this.currentSection);
    }

    /**
     * @return every item registered through this registrate, in registration order
     */
    public List<Item> getItems() {
        return Collections.unmodifiableList(this.items);
    }

    // Block entities, entities, menus

    @FunctionalInterface
    public interface BlockEntityFactory<T extends BlockEntity> {
        T create(BlockEntityType<T> type, BlockPos pos, BlockState state);
    }

    /**
     * Registers a block entity type whose block entities take their own type as constructor argument
     * (Registrate's {@code BlockEntityFactory}).
     */
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityType<T> blockEntity(final String name, final BlockEntityFactory<T> factory, final Block... blocks) {
        final BlockEntityType<T>[] type = new BlockEntityType[1];
        type[0] = FabricBlockEntityTypeBuilder.<T>create((pos, state) -> factory.create(type[0], pos, state), blocks).build();
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, this.id(name), type[0]);
    }

    public <T extends BlockEntity> BlockEntityType<T> blockEntity(final String name, final FabricBlockEntityTypeBuilder.Factory<T> factory, final Block... blocks) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, this.id(name), FabricBlockEntityTypeBuilder.<T>create(factory, blocks).build());
    }

    public <T extends Entity> EntityType<T> entity(final String name, final EntityType.Builder<T> builder) {
        final ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, this.id(name));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    /**
     * Registers a Create Fly menu type (opened with Create Fly's {@code MenuProvider}, screens are registered with
     * {@code AllMenuScreens.register}).
     */
    public <H> MenuType<H> menu(final String name, final MenuType<H> type) {
        return Registry.register(CreateRegistries.MENU_TYPE, this.id(name), type);
    }

    // Create registries

    public <T extends DisplaySource> T displaySource(final String name, final Supplier<T> factory) {
        return Registry.register(CreateRegistries.DISPLAY_SOURCE, this.id(name), factory.get());
    }

    public <T extends DisplayTarget> T displayTarget(final String name, final Supplier<T> factory) {
        return Registry.register(CreateRegistries.DISPLAY_TARGET, this.id(name), factory.get());
    }

    // Other (custom) registries

    /**
     * Registers into any registry already present in {@link BuiltInRegistries#REGISTRY}, and remembers the entry
     * for {@link #byNameCodecExpanded(ResourceKey)}.
     */
    @SuppressWarnings("unchecked")
    public <R, T extends R> T simple(final String name, final ResourceKey<? extends Registry<R>> registryKey, final Supplier<T> factory) {
        final Registry<R> registry = (Registry<R>) BuiltInRegistries.REGISTRY.getValue(registryKey.identifier());
        if (registry == null) {
            throw new IllegalStateException("Registry " + registryKey.identifier() + " does not exist (yet)");
        }

        final Identifier id = this.id(name);
        final T value = Registry.register(registry, id, factory.get());
        this.simpleEntries.computeIfAbsent(registryKey, k -> Collections.synchronizedMap(new LinkedHashMap<>())).put(id, value);
        return value;
    }

    public <T extends NavigationTarget> T navTarget(final String name, final Supplier<T> navTableItem, final Supplier<ItemLike> itemSupplier) {
        final T target = this.simple(name, SimRegistries.Keys.NAVIGATION_TARGET, navTableItem);
        NAVIGATION_TARGET_ITEMS.put(this.id(name), itemSupplier);
        return target;
    }

    public <T extends NavigationTarget> T navTarget(final String name, final Supplier<T> navTableItem, final ItemLike item) {
        return this.navTarget(name, navTableItem, () -> item);
    }

    public <T extends BlockPropertiesTooltip.Entry> T propertyTooltip(final String name, final Supplier<T> tooltipFunction) {
        return this.simple(name, SimRegistries.Keys.PROPERTY_TOOLTIP, tooltipFunction);
    }

    /**
     * A codec over the entries this registrate registered into the given registry (by id).
     */
    @SuppressWarnings("unchecked")
    public <T> Codec<T> byNameCodecExpanded(final ResourceKey<? extends Registry<T>> key) {
        return Identifier.CODEC.flatXmap(id -> {
            final Object value = this.entriesOf(key).get(id);
            if (value != null) {
                return DataResult.success((T) value);
            }
            return DataResult.error(() -> "Unknown registry element in " + key + ":" + id);
        }, value -> {
            for (final Map.Entry<Identifier, Object> entry : this.entriesOf(key).entrySet()) {
                if (entry.getValue() == value) {
                    return DataResult.success(entry.getKey());
                }
            }
            return DataResult.error(() -> "Unknown registry element in " + key + ":" + value);
        });
    }

    private Map<Identifier, Object> entriesOf(final ResourceKey<?> key) {
        return this.simpleEntries.getOrDefault(key, Map.of());
    }

    // Creative tab sections / default components

    public static Identifier sectionOf(final Item item) {
        return ITEM_TO_SECTION.get(BuiltInRegistries.ITEM.getKey(item));
    }

    /**
     * Gives every item registered as a navigation target its {@link SimDataComponents#TARGET} default component.
     * Wire to Fabric's {@code DefaultItemComponentEvents.MODIFY} ({@code context::modify}).
     */
    public static void onAddDefaultComponents(final BiConsumer<ItemLike, Consumer<DataComponentPatch.Builder>> modify) {
        forEachNavigationTargetItem((item, target) -> modify.accept(item, builder -> builder.set(SimDataComponents.TARGET, target)));
    }

    public static void forEachNavigationTargetItem(final BiConsumer<ItemLike, NavigationTarget> consumer) {
        for (final Map.Entry<Identifier, Supplier<ItemLike>> entry : NAVIGATION_TARGET_ITEMS.entrySet()) {
            final NavigationTarget target = SimRegistries.NAVIGATION_TARGET.getValue(entry.getKey());
            final ItemLike item = entry.getValue().get();
            if (target != null && item != null) {
                consumer.accept(item, target);
            }
        }
    }
}
