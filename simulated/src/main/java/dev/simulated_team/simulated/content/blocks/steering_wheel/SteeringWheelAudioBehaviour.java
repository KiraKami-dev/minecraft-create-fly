package dev.simulated_team.simulated.content.blocks.steering_wheel;

import com.zurrtum.create.AllSoundEvents;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.audio.KineticAudioBehaviour;

/**
 * Client behaviour: the spoke "clicks" played while the wheel turns (upstream SteeringWheelBlockEntity#tickAudio).
 */
public class SteeringWheelAudioBehaviour extends KineticAudioBehaviour<SteeringWheelBlockEntity> {
    // 0 or -1 at center
    // 4 or -5 at edges
    private int lastPlayedIncrement = 0;

    public SteeringWheelAudioBehaviour(final SteeringWheelBlockEntity be) {
        super(be);
    }

    @Override
    public void tickAudio() {
        super.tickAudio();
        final SteeringWheelBlockEntity be = this.blockEntity;
        float renderAngle = be.getRenderAngle(0);
        // angle rarely caps at exactly 180

        if (Math.abs(Math.abs(be.getAngle()) - be.angleInput.getValue()) < 0.01) {
            renderAngle += (float) (Math.signum(be.getAngle()) * 0.01);
        }

        final int playingIncrement = (int) Math.floor(Math.toDegrees(renderAngle) / 45);

        if (this.lastPlayedIncrement != playingIncrement) {
            int spokeCrossed = playingIncrement;
            if (this.lastPlayedIncrement - playingIncrement > 0) { // make - travel over a spoke match + travel
                spokeCrossed++;
            }
            // if not travelling away from edge spoke
            if (spokeCrossed != Math.signum(this.lastPlayedIncrement - playingIncrement) * 4) {
                switch (spokeCrossed) {
                    case -4, 4 -> {
                        AllSoundEvents.CRANKING.playAt(be.getLevel(), be.getBlockPos(), 1.25f, 0.85f, true);
                    }

                    case 0 -> {
                        AllSoundEvents.CRANKING.playAt(be.getLevel(), be.getBlockPos(), 1.25f, 0.5f, true);
                    }

                    default -> {
                        AllSoundEvents.CRANKING.playAt(be.getLevel(), be.getBlockPos(), 0.5f, 1.25f, true);
                    }
                }
            }
            this.lastPlayedIncrement = playingIncrement;
        }
    }
}
