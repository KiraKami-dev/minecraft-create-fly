package dev.simulated_team.simulated.content.blocks.steering_wheel;

import com.zurrtum.create.content.equipment.goggles.GogglesItem;
import dev.ryanhcode.sable.veillite.api.network.VeilPacketManager;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.network.packets.SteeringWheelPacket;
import dev.simulated_team.simulated.service.SimConfigService;
import dev.simulated_team.simulated.util.hold_interaction.BlockHoldInteraction;
import dev.simulated_team.simulated.util.hold_interaction.HoldInteractionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class SteeringWheelHandler extends BlockHoldInteraction {
    // 1.21.11 drops text whose color has no alpha
    private static final int TEXT_OUTLINE_COLOR = 0xFF2B2117;
    private static final int TEXT_COLOR = 0xFF886539;

    private static SteeringWheelBlockEntity blockEntity = null;

    private static boolean updated = false;
    private static float rawAngle = 0;
    private static float effectiveAngle = 0;
    private static boolean wasShiftKeyDown = false;
    private static int angleSgn = 1;
    private static float angleLimit = 0;

    @Override
    public void startHold(final Level level, final Player player, final BlockPos blockPos) {
        super.startHold(level, player, blockPos);
        blockEntity = level.getBlockEntity(blockPos, SimBlockEntityTypes.STEERING_WHEEL).orElseThrow();
        rawAngle = blockEntity.getInteractionAngle(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaTicks());
        angleSgn = (int) blockEntity.directionConvert(1);
        updated = true;
        angleLimit = blockEntity.angleInput.getValue();
    }


    @Override
    public void renderOverlay(final GuiGraphics guiGraphics, final int width1, final int height1, final boolean hideGui) {
        final Minecraft mc = Minecraft.getInstance();
        if (hideGui) {
            return;
        }

        if (mc.player != null && !GogglesItem.isWearingGoggles(mc.player)) {
            return;
        }

        final Identifier tex = Simulated.path("textures/gui/steering_wheel.png");
        final float magicOffset = 0.56f;

        final int x = ((width1 - 223) / 2) + SimConfigService.INSTANCE.client().blockConfig.steeringWheelXOffset.get();
        final int y = 10 + SimConfigService.INSTANCE.client().blockConfig.steeringWheelYOffset.get();

        blit(guiGraphics, tex, x, y, 0, 0, 223, 31);

        final float offset = wrapDegrees(angleLimit) * magicOffset;
        final int activeWidth = (int) Math.abs(offset);

        final int centerX = x + 111 - 4;
        final float realDegrees = angleSgn * -effectiveAngle;
        if (Math.abs(angleLimit) <= 180) {
            final int leftDeadZoneWidth = (centerX - x) - activeWidth + 4;
            if (leftDeadZoneWidth > 0) {
                blit(guiGraphics, tex, x, y, 0, 32, leftDeadZoneWidth, 31);
            }

            final int rightSideStart = (centerX + activeWidth) + 8;
            final int rightDeadZoneWidth = (x + 223) - rightSideStart;
            if (rightDeadZoneWidth > 0) {
                blit(guiGraphics, tex, rightSideStart, y, (rightSideStart - x), 32, rightDeadZoneWidth, 31);
            }
        } else {
            if (realDegrees <= -180) {
                final int rightSideStart = (centerX - activeWidth) + 4;
                final int rightDeadZoneWidth = (x + 223) - rightSideStart;
                if (rightDeadZoneWidth > 0) {
                    blit(guiGraphics, tex, rightSideStart, y, (rightSideStart - x), 32, rightDeadZoneWidth, 31);
                }
            }

            if (realDegrees >= 180) {
                final int leftDeadZoneWidth = (centerX - x) + activeWidth + 4;
                if (leftDeadZoneWidth > 0) {
                    blit(guiGraphics, tex, x, y, 0, 32, leftDeadZoneWidth, 31);
                }
            }
        }

        if (Math.abs(angleLimit) > 180) {
            if (-realDegrees >= 180) {
                blit(guiGraphics, tex, (int)(centerX + offset) + 2, y + 10, 239, 0, 6, 20);
            }

            if (-realDegrees <= -180) {
                blit(guiGraphics, tex, (int)(centerX - offset) + 2, y + 10, 239, 0, 6, 20);
            }
        } else {
            blit(guiGraphics, tex, (int)(centerX + offset) + 2, y + 10, 239, 0, 6, 20);
            blit(guiGraphics, tex, (int)(centerX - offset) + 2, y + 10, 239, 0, 6, 20);
        }

        final float degrees = Math.abs(angleLimit) <= 180 ? Mth.clamp(realDegrees, -180f, 180f) : wrapDegrees(realDegrees);
        final int markerX = (int) (centerX - degrees * magicOffset) + 1;
        blit(guiGraphics, tex, markerX, y + 11, 224, 0, 9, 18);

        final String text = (int) -realDegrees + "°";
        final int textWidth = mc.font.width(text);

        final int centeredX = markerX + 6 - (textWidth / 2);
        for (int xoff = -1; xoff < 2; xoff++) {
            for (int yoff = -1; yoff < 2; yoff++) {
                if (xoff == 0 && yoff == 0) continue;

                guiGraphics.drawString(mc.font, text, centeredX + xoff, y + yoff, TEXT_OUTLINE_COLOR, false);
            }
        }

        guiGraphics.drawString(mc.font, text, centeredX, y, TEXT_COLOR, false);
    }

    private static void blit(final GuiGraphics guiGraphics, final Identifier tex, final int x, final int y, final float u, final float v, final int width, final int height) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, u, v, width, height, 256, 256);
    }

    //custom wrap because mc is a jerk (we need else if here otherwise it'll pass both if statements
    public static float wrapDegrees(final float value) {
        float f = value % 360.0F;

        if (f >= 180.0F) {
            f -= 360.0F;
        } else if (f <= -180.0F) {
            f += 360.0F;
        }

        return f;
    }

    @Override
    public void stop() {
        if (blockEntity != null && !blockEntity.isRemoved()) {
            blockEntity.held = false;
            blockEntity = null;
        }

        VeilPacketManager.server().sendPacket(new SteeringWheelPacket(true, effectiveAngle, this.getInteractionPos()));
        super.stop();
    }

    @Override
    public boolean activeOnMouseMove(final double yaw, final double pitch) {
        if (yaw != 0) {
            final float oldAngle = rawAngle;
            rawAngle += (float) (yaw / 10 * angleSgn);
            rawAngle = Mth.clamp(rawAngle, -blockEntity.angleInput.getValue(), blockEntity.angleInput.getValue());
            updated |= oldAngle != rawAngle;
        }

        return true;
    }

    @Override
    public boolean activeTick(final Level level, final LocalPlayer player) {
        effectiveAngle = rawAngle;
        if (HoldInteractionManager.unblockedShift()) {
            effectiveAngle = Mth.clamp(Math.round(effectiveAngle / 45) * 45, -blockEntity.angleInput.getValue(), blockEntity.angleInput.getValue());
            if (!wasShiftKeyDown) {
                updated = true;
            }
            wasShiftKeyDown = true;
        } else {
            if (wasShiftKeyDown) {
                updated = true;
            }
            wasShiftKeyDown = false;
        }

        this.setTargetAngle(effectiveAngle);
        return !BlockHoldInteraction.inInteractionRange(player, this.getInteractionPos().getCenter(), 4);
    }

    @Override
    public boolean isBlockActive(final BlockPos pos) {
        return super.isBlockActive(pos) && !Float.isNaN(SteeringWheelHandler.rawAngle);
    }

    public void setTargetAngle(final float targetAngle) {
        if (updated) {
            VeilPacketManager.server().sendPacket(new SteeringWheelPacket(false, targetAngle, this.getInteractionPos()));

            updated = false;
            blockEntity.targetAngleToUpdate = targetAngle;
            blockEntity.held = !Float.isNaN(targetAngle);
        }
    }

    @Override
    public int getCrouchBlockingTicks() {
        return 6;
    }
}
