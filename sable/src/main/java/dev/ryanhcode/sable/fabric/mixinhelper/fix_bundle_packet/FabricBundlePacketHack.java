package dev.ryanhcode.sable.fabric.mixinhelper.fix_bundle_packet;

import io.netty.util.concurrent.FastThreadLocal;

// PORT-TODO(1.21.11): ClientPlayNetworkAddonMixin removed. Fabric API 1.21.11 no longer calls Minecraft.execute() when
// dispatching play payloads (it throws RunningOnDifferentThreadException off-thread instead), so payloads inside a bundle
// are already handled immediately on the main thread and the reordering bug this hack fixed no longer exists.
public class FabricBundlePacketHack {

    public static final FastThreadLocal<Boolean> IS_HANDLING_BUNDLE_PACKET = new FastThreadLocal<>();

}
