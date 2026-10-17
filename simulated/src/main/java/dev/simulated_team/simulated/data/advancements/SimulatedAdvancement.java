package dev.simulated_team.simulated.data.advancements;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

// i pretty much yoinked everything from Create's advancement/trigger stuff
// the only notable difference is it's not hardcoded to simulated, and addons *should* be able to use it
//
// Port note: the advancement JSON is shipped (no datagen), so only the runtime part is kept: the builtin
// "<id>_builtin" trigger and the award helpers. Builder calls that only fed datagen (icon, criteria) are no-ops.
public class SimulatedAdvancement {
    static final String SECRET_SUFFIX = "\n§7(Hidden Advancement)";

    private SimpleSimulatedTrigger builtinTrigger;
    private SimulatedAdvancement parent;

    private final Identifier background;
    private final String lang;

    private final String id;
    private final String modid;
    private String title;
    private String description;

    public SimulatedAdvancement(final String id, final UnaryOperator<Builder> b, final Identifier background, final String modid, final BiFunction<String,String, SimpleSimulatedTrigger> triggerHandler) {
        this.id = id;
        this.modid = modid;
        this.background = background;
        this.lang = "advancement." + modid + ".";

        final Builder t = new Builder();
        b.apply(t);

        if(!t.externalTrigger) {
            this.builtinTrigger = triggerHandler.apply(modid,id + "_builtin");
        }

        if(t.type == TaskType.SECRET)
            this.description += SECRET_SUFFIX;
    }

    private String titleKey() {
        return this.lang + this.id;
    }

    private String descriptionKey() {
        return this.titleKey() + ".desc";
    }

    public Identifier getId() {
        return Identifier.fromNamespaceAndPath(this.modid, this.id);
    }

    public SimulatedAdvancement getParent() {
        return this.parent;
    }

    public boolean isAlreadyAwardedTo(final Player player) {
        if (!(player instanceof final ServerPlayer sp))
            return true;
        final AdvancementHolder advancement = sp.level().getServer()
                .getAdvancements()
                .get(this.getId());
        if (advancement == null)
            return true;
        return sp.getAdvancements()
                .getOrStartProgress(advancement)
                .isDone();
    }

    public void awardTo(final Player player) {
        if(this.isAlreadyAwardedTo(player)) return;
        if (!(player instanceof final ServerPlayer sp)) {
            return;
        }
        if (this.builtinTrigger == null) {
            throw new UnsupportedOperationException("Advancement " + this.id + " uses external Triggers, it cannot be awarded directly");
        }

        this.builtinTrigger.trigger(sp);
    }

    /**
     * Only attempts to award nearby players every x amount of ticks, use this when you need to award something in a method called every tick
     */
    public void awardToNearby(final BlockPos pos, final Level level, final int ticks, final double radius) {
        if(level.getGameTime() % ticks == 0) {
            this.awardToNearby(pos, level, radius);
        }
    }

    public void awardToNearby(final BlockPos pos, final Level level) {
        this.awardToNearby(pos, level, 10);
    }
    public void awardToNearby(final BlockPos pos, final Level level, final double radius) {
        final AABB aabb = new AABB(pos).inflate(radius);
        final List<Player> nearbyPlayers = level.getEntitiesOfClass(Player.class, aabb);
        for (final Player player : nearbyPlayers) {
            this.awardTo(player);
        }
    }

    public void provideLang(final BiConsumer<String, String> consumer) {
        consumer.accept(this.titleKey(), this.title);
        consumer.accept(this.descriptionKey(), this.description);
    }

    /**
     * <strong>Silent</strong> - No toast or chat announcement<br>
     * <strong>Normal</strong> - Toast but no chat announcement (default)<br>
     * <strong>Noisy </strong> - Toast and chat announcement<br>
     * <strong>Expert</strong> - Toast and chat announcement, but purple<br>
     * <strong>Secret</strong> - Toast and chat announcement, but purple and hidden<br>
     */
    public enum TaskType {

        SILENT,
        NORMAL,
        NOISY,
        EXPERT,
        SECRET,

        ;
    }

    public class Builder {

        private TaskType type = TaskType.NORMAL;
        private boolean externalTrigger;

        public Builder special(final TaskType type) {
            this.type = type;
            return this;
        }

        public Builder after(final SimulatedAdvancement other) {
            SimulatedAdvancement.this.parent = other;
            return this;
        }

        // display data lives in the advancement JSON
        public Builder icon(final ItemLike item) {
            return this;
        }

        public Builder icon(final ItemStack stack) {
            return this;
        }

        public Builder title(final String title) {
            SimulatedAdvancement.this.title = title;
            return this;
        }

        public Builder description(final String description) {
            SimulatedAdvancement.this.description = description;
            return this;
        }

        // the criteria live in the advancement JSON; these only mark the advancement as not using the builtin trigger

        public Builder whenBlockPlaced(final Block block) {
            return this.markExternal();
        }

        public Builder whenIconCollected() {
            return this.markExternal();
        }

        public Builder whenIconPlaced() {
            return this.markExternal();
        }

        public Builder whenItemCollected(final ItemLike itemProvider) {
            return this.markExternal();
        }

        public Builder whenItemCollected(final TagKey<Item> tag) {
            return this.markExternal();
        }

        public  Builder awardedForFree() {
            return this.markExternal();
        }

        public Builder externalTrigger(final Criterion<? extends CriterionTriggerInstance> trigger) {
            return this.markExternal();
        }

        private Builder markExternal() {
            this.externalTrigger = true;
            return this;
        }
    }
}
