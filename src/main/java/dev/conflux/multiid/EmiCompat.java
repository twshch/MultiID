package dev.conflux.multiid;

import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

final class EmiCompat {
    private EmiCompat() {
    }

    static List<ItemStack> getHoveredStacks() {
        try {
            Class<?> emiApiClass = Class.forName("dev.emi.emi.api.EmiApi");
            Method getHoveredStack;
            Object interaction;

            try {
                getHoveredStack = emiApiClass.getMethod("getHoveredStack", boolean.class);
                interaction = getHoveredStack.invoke(null, true);
            } catch (NoSuchMethodException ignored) {
                getHoveredStack = emiApiClass.getMethod("getHoveredStack");
                interaction = getHoveredStack.invoke(null);
            }

            if (interaction == null) {
                return List.of();
            }

            Method isEmpty = interaction.getClass().getMethod("isEmpty");

            if (Boolean.TRUE.equals(isEmpty.invoke(interaction))) {
                return List.of();
            }

            Object ingredient = interaction.getClass().getMethod("getStack").invoke(interaction);

            if (ingredient == null) {
                return List.of();
            }

            Object values = ingredient.getClass().getMethod("getEmiStacks").invoke(ingredient);

            if (!(values instanceof Iterable<?> iterable)) {
                return List.of();
            }

            List<ItemStack> result = new ArrayList<>();

            for (Object emiStack : iterable) {
                if (emiStack == null) {
                    continue;
                }

                Object minecraftStack = emiStack.getClass().getMethod("getItemStack").invoke(emiStack);

                if (minecraftStack instanceof ItemStack stack && !stack.isEmpty()) {
                    result.add(stack.copy());
                }
            }

            return result;
        } catch (Throwable ignored) {
            return List.of();
        }
    }
}
