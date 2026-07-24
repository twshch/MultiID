package dev.conflux.multiid;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class MultiIdCollector {
    private static final long TOOLTIP_LIFETIME_NANOS = 1_000_000_000L;
    private static final Set<String> SELECTED_IDS = new LinkedHashSet<>();

    private static ItemStack lastTooltipStack = ItemStack.EMPTY;
    private static Screen lastTooltipScreen;
    private static long lastTooltipTime;

    private MultiIdCollector() {
    }

    static void onTooltip(RenderTooltipEvent.Pre event) {
        ItemStack stack = event.getItemStack();

        if (stack.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        lastTooltipStack = stack.copy();
        lastTooltipScreen = minecraft.screen;
        lastTooltipTime = System.nanoTime();
    }

    static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (event.getKeyCode() != GLFW.GLFW_KEY_F8) {
            return;
        }

        int modifiers = event.getModifiers();
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        boolean control = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean alt = (modifiers & GLFW.GLFW_MOD_ALT) != 0;

        if (alt) {
            clear();
        } else if (control && shift) {
            copyPlainList();
        } else if (control) {
            copyIncludeItems();
        } else if (shift) {
            removeHovered(event.getScreen());
        } else {
            addHovered(event.getScreen());
        }

        event.setCanceled(true);
    }

    private static void addHovered(Screen screen) {
        List<ItemStack> hoveredStacks = getHoveredStacks(screen);

        if (hoveredStacks.isEmpty()) {
            show(Component.translatable("multiid.not_found"));
            return;
        }

        int before = SELECTED_IDS.size();

        for (ItemStack stack : hoveredStacks) {
            for (ItemStack expanded : FtbFilterCompat.expand(stack)) {
                String itemId = getItemId(expanded);

                if (itemId != null) {
                    SELECTED_IDS.add(itemId);
                }
            }
        }

        int added = SELECTED_IDS.size() - before;
        show(Component.translatable("multiid.added", added, SELECTED_IDS.size()));
    }

    private static void removeHovered(Screen screen) {
        List<ItemStack> hoveredStacks = getHoveredStacks(screen);

        if (hoveredStacks.isEmpty()) {
            show(Component.translatable("multiid.not_found"));
            return;
        }

        int before = SELECTED_IDS.size();

        for (ItemStack stack : hoveredStacks) {
            for (ItemStack expanded : FtbFilterCompat.expand(stack)) {
                String itemId = getItemId(expanded);

                if (itemId != null) {
                    SELECTED_IDS.remove(itemId);
                }
            }
        }

        int removed = before - SELECTED_IDS.size();
        show(Component.translatable("multiid.removed", removed, SELECTED_IDS.size()));
    }

    private static List<ItemStack> getHoveredStacks(Screen screen) {
        List<ItemStack> emiStacks = EmiCompat.getHoveredStacks();

        if (!emiStacks.isEmpty()) {
            return emiStacks;
        }

        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            Slot slot = containerScreen.getSlotUnderMouse();

            if (slot != null && slot.hasItem()) {
                return List.of(slot.getItem().copy());
            }
        }

        if (
            screen == lastTooltipScreen &&
            !lastTooltipStack.isEmpty() &&
            System.nanoTime() - lastTooltipTime <= TOOLTIP_LIFETIME_NANOS
        ) {
            return List.of(lastTooltipStack.copy());
        }

        return List.of();
    }

    private static String getItemId(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());

        if (id == null) {
            return null;
        }

        String itemId = id.toString();
        return itemId.equals("minecraft:air") ? null : itemId;
    }

    private static void copyIncludeItems() {
        if (SELECTED_IDS.isEmpty()) {
            show(Component.translatable("multiid.empty"));
            return;
        }

        StringBuilder output = new StringBuilder("includeItems: [\n");
        int index = 0;

        for (String id : SELECTED_IDS) {
            output.append("    '").append(id).append("'");

            if (index < SELECTED_IDS.size() - 1) {
                output.append(',');
            }

            output.append('\n');
            index++;
        }

        output.append(']');
        copy(output.toString());
    }

    private static void copyPlainList() {
        if (SELECTED_IDS.isEmpty()) {
            show(Component.translatable("multiid.empty"));
            return;
        }

        copy(String.join("\n", SELECTED_IDS));
    }

    private static void copy(String text) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.keyboardHandler.setClipboard(text);
        show(Component.translatable("multiid.copied", SELECTED_IDS.size()));
    }

    private static void clear() {
        SELECTED_IDS.clear();
        show(Component.translatable("multiid.cleared"));
    }

    private static void show(Component message) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(message, true);
        }
    }
}
