package dev.conflux.multiid;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = MultiIdMod.MOD_ID, dist = Dist.CLIENT)
public final class MultiIdMod {
    public static final String MOD_ID = "multiid";

    public MultiIdMod(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(MultiIdCollector::onKeyPressed);
        NeoForge.EVENT_BUS.addListener(MultiIdCollector::onTooltip);
    }
}
