package com.techphonesnews.HyUniverseThread;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class HyUniverseConfig {
    public static final BuilderCodec<HyUniverseConfig> CODEC;

    private Integer Tps = 10;

    public HyUniverseConfig() {
    }

    private void setTps(Integer tps) {
        Tps = tps;
    }

    public Integer getTps() {
        return Tps;
    }

    static {
        CODEC = BuilderCodec.<HyUniverseConfig>builder(HyUniverseConfig.class, HyUniverseConfig::new).append(
            new KeyedCodec<Integer>("TPS", Codec.INTEGER),
            HyUniverseConfig::setTps,
            HyUniverseConfig::getTps
        ).documentation("Set the tps the Universe Thread should run").add().build();
    }
}
