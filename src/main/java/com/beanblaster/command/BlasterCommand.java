package com.beanblaster.command;

import com.beanblaster.calculation.BeanBlasterCalculator;
import com.beanblaster.config.ConfigManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.Locale;
import java.util.Optional;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class BlasterCommand {
    private static final int MIN_COORDINATE = -30_000_000;
    private static final int MAX_COORDINATE = 30_000_000;

    private BlasterCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                literal("blaster")
                        .then(literal("setup")
                                .executes(context -> setup(context.getSource())))
                        .then(literal("help")
                                .executes(context -> help(context.getSource())))
                        .then(argument(
                                "x",
                                IntegerArgumentType.integer(MIN_COORDINATE, MAX_COORDINATE)
                        )
                                .suggests((context, builder) -> {
                                    builder.suggest(MathHelper.floor(context.getSource().getPosition().x));
                                    return builder.buildFuture();
                                })
                                .then(argument(
                                        "z",
                                        IntegerArgumentType.integer(MIN_COORDINATE, MAX_COORDINATE)
                                )
                                        .suggests((context, builder) -> {
                                            builder.suggest(MathHelper.floor(context.getSource().getPosition().z));
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> calculate(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "x"),
                                                IntegerArgumentType.getInteger(context, "z")
                                        ))))
        ));
    }

    private static int setup(FabricClientCommandSource source) {
        MinecraftClient client = source.getClient();

        if (!isInWorld(client)) {
            sendError(source, "This command can only be used while playing in a world or server.");
            return 0;
        }

        HitResult target = client.crosshairTarget;
        if (!(target instanceof BlockHitResult blockHit) || target.getType() != HitResult.Type.BLOCK) {
            sendSetupError(source);
            return 0;
        }

        BlockPos position = blockHit.getBlockPos();
        if (!client.world.getBlockState(position).isOf(Blocks.MAGMA_BLOCK)) {
            sendSetupError(source);
            return 0;
        }

        if (!ConfigManager.save(client, position)) {
            sendError(source, "The magma block was found, but the setup could not be saved.");
            return 0;
        }

        Optional<ConfigManager.Context> context = ConfigManager.resolveContext(client);

        sendHeader(source);
        source.sendFeedback(Text.literal("Setup saved successfully.").formatted(Formatting.GREEN));
        sendLine(source, "Magma block: ", formatCoordinates(position), Formatting.AQUA);

        context.ifPresent(value -> {
            sendLine(source, "Dimension: ", value.dimension(), Formatting.AQUA);
            sendLine(source, "World/server: ", value.displayName(), Formatting.AQUA);
        });

        return 1;
    }

    private static int calculate(FabricClientCommandSource source, int targetX, int targetZ) {
        MinecraftClient client = source.getClient();

        if (!isInWorld(client)) {
            sendError(source, "This command can only be used while playing in a world or server.");
            return 0;
        }

        Optional<ConfigManager.SavedPosition> savedSetup = ConfigManager.get(client);
        if (savedSetup.isEmpty()) {
            sendHeader(source);
            sendWarning(source, "Bean Blaster setup has not been completed for this world, server, or dimension.");
            source.sendFeedback(Text.literal("Look directly at the blaster's magma block and run:")
                    .formatted(Formatting.GRAY));
            source.sendFeedback(Text.literal("  ").append(commandText("/blaster setup", true)));
            source.sendFeedback(Text.literal("For complete instructions, run:").formatted(Formatting.GRAY));
            source.sendFeedback(Text.literal("  ").append(commandText("/blaster help", true)));
            return 0;
        }

        ConfigManager.SavedPosition magma = savedSetup.get();
        BeanBlasterCalculator.Result result = BeanBlasterCalculator.calculate(
                magma.x,
                magma.z,
                targetX,
                targetZ
        );

        sendHeader(source);
        sendLine(source, "Target: ", "X=" + targetX + ", Z=" + targetZ, Formatting.AQUA);
        sendLine(
                source,
                "Bean Blaster: ",
                "X=" + magma.x + ", Y=" + magma.y + ", Z=" + magma.z,
                Formatting.AQUA
        );

        if (!result.success()) {
            sendError(source, result.message());
            sendLine(
                    source,
                    "Relative target: ",
                    formatDecimal(result.relativeX()) + ", " + formatDecimal(result.relativeZ()),
                    Formatting.RED
            );
            return 0;
        }

        sendLine(source, "Sector: ", result.quadrant(), Formatting.LIGHT_PURPLE);
        sendDispenser(source, result.firstLoad());
        sendDispenser(source, result.secondLoad());
        sendLine(
                source,
                "Relative X/Z: ",
                formatDecimal(result.relativeX()) + " / " + formatDecimal(result.relativeZ()),
                Formatting.GRAY
        );
        sendLine(
                source,
                "Crossover X/Z: ",
                formatDecimal(result.crossoverX()) + " / " + formatDecimal(result.crossoverZ()),
                Formatting.GRAY
        );

        for (String warning : result.warnings()) {
            sendWarning(source, warning);
        }

        return 1;
    }

    private static int help(FabricClientCommandSource source) {
        sendHeader(source);
        source.sendFeedback(Text.literal(
                "Calculates the Wind Charges required in each Bean Blaster dispenser."
        ).formatted(Formatting.WHITE));
        source.sendFeedback(Text.literal(
                "A Bean Blaster launches an Ender Pearl toward distant X/Z coordinates."
        ).formatted(Formatting.GRAY));
        source.sendFeedback(Text.empty());

        source.sendFeedback(Text.literal("1. Look directly at the launcher's magma block:")
                .formatted(Formatting.GRAY));
        source.sendFeedback(Text.literal("  ").append(commandText("/blaster setup", true)));

        source.sendFeedback(Text.literal("2. Enter the target X and Z coordinates:")
                .formatted(Formatting.GRAY));
        source.sendFeedback(Text.literal("  ").append(commandText("/blaster 1250 -840", false)));

        source.sendFeedback(Text.literal("3. Load the displayed amounts into the named dispensers.")
                .formatted(Formatting.GRAY));
        source.sendFeedback(Text.empty());

        source.sendFeedback(Text.literal("Commands:").formatted(Formatting.GOLD));
        source.sendFeedback(Text.literal("  /blaster setup").formatted(Formatting.AQUA)
                .append(Text.literal(" — save the magma block").formatted(Formatting.GRAY)));
        source.sendFeedback(Text.literal("  /blaster <x> <z>").formatted(Formatting.AQUA)
                .append(Text.literal(" — calculate dispenser loads").formatted(Formatting.GRAY)));
        source.sendFeedback(Text.literal("  /blaster help").formatted(Formatting.AQUA)
                .append(Text.literal(" — show this help").formatted(Formatting.GRAY)));
        source.sendFeedback(Text.literal("Saved in config/" + ConfigManager.FILE_NAME)
                .formatted(Formatting.DARK_GRAY));

        return 1;
    }

    private static void sendSetupError(FabricClientCommandSource source) {
        sendError(source, "You must look directly at the Bean Blaster's magma block.");
        source.sendFeedback(Text.literal("Then run: ").formatted(Formatting.GRAY)
                .append(commandText("/blaster setup", true)));
    }

    private static void sendDispenser(
            FabricClientCommandSource source,
            BeanBlasterCalculator.Load load
    ) {
        source.sendFeedback(Text.literal("Dispenser " + load.dispenser() + ": ")
                .formatted(Formatting.GRAY)
                .append(Text.literal(Integer.toString(load.windCharges()))
                        .formatted(Formatting.YELLOW, Formatting.BOLD))
                .append(Text.literal(" Wind Charges").formatted(Formatting.GREEN)));
    }

    private static boolean isInWorld(MinecraftClient client) {
        return client != null && client.world != null && client.player != null;
    }

    private static void sendHeader(FabricClientCommandSource source) {
        source.sendFeedback(Text.literal("— Bean Blaster Calculator —")
                .formatted(Formatting.GOLD, Formatting.BOLD));
    }

    private static void sendWarning(FabricClientCommandSource source, String message) {
        source.sendFeedback(Text.literal("Warning: " + message).formatted(Formatting.GOLD));
    }

    private static void sendError(FabricClientCommandSource source, String message) {
        source.sendError(Text.literal(message).formatted(Formatting.RED));
    }

    private static void sendLine(
            FabricClientCommandSource source,
            String label,
            String value,
            Formatting valueColor
    ) {
        source.sendFeedback(Text.literal(label).formatted(Formatting.GRAY)
                .append(Text.literal(value).formatted(valueColor)));
    }

    private static Text commandText(String command, boolean runImmediately) {
        ClickEvent clickEvent = runImmediately
                ? new ClickEvent.RunCommand(command)
                : new ClickEvent.SuggestCommand(command);

        Style style = Style.EMPTY
                .withColor(Formatting.AQUA)
                .withUnderline(true)
                .withClickEvent(clickEvent)
                .withHoverEvent(new HoverEvent.ShowText(
                        Text.literal(runImmediately ? "Click to run" : "Click to edit")
                ));

        return Text.literal(command).setStyle(style);
    }

    private static String formatCoordinates(BlockPos position) {
        return "X=" + position.getX() + ", Y=" + position.getY() + ", Z=" + position.getZ();
    }

    private static String formatDecimal(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }
}
