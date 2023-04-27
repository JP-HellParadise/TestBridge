package net.jp.hellparadise.testbridge.helpers;

import java.util.*;
import java.util.stream.Collectors;

import logisticspipes.utils.item.ItemIdentifierStack;

public class IISHelper {

    /**
     * Combines a list of duplicate ItemIdentifierStack objects into a non-duplicate list with combined amounts
     *
     * @param stacks list of ItemIdentifierStack objects
     * @return list of combined ItemIdentifierStack objects
     */
    public static List<ItemIdentifierStack> combine(List<ItemIdentifierStack> stacks) {
        return stacks.stream()
            // Filter out the null and empty stacks and group them by item, summing their stack sizes
            .filter(
                stack -> stack != null && !stack.makeNormalStack()
                    .isEmpty())
            .collect(
                Collectors
                    .groupingBy(ItemIdentifierStack::getItem, Collectors.summingInt(ItemIdentifierStack::getStackSize)))
            .entrySet()
            .stream()
            // Create a new list of ItemIdentifierStack objects from the grouped stacks
            .map(e -> new ItemIdentifierStack(e.getKey(), e.getValue()))
            .collect(Collectors.toList());
    }
}
