package dev.conflux.multiid;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

final class FtbFilterCompat {
    private FtbFilterCompat() {
    }

    static List<ItemStack> expand(ItemStack stack) {
        List<ItemStack> expanded = tryExpand(stack);

        if (!expanded.isEmpty()) {
            return expanded;
        }

        return List.of(stack.copy());
    }

    private static List<ItemStack> tryExpand(ItemStack stack) {
        try {
            Minecraft minecraft = Minecraft.getInstance();

            if (minecraft.level == null) {
                return List.of();
            }

            Class<?> systemClass = Class.forName(
                "dev.ftb.mods.ftbquests.integration.item_filtering.ItemMatchingSystem"
            );
            Field instanceField = systemClass.getField("INSTANCE");
            Object instance = instanceField.get(null);
            Method matchingMethod = null;

            for (Method method : systemClass.getMethods()) {
                int parameterCount = method.getParameterCount();

                if (
                    method.getName().equals("getAllMatchingStacks") &&
                    (parameterCount == 1 || parameterCount == 2)
                ) {
                    matchingMethod = method;
                    break;
                }
            }

            if (matchingMethod == null) {
                return List.of();
            }

            Object values;

            if (matchingMethod.getParameterCount() == 1) {
                values = matchingMethod.invoke(instance, stack);
            } else if (matchingMethod.getParameterCount() == 2) {
                values = matchingMethod.invoke(
                    instance,
                    stack,
                    minecraft.level.registryAccess()
                );
            } else {
                return List.of();
            }

            if (!(values instanceof Iterable<?> iterable)) {
                return List.of();
            }

            List<ItemStack> result = new ArrayList<>();

            for (Object value : iterable) {
                if (value instanceof ItemStack itemStack && !itemStack.isEmpty()) {
                    result.add(itemStack.copy());
                }
            }

            return result;
        } catch (Throwable ignored) {
            return List.of();
        }
    }
}
