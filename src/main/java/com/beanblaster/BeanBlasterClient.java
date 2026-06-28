package com.beanblaster;

import com.beanblaster.command.BlasterCommand;
import com.beanblaster.config.ConfigManager;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BeanBlasterClient implements ClientModInitializer {
    public static final String MOD_ID = "beanblaster";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        BlasterCommand.register();
        LOGGER.info("Bean Blaster Calculator loaded.");
    }
}
