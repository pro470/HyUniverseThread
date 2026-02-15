package com.techphonesnews.HyUniverseThread;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.common.util.FormatUtil;
import com.hypixel.hytale.component.ComponentRegistry;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.function.consumer.BooleanConsumer;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.logger.sentry.SkipSentryException;
import com.hypixel.hytale.metrics.ExecutorMetricsRegistry;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.modules.time.TimeSystem;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.storage.resources.DiskResourceStorageProvider;
import com.hypixel.hytale.server.core.util.thread.TickingThread;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

import javax.annotation.Nonnull;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class UniverseThread extends TickingThread implements Executor, ExecutorMetricsRegistry.ExecutorMetric {
    @Nonnull
    private final AtomicBoolean alive = new AtomicBoolean(true);
    @Nonnull
    private final HytaleLogger logger;
    private Store<EntityStore> store;
    private boolean isPaused = false;
    private final Deque<Runnable> taskQueue = new LinkedBlockingDeque<>();
    private final AtomicBoolean acceptingTasks = new AtomicBoolean(true);
    public final static String name = "UniverseThread";
    @Nonnull
    public static final ComponentRegistry<EntityStore> REGISTRY;
    private final Map<PluginIdentifier, CopyOnWriteArrayList<BooleanConsumer>> shutdownTasksMap =
        new Object2ObjectLinkedOpenHashMap<>();
    private final Map<PluginIdentifier, ComponentRegistryProxy<EntityStore>> entityStoreRegistryMap = new Object2ObjectLinkedOpenHashMap<>();
    private long tick;
    private ResourceType<EntityStore, TimeResource> timeResourceType;

    public UniverseThread(int tps) {
        super(name, tps, false);
        this.logger = HytaleLogger.get(name);
    }

    public UniverseThread(HyUniverseConfig config) {
        super(name, config.getTps(), false);
        this.logger = HytaleLogger.get(name);
    }

    protected void setup() {
        ComponentRegistryProxy<EntityStore> hyUniverseProxy = getUniverseEntityStoreRegistry(HyUniverseThread.get().getIdentifier());
        this.timeResourceType = hyUniverseProxy.registerResource(TimeResource.class, "Time", TimeResource.CODEC);
        hyUniverseProxy.registerSystem(new TimeSystem(this.timeResourceType));
    }

    @Override
    protected void tick(float dt) {
        if (this.alive.get()) {
            TimeResource worldTimeResource = this.store.getResource(this.timeResourceType);
            dt *= worldTimeResource.getTimeDilationModifier();
            AssetRegistry.ASSET_LOCK.readLock().lock();

            try {
                this.consumeTaskQueue();
                if (!this.isPaused) {
                    this.store.tick(dt);
                } else {
                    this.store.pausedTick(dt);
                }
                this.consumeTaskQueue();
            } finally {
                AssetRegistry.ASSET_LOCK.readLock().unlock();
            }
            ++this.tick;
        }
    }

    protected void onStart() {
        Universe.get().getUniverseReady().join();
        DiskResourceStorageProvider.DiskResourceStorage
            resourceStorage = new DiskResourceStorageProvider.DiskResourceStorage(HyUniverseThread.get().getDataDirectory().resolve("resources"));
        this.store = REGISTRY.addStore(Objects.requireNonNull(Universe.get().getDefaultWorld()).getEntityStore(), resourceStorage, (store) -> this.store = store);
    }

    @Override
    protected void onShutdown() {
        this.consumeTaskQueue();
        this.store.shutdown();
        this.consumeTaskQueue();
    }

    @Override
    public void execute(@Nonnull Runnable command) {
        if (!this.acceptingTasks.get()) {
            throw new SkipSentryException(new IllegalThreadStateException("Universe thread is not accepting tasks: " + name + ", " + this.getThread()));
        } else {
            this.taskQueue.offer(command);
        }
    }


    public void consumeTaskQueue() {
        this.debugAssertInTickingThread();
        int tickStepNanos = this.getTickStepNanos();

        Runnable runnable;
        while ((runnable = this.taskQueue.poll()) != null) {
            try {
                long before = System.nanoTime();
                runnable.run();
                long after = System.nanoTime();
                long diff = after - before;
                if (diff > (long) tickStepNanos) {
                    this.logger.at(Level.WARNING).log("Task took %s ns: %s", FormatUtil.nanosToString(diff), runnable);
                }
            } catch (Exception t) {
                this.logger.at(Level.SEVERE).withCause(t).log("Failed to run task!");
            }
        }

    }

    public long getTick() {
        return tick;
    }

    public void setPaused(boolean paused) {
        isPaused = paused;
    }

    public ResourceType<EntityStore, TimeResource> getTimeResourceType() {
        return timeResourceType;
    }

    @Nonnull
    public ComponentRegistryProxy<EntityStore> getUniverseEntityStoreRegistry(PluginIdentifier pluginIdentifier) {
        ComponentRegistryProxy<EntityStore> proxy = entityStoreRegistryMap.get(pluginIdentifier);
        if (proxy == null) {
            CopyOnWriteArrayList<BooleanConsumer> copyOnWriteArrayList = shutdownTasksMap.computeIfAbsent(pluginIdentifier, _ -> new CopyOnWriteArrayList<>());

            entityStoreRegistryMap.put(pluginIdentifier, new ComponentRegistryProxy<>(copyOnWriteArrayList, REGISTRY));
            proxy = entityStoreRegistryMap.get(pluginIdentifier);
        }

        return proxy;
    }

    public void shutdown(PluginIdentifier pluginIdentifier) {
        if (HytaleServer.get().isShuttingDown()) return;

        CopyOnWriteArrayList<BooleanConsumer> shutdownTasks = shutdownTasksMap.get(pluginIdentifier);
        if (shutdownTasks == null) return;

        for (int i = shutdownTasks.size() - 1; i >= 0; --i) {
            shutdownTasks.get(i).accept(false);
        }
    }

    static {
        REGISTRY = new ComponentRegistry<>();
    }
}
