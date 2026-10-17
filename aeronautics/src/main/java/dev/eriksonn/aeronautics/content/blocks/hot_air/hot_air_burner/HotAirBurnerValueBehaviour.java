package dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner;

import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.blockEntity.behaviour.ValueSettings;
import com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerScrollValueBehaviour;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.function.Supplier;

/**
 * Server half of the hot air amount value box (value, range, stepping). Create Fly splits value boxes: the label, slot,
 * board and formatting are in the client {@link HotAirBurnerScrollValueBehaviour}.
 */
public class HotAirBurnerValueBehaviour extends ServerScrollValueBehaviour {
    private int interval = 5;

    private Supplier<Integer> minSupplier, maxSupplier;

    public HotAirBurnerValueBehaviour(final SmartBlockEntity be) {
        super(be);
    }

    private void updateInterval()
    {
        this.interval = (this.maxSupplier.get()- this.minSupplier.get()+250)/500;
        this.interval *=5;
        this.interval = Math.max(1, this.interval);
    }

    /**
     * @return the step between two value settings (the board shows {@code max / interval} settings)
     */
    public int getInterval() {
        this.updateInterval();
        return this.interval;
    }

    public int getMinValue() {
        return this.minSupplier.get();
    }

    public int getMaxValue() {
        return this.maxSupplier.get();
    }

    @Override
    public ValueSettings getValueSettings() {
        return new ValueSettings(0, this.value / this.interval);
    }

    @Override
    public void setValueSettings(final Player player, final ValueSettings valueSetting, final boolean ctrlDown) {
        if (!valueSetting.equals(this.getValueSettings()))
            this.playFeedbackSound(this);
        this.updateInterval();
        this.setValue(valueSetting.value() * this.interval);
    }

    public HotAirBurnerValueBehaviour between(final Supplier<Integer> min, final Supplier<Integer> max) {
        this.minSupplier = min;
        this.maxSupplier = max;

        this.between(min.get(), max.get());
        return this;
    }

    /**
     * Sets the initial value without notifying the block entity (upstream assigned the value field directly)
     */
    public void initValue(final int value) {
        this.value = value;
    }

    @Override
    public void setValue(int value) {
        value = Mth.clamp(value, this.minSupplier.get(), this.maxSupplier.get());
        if (value == this.value)
            return;

        this.value = value;

        this.blockEntity.setChanged();
        this.blockEntity.sendData();
    }

    @Override
    public String getClipboardKey() {
        return "Hot Air";
    }
}
