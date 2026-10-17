package dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter;

import com.zurrtum.create.AllSoundEvents;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.content.equipment.clipboard.ClipboardCloneable;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.gui.menu.MenuBase;
import com.zurrtum.create.foundation.gui.menu.MenuProvider;
import dev.ryanhcode.sable.Sable;
import dev.simulated_team.simulated.compat.computercraft.AttachedComputerHandler;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.LinkedTypewriterMenuCommon;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimSoundEvents;
import dev.simulated_team.simulated.mixin_interface.PlayerTypewriterExtension;
import dev.simulated_team.simulated.service.SimPlatformService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LinkedTypewriterBlockEntity extends SmartBlockEntity implements MenuProvider, ClipboardCloneable {

    private static final boolean CC_LOADED = SimPlatformService.INSTANCE.isLoaded("computercraft");

    private LinkedTypewriterEntries entryMap;
    private final List<Integer> pressedKeys = new ArrayList<>();
    private UUID currentUser;
    private String typedEntry = "";

    public boolean powered;
    public final AttachedComputerHandler computerHandler;

    public LinkedTypewriterBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
        this.entryMap = new LinkedTypewriterEntries();
        if (CC_LOADED) {
            this.computerHandler = new AttachedComputerHandler();
        } else {
            this.computerHandler = null;
        }
    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour<?>> list) {
    }

    @Override
    public void tick() {
        super.tick();

        assert this.level != null;
        this.entryMap.updateNetworks(this.level);

        if (!this.level.isClientSide()) {
            if (this.getBlockState().getValue(LinkedTypewriterBlock.POWERED) != this.powered) {
                this.level.setBlockAndUpdate(this.getBlockPos(), this.getBlockState().setValue(LinkedTypewriterBlock.POWERED, this.powered));
            }

            if (this.currentUser != null) {
                final Player currentPlayer = this.level.getPlayerByUUID(this.currentUser);
                if (currentPlayer == null || !playerInRange(currentPlayer, this.level, this.getBlockPos())) {
                    this.disconnectUser();
                }
            }
        }
    }

    public static boolean playerInRange(final Player player, final Level world, final BlockPos pos) {
        final double range = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);

        // Make sure we take into account sub-levels! We are a sable addon after all!
        return Sable.HELPER.distanceSquaredWithSubLevels(world, player.getEyePosition(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < range * range;
    }

    public LinkedTypewriterEntries getTypewriterEntries() {
        return this.entryMap;
    }

    /**
     * Attempts to start using this typewriter if there is not already another user interacting with it. <br>
     * Disconnects the previous typewriter if the given user starts using a new one.
     *
     * @param userID The user interacting with this typewriter
     * @return Whether the user was able to successfully start using this typewriter
     */
    public boolean checkAndStartUsing(final UUID userID) {
        if (this.currentUser == null) {
            final Player player = this.level.getPlayerByUUID(userID);
            if (player != null) {
                final PlayerTypewriterExtension playerEx = (PlayerTypewriterExtension) player;
                this.currentUser = userID;
                final BlockPos previousTypewriter = playerEx.simulated$getCurrentTypewriter();
                if (previousTypewriter != null) {
                    if (this.level.getBlockEntity(previousTypewriter) instanceof final LinkedTypewriterBlockEntity nbe && nbe != this) {
                        nbe.disconnectUser();
                    }
                }

                this.powered = true;
                playerEx.simulated$setCurrentTypewriter(this.getBlockPos());
                if (this.level.isClientSide()) {
                    LinkedTypewriterInteractionHandler.associateTypewriter(this);
                } else {
                    this.level.playSound(null, this.worldPosition, AllSoundEvents.CONTROLLER_PUT.getMainEvent(), SoundSource.BLOCKS, 1.0F, 0.95F + 0.1F * this.level.getRandom().nextFloat());
                    this.sendConnectMessage(player);
                }

                return true;
            }
        }

        return false;
    }

    public void sendConnectMessage(final Player player) {
        final Component customName = this.components().getOrDefault(DataComponents.CUSTOM_NAME, SimLang.translate("linked_typewriter.title").component());
        player.displayClientMessage(SimLang.translate("linked_typewriter.start_controlling", customName.getString()).component(), true);
    }

    public void sendDisconnectMessage(final Player player) {
        final Component customName = this.components().getOrDefault(DataComponents.CUSTOM_NAME, SimLang.translate("linked_typewriter.title").component());
        player.displayClientMessage(SimLang.translate("linked_typewriter.stop_controlling", customName.getString()).component(), true);
    }

    public boolean checkUser(final UUID user) {
        return user.equals(this.currentUser);
    }

    public boolean isInUse() {
        return this.currentUser != null;
    }

    /**
     * Disconnects the current user.
     */
    public void disconnectUser() {
        if (!this.level.isClientSide()) {
            this.pressedKeys.clear();
            this.entryMap.deactivateAll();
            this.setChanged();
            this.sendData();

            this.level.playSound(null, this.worldPosition, SimSoundEvents.LINKED_TYPEWRITER_DING.event(), SoundSource.BLOCKS, 1.0F, 0.95F + 0.1F * this.level.getRandom().nextFloat());
            final Player player = this.level.getPlayerByUUID(this.currentUser);
            if (player != null) {
                this.sendDisconnectMessage(player);
                ((PlayerTypewriterExtension)player).simulated$setCurrentTypewriter(null);
            }
        } else {
            LinkedTypewriterInteractionHandler.associateTypewriter(null);
        }

        this.powered = false;
        this.currentUser = null;
    }

    public List<Integer> getPressedKeys() {
        return this.pressedKeys;
    }

    /**
     * Called whenever a key is interacted with. Used to bind a frequency to a key, or press / release the given key
     *
     * @param user   The user attempting to interact with this typewwriter
     * @param toBind The frequency to bind to this key. Nullable
     * @param key    They key to interact with
     * @param press  Whether this action is a press or release action
     */
    public void onKeyInteraction(final UUID user, @Nullable final LinkedTypewriterEntries.KeyboardEntry toBind, final int key, final boolean press) {
        if (!this.checkUser(user)) {
            return;
        }

        if (press && toBind != null) {
            this.entryMap.setKey(key, toBind);
            return;
        }

        if (press) {
            this.pressKey(key);
        } else {
            this.releaseKey(key);
        }
    }

    /**
     * @param key the key to activate
     */
    public void pressKey(final int key) {
        this.pressedKeys.add(key);
        if (key == 259) {
            if (!this.typedEntry.isEmpty()) {
                this.typedEntry = this.typedEntry.substring(0, this.typedEntry.length() - 1);
            }
        } else {
            this.typedEntry = this.typedEntry + ((char) key);
        }
        if (this.typedEntry.length() >= 25) {
            this.typedEntry = this.typedEntry.substring(1);
        }
        if (this.computerHandler != null) {
            this.computerHandler.queueEvent("key", key, this.entryMap.getEntry(key).isAlive());
        }
        this.entryMap.activateKey(key, this);
    }

    /**
     * @param key The key to deactivate
     */
    public void releaseKey(final int key) {
        this.pressedKeys.remove((Integer) key);
        this.entryMap.deactivateKey(key);
        if (this.computerHandler != null) {
            this.computerHandler.queueEvent("key_up", key);
        }
    }

    @Override
    protected void write(final ValueOutput view, final boolean clientPacket) {
        super.write(view, clientPacket);

        view.putString("typedEntry", this.typedEntry);
        this.entryMap.saveKeys(view, "Keys");

        if (this.currentUser != null) {
            view.store("CurrentUser", UUIDUtil.CODEC, this.currentUser);
        }
    }

    @Override
    protected void read(final ValueInput view, final boolean clientPacket) {
        super.read(view, clientPacket);

        this.typedEntry = view.getStringOr("typedEntry", "");
        this.entryMap = LinkedTypewriterEntries.readKeys(view, "Keys", this.getBlockPos());
        this.currentUser = view.read("CurrentUser", UUIDUtil.CODEC).orElse(null);
    }

    public String getTypedEntry() {
        return this.typedEntry;
    }

    @Override
    public void invalidate() {
        this.pressedKeys.clear();
        this.entryMap.deactivateAll();
        this.entryMap.updateNetworks(this.level);

        super.invalidate();
    }

    @Override
    public void destroy() {
        this.pressedKeys.clear();
        this.entryMap.deactivateAll();
        this.entryMap.updateNetworks(this.level);

        super.destroy();
    }

    @Override
    public @Nullable MenuBase<?> createMenu(final int id, final Inventory inventory, final Player player, final RegistryFriendlyByteBuf extraData) {
        // the client menu reads the position and the block entity data (see LinkedTypewriterScreen#create)
        this.sendToMenu(extraData);
        return new LinkedTypewriterMenuCommon(id, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return SimBlocks.LINKED_TYPEWRITER.getName();
    }

    @Override
    public String getClipboardKey() {
        return "TypewriterKeys";
    }

    @Override
    public boolean writeToClipboard(final ValueOutput view, final Direction side) {
        this.entryMap.saveKeys(view, "Keys");
        return true;
    }

    @Override
    public boolean readFromClipboard(final ValueInput view, final Player player, final Direction side, final boolean simulate) {
        if (simulate) {
            return true;
        }
        this.entryMap = LinkedTypewriterEntries.readKeys(view, "Keys", this.getBlockPos());
        return true;
    }

}
