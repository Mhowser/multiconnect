package net.earthcomputer.multiconnect.mixin;

import com.google.common.collect.BiMap;
import net.earthcomputer.multiconnect.impl.IInt2ObjectBiMap;
import net.earthcomputer.multiconnect.impl.ISimpleRegistry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Int2ObjectBiMap;
import net.minecraft.util.registry.SimpleRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Mixin(SimpleRegistry.class)
public abstract class MixinSimpleRegistry<T> implements ISimpleRegistry<T> {

    @Shadow @Final protected Int2ObjectBiMap<T> indexedEntries;
    @Shadow @Final protected BiMap<Identifier, T> entries;
    @Shadow protected Object[] randomEntries;
    @Shadow private int nextId;

    @Shadow public abstract <V extends T> V set(int int_1, Identifier identifier_1, V object_1);

    @Accessor
    @Override
    public abstract int getNextId();

    @Accessor
    @Override
    public abstract void setNextId(int nextId);

    @Accessor
    @Override
    public abstract Int2ObjectBiMap<T> getIndexedEntries();

    @Accessor
    @Override
    public abstract BiMap<Identifier, T> getEntries();

    @Unique private List<Consumer<T>> registerListeners = new ArrayList<>(0);
    @Unique private List<Consumer<T>> unregisterListeners = new ArrayList<>(0);

    @Override
    public void register(T t, int id, Identifier name, boolean sideEffects) {
        for (int remapId = getNextId(); remapId > id; remapId--) {
            T toRemap = indexedEntries.get(remapId - 1);
            //noinspection unchecked
            ((IInt2ObjectBiMap<T>) indexedEntries).remove(toRemap);
            indexedEntries.put(toRemap, remapId);
        }
        setNextId(getNextId() + 1);
        set(id, name, t);

        if (sideEffects)
            registerListeners.forEach(listener -> listener.accept(t));
    }

    @Override
    public void registerInPlace(T t, int id, Identifier name, boolean sideEffects) {
        if (id == getNextId())
            setNextId(id + 1);
        set(id, name, t);

        if (sideEffects)
            registerListeners.forEach(listener -> listener.accept(t));
    }

    @Override
    public void unregister(T t, boolean sideEffects) {
        if (!entries.containsValue(t))
            return;

        int id = indexedEntries.getId(t);
        //noinspection unchecked
        ((IInt2ObjectBiMap<T>) indexedEntries).remove(t);
        entries.inverse().remove(t);

        for (int remapId = id; remapId < getNextId() - 1; remapId++) {
            T toRemap = indexedEntries.get(remapId + 1);
            //noinspection unchecked
            ((IInt2ObjectBiMap<T>) indexedEntries).remove(toRemap);
            indexedEntries.put(toRemap, remapId);
        }
        setNextId(getNextId() - 1);

        randomEntries = null;

        if (sideEffects)
            unregisterListeners.forEach(listener -> listener.accept(t));
    }

    @Override
    public void purge(T t, boolean sideEffects) {
        if (!entries.containsValue(t))
            return;

        int id = indexedEntries.getId(t);
        //noinspection unchecked
        ((IInt2ObjectBiMap<T>) indexedEntries).remove(t);
        entries.inverse().remove(t);

        if (id == getNextId() - 1)
            setNextId(id);

        randomEntries = null;

        if (sideEffects)
            unregisterListeners.forEach(listener -> listener.accept(t));
    }

    @Override
    public void clear(boolean sideEffects) {
        if (sideEffects) {
            for (int id = getNextId() - 1; id >= 0; id--) {
                T value = indexedEntries.get(id);
                if (value != null)
                    unregisterListeners.forEach(listener -> listener.accept(value));
            }
        }
        entries.clear();
        indexedEntries.clear();
        randomEntries = null;
        setNextId(0);
    }

    @Override
    public void addRegisterListener(Consumer<T> listener) {
        registerListeners.add(listener);
    }

    @Override
    public void addUnregisterListener(Consumer<T> listener) {
        unregisterListeners.add(listener);
    }

    @Override
    public SimpleRegistry<T> copy() {
        SimpleRegistry<T> newRegistry = new SimpleRegistry<>();
        for (T t : indexedEntries) {
            newRegistry.set(indexedEntries.getId(t), entries.inverse().get(t), t);
        }
        return newRegistry;
    }

    @Override
    public void dump() {
        for (int id = 0; id < nextId; id++) {
            try {
                T val = indexedEntries.get(id);
                if (val != null)
                    System.out.println(id + ": " + entries.inverse().get(val));
            } catch (Throwable t) {
                System.out.println(id + ": ERROR: " + t);
            }
        }
    }
}
