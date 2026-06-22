package net.minecraft.client;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.BanDetails;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.minecraft.UserApiService.UserFlag;
import com.mojang.authlib.minecraft.UserApiService.UserProperties;
import com.mojang.authlib.yggdrasil.ProfileActionType;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.blaze3d.TracyFrameCapture;
import com.mojang.blaze3d.opengl.GlBackend;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.BackendOptions;
import com.mojang.blaze3d.platform.ClientShutdownWatchdog;
import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.FramerateLimitTracker;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.IconSet;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.MessageBox;
import com.mojang.blaze3d.platform.TextInputManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import com.mojang.blaze3d.shaders.GpuDebugOptions;
import com.mojang.blaze3d.systems.BackendCreationException;
import com.mojang.blaze3d.systems.GpuBackend;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.TimerQuery;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.datafixers.DataFixer;
import com.mojang.jtracy.DiscontinuousFrame;
import com.mojang.jtracy.TracyClient;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.net.Proxy;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.Optionull;
import net.minecraft.ReportType;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.SystemReport;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.entity.ClientMannequin;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryList;
import net.minecraft.client.gui.components.debugchart.ProfilerPieChart;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.providers.FreeTypeUtil;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.BanNoticeScreens;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.OutOfMemoryScreen;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.main.SilentInitException;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.chat.ChatAbilities;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.client.multiplayer.chat.ChatRestriction;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleResources;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.LocalPlayerResolver;
import net.minecraft.client.profiling.ClientMetricsSamplersProvider;
import net.minecraft.client.quickplay.QuickPlay;
import net.minecraft.client.quickplay.QuickPlayLog;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.GpuWarnlistManager;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.WindowRenderState;
import net.minecraft.client.renderer.texture.SkinTextureDownloader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.ClientPackSource;
import net.minecraft.client.resources.DryFoliageColorReloadListener;
import net.minecraft.client.resources.FoliageColorReloadListener;
import net.minecraft.client.resources.GrassColorReloadListener;
import net.minecraft.client.resources.MapTextureManager;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.client.resources.SplashManager;
import net.minecraft.client.resources.WaypointStyleManager;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.telemetry.ClientTelemetryManager;
import net.minecraft.client.telemetry.TelemetryProperty;
import net.minecraft.client.telemetry.events.GameLoadTimesEvent;
import net.minecraft.client.tutorial.Tutorial;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.SimpleGizmoCollector;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketProcessor;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.KeybindResolver;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.Dialogs;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.server.level.progress.LoggingLevelLoadListener;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DialogTags;
import net.minecraft.util.CommonLinks;
import net.minecraft.util.FileUtil;
import net.minecraft.util.FileZipper;
import net.minecraft.util.MemoryReserve;
import net.minecraft.util.ModCheck;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.profiling.ContinuousProfiler;
import net.minecraft.util.profiling.EmptyProfileResults;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.SingleTickProfiler;
import net.minecraft.util.profiling.Zone;
import net.minecraft.util.profiling.metrics.profiling.ActiveMetricsRecorder;
import net.minecraft.util.profiling.metrics.profiling.InactiveMetricsRecorder;
import net.minecraft.util.profiling.metrics.profiling.MetricsRecorder;
import net.minecraft.util.profiling.metrics.storage.MetricsPersister;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.io.FileUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class Minecraft extends ReentrantBlockableEventLoop<Runnable> implements WindowEventHandler, net.neoforged.neoforge.client.extensions.IMinecraftExtension {
    private static Minecraft instance;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_TICKS_PER_UPDATE = 10;
    public static final Identifier DEFAULT_FONT = Identifier.withDefaultNamespace("default");
    public static final Identifier UNIFORM_FONT = Identifier.withDefaultNamespace("uniform");
    public static final Identifier ALT_FONT = Identifier.withDefaultNamespace("alt");
    private static final Identifier REGIONAL_COMPLIANCIES = Identifier.withDefaultNamespace("regional_compliancies.json");
    private static final CompletableFuture<Unit> RESOURCE_RELOAD_INITIAL_TASK = CompletableFuture.completedFuture(Unit.INSTANCE);
    private static final Component SOCIAL_INTERACTIONS_NOT_AVAILABLE = Component.translatable("multiplayer.socialInteractions.not_available");
    private static final Component SAVING_LEVEL = Component.translatable("menu.savingLevel");
    public static final String UPDATE_DRIVERS_ADVICE = "Please make sure you have up-to-date drivers (see aka.ms/mcdriver for instructions).";
    private final long canary = Double.doubleToLongBits(Math.PI);
    private final Path resourcePackDirectory;
    private final CompletableFuture<@Nullable ProfileResult> profileFuture;
    private final TextureManager textureManager;
    private final ShaderManager shaderManager;
    private final DataFixer fixerUpper;
    private final Window window;
    private final TextInputManager textInputManager;
    private final DeltaTracker.Timer deltaTracker = new DeltaTracker.Timer(20.0F, 0L, this::getTickTargetMillis);
    private final RenderBuffers renderBuffers;
    public final LevelRenderer levelRenderer;
    private final EntityRenderDispatcher entityRenderDispatcher;
    private final BlockModelResolver blockModelResolver;
    private final ItemModelResolver itemModelResolver;
    private final MapRenderer mapRenderer;
    public final ParticleEngine particleEngine;
    private final ParticleResources particleResources;
    private final User user;
    public final Font font;
    public final Font fontFilterFishy;
    public final GameRenderer gameRenderer;
    public final Gui gui;
    public final Options options;
    public final DebugScreenEntryList debugEntries;
    private final HotbarManager hotbarManager;
    public final MouseHandler mouseHandler;
    public final KeyboardHandler keyboardHandler;
    private InputType lastInputType = InputType.NONE;
    public final File gameDirectory;
    private final String launchedVersion;
    private final String versionType;
    private final Proxy proxy;
    private final boolean offlineDeveloperMode;
    private final LevelStorageSource levelSource;
    private final boolean demo;
    private final boolean allowsMultiplayer;
    private final boolean allowsChat;
    private final ReloadableResourceManager resourceManager;
    private final VanillaPackResources vanillaPackResources;
    private final DownloadedPackSource downloadedPackSource;
    private final PackRepository resourcePackRepository;
    private final LanguageManager languageManager;
    private final BlockColors blockColors;
    private final RenderTarget mainRenderTarget;
    private final @Nullable TracyFrameCapture tracyFrameCapture;
    private final SoundManager soundManager;
    private final MusicManager musicManager;
    private final FontManager fontManager;
    private final SplashManager splashManager;
    private final GpuWarnlistManager gpuWarnlistManager;
    private final PeriodicNotificationManager regionalCompliancies = new PeriodicNotificationManager(REGIONAL_COMPLIANCIES, Minecraft::countryEqualsISO3);
    private final UserApiService userApiService;
    private final CompletableFuture<UserProperties> userPropertiesFuture;
    private final SkinManager skinManager;
    private final AtlasManager atlasManager;
    private final ModelManager modelManager;
    private final MapTextureManager mapTextureManager;
    private final WaypointStyleManager waypointStyles;
    private final ToastManager toastManager;
    private final Tutorial tutorial;
    private final PlayerSocialManager playerSocialManager;
    private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
    private final ClientTelemetryManager telemetryManager;
    private final ProfileKeyPairManager profileKeyPairManager;
    private final RealmsDataFetcher realmsDataFetcher;
    private final QuickPlayLog quickPlayLog;
    private final Services services;
    private final PlayerSkinRenderCache playerSkinRenderCache;
    public @Nullable MultiPlayerGameMode gameMode;
    public @Nullable ClientLevel level;
    public @Nullable LocalPlayer player;
    private @Nullable IntegratedServer singleplayerServer;
    private @Nullable Connection pendingConnection;
    private boolean isLocalServer;
    public @Nullable Entity crosshairPickEntity;
    public @Nullable HitResult hitResult;
    private int rightClickDelay;
    public int missTime;
    private volatile boolean pause;
    private long lastNanoTime = Util.getNanos();
    private long lastTime;
    private int frames;
    public @Nullable Screen screen;
    private @Nullable Overlay overlay;
    private boolean clientLevelTeardownInProgress;
    private Thread gameThread;
    private volatile boolean running;
    private static int fps;
    private long frameTimeNs;
    private final FramerateLimitTracker framerateLimitTracker;
    public boolean wireframe;
    public boolean smartCull = true;
    private long lastActiveTime = Util.getMillis();
    private @Nullable CompletableFuture<Void> pendingReload;
    private @Nullable TutorialToast socialInteractionsToast;
    private int fpsPieRenderTicks;
    private final ContinuousProfiler fpsPieProfiler;
    private MetricsRecorder metricsRecorder = InactiveMetricsRecorder.INSTANCE;
    private final ResourceLoadStateTracker reloadStateTracker = new ResourceLoadStateTracker();
    private long savedCpuDuration;
    private double gpuUtilization;
    private TimerQuery.@Nullable FrameProfile currentFrameProfile;
    private final GameNarrator narrator;
    private final ChatListener chatListener;
    private ReportingContext reportingContext;
    private final CommandHistory commandHistory;
    private final DirectoryValidator directoryValidator;
    private boolean gameLoadFinished;
    private final long clientStartTimeMs;
    private long clientTickCount;
    private final PacketProcessor packetProcessor;
    private final SimpleGizmoCollector perTickGizmos = new SimpleGizmoCollector();
    private List<SimpleGizmoCollector.GizmoInstance> drainedLatestTickGizmos = new ArrayList<>();

    public Minecraft(GameConfig gameConfig) {
        super("Client", true);
        instance = this;
        this.clientStartTimeMs = System.currentTimeMillis();
        this.gameDirectory = gameConfig.location.gameDirectory;
        File assetsDirectory = gameConfig.location.assetDirectory;
        this.resourcePackDirectory = gameConfig.location.resourcePackDirectory.toPath();
        this.launchedVersion = gameConfig.game.launchVersion;
        this.versionType = gameConfig.game.versionType;
        Path gameDirPath = this.gameDirectory.toPath();
        this.directoryValidator = LevelStorageSource.parseValidator(gameDirPath.resolve("allowed_symlinks.txt"));
        ClientPackSource clientPackSource = new ClientPackSource(gameConfig.location.getExternalAssetSource(), this.directoryValidator);
        this.downloadedPackSource = new DownloadedPackSource(this, gameDirPath.resolve("downloads"), gameConfig.user);
        RepositorySource directoryPacks = new FolderRepositorySource(
            this.resourcePackDirectory, PackType.CLIENT_RESOURCES, PackSource.DEFAULT, this.directoryValidator
        );
        this.resourcePackRepository = new PackRepository(clientPackSource, this.downloadedPackSource.createRepositorySource(), directoryPacks);
        this.vanillaPackResources = clientPackSource.getVanillaPack();
        this.proxy = gameConfig.user.proxy;
        this.offlineDeveloperMode = gameConfig.game.offlineDeveloperMode;
        YggdrasilAuthenticationService authenticationService = this.offlineDeveloperMode
            ? YggdrasilAuthenticationService.createOffline(this.proxy)
            : new YggdrasilAuthenticationService(this.proxy);
        this.services = Services.create(authenticationService, this.gameDirectory);
        this.user = gameConfig.user.user;
        this.profileFuture = this.offlineDeveloperMode
            ? CompletableFuture.completedFuture(null)
            : CompletableFuture.supplyAsync(() -> this.services.sessionService().fetchProfile(this.user.getProfileId(), true), Util.nonCriticalIoPool());
        this.userApiService = this.createUserApiService(authenticationService, gameConfig);
        this.userPropertiesFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return this.userApiService.fetchProperties();
            } catch (AuthenticationException var2x) {
                LOGGER.error("Failed to fetch user properties", (Throwable)var2x);
                return UserApiService.OFFLINE_PROPERTIES;
            }
        }, Util.nonCriticalIoPool());
        LOGGER.info("Setting user: {}", this.user.getName());
        this.demo = gameConfig.game.demo;
        this.allowsMultiplayer = !gameConfig.game.disableMultiplayer;
        this.allowsChat = !gameConfig.game.disableChat;
        this.singleplayerServer = null;
        KeybindResolver.setKeyResolver(KeyMapping::createNameSupplier);
        this.fixerUpper = DataFixers.getDataFixer();
        this.gameThread = Thread.currentThread();
        this.options = new Options(this, this.gameDirectory);
        this.debugEntries = new DebugScreenEntryList(this.gameDirectory, this.fixerUpper);
        this.toastManager = new ToastManager(this, this.options);
        boolean lastStartWasClean = this.options.startedCleanly;
        this.options.startedCleanly = false;
        this.options.save();
        this.running = true;
        this.tutorial = new Tutorial(this, this.options);
        this.hotbarManager = new HotbarManager(gameDirPath, this.fixerUpper);
        LOGGER.info("Backend library: {}", RenderSystem.getBackendDescription());
        DisplayData displayData = gameConfig.display;
        if (this.options.overrideHeight > 0 && this.options.overrideWidth > 0) {
            displayData = gameConfig.display.withSize(this.options.overrideWidth, this.options.overrideHeight);
        }

        if (!lastStartWasClean) {
            displayData = displayData.withFullscreen(false);
            this.options.fullscreenVideoModeString = null;
            LOGGER.warn("Detected unexpected shutdown during last game startup: resetting fullscreen mode");
        }

        Util.timeSource = RenderSystem.initBackendSystem(new BackendOptions(this.options.exclusiveFullscreen().get()));
        GpuBackend[] backends = new GpuBackend[]{new GlBackend()};
        StringBuilder errorMsgBuilder = new StringBuilder("No supported graphics backend was found.");
        Window windowCandidate = null;
        GpuDevice device = null;
        // Neo: Enable GL debug labels and synchronous GL debug output via FML config
        boolean enableGlDebug = net.neoforged.fml.loading.FMLConfig.getBoolConfigValue(net.neoforged.fml.loading.FMLConfig.ConfigValue.DEBUG_OPENGL);

        for (GpuBackend backend : backends) {
            try {
                windowCandidate = new Window(this, displayData, this.options.fullscreenVideoModeString, this.createTitle(), backend);
                device = windowCandidate.backend()
                    .createDevice(
                        windowCandidate.handle(),
                        (id, type) -> this.getShaderManager().getShader(id, type),
                        new GpuDebugOptions(this.options.glDebugVerbosity, SharedConstants.DEBUG_SYNCHRONOUS_GL_LOGS || enableGlDebug, gameConfig.game.renderDebugLabels || enableGlDebug)
                    );
                RenderSystem.initRenderer(device);
                break;
            } catch (BackendCreationException var24) {
                LOGGER.error("Failed to create backend {}", backend.getName(), var24);
                errorMsgBuilder.append("\n\n- Tried ").append(backend.getName()).append(": \n  ").append(var24.getMessage());
                if (windowCandidate != null) {
                    windowCandidate.close();
                    windowCandidate = null;
                }
            }
        }

        if (windowCandidate == null) {
            String errorMsg = errorMsgBuilder.toString();
            MessageBox.error(errorMsg);
            throw new Window.WindowInitFailed(errorMsg);
        } else {
            this.window = windowCandidate;
            this.textInputManager = new TextInputManager(this.window);
            this.window.setWindowCloseCallback(new Runnable() {
                private boolean threadStarted;

                {
                    Objects.requireNonNull(Minecraft.this);
                }

                @Override
                public void run() {
                    if (!this.threadStarted) {
                        this.threadStarted = true;
                        ClientShutdownWatchdog.startShutdownWatchdog(Minecraft.this, gameConfig.location.gameDirectory, Minecraft.this.gameThread.threadId());
                    }
                }
            });
            GameLoadTimesEvent.INSTANCE.endStep(TelemetryProperty.LOAD_TIME_PRE_WINDOW_MS);

            try {
                this.window.setIcon(this.vanillaPackResources, SharedConstants.getCurrentVersion().stable() ? IconSet.RELEASE : IconSet.SNAPSHOT);
            } catch (IOException var23) {
                LOGGER.error("Couldn't set icon", (Throwable)var23);
            }

            // FORGE: Move mouse and keyboard handler setup further below
            this.mouseHandler = new MouseHandler(this);
            this.keyboardHandler = new KeyboardHandler(this);
            this.options.applyGraphicsPreset(this.options.graphicsPreset().get());
            LOGGER.info("Using optional rendering extensions: {}", String.join(", ", RenderSystem.getDevice().getEnabledExtensions()));
            this.mainRenderTarget = net.neoforged.neoforge.client.ClientHooks.instantiateMainTarget(this.window.getWidth(), this.window.getHeight());
            this.resourceManager = new ReloadableResourceManager(PackType.CLIENT_RESOURCES);
            net.neoforged.neoforge.client.loading.ClientModLoader.setupModResourcePacks(this.resourcePackRepository);
            //Move client bootstrap to after mod loading so that events can be fired for it.
            ClientBootstrap.bootstrap();
            this.resourcePackRepository.reload();
            this.options.loadSelectedResourcePacks(this.resourcePackRepository);
            this.languageManager = new LanguageManager(this.options.languageCode, languageData -> {
                if (this.player != null) {
                    this.player.connection.updateSearchTrees();
                }
            });
            this.resourceManager.registerReloadListener(this.languageManager);
            this.textureManager = new TextureManager(this.resourceManager);
            this.resourceManager.registerReloadListener(this.textureManager);
            this.shaderManager = new ShaderManager(this.textureManager, this::triggerResourcePackRecovery);
            this.resourceManager.registerReloadListener(this.shaderManager);
            SkinTextureDownloader skinTextureDownloader = new SkinTextureDownloader(this.proxy, this.textureManager, this);
            this.skinManager = new SkinManager(assetsDirectory.toPath().resolve("skins"), this.services, skinTextureDownloader, this);
            this.levelSource = new LevelStorageSource(gameDirPath.resolve("saves"), gameDirPath.resolve("backups"), this.directoryValidator, this.fixerUpper);
            this.commandHistory = new CommandHistory(gameDirPath);
            this.musicManager = new MusicManager(this);
            this.soundManager = new SoundManager(this.options);
            this.resourceManager.registerReloadListener(this.soundManager);
            this.splashManager = new SplashManager(this.user);
            this.resourceManager.registerReloadListener(this.splashManager);
            this.atlasManager = new AtlasManager(this.textureManager, this.options.mipmapLevels().get());
            this.resourceManager.registerReloadListener(this.atlasManager);
            ProfileResolver localProfileResolver = new LocalPlayerResolver(this, this.services.profileResolver());
            this.playerSkinRenderCache = new PlayerSkinRenderCache(this.textureManager, this.skinManager, localProfileResolver);
            ClientMannequin.registerOverrides(this.playerSkinRenderCache);
            this.fontManager = new FontManager(this.textureManager, this.atlasManager, this.playerSkinRenderCache);
            this.font = this.fontManager.createFont();
            this.fontFilterFishy = this.fontManager.createFontFilterFishy();
            this.resourceManager.registerReloadListener(this.fontManager);
            this.updateFontOptions();
            this.resourceManager.registerReloadListener(new GrassColorReloadListener());
            this.resourceManager.registerReloadListener(new FoliageColorReloadListener());
            this.resourceManager.registerReloadListener(new DryFoliageColorReloadListener());
            this.window.setErrorSection("Startup");
            RenderSystem.setupDefaultState();
            this.window.setErrorSection("Post startup");
            this.blockColors = BlockColors.createDefault();
            net.neoforged.neoforge.client.model.UnbakedModelParser.init();
            this.modelManager = new ModelManager(this.blockColors, this.atlasManager, this.playerSkinRenderCache);
            this.resourceManager.registerReloadListener(this.modelManager);
            EquipmentAssetManager equipmentAssets = new EquipmentAssetManager();
            this.resourceManager.registerReloadListener(equipmentAssets);
            this.blockModelResolver = new BlockModelResolver(this.modelManager);
            this.itemModelResolver = new ItemModelResolver(this.modelManager);
            this.mapTextureManager = new MapTextureManager(this.textureManager);
            this.mapRenderer = new MapRenderer(this.atlasManager, this.mapTextureManager);

            try {
                int maxSectionBuilders = Runtime.getRuntime().availableProcessors();
                Tesselator.init();
                this.renderBuffers = new RenderBuffers(maxSectionBuilders);
            } catch (OutOfMemoryError var22) {
                MessageBox.error(
                    "Oh no! The game was unable to allocate memory off-heap while trying to start. You may try to free some memory by closing other applications on your computer, check that your system meets the minimum requirements, and try again. If the problem persists, please visit: "
                        + CommonLinks.GENERAL_HELP
                );
                throw new SilentInitException("Unable to allocate render buffers", var22);
            }

            this.playerSocialManager = new PlayerSocialManager(this, this.userApiService);
            this.entityRenderDispatcher = new EntityRenderDispatcher(
                this,
                this.textureManager,
                this.blockModelResolver,
                this.itemModelResolver,
                this.mapRenderer,
                this.atlasManager,
                this.font,
                this.options,
                this.modelManager.entityModels(),
                equipmentAssets,
                this.playerSkinRenderCache
            );
            this.resourceManager.registerReloadListener(this.entityRenderDispatcher);
            this.blockEntityRenderDispatcher = new BlockEntityRenderDispatcher(
                this.font,
                this.modelManager.entityModels(),
                this.blockModelResolver,
                this.itemModelResolver,
                this.entityRenderDispatcher,
                this.atlasManager,
                this.playerSkinRenderCache
            );
            this.resourceManager.registerReloadListener(this.blockEntityRenderDispatcher);
            this.particleResources = new ParticleResources();
            this.resourceManager.registerReloadListener(this.particleResources);
            this.particleEngine = new ParticleEngine(this.level, this.particleResources);
            this.particleResources.onReload(this.particleEngine::clearParticles);
            net.neoforged.neoforge.client.ClientHooks.onRegisterParticleProviders(this.particleResources);
            this.waypointStyles = new WaypointStyleManager();
            this.resourceManager.registerReloadListener(this.waypointStyles);
            this.gameRenderer = new GameRenderer(this, this.entityRenderDispatcher.getItemInHandRenderer(), this.renderBuffers, this.modelManager);
            WindowRenderState windowRenderState = this.gameRenderer.getGameRenderState().windowRenderState;
            windowRenderState.width = this.window.getWidth();
            windowRenderState.height = this.window.getHeight();
            this.levelRenderer = new LevelRenderer(
                this,
                this.entityRenderDispatcher,
                this.blockEntityRenderDispatcher,
                this.renderBuffers,
                this.gameRenderer.getGameRenderState(),
                this.gameRenderer.getFeatureRenderDispatcher()
            );
            this.resourceManager.registerReloadListener(this.levelRenderer);
            this.resourceManager.registerReloadListener(this.levelRenderer.getCloudRenderer());
            this.gpuWarnlistManager = new GpuWarnlistManager();
            this.resourceManager.registerReloadListener(this.gpuWarnlistManager);
            this.resourceManager.registerReloadListener(this.regionalCompliancies);
            // NEO: Moved keyboard and mouse handler setup below ingame gui creation to prevent NPEs in them.
            this.mouseHandler.setup(this.window);
            this.keyboardHandler.setup(this.window);
            this.gui = new Gui(this);
            RealmsClient realmsClient = RealmsClient.getOrCreate(this);
            this.realmsDataFetcher = new RealmsDataFetcher(realmsClient);
            RenderSystem.setErrorCallback(this::onFullscreenError);
            if (this.mainRenderTarget.width != this.window.getWidth() || this.mainRenderTarget.height != this.window.getHeight()) {
                StringBuilder message = new StringBuilder(
                    "Recovering from unsupported resolution ("
                        + this.window.getWidth()
                        + "x"
                        + this.window.getHeight()
                        + ").\nPlease make sure you have up-to-date drivers (see aka.ms/mcdriver for instructions)."
                );

                try {
                    List<String> messages = device.getLastDebugMessages();
                    if (!messages.isEmpty()) {
                        message.append("\n\nReported GL debug messages:\n").append(String.join("\n", messages));
                    }
                } catch (Throwable var21) {
                }

                this.window.setWindowed(this.mainRenderTarget.width, this.mainRenderTarget.height);
                MessageBox.error(message.toString());
            } else if (this.options.fullscreen().get() && !this.window.isFullscreen()) {
                if (lastStartWasClean) {
                    this.window.toggleFullScreen();
                    this.options.fullscreen().set(this.window.isFullscreen());
                } else {
                    this.options.fullscreen().set(false);
                }
            }

            net.neoforged.neoforge.client.ClientHooks.initClientHooks(this, this.resourceManager);
            this.window.updateVsync(this.options.enableVsync().get());
            this.window.updateRawMouseInput(this.options.rawMouseInput().get());
            this.window.setAllowCursorChanges(this.options.allowCursorChanges().get());
            this.window.setDefaultErrorCallback();
            this.resizeGui();
            this.gameRenderer.preloadUiShader(this.vanillaPackResources.asProvider());
            this.telemetryManager = new ClientTelemetryManager(this, this.userApiService, this.user);
            this.profileKeyPairManager = this.offlineDeveloperMode
                ? ProfileKeyPairManager.EMPTY_KEY_MANAGER
                : ProfileKeyPairManager.create(this.userApiService, this.user, gameDirPath);
            this.narrator = new GameNarrator(this);
            this.narrator.checkStatus(this.options.narrator().get() != NarratorStatus.OFF);
            this.chatListener = new ChatListener(this);
            this.chatListener.setMessageDelay(this.options.chatDelay().get());
            this.reportingContext = ReportingContext.create(ReportEnvironment.local(), this.userApiService);
            TitleScreen.registerTextures(this.textureManager);
            LoadingOverlay.registerTextures(this.textureManager);
            this.gameRenderer.registerPanoramaTextures(this.textureManager);
            net.neoforged.neoforge.client.loading.ClientModLoader.finish();
            this.setScreen(new GenericMessageScreen(Component.translatable("gui.loadingMinecraft")));
            List<PackResources> packs = this.resourcePackRepository.openAllSelected();
            this.reloadStateTracker.startReload(ResourceLoadStateTracker.ReloadReason.INITIAL, packs);
            ReloadInstance reloadInstance = this.resourceManager
                .createReload(Util.backgroundExecutor().forName("resourceLoad"), this, RESOURCE_RELOAD_INITIAL_TASK, packs);
            GameLoadTimesEvent.INSTANCE.beginStep(TelemetryProperty.LOAD_TIME_LOADING_OVERLAY_MS);
            Minecraft.GameLoadCookie loadCookie = new Minecraft.GameLoadCookie(realmsClient, gameConfig.quickPlay);
            this.setOverlay(new LoadingOverlay(this, reloadInstance, maybeT -> Util.ifElse(maybeT, t -> this.rollbackResourcePacks(t, loadCookie), () -> {
                if (SharedConstants.IS_RUNNING_IN_IDE && false /* Neo: Disable self-test */) {
                    this.selfTest();
                }

                this.reloadStateTracker.finishReload();
                this.onResourceLoadFinished(loadCookie);
            }), false));
            this.quickPlayLog = QuickPlayLog.of(gameConfig.quickPlay.logPath());
            this.framerateLimitTracker = new FramerateLimitTracker(this.options, this);
            this.fpsPieProfiler = new ContinuousProfiler(Util.timeSource, () -> this.fpsPieRenderTicks, this.framerateLimitTracker::isHeavilyThrottled);
            if (TracyClient.isAvailable() && gameConfig.game.captureTracyImages) {
                this.tracyFrameCapture = new TracyFrameCapture();
            } else {
                this.tracyFrameCapture = null;
            }

            this.packetProcessor = new PacketProcessor(this.gameThread);
        }
    }

    public boolean hasShiftDown() {
        Window window = this.getWindow();
        return InputConstants.isKeyDown(window, 340) || InputConstants.isKeyDown(window, 344);
    }

    public boolean hasControlDown() {
        Window window = this.getWindow();
        return InputConstants.isKeyDown(window, 341) || InputConstants.isKeyDown(window, 345);
    }

    public boolean hasAltDown() {
        Window window = this.getWindow();
        return InputConstants.isKeyDown(window, 342) || InputConstants.isKeyDown(window, 346);
    }

    private void onResourceLoadFinished(Minecraft.@Nullable GameLoadCookie loadCookie) {
        net.neoforged.neoforge.client.ClientHooks.fireResourceLoadFinishedEvent(!this.gameLoadFinished);
        if (!this.gameLoadFinished) {
            this.gameLoadFinished = true;
            this.onGameLoadFinished(loadCookie);
        }
    }

    private void onGameLoadFinished(Minecraft.@Nullable GameLoadCookie cookie) {
        Runnable showScreen = this.buildInitialScreens(cookie);
        GameLoadTimesEvent.INSTANCE.endStep(TelemetryProperty.LOAD_TIME_LOADING_OVERLAY_MS);
        GameLoadTimesEvent.INSTANCE.endStep(TelemetryProperty.LOAD_TIME_TOTAL_TIME_MS);
        GameLoadTimesEvent.INSTANCE.send(this.telemetryManager.getOutsideSessionSender());
        showScreen.run();
        this.options.startedCleanly = true;
        this.options.save();
    }

    public boolean isGameLoadFinished() {
        return this.gameLoadFinished;
    }

    private Runnable buildInitialScreens(Minecraft.@Nullable GameLoadCookie cookie) {
        List<Function<Runnable, Screen>> screens = new ArrayList<>();
        boolean onboardingScreenAdded = this.addInitialScreens(screens);
        Runnable nextStep = () -> {
            if (cookie != null && cookie.quickPlayData.isEnabled()) {
                QuickPlay.connect(this, cookie.quickPlayData.variant(), cookie.realmsClient());
            } else {
                this.setScreen(new TitleScreen(true, new LogoRenderer(onboardingScreenAdded)));
            }
        };

        for (Function<Runnable, Screen> function : Lists.reverse(screens)) {
            Screen screen = function.apply(nextStep);
            nextStep = () -> this.setScreen(screen);
        }

        nextStep = net.neoforged.neoforge.client.loading.ClientModLoader.completeModLoading(nextStep);

        return nextStep;
    }

    private boolean addInitialScreens(List<Function<Runnable, Screen>> screens) {
        boolean onboardingScreenAdded = false;
        if (this.options.onboardAccessibility || SharedConstants.DEBUG_FORCE_ONBOARDING_SCREEN) {
            screens.add(next -> new AccessibilityOnboardingScreen(this.options, next));
            onboardingScreenAdded = true;
        }

        BanDetails multiplayerBan = this.multiplayerBan();
        if (multiplayerBan != null) {
            screens.add(next -> BanNoticeScreens.create(result -> {
                if (result) {
                    Util.getPlatform().openUri(CommonLinks.SUSPENSION_HELP);
                }

                next.run();
            }, multiplayerBan));
        }

        ProfileResult profileResult = this.profileFuture.join();
        if (profileResult != null) {
            GameProfile profile = profileResult.profile();
            Set<ProfileActionType> actions = profileResult.actions();
            if (actions.contains(ProfileActionType.FORCED_NAME_CHANGE)) {
                screens.add(onClose -> BanNoticeScreens.createNameBan(profile.name(), onClose));
            }

            if (actions.contains(ProfileActionType.USING_BANNED_SKIN)) {
                screens.add(BanNoticeScreens::createSkinBan);
            }
        }

        return onboardingScreenAdded;
    }

    private static boolean countryEqualsISO3(Object iso3Locale) {
        try {
            return Locale.getDefault().getISO3Country().equals(iso3Locale);
        } catch (MissingResourceException var2) {
            return false;
        }
    }

    public void updateTitle() {
        this.window.setTitle(this.createTitle());
    }

    private String createTitle() {
        StringBuilder builder = new StringBuilder("Minecraft");
        if (checkModStatus().shouldReportAsModified()) {
            builder.append(' ').append(net.neoforged.neoforge.internal.BrandingControl.BRANDING_NAME).append('*');
        }

        builder.append(" ");
        builder.append(SharedConstants.getCurrentVersion().name());
        ClientPacketListener connection = this.getConnection();
        if (connection != null && connection.getConnection().isConnected()) {
            builder.append(" - ");
            ServerData server = this.getCurrentServer();
            if (this.singleplayerServer != null && !this.singleplayerServer.isPublished()) {
                builder.append(I18n.get("title.singleplayer"));
            } else if (server != null && server.isRealm()) {
                builder.append(I18n.get("title.multiplayer.realms"));
            } else if (this.singleplayerServer == null && (server == null || !server.isLan())) {
                builder.append(I18n.get("title.multiplayer.other"));
            } else {
                builder.append(I18n.get("title.multiplayer.lan"));
            }
        }

        return builder.toString();
    }

    private UserApiService createUserApiService(YggdrasilAuthenticationService authService, GameConfig config) {
        return config.game.offlineDeveloperMode ? UserApiService.OFFLINE : authService.createUserApiService(config.user.user.getAccessToken());
    }

    public boolean isOfflineDeveloperMode() {
        return this.offlineDeveloperMode;
    }

    public static ModCheck checkModStatus() {
        return ModCheck.identify("vanilla", ClientBrandRetriever::getClientModName, "Client", Minecraft.class);
    }

    private void rollbackResourcePacks(Throwable t, Minecraft.@Nullable GameLoadCookie loadCookie) {
        if (this.resourcePackRepository.getSelectedPacks().stream().anyMatch(e -> !e.isRequired())) { //Forge: This caused infinite loop if any resource packs are forced. Such as mod resources. So check if we can disable any.
            this.clearResourcePacksOnError(t, null, loadCookie);
        } else {
            Util.throwAsRuntime(t);
        }
    }

    public void clearResourcePacksOnError(Throwable t, @Nullable Component message, Minecraft.@Nullable GameLoadCookie loadCookie) {
        LOGGER.info("Caught error loading resourcepacks, removing all selected resourcepacks", t);
        this.reloadStateTracker.startRecovery(t);
        this.downloadedPackSource.onRecovery();
        this.resourcePackRepository.setSelected(Collections.emptyList());
        this.options.resourcePacks.clear();
        this.options.incompatibleResourcePacks.clear();
        this.options.save();
        this.reloadResourcePacks(true, loadCookie).thenRunAsync(() -> this.addResourcePackLoadFailToast(message), this);
    }

    private void abortResourcePackRecovery() {
        this.setOverlay(null);
        if (this.level != null) {
            this.level.disconnect(ClientLevel.DEFAULT_QUIT_MESSAGE);
            this.disconnectWithProgressScreen();
        }

        this.setScreen(new TitleScreen());
        this.addResourcePackLoadFailToast(null);
    }

    private void addResourcePackLoadFailToast(@Nullable Component message) {
        ToastManager toastManager = this.getToastManager();
        SystemToast.addOrUpdate(toastManager, SystemToast.SystemToastId.PACK_LOAD_FAILURE, Component.translatable("resourcePack.load_fail"), message);
    }

    public void triggerResourcePackRecovery(Exception exception) {
        if (!this.resourcePackRepository.isAbleToClearAnyPack()) {
            if (this.resourcePackRepository.getSelectedIds().size() <= 1) {
                LOGGER.error(LogUtils.FATAL_MARKER, exception.getMessage(), (Throwable)exception);
                this.emergencySaveAndCrash(new CrashReport(exception.getMessage(), exception));
            } else {
                this.schedule(this::abortResourcePackRecovery);
            }
        } else {
            this.clearResourcePacksOnError(exception, Component.translatable("resourcePack.runtime_failure"), null);
        }
    }

    public void run() {
        this.gameThread = Thread.currentThread();
        if (Runtime.getRuntime().availableProcessors() > 4) {
            this.gameThread.setPriority(10);
        }

        DiscontinuousFrame tickFrame = TracyClient.createDiscontinuousFrame("Client Tick");

        try {
            net.neoforged.neoforge.client.ClientLifecycleHooks.handleClientStarted(this);

            boolean oomRecovery = false;

            while (this.running) {
                try {
                    SingleTickProfiler tickProfiler = SingleTickProfiler.createTickProfiler("Renderer");
                    boolean shouldCollectFrameProfile = this.getDebugOverlay().showProfilerChart();

                    try (Profiler.Scope ignored = Profiler.use(this.constructProfiler(shouldCollectFrameProfile, tickProfiler))) {
                        this.metricsRecorder.startTick();
                        tickFrame.start();
                        this.window.resetIsResized();
                        RenderSystem.pollEvents();
                        this.runTick(!oomRecovery);
                        tickFrame.end();
                        this.metricsRecorder.endTick();
                    }

                    this.finishProfilers(shouldCollectFrameProfile, tickProfiler);
                } catch (OutOfMemoryError var10) {
                    if (oomRecovery) {
                        throw var10;
                    }

                    this.emergencySave();
                    this.setScreen(new OutOfMemoryScreen());
                    System.gc();
                    LOGGER.error(LogUtils.FATAL_MARKER, "Out of memory", (Throwable)var10);
                    oomRecovery = true;
                }
            }
        } catch (ReportedException var11) {
            LOGGER.error(LogUtils.FATAL_MARKER, "Reported exception thrown!", (Throwable)var11);
            this.emergencySaveAndCrash(var11.getReport());
        } catch (Throwable var12) {
            LOGGER.error(LogUtils.FATAL_MARKER, "Unreported exception thrown!", var12);
            this.emergencySaveAndCrash(new CrashReport("Unexpected error", var12));
        }
    }

    void updateFontOptions() {
        this.fontManager.updateOptions(this.options);
    }

    private void onFullscreenError(int error, long description) {
        this.options.enableVsync().set(false);
        this.options.save();
    }

    public RenderTarget getMainRenderTarget() {
        return this.mainRenderTarget;
    }

    public String getLaunchedVersion() {
        return this.launchedVersion;
    }

    public String getVersionType() {
        return this.versionType;
    }

    @Override
    public void delayCrash(CrashReport crash) {
        super.delayCrash(this.fillReport(crash));
    }

    public void emergencySaveAndCrash(CrashReport partialReport) {
        MemoryReserve.release();
        CrashReport finalReport = this.fillReport(partialReport);
        int exitCode = saveReportAndShutdownSoundManager(this, this.gameDirectory, finalReport);
        this.emergencySave();
        net.neoforged.neoforge.server.ServerLifecycleHooks.handleExit(exitCode);
    }

    public static int saveReport(File gameDirectory, CrashReport crash) {
        Path crashDir = gameDirectory.toPath().resolve("crash-reports");
        Path crashFile = crashDir.resolve("crash-" + Util.getFilenameFormattedDateTime() + "-client.txt");
        Bootstrap.realStdoutPrintln(crash.getFriendlyReport(ReportType.CRASH));
        LOGGER.debug("Disabling console - remaining logs will be available only in log file");

        byte var4;
        try {
            if (crash.getSaveFile() != null) {
                Bootstrap.realStdoutPrintln("#@!@# Game crashed! Crash report saved to: #@!@# " + crash.getSaveFile().toAbsolutePath());
                return -1;
            }

            if (!crash.saveToFile(crashFile, ReportType.CRASH)) {
                Bootstrap.realStdoutPrintln("#@?@# Game crashed! Crash report could not be saved. #@?@#");
                return -2;
            }

            Bootstrap.realStdoutPrintln("#@!@# Game crashed! Crash report saved to: #@!@# " + crashFile.toAbsolutePath());
            var4 = -1;
        } finally {
            Bootstrap.shutdownStdout();
        }

        return var4;
    }

    public static void crash(@Nullable Minecraft minecraft, File gameDirectory, CrashReport crash) {
        int exitCode = saveReportAndShutdownSoundManager(minecraft, gameDirectory, crash);
        net.neoforged.neoforge.server.ServerLifecycleHooks.handleExit(exitCode);
    }

    private static int saveReportAndShutdownSoundManager(@Nullable Minecraft minecraft, File gameDirectory, CrashReport crash) {
        int exitCode = saveReport(gameDirectory, crash);
        if (minecraft != null) {
            minecraft.soundManager.emergencyShutdown();
        }

        return exitCode;
    }

    public boolean isEnforceUnicode() {
        return this.options.forceUnicodeFont().get();
    }

    public CompletableFuture<Void> reloadResourcePacks() {
        return this.reloadResourcePacks(false, null);
    }

    private CompletableFuture<Void> reloadResourcePacks(boolean isRecovery, Minecraft.@Nullable GameLoadCookie loadCookie) {
        if (this.pendingReload != null) {
            return this.pendingReload;
        } else {
            CompletableFuture<Void> result = new CompletableFuture<>();
            if (!isRecovery && this.overlay instanceof LoadingOverlay) {
                this.pendingReload = result;
                return result;
            } else {
                this.resourcePackRepository.reload();
                List<PackResources> packs = this.resourcePackRepository.openAllSelected();
                if (!isRecovery) {
                    this.reloadStateTracker.startReload(ResourceLoadStateTracker.ReloadReason.MANUAL, packs);
                }

                this.setOverlay(
                    new LoadingOverlay(
                        this,
                        this.resourceManager.createReload(Util.backgroundExecutor().forName("resourceLoad"), this, RESOURCE_RELOAD_INITIAL_TASK, packs),
                        maybeT -> Util.ifElse(maybeT, t -> {
                            if (isRecovery) {
                                this.downloadedPackSource.onRecoveryFailure();
                                this.abortResourcePackRecovery();
                            } else {
                                this.rollbackResourcePacks(t, loadCookie);
                            }
                        }, () -> {
                            this.levelRenderer.allChanged();
                            this.reloadStateTracker.finishReload();
                            this.downloadedPackSource.onReloadSuccess();
                            result.complete(null);
                            this.onResourceLoadFinished(loadCookie);
                        }),
                        !isRecovery
                    )
                );
                return result;
            }
        }
    }

    private void selfTest() {
        boolean error = false;
        BlockStateModelSet blockModelSet = this.getModelManager().getBlockStateModelSet();
        BlockStateModel missingModel = blockModelSet.missingModel();

        for (Block block : BuiltInRegistries.BLOCK) {
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                if (state.getRenderShape() == RenderShape.MODEL) {
                    BlockStateModel model = blockModelSet.get(state);
                    if (model == missingModel) {
                        LOGGER.debug("Missing model for: {}", state);
                        error = true;
                    }
                }
            }
        }

        TextureAtlasSprite missingIcon = missingModel.particleMaterial().sprite();

        for (Block block : BuiltInRegistries.BLOCK) {
            for (BlockState statex : block.getStateDefinition().getPossibleStates()) {
                TextureAtlasSprite particleIcon = blockModelSet.getParticleMaterial(statex).sprite();
                if (!statex.isAir() && particleIcon == missingIcon) {
                    LOGGER.debug("Missing particle icon for: {}", statex);
                }
            }
        }

        BuiltInRegistries.ITEM.listElements().forEach(holder -> {
            Item item = holder.value();
            String descriptionId = item.getDescriptionId();
            String name = Component.translatable(descriptionId).getString();
            if (name.toLowerCase(Locale.ROOT).equals(item.getDescriptionId())) {
                LOGGER.debug("Missing translation for: {} {} {}", holder.key().identifier(), descriptionId, item);
            }
        });
        error |= MenuScreens.selfTest();
        error |= EntityRenderers.validateRegistrations();
        if (error) {
            throw new IllegalStateException("Your game data is foobar, fix the errors above!");
        }
    }

    public LevelStorageSource getLevelSource() {
        return this.levelSource;
    }

    public void openChatScreen(ChatComponent.ChatMethod chatMethod) {
        if (this.player != null) {
            this.gui.getChat().openScreen(chatMethod, ChatScreen::new);
        }
    }

    public void setScreen(@Nullable Screen screen) {
        if (SharedConstants.IS_RUNNING_IN_IDE && Thread.currentThread() != this.gameThread) {
            LOGGER.error("setScreen called from non-game thread");
        }

        if (this.screen == null) {
            this.setLastInputType(InputType.NONE);
        }

        if (screen == null) {
            if (this.clientLevelTeardownInProgress) {
                throw new IllegalStateException("Trying to return to in-game GUI during disconnection");
            }

            if (this.level == null) {
                screen = new TitleScreen();
            } else if (this.player.isDeadOrDying()) {
                if (this.player.shouldShowDeathScreen()) {
                    screen = new DeathScreen(null, this.level.getLevelData().isHardcore(), this.player);
                } else {
                    this.player.respawn();
                }
            } else {
                screen = this.gui.getChat().restoreChatScreen();
            }
        }

        net.neoforged.neoforge.client.ClientHooks.clearGuiLayers(this);
        Screen old = this.screen;
        if (screen != null) {
            var event = new net.neoforged.neoforge.client.event.ScreenEvent.Opening(old, screen);
            if (net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event).isCanceled()) return;
            screen = event.getNewScreen();
        }

        if (old != null && screen != old) {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.ScreenEvent.Closing(old));
            old.removed();
        }

        this.screen = screen;
        if (this.screen != null) {
            this.screen.added();
        }

        if (screen != null) {
            this.mouseHandler.releaseMouse();
            KeyMapping.releaseAll();
            screen.init(this.window.getGuiScaledWidth(), this.window.getGuiScaledHeight());
        } else {
            this.textInputManager.stopTextInput();
            if (this.level != null) {
                KeyMapping.restoreToggleStatesOnScreenClosed();
            }

            this.soundManager.resume();
            this.mouseHandler.grabMouse();
        }

        this.updateTitle();
    }

    public void setOverlay(@Nullable Overlay overlay) {
        this.overlay = overlay;
    }

    public void destroy() {
        try {
            net.neoforged.neoforge.client.ClientLifecycleHooks.handleClientStopping(this);
            LOGGER.info("Stopping!");

            try {
                this.narrator.destroy();
            } catch (Throwable var7) {
            }

            try {
                if (this.level != null) {
                    this.level.disconnect(ClientLevel.DEFAULT_QUIT_MESSAGE);
                }

                this.disconnectWithProgressScreen();
            } catch (Throwable var6) {
            }

            if (this.screen != null) {
                this.screen.removed();
            }

            this.close();
            net.neoforged.neoforge.client.ClientLifecycleHooks.handleClientStopped(this);
        } finally {
            Util.timeSource = System::nanoTime;
            if (!this.hasDelayedCrash()) {
                System.exit(0);
            }
        }
    }

    @Override
    public void close() {
        if (this.currentFrameProfile != null) {
            this.currentFrameProfile.cancel();
        }

        try {
            this.telemetryManager.close();
            this.regionalCompliancies.close();
            this.atlasManager.close();
            this.fontManager.close();
            this.gameRenderer.close();
            this.shaderManager.close();
            this.levelRenderer.close();
            this.soundManager.destroy();
            this.mapTextureManager.close();
            this.textureManager.close();
            this.resourceManager.close();
            if (this.tracyFrameCapture != null) {
                this.tracyFrameCapture.close();
            }

            FreeTypeUtil.destroy();
            Util.shutdownExecutors();
            RenderSystem.getSamplerCache().close();
            RenderSystem.getDevice().close();
        } catch (Throwable var5) {
            LOGGER.error("Shutdown failure!", var5);
            throw var5;
        } finally {
            this.window.close();
        }
    }

    private void runTick(boolean advanceGameTime) {
        this.window.setErrorSection("Pre render");
        if (this.window.shouldClose()) {
            this.stop();
        }

        if (this.pendingReload != null && !(this.overlay instanceof LoadingOverlay)) {
            CompletableFuture<Void> future = this.pendingReload;
            this.pendingReload = null;
            this.reloadResourcePacks().thenRun(() -> future.complete(null));
        }

        int ticksToDo = advanceGameTime ? this.deltaTracker.advanceGameTime(Util.getMillis()) : 0;
        ProfilerFiller profiler = Profiler.get();
        if (advanceGameTime) {
            try (Gizmos.TemporaryCollection ignored = this.collectPerTickGizmos()) {
                profiler.push("scheduledPacketProcessing");
                this.packetProcessor.processQueuedPackets();
                profiler.popPush("scheduledExecutables");
                this.runAllTasks();
                profiler.pop();
            }

            profiler.push("tick");
            if (ticksToDo > 0 && this.isLevelRunningNormally()) {
                profiler.push("textures");
                this.textureManager.tick();
                profiler.pop();
            }

            for (int i = 0; i < Math.min(10, ticksToDo); i++) {
                profiler.incrementCounter("clientTick");

                try (Gizmos.TemporaryCollection ignored = this.collectPerTickGizmos()) {
                    this.tick();
                }
            }

            if (ticksToDo > 0 && (this.level == null || this.level.tickRateManager().runsNormally())) {
                this.drainedLatestTickGizmos = this.perTickGizmos.drainGizmos();
            }

            profiler.pop();
        }

        this.window.setErrorSection("Render");

        try (Gizmos.TemporaryCollection ignored = this.levelRenderer.collectPerFrameGizmos()) {
            profiler.push("sound");
            this.soundManager.updateSource(this.gameRenderer.getMainCamera());
            profiler.popPush("toasts");
            this.toastManager.update();
            profiler.popPush("mouse");
            this.mouseHandler.handleAccumulatedMovement();
            profiler.popPush("frame");
            this.renderFrame(advanceGameTime);
            profiler.pop();
        }

        this.window.setErrorSection("Post render");
        boolean previouslyPaused = this.pause;
        boolean pause = this.hasSingleplayerServer()
            && (this.screen != null && this.screen.isPauseScreen() || this.overlay != null && this.overlay.isPauseScreen())
            && !this.singleplayerServer.isPublished();
        if (pause != this.pause && !net.neoforged.neoforge.client.ClientHooks.onClientPauseChangePre(pause)) {
            this.pause = pause;
            net.neoforged.neoforge.client.ClientHooks.onClientPauseChangePost(pause);
        }
        if (!previouslyPaused && this.pause) {
            this.soundManager.pauseAllExcept(SoundSource.MUSIC, SoundSource.UI);
        }

        this.deltaTracker.updatePauseState(this.pause);
        this.deltaTracker.updateFrozenState(!this.isLevelRunningNormally());
    }

    private void renderFrame(boolean advanceGameTime) {
        ProfilerFiller profiler = Profiler.get();
        profiler.push("update");
        this.deltaTracker.advanceRealTime(Util.getMillis());
        boolean recordGpuUtilization;
        if (!this.debugEntries.isCurrentlyEnabled(DebugScreenEntries.GPU_UTILIZATION) && !this.metricsRecorder.isRecording()) {
            recordGpuUtilization = false;
            this.gpuUtilization = 0.0;
        } else {
            recordGpuUtilization = (this.currentFrameProfile == null || this.currentFrameProfile.isDone()) && !TimerQuery.getInstance().isRecording();
            if (recordGpuUtilization) {
                TimerQuery.getInstance().beginProfile();
            }
        }

        long renderStartTimer = Util.getNanos();
        this.pauseIfInactive();
        this.window.updateFullscreenIfChanged();
        if (this.isGameLoadFinished() && advanceGameTime && this.level != null) {
            this.level.update();
        }

        this.gameRenderer.update(this.deltaTracker, advanceGameTime);
        float worldPartialTicks = this.deltaTracker.getGameTimeDeltaPartialTick(false);
        this.pick(worldPartialTicks);
        profiler.popPush("extract");
        this.gameRenderer.getGameRenderState().framerateLimit = this.framerateLimitTracker.getFramerateLimit();
        this.gameRenderer.extract(this.deltaTracker, advanceGameTime);
        profiler.popPush("gpuAsync");
        RenderSystem.executePendingTasks();
        profiler.pop();
        net.neoforged.neoforge.client.ClientHooks.fireRenderFramePre(this.deltaTracker);
        this.gameRenderer.render(this.deltaTracker, advanceGameTime);
        net.neoforged.neoforge.client.ClientHooks.fireRenderFramePost(this.deltaTracker);
        profiler.push("present");
        if (!this.gameRenderer.getGameRenderState().windowRenderState.isMinimized) {
            this.mainRenderTarget.blitToScreen();
        }

        this.frameTimeNs = Util.getNanos() - renderStartTimer;
        if (recordGpuUtilization) {
            this.currentFrameProfile = TimerQuery.getInstance().endProfile();
        }

        profiler.popPush("swapBuffers");
        if (this.tracyFrameCapture != null) {
            this.tracyFrameCapture.upload();
            this.tracyFrameCapture.capture(this.mainRenderTarget);
        }

        RenderSystem.flipFrame(this.tracyFrameCapture);
        profiler.popPush("frameLimiter");
        int framerateLimit = this.gameRenderer.getGameRenderState().framerateLimit;
        if (framerateLimit < 260) {
            FramerateLimiter.limitDisplayFPS(framerateLimit);
        }

        profiler.popPush("fpsUpdate");
        this.frames++;
        long currentTime = Util.getNanos();
        long frameDuration = currentTime - this.lastNanoTime;
        if (recordGpuUtilization) {
            this.savedCpuDuration = frameDuration;
        }

        this.getDebugOverlay().logFrameDuration(frameDuration);
        this.lastNanoTime = currentTime;
        if (this.currentFrameProfile != null && this.currentFrameProfile.isDone()) {
            this.gpuUtilization = this.currentFrameProfile.get() * 100.0 / this.savedCpuDuration;
        }

        while (Util.getMillis() >= this.lastTime + 1000L) {
            fps = this.frames;
            this.lastTime += 1000L;
            this.frames = 0;
        }

        profiler.pop();
    }

    private void pauseIfInactive() {
        if (!this.window.isFocused() && this.options.pauseOnLostFocus && (!this.options.touchscreen().get() || !this.mouseHandler.isRightPressed())) {
            if (Util.getMillis() - this.lastActiveTime > 500L) {
                this.pauseGame(false);
            }
        } else {
            this.lastActiveTime = Util.getMillis();
        }
    }

    private ProfilerFiller constructProfiler(boolean shouldCollectFrameProfile, @Nullable SingleTickProfiler tickProfiler) {
        if (!shouldCollectFrameProfile) {
            this.fpsPieProfiler.disable();
            if (!this.metricsRecorder.isRecording() && tickProfiler == null) {
                return InactiveProfiler.INSTANCE;
            }
        }

        ProfilerFiller result;
        if (shouldCollectFrameProfile) {
            if (!this.fpsPieProfiler.isEnabled()) {
                this.fpsPieRenderTicks = 0;
                this.fpsPieProfiler.enable();
            }

            this.fpsPieRenderTicks++;
            result = this.fpsPieProfiler.getFiller();
        } else {
            result = InactiveProfiler.INSTANCE;
        }

        if (this.metricsRecorder.isRecording()) {
            result = ProfilerFiller.combine(result, this.metricsRecorder.getProfiler());
        }

        return SingleTickProfiler.decorateFiller(result, tickProfiler);
    }

    private void finishProfilers(boolean shouldCollectFrameProfile, @Nullable SingleTickProfiler tickProfiler) {
        if (tickProfiler != null) {
            tickProfiler.endTick();
        }

        ProfilerPieChart profilerPieChart = this.getDebugOverlay().getProfilerPieChart();
        if (shouldCollectFrameProfile) {
            profilerPieChart.setPieChartResults(this.fpsPieProfiler.getResults());
        } else {
            profilerPieChart.setPieChartResults(null);
        }
    }

    @Override
    public void resizeGui() {
        int guiScale = this.window.calculateScale(this.options.guiScale().get(), this.isEnforceUnicode());
        this.window.setGuiScale(guiScale);
        net.neoforged.neoforge.client.ClientHooks.fireWindowResize(this.window);
        if (this.screen != null) {
            this.screen.resize(this.window.getGuiScaledWidth(), this.window.getGuiScaledHeight());
            net.neoforged.neoforge.client.ClientHooks.resizeGuiLayers(this.window.getGuiScaledWidth(), this.window.getGuiScaledHeight());
        }

        this.mouseHandler.setIgnoreFirstMove();
    }

    @Override
    public void cursorEntered() {
        this.mouseHandler.cursorEntered();
    }

    public int getFps() {
        return fps;
    }

    public long getFrameTimeNs() {
        return this.frameTimeNs;
    }

    public void sendLowDiskSpaceWarning() {
        this.execute(() -> SystemToast.onLowDiskSpace(this));
    }

    private void emergencySave() {
        MemoryReserve.release();

        try {
            if (this.isLocalServer && this.singleplayerServer != null) {
                this.singleplayerServer.halt(true);
            }

            this.disconnectWithSavingScreen();
        } catch (Throwable var2) {
        }

        System.gc();
    }

    public boolean debugClientMetricsStart(Consumer<Component> debugFeedback) {
        if (this.metricsRecorder.isRecording()) {
            this.debugClientMetricsStop();
            return false;
        } else {
            Consumer<ProfileResults> onStopped = results -> {
                if (results != EmptyProfileResults.EMPTY) {
                    int ticks = results.getTickDuration();
                    double durationInSeconds = (double)results.getNanoDuration() / TimeUtil.NANOSECONDS_PER_SECOND;
                    this.execute(
                        () -> debugFeedback.accept(
                            Component.translatable(
                                "commands.debug.stopped",
                                String.format(Locale.ROOT, "%.2f", durationInSeconds),
                                ticks,
                                String.format(Locale.ROOT, "%.2f", ticks / durationInSeconds)
                            )
                        )
                    );
                }
            };
            Consumer<Path> onFinished = profilePath -> {
                Component profilePathComponent = Component.literal(profilePath.toString())
                    .withStyle(ChatFormatting.UNDERLINE)
                    .withStyle(s -> s.withClickEvent(new ClickEvent.OpenFile(profilePath.getParent())));
                this.execute(() -> debugFeedback.accept(Component.translatable("debug.profiling.stop", profilePathComponent)));
            };
            SystemReport systemReport = fillSystemReport(new SystemReport(), this, this.languageManager, this.launchedVersion, this.options);
            Consumer<List<Path>> profileReports = logs -> {
                Path profilePath = this.archiveProfilingReport(systemReport, logs);
                onFinished.accept(profilePath);
            };
            Consumer<Path> whenClientMetricsRecordingFinished;
            if (this.singleplayerServer == null) {
                whenClientMetricsRecordingFinished = path -> profileReports.accept(ImmutableList.of(path));
            } else {
                this.singleplayerServer.fillSystemReport(systemReport);
                CompletableFuture<Path> clientMetricRecordingResult = new CompletableFuture<>();
                CompletableFuture<Path> serverMetricRecordingResult = new CompletableFuture<>();
                CompletableFuture.allOf(clientMetricRecordingResult, serverMetricRecordingResult)
                    .thenRunAsync(
                        () -> profileReports.accept(ImmutableList.of(clientMetricRecordingResult.join(), serverMetricRecordingResult.join())), Util.ioPool()
                    );
                this.singleplayerServer.startRecordingMetrics(ignored -> {}, serverMetricRecordingResult::complete);
                whenClientMetricsRecordingFinished = clientMetricRecordingResult::complete;
            }

            this.metricsRecorder = ActiveMetricsRecorder.createStarted(
                new ClientMetricsSamplersProvider(Util.timeSource, this.levelRenderer),
                Util.timeSource,
                Util.ioPool(),
                new MetricsPersister("client"),
                results -> {
                    this.metricsRecorder = InactiveMetricsRecorder.INSTANCE;
                    onStopped.accept(results);
                },
                whenClientMetricsRecordingFinished
            );
            return true;
        }
    }

    private void debugClientMetricsStop() {
        this.metricsRecorder.end();
        if (this.singleplayerServer != null) {
            this.singleplayerServer.finishRecordingMetrics();
        }
    }

    private void debugClientMetricsCancel() {
        this.metricsRecorder.cancel();
        if (this.singleplayerServer != null) {
            this.singleplayerServer.cancelRecordingMetrics();
        }
    }

    private Path archiveProfilingReport(SystemReport systemReport, List<Path> profilingResultPaths) {
        String levelName;
        if (this.isLocalServer()) {
            levelName = this.getSingleplayerServer().getWorldData().getLevelName();
        } else {
            ServerData server = this.getCurrentServer();
            levelName = server != null ? server.name : "unknown";
        }

        Path archivePath;
        try {
            String profilingName = String.format(
                Locale.ROOT, "%s-%s-%s", Util.getFilenameFormattedDateTime(), levelName, SharedConstants.getCurrentVersion().id()
            );
            String zipFileName = FileUtil.findAvailableName(MetricsPersister.PROFILING_RESULTS_DIR, profilingName, ".zip");
            archivePath = MetricsPersister.PROFILING_RESULTS_DIR.resolve(zipFileName);
        } catch (IOException var21) {
            throw new UncheckedIOException(var21);
        }

        try (FileZipper fileZipper = new FileZipper(archivePath)) {
            fileZipper.add(Paths.get("system.txt"), systemReport.toLineSeparatedString());
            fileZipper.add(Paths.get("client").resolve(this.options.getFile().getName()), this.options.dumpOptionsForReport());
            profilingResultPaths.forEach(fileZipper::add);
        } finally {
            for (Path path : profilingResultPaths) {
                try {
                    FileUtils.forceDelete(path.toFile());
                } catch (IOException var18) {
                    LOGGER.warn("Failed to delete temporary profiling result {}", path, var18);
                }
            }
        }

        return archivePath;
    }

    public void stop() {
        if (this.isRunning()) net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.GameShuttingDownEvent());
        this.running = false;
    }

    public boolean isRunning() {
        return this.running;
    }

    public void pauseGame(boolean suppressPauseMenuIfWeReallyArePausing) {
        if (this.screen == null) {
            boolean canGameReallyBePaused = this.hasSingleplayerServer() && !this.singleplayerServer.isPublished();
            if (canGameReallyBePaused) {
                this.setScreen(new PauseScreen(!suppressPauseMenuIfWeReallyArePausing));
            } else {
                this.setScreen(new PauseScreen(true));
            }
        }
    }

    private void continueAttack(boolean down) {
        if (!down) {
            this.missTime = 0;
        }

        if (this.missTime <= 0 && !this.player.isUsingItem()) {
            ItemStack heldItem = this.player.getItemInHand(InteractionHand.MAIN_HAND);
            if (!heldItem.has(DataComponents.PIERCING_WEAPON)) {
                if (down && this.hitResult != null && this.hitResult.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult blockHit = (BlockHitResult)this.hitResult;
                    BlockPos pos = blockHit.getBlockPos();
                    if (!this.level.getBlockState(pos).isAir()) {
                        Direction direction = blockHit.getDirection();
                        var inputEvent = net.neoforged.neoforge.client.ClientHooks.onClickInput(0, this.options.keyAttack, InteractionHand.MAIN_HAND);
                        if (inputEvent.isCanceled()) {
                            if (inputEvent.shouldSwingHand()) {
                                this.level.addBreakingBlockEffect(pos, direction, blockHit);
                                this.player.swing(InteractionHand.MAIN_HAND);
                            }
                            return;
                        }
                        if (this.gameMode.continueDestroyBlock(pos, direction) && inputEvent.shouldSwingHand()) {
                            this.level.addBreakingBlockEffect(pos, direction, blockHit);
                            this.player.swing(InteractionHand.MAIN_HAND);
                        }
                    }
                } else {
                    this.gameMode.stopDestroyBlock();
                }
            }
        }
    }

    private boolean startAttack() {
        if (this.missTime > 0) {
            return false;
        } else if (this.hitResult == null) {
            LOGGER.error("Null returned as 'hitResult', this shouldn't happen!");
            if (this.gameMode.hasMissTime()) {
                this.missTime = 10;
            }

            return false;
        } else if (this.player.isHandsBusy()) {
            return false;
        } else if (this.gameMode.isSpectator()) {
            if (this.hitResult instanceof EntityHitResult entityHitResult) {
                this.gameMode.spectate(entityHitResult.getEntity());
            }

            return true;
        } else {
            ItemStack heldItem = this.player.getItemInHand(InteractionHand.MAIN_HAND);
            if (!heldItem.isItemEnabled(this.level.enabledFeatures())) {
                return false;
            } else if (this.player.cannotAttackWithItem(heldItem, 0)) {
                return false;
            } else {
                boolean endAttack = false;
                var inputEvent = net.neoforged.neoforge.client.ClientHooks.onClickInput(0, this.options.keyAttack, InteractionHand.MAIN_HAND);
                if (inputEvent.isCanceled()) {
                    if (inputEvent.shouldSwingHand())
                        this.player.swing(InteractionHand.MAIN_HAND);
                    return endAttack;
                }
                PiercingWeapon piercingWeapon = heldItem.get(DataComponents.PIERCING_WEAPON);
                if (piercingWeapon != null) {
                    this.gameMode.piercingAttack(piercingWeapon);
                    if (inputEvent.shouldSwingHand())
                    this.player.swing(InteractionHand.MAIN_HAND);
                    return true;
                } else {
                    switch (this.hitResult.getType()) {
                        case ENTITY:
                            AttackRange customItemRange = heldItem.get(DataComponents.ATTACK_RANGE);
                            if (customItemRange == null || customItemRange.isInRange(this.player, this.hitResult.getLocation())) {
                                this.gameMode.attack(this.player, ((EntityHitResult)this.hitResult).getEntity());
                            }
                            break;
                        case BLOCK:
                            BlockHitResult blockHit = (BlockHitResult)this.hitResult;
                            BlockPos pos = blockHit.getBlockPos();
                            if (!this.level.getBlockState(pos).isAir()) {
                                this.gameMode.startDestroyBlock(pos, blockHit.getDirection());
                                if (this.level.getBlockState(pos).isAir()) {
                                    endAttack = true;
                                }
                                break;
                            }
                        case MISS:
                            if (this.gameMode.hasMissTime()) {
                                this.missTime = 10;
                            }

                            this.player.resetAttackStrengthTicker();
                            net.neoforged.neoforge.common.CommonHooks.onEmptyLeftClick(this.player);
                    }

                    if (inputEvent.shouldSwingHand())
                    this.player.swing(InteractionHand.MAIN_HAND);
                    return endAttack;
                }
            }
        }
    }

    private void startUseItem() {
        if (!this.gameMode.isDestroying()) {
            this.rightClickDelay = 4;
            if (!this.player.isHandsBusy()) {
                if (this.hitResult == null) {
                    LOGGER.warn("Null returned as 'hitResult', this shouldn't happen!");
                }

                for (InteractionHand hand : InteractionHand.values()) {
                    var inputEvent = net.neoforged.neoforge.client.ClientHooks.onClickInput(1, this.options.keyUse, hand);
                    if (inputEvent.isCanceled()) {
                        if (inputEvent.shouldSwingHand()) this.player.swing(hand);
                        return;
                    }
                    ItemStack heldItem = this.player.getItemInHand(hand);
                    if (!heldItem.isItemEnabled(this.level.enabledFeatures())) {
                        return;
                    }

                    if (this.hitResult != null) {
                        switch (this.hitResult.getType()) {
                            case ENTITY:
                                EntityHitResult entityHit = (EntityHitResult)this.hitResult;
                                Entity entity = entityHit.getEntity();
                                if (!this.level.getWorldBorder().isWithinBounds(entity.blockPosition())) {
                                    return;
                                }

                                if (this.player.isWithinEntityInteractionRange(entity, 0.0)
                                    && this.gameMode.interact(this.player, entity, entityHit, hand) instanceof InteractionResult.Success success) {
                                    if (success.swingSource() == InteractionResult.SwingSource.CLIENT && inputEvent.shouldSwingHand()) {
                                        this.player.swing(hand);
                                    }

                                    return;
                                }
                                break;
                            case BLOCK:
                                BlockHitResult blockHit = (BlockHitResult)this.hitResult;
                                int oldCount = heldItem.getCount();
                                InteractionResult useResult = this.gameMode.useItemOn(this.player, hand, blockHit);
                                if (useResult instanceof InteractionResult.Success success) {
                                    if (success.swingSource() == InteractionResult.SwingSource.CLIENT && inputEvent.shouldSwingHand()) {
                                        this.player.swing(hand);
                                        if (!heldItem.isEmpty() && (heldItem.getCount() != oldCount || this.player.hasInfiniteMaterials())) {
                                            this.gameRenderer.itemInHandRenderer.itemUsed(hand);
                                        }
                                    }

                                    return;
                                }

                                if (useResult instanceof InteractionResult.Fail) {
                                    return;
                                }
                        }
                    }

                    if (heldItem.isEmpty() && (this.hitResult == null || this.hitResult.getType() == HitResult.Type.MISS))
                        net.neoforged.neoforge.common.CommonHooks.onEmptyClick(this.player, hand);

                    if (!heldItem.isEmpty() && this.gameMode.useItem(this.player, hand) instanceof InteractionResult.Success success) {
                        if (success.swingSource() == InteractionResult.SwingSource.CLIENT && inputEvent.shouldSwingHand()) {
                            this.player.swing(hand);
                        }

                        this.gameRenderer.itemInHandRenderer.itemUsed(hand);
                        return;
                    }
                }
            }
        }
    }

    public MusicManager getMusicManager() {
        return this.musicManager;
    }

    public void tick() {
        this.clientTickCount++;
        if (this.gameLoadFinished) {
            net.neoforged.neoforge.client.ClientHooks.fireClientTickPre();
        }

        if (this.level != null && !this.pause) {
            this.level.tickRateManager().tick();
        }

        if (this.rightClickDelay > 0) {
            this.rightClickDelay--;
        }

        ProfilerFiller profiler = Profiler.get();
        profiler.push("gui");
        this.textInputManager.tick();
        this.chatListener.tick();
        this.gui.tick(this.pause);
        profiler.pop();
        this.pick(1.0F);
        this.tutorial.onLookAt(this.level, this.hitResult);
        profiler.push("gameMode");
        if (!this.pause && this.level != null) {
            this.gameMode.tick();
        }

        profiler.popPush("screen");
        if (this.screen != null || this.player == null) {
            if (this.screen instanceof InBedChatScreen inBedScreen && !this.player.isSleeping()) {
                inBedScreen.onPlayerWokeUp();
            }
        } else if (this.player.isDeadOrDying() && !(this.screen instanceof DeathScreen)) {
            this.setScreen(null);
        } else if (this.player.isSleeping() && this.level != null) {
            this.gui.getChat().openScreen(ChatComponent.ChatMethod.MESSAGE, InBedChatScreen::new);
        }

        if (this.screen != null) {
            this.missTime = 10000;
        }

        if (this.screen != null) {
            try {
                this.screen.tick();
            } catch (Throwable var5) {
                CrashReport report = CrashReport.forThrowable(var5, "Ticking screen");
                this.screen.fillCrashDetails(report);
                throw new ReportedException(report);
            }
        }

        if (this.overlay != null) {
            this.overlay.tick();
        }

        if (!this.getDebugOverlay().showDebugScreen()) {
            this.gui.clearCache();
        }

        if (this.overlay == null && this.screen == null) {
            profiler.popPush("Keybindings");
            this.handleKeybinds();
            if (this.missTime > 0) {
                this.missTime--;
            }
        }

        if (this.level != null) {
            if (!this.pause) {
                profiler.popPush("gameRenderer");
                this.gameRenderer.tick();
                profiler.popPush("entities");
                this.level.tickEntities();
                profiler.popPush("blockEntities");
                this.level.tickBlockEntities();
            }
        } else if (this.gameRenderer.currentPostEffect() != null) {
            this.gameRenderer.clearPostEffect();
        }

        this.musicManager.tick();
        this.soundManager.tick(this.pause);
        if (this.level != null) {
            if (!this.pause) {
                profiler.popPush("level");
                if (!this.options.joinedFirstServer && this.isMultiplayerServer()) {
                    Component title = Component.translatable("tutorial.socialInteractions.title");
                    Component message = Component.translatable("tutorial.socialInteractions.description", Tutorial.key("socialInteractions"));
                    this.socialInteractionsToast = new TutorialToast(this.font, TutorialToast.Icons.SOCIAL_INTERACTIONS, title, message, true, 8000);
                    this.toastManager.addToast(this.socialInteractionsToast);
                    this.options.joinedFirstServer = true;
                    this.options.save();
                }

                this.tutorial.tick();

                net.neoforged.neoforge.event.EventHooks.fireLevelTickPre(this.level, () -> true);
                try {
                    this.level.tick(() -> true);
                } catch (Throwable var6) {
                    CrashReport report = CrashReport.forThrowable(var6, "Exception in world tick");
                    if (this.level == null) {
                        CrashReportCategory levelCategory = report.addCategory("Affected level");
                        levelCategory.setDetail("Problem", "Level is null!");
                    } else {
                        this.level.fillReportDetails(report);
                    }

                    throw new ReportedException(report);
                }
                net.neoforged.neoforge.event.EventHooks.fireLevelTickPost(this.level, () -> true);
            }

            profiler.popPush("animateTick");
            if (!this.pause && this.isLevelRunningNormally()) {
                this.level.animateTick(this.player.getBlockX(), this.player.getBlockY(), this.player.getBlockZ());
            }

            profiler.popPush("particles");
            if (!this.pause && this.isLevelRunningNormally()) {
                this.particleEngine.tick();
            }

            ClientPacketListener connection = this.getConnection();
            if (connection != null && !this.pause) {
                connection.send(ServerboundClientTickEndPacket.INSTANCE);
            }
        } else if (this.pendingConnection != null) {
            profiler.popPush("pendingConnection");
            this.pendingConnection.tick();
        }

        profiler.popPush("keyboard");
        this.keyboardHandler.tick();
        profiler.pop();

        if (this.gameLoadFinished) {
            net.neoforged.neoforge.client.ClientHooks.fireClientTickPost();
        }
    }

    private boolean isLevelRunningNormally() {
        return this.level == null || this.level.tickRateManager().runsNormally();
    }

    private boolean isMultiplayerServer() {
        return !this.isLocalServer || this.singleplayerServer != null && this.singleplayerServer.isPublished();
    }

    public void handleKeybinds() {
        while (this.options.keyTogglePerspective.consumeClick()) {
            CameraType previous = this.options.getCameraType();
            this.options.setCameraType(this.options.getCameraType().cycle());
            if (previous.isFirstPerson() != this.options.getCameraType().isFirstPerson()) {
                this.gameRenderer.checkEntityPostEffect(this.options.getCameraType().isFirstPerson() ? this.getCameraEntity() : null);
            }

            this.levelRenderer.needsUpdate();
        }

        while (this.options.keySmoothCamera.consumeClick()) {
            this.options.smoothCamera = !this.options.smoothCamera;
        }

        while (this.options.keyToggleGui.consumeClick()) {
            this.options.hideGui = !this.options.hideGui;
        }

        while (this.options.keyToggleSpectatorShaderEffects.consumeClick()) {
            this.gameRenderer.togglePostEffect();
        }

        for (int i = 0; i < 9; i++) {
            boolean savePressed = this.options.keySaveHotbarActivator.isDown();
            boolean loadPressed = this.options.keyLoadHotbarActivator.isDown();
            if (this.options.keyHotbarSlots[i].consumeClick()) {
                if (this.player.isSpectator()) {
                    this.gui.getSpectatorGui().onHotbarSelected(i);
                } else if (!this.player.hasInfiniteMaterials() || this.screen != null || !loadPressed && !savePressed) {
                    this.player.getInventory().setSelectedSlot(i);
                } else {
                    CreativeModeInventoryScreen.handleHotbarLoadOrSave(this, i, loadPressed, savePressed);
                }
            }
        }

        while (this.options.keySocialInteractions.consumeClick()) {
            if (!this.isMultiplayerServer() && !SharedConstants.DEBUG_SOCIAL_INTERACTIONS) {
                this.chatListener.handleOverlay(SOCIAL_INTERACTIONS_NOT_AVAILABLE);
                this.narrator.saySystemNow(SOCIAL_INTERACTIONS_NOT_AVAILABLE);
            } else {
                if (this.socialInteractionsToast != null) {
                    this.socialInteractionsToast.hide();
                    this.socialInteractionsToast = null;
                }

                this.setScreen(new SocialInteractionsScreen());
            }
        }

        while (this.options.keyInventory.consumeClick()) {
            if (this.gameMode.isServerControlledInventory()) {
                this.player.sendOpenInventory();
            } else {
                this.tutorial.onOpenInventory();
                this.setScreen(new InventoryScreen(this.player));
            }
        }

        while (this.options.keyAdvancements.consumeClick()) {
            this.setScreen(new AdvancementsScreen(this.player.connection.getAdvancements()));
        }

        while (this.options.keyQuickActions.consumeClick()) {
            this.getQuickActionsDialog().ifPresent(dialog -> this.player.connection.showDialog((Holder<Dialog>)dialog, this.screen));
        }

        while (this.options.keySwapOffhand.consumeClick()) {
            if (!this.player.isSpectator()) {
                this.getConnection()
                    .send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
            }
        }

        while (this.options.keyDrop.consumeClick()) {
            if (!this.player.isSpectator() && this.player.drop(this.hasControlDown())) {
                this.player.swing(InteractionHand.MAIN_HAND);
            }
        }

        while (this.options.keyChat.consumeClick()) {
            this.openChatScreen(ChatComponent.ChatMethod.MESSAGE);
        }

        if (this.screen == null && this.overlay == null && this.options.keyCommand.consumeClick()) {
            this.openChatScreen(ChatComponent.ChatMethod.COMMAND);
        }

        boolean instantAttack = false;
        if (this.player.isUsingItem()) {
            if (!this.options.keyUse.isDown()) {
                this.gameMode.releaseUsingItem(this.player);
            }

            while (this.options.keyAttack.consumeClick()) {
            }

            while (this.options.keyUse.consumeClick()) {
            }

            while (this.options.keyPickItem.consumeClick()) {
            }
        } else {
            while (this.options.keyAttack.consumeClick()) {
                instantAttack |= this.startAttack();
            }

            while (this.options.keyUse.consumeClick()) {
                this.startUseItem();
            }

            while (this.options.keyPickItem.consumeClick()) {
                this.pickBlockOrEntity();
            }

            if (this.player.isSpectator()) {
                while (this.options.keySpectatorHotbar.consumeClick()) {
                    this.gui.getSpectatorGui().onHotbarActionKeyPressed();
                }
            }
        }

        if (this.options.keyUse.isDown() && this.rightClickDelay == 0 && !this.player.isUsingItem()) {
            this.startUseItem();
        }

        this.continueAttack(this.screen == null && !instantAttack && this.options.keyAttack.isDown() && this.mouseHandler.isMouseGrabbed());
    }

    private Optional<Holder<Dialog>> getQuickActionsDialog() {
        Registry<Dialog> dialogRegistry = this.player.connection.registryAccess().lookupOrThrow(Registries.DIALOG);
        return dialogRegistry.get(DialogTags.QUICK_ACTIONS).flatMap(quickActions -> {
            if (quickActions.size() == 0) {
                return Optional.empty();
            } else {
                return quickActions.size() == 1 ? Optional.of(quickActions.get(0)) : dialogRegistry.get(Dialogs.QUICK_ACTIONS);
            }
        });
    }

    public ClientTelemetryManager getTelemetryManager() {
        return this.telemetryManager;
    }

    public double getGpuUtilization() {
        return this.gpuUtilization;
    }

    public ProfileKeyPairManager getProfileKeyPairManager() {
        return this.profileKeyPairManager;
    }

    public WorldOpenFlows createWorldOpenFlows() {
        return new WorldOpenFlows(this, this.levelSource);
    }

    public void doWorldLoad(
        LevelStorageSource.LevelStorageAccess levelSourceAccess,
        PackRepository packRepository,
        WorldStem worldStem,
        Optional<GameRules> gameRules,
        boolean newWorld
    ) {
        this.disconnectWithProgressScreen();
        Instant worldLoadStart = Instant.now();
        LevelLoadTracker loadTracker = new LevelLoadTracker(newWorld ? 500L : 0L);
        LevelLoadingScreen screen = new LevelLoadingScreen(loadTracker, LevelLoadingScreen.Reason.OTHER);
        this.setScreen(screen);
        int chunkStatusViewRadius = Math.max(5, 3) + ChunkLevel.RADIUS_AROUND_FULL_CHUNK + 1;

        try {
            levelSourceAccess.saveDataTag(worldStem.worldDataAndGenSettings().data());
            LevelLoadListener loadListener = LevelLoadListener.compose(loadTracker, LoggingLevelLoadListener.forSingleplayer());
            this.singleplayerServer = MinecraftServer.spin(
                thread -> new IntegratedServer(thread, this, levelSourceAccess, packRepository, worldStem, gameRules, this.services, loadListener)
            );
            loadTracker.setServerChunkStatusView(this.singleplayerServer.createChunkLoadStatusView(chunkStatusViewRadius));
            this.isLocalServer = true;
            this.updateReportEnvironment(ReportEnvironment.local());
            this.quickPlayLog
                .setWorldData(QuickPlayLog.Type.SINGLEPLAYER, levelSourceAccess.getLevelId(), worldStem.worldDataAndGenSettings().data().getLevelName());
        } catch (Throwable var16) {
            CrashReport report = CrashReport.forThrowable(var16, "Starting integrated server");
            CrashReportCategory category = report.addCategory("Starting integrated server");
            category.setDetail("Level ID", levelSourceAccess.getLevelId());
            category.setDetail("Level Name", () -> worldStem.worldDataAndGenSettings().data().getLevelName());
            throw new ReportedException(report);
        }

        ProfilerFiller profiler = Profiler.get();
        profiler.push("waitForServer");
        long tickLengthNs = TimeUnit.SECONDS.toNanos(1L) / 60L;

        while (!this.singleplayerServer.isReady() || this.overlay != null) {
            long finishTime = Util.getNanos() + tickLengthNs;
            screen.tick();
            if (this.overlay != null) {
                this.overlay.tick();
            }

            this.renderFrame(false);
            this.runAllTasks();
            this.managedBlock(() -> Util.getNanos() > finishTime);
        }

        profiler.pop();
        Duration worldLoadDuration = Duration.between(worldLoadStart, Instant.now());
        SocketAddress socketAddress = this.singleplayerServer.getConnection().startMemoryChannel();
        Connection connection = Connection.connectToLocalServer(socketAddress);
        connection.initiateServerboundPlayConnection(
            socketAddress.toString(),
            0,
            new ClientHandshakePacketListenerImpl(connection, this, null, null, newWorld, worldLoadDuration, status -> {}, loadTracker, null)
        );
        connection.send(new ServerboundHelloPacket(this.getUser().getName(), this.getUser().getProfileId()));
        this.pendingConnection = connection;
    }

    public void setLevel(ClientLevel level) {
        if (this.level != null) net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.level.LevelEvent.Unload(this.level));
        this.level = level;
        this.updateLevelInEngines(level);
    }

    public void disconnectFromWorld(Component message) {
        boolean localServer = this.isLocalServer();
        ServerData currentServer = this.getCurrentServer();
        if (this.level != null) {
            this.level.disconnect(message);
        }

        if (localServer) {
            this.disconnectWithSavingScreen();
        } else {
            this.disconnectWithProgressScreen();
        }

        TitleScreen titleScreen = new TitleScreen();
        if (localServer) {
            this.setScreen(titleScreen);
        } else if (currentServer != null && currentServer.isRealm()) {
            this.setScreen(new RealmsMainScreen(titleScreen));
        } else {
            this.setScreen(new JoinMultiplayerScreen(titleScreen));
        }
    }

    public void disconnectWithSavingScreen() {
        this.disconnect(new GenericMessageScreen(SAVING_LEVEL), false);
    }

    public void disconnectWithProgressScreen() {
        this.disconnectWithProgressScreen(true);
    }

    public void disconnectWithProgressScreen(boolean stopSound) {
        this.disconnect(new ProgressScreen(true), false, stopSound);
    }

    public void disconnect(Screen screen, boolean keepResourcePacks) {
        this.disconnect(screen, keepResourcePacks, true);
    }

    public void disconnect(Screen screen, boolean keepResourcePacks, boolean stopSound) {
        ClientPacketListener connection = this.getConnection();
        if (connection != null) {
            this.dropAllTasks();
            connection.close();
            if (!keepResourcePacks) {
                this.clearDownloadedResourcePacks();
            }
        }

        this.playerSocialManager.stopOnlineMode();
        if (this.metricsRecorder.isRecording()) {
            this.debugClientMetricsCancel();
        }

        IntegratedServer server = this.singleplayerServer;
        this.singleplayerServer = null;
        this.gameRenderer.resetData();
        net.neoforged.neoforge.client.ClientHooks.firePlayerLogout(this.gameMode, this.player);
        this.gameMode = null;
        this.narrator.clear();
        this.clientLevelTeardownInProgress = true;

        var shouldRevertRegistriesToFrozen = this.getConnection() != null && this.getConnection().getConnection() != null; // Neo: Track whether to revert registries after disconnect
        try {
            if (this.level != null) {
                this.gui.onDisconnected();
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.level.LevelEvent.Unload(this.level));
            }

            this.level = null;
            if (server != null) {
                server.halt(false);
                this.setScreen(new GenericMessageScreen(SAVING_LEVEL));
                ProfilerFiller profiler = Profiler.get();
                profiler.push("waitForServer");

                while (!server.isShutdown()) {
                    this.renderFrame(false);
                }

                profiler.pop();
            }

            this.setScreenAndShow(screen);
            this.isLocalServer = false;
            this.updateLevelInEngines(null, stopSound);
            this.player = null;
        } finally {
            this.clientLevelTeardownInProgress = false;
        }
        if(shouldRevertRegistriesToFrozen) net.neoforged.neoforge.registries.RegistryManager.revertToFrozen(); // Neo: Revert registries to frozen on disconnect
    }

    public void clearDownloadedResourcePacks() {
        this.downloadedPackSource.cleanupAfterDisconnect();
        this.runAllTasks();
    }

    public void clearClientLevel(Screen screen) {
        ClientPacketListener connection = this.getConnection();
        if (connection != null) {
            connection.clearLevel();
        }

        if (this.metricsRecorder.isRecording()) {
            this.debugClientMetricsCancel();
        }

        this.gameRenderer.resetData();
        this.gameMode = null;
        this.narrator.clear();
        this.clientLevelTeardownInProgress = true;

        try {
            this.setScreenAndShow(screen);
            this.gui.onDisconnected();
            if (this.level != null) net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.level.LevelEvent.Unload(this.level));
            this.level = null;
            this.updateLevelInEngines(null);
            this.player = null;
        } finally {
            this.clientLevelTeardownInProgress = false;
        }
    }

    public void setScreenAndShow(Screen screen) {
        try (Zone ignored = Profiler.get().zone("forcedTick")) {
            this.setScreen(screen);
            this.renderFrame(false);
        }
    }

    private void updateLevelInEngines(@Nullable ClientLevel level) {
        this.updateLevelInEngines(level, true);
    }

    private void updateLevelInEngines(@Nullable ClientLevel level, boolean stopSound) {
        if (stopSound) {
            this.soundManager.stop();
        }

        this.setCameraEntity(null);
        this.pendingConnection = null;
        this.levelRenderer.setLevel(level);
        this.particleEngine.setLevel(level);
        this.gameRenderer.setLevel(level);
        this.updateTitle();
    }

    private UserProperties userProperties() {
        return this.userPropertiesFuture.join();
    }

    public boolean telemetryOptInExtra() {
        return this.extraTelemetryAvailable() && this.options.telemetryOptInExtra().get();
    }

    public boolean extraTelemetryAvailable() {
        return this.allowsTelemetry() && this.userProperties().flag(UserFlag.OPTIONAL_TELEMETRY_AVAILABLE);
    }

    public boolean allowsTelemetry() {
        return SharedConstants.IS_RUNNING_IN_IDE && !SharedConstants.DEBUG_FORCE_TELEMETRY ? false : this.userProperties().flag(UserFlag.TELEMETRY_ENABLED);
    }

    public boolean allowsMultiplayer() {
        return this.allowsMultiplayer && this.userProperties().flag(UserFlag.SERVERS_ALLOWED) && this.multiplayerBan() == null && !this.isNameBanned();
    }

    public boolean allowsRealms() {
        return this.userProperties().flag(UserFlag.REALMS_ALLOWED) && this.multiplayerBan() == null;
    }

    public @Nullable BanDetails multiplayerBan() {
        return this.userProperties().bannedScopes().get("MULTIPLAYER");
    }

    public boolean isNameBanned() {
        ProfileResult result = this.profileFuture.getNow(null);
        return result != null && result.actions().contains(ProfileActionType.FORCED_NAME_CHANGE);
    }

    public boolean isBlocked(UUID uuid) {
        return !this.isLocalOrUnknownPlayer(uuid) && this.playerSocialManager.shouldHideMessageFrom(uuid);
    }

    private boolean isLocalOrUnknownPlayer(UUID uuid) {
        return uuid.equals(Util.NIL_UUID) ? true : this.player != null && uuid.equals(this.player.getUUID());
    }

    public ChatAbilities computeChatAbilities() {
        ChatAbilities.Builder builder = new ChatAbilities.Builder();
        ChatVisiblity visiblityOption = this.options.chatVisibility().get();
        if (visiblityOption == ChatVisiblity.HIDDEN) {
            builder.addRestriction(ChatRestriction.CHAT_AND_COMMANDS_DISABLED_BY_OPTIONS);
        } else if (visiblityOption == ChatVisiblity.SYSTEM) {
            builder.addRestriction(ChatRestriction.CHAT_DISABLED_BY_OPTIONS);
        }

        if (this.isMultiplayerServer()) {
            if (!this.allowsChat) {
                builder.addRestriction(ChatRestriction.DISABLED_BY_LAUNCHER);
            }

            if (SharedConstants.DEBUG_CHAT_DISABLED || !this.userProperties().flag(UserFlag.CHAT_ALLOWED)) {
                builder.addRestriction(ChatRestriction.DISABLED_BY_PROFILE);
            }
        }

        return builder.build();
    }

    public final boolean isDemo() {
        return this.demo;
    }

    public final boolean canSwitchGameMode() {
        return this.player != null && this.gameMode != null;
    }

    public @Nullable ClientPacketListener getConnection() {
        return this.player == null ? null : this.player.connection;
    }

    public static boolean renderNames() {
        return !instance.options.hideGui;
    }

    public static boolean useShaderTransparency() {
        GameRenderState gameRenderState = instance.gameRenderer.getGameRenderState();
        return !gameRenderState.levelRenderState.cameraRenderState.isPanoramicMode && gameRenderState.optionsRenderState.improvedTransparency;
    }

    private void pickBlockOrEntity() {
        if (this.hitResult != null && this.hitResult.getType() != HitResult.Type.MISS) {
            if (net.neoforged.neoforge.client.ClientHooks.onClickInput(2, this.options.keyPickItem, InteractionHand.MAIN_HAND).isCanceled()) return;
            boolean includeData = this.hasControlDown();
            switch (this.hitResult) {
                case BlockHitResult blockHitResult:
                    this.gameMode.handlePickItemFromBlock(blockHitResult.getBlockPos(), includeData);
                    break;
                case EntityHitResult entityHitResult:
                    this.gameMode.handlePickItemFromEntity(entityHitResult.getEntity(), includeData);
                    break;
                default:
            }
        }
    }

    public CrashReport fillReport(CrashReport report) {
        SystemReport systemReport = report.getSystemReport();

        try {
            fillSystemReport(systemReport, this, this.languageManager, this.launchedVersion, this.options);
            this.fillUptime(report.addCategory("Uptime"));
            if (this.level != null) {
                this.level.fillReportDetails(report);
            }

            if (this.singleplayerServer != null) {
                this.singleplayerServer.fillSystemReport(systemReport);
            }

            this.reloadStateTracker.fillCrashReport(report);
        } catch (Throwable var4) {
            LOGGER.error("Failed to collect details", var4);
        }

        return report;
    }

    public static void fillReport(
        @Nullable Minecraft minecraft, @Nullable LanguageManager languageManager, String launchedVersion, @Nullable Options options, CrashReport report
    ) {
        SystemReport system = report.getSystemReport();
        fillSystemReport(system, minecraft, languageManager, launchedVersion, options);
    }

    private static String formatSeconds(double timeInSeconds) {
        return String.format(Locale.ROOT, "%.3fs", timeInSeconds);
    }

    private void fillUptime(CrashReportCategory category) {
        category.setDetail("JVM uptime", () -> formatSeconds(ManagementFactory.getRuntimeMXBean().getUptime() / 1000.0));
        category.setDetail("Wall uptime", () -> formatSeconds((System.currentTimeMillis() - this.clientStartTimeMs) / 1000.0));
        category.setDetail("High-res time", () -> formatSeconds(Util.getMillis() / 1000.0));
        category.setDetail("Client ticks", () -> String.format(Locale.ROOT, "%d ticks / %.3fs", this.clientTickCount, this.clientTickCount / 20.0));
    }

    private static SystemReport fillSystemReport(
        SystemReport systemReport, @Nullable Minecraft minecraft, @Nullable LanguageManager languageManager, String launchedVersion, @Nullable Options options
    ) {
        systemReport.setDetail("Launched Version", () -> launchedVersion);
        String launcherBrand = getLauncherBrand();
        if (launcherBrand != null) {
            systemReport.setDetail("Launcher name", launcherBrand);
        }

        systemReport.setDetail("Backend library", RenderSystem::getBackendDescription);
        systemReport.setDetail("Backend API", RenderSystem::getApiDescription);
        systemReport.setDetail("Window size", () -> minecraft != null ? minecraft.window.getWidth() + "x" + minecraft.window.getHeight() : "<not initialized>");
        systemReport.setDetail("GFLW Platform", Window::getPlatform);
        systemReport.setDetail("Render Extensions", () -> {
            GpuDevice device = RenderSystem.tryGetDevice();
            if (device == null) {
                return "<no renderer available>";
            } else {
                return String.join(", ", device.getEnabledExtensions());
            }
        });
        systemReport.setDetail("GL debug messages", () -> {
            GpuDevice device = RenderSystem.tryGetDevice();
            if (device == null) {
                return "<no renderer available>";
            } else {
                return device.isDebuggingEnabled() ? String.join("\n", device.getLastDebugMessages()) : "<debugging unavailable>";
            }
        });
        systemReport.setDetail("Is Modded", () -> checkModStatus().fullDescription());
        systemReport.setDetail("Universe", () -> minecraft != null ? Long.toHexString(minecraft.canary) : "404");
        systemReport.setDetail("Type", "Client");
        if (options != null) {
            if (minecraft != null) {
                String gpuWarnings = minecraft.getGpuWarnlistManager().getAllWarnings();
                if (gpuWarnings != null) {
                    systemReport.setDetail("GPU Warnings", gpuWarnings);
                }
            }

            systemReport.setDetail("Transparency", options.improvedTransparency().get() ? "shader" : "regular");
            systemReport.setDetail("Render Distance", options.getEffectiveRenderDistance() + "/" + options.renderDistance().get() + " chunks");
        }

        if (minecraft != null) {
            systemReport.setDetail("Resource Packs", () -> PackRepository.displayPackList(minecraft.getResourcePackRepository().getSelectedPacks()));
            systemReport.setDetail("Sound Cache", () -> {
                SoundBufferLibrary.DebugOutput.Counter counter = new SoundBufferLibrary.DebugOutput.Counter();
                minecraft.getSoundManager().getSoundCacheDebugStats(counter);
                return String.format(Locale.ROOT, "%d bytes in %d buffers", counter.totalSize(), counter.totalCount());
            });
        }

        if (languageManager != null) {
            systemReport.setDetail("Current Language", () -> languageManager.getSelected());
        }

        systemReport.setDetail("Locale", String.valueOf(Locale.getDefault()));
        systemReport.setDetail("System encoding", () -> System.getProperty("sun.jnu.encoding", "<not set>"));
        systemReport.setDetail("File encoding", () -> System.getProperty("file.encoding", "<not set>"));
        systemReport.setDetail("CPU", GLX::_getCpuInfo);
        return systemReport;
    }

    public static Minecraft getInstance() {
        return instance;
    }

    public CompletableFuture<Void> delayTextureReload() {
        return this.<CompletableFuture<Void>>submit((java.util.function.Supplier<CompletableFuture<Void>>)this::reloadResourcePacks).thenCompose(result -> (CompletionStage<Void>)result);
    }

    public void updateReportEnvironment(ReportEnvironment environment) {
        if (!this.reportingContext.matches(environment)) {
            this.reportingContext = ReportingContext.create(environment, this.userApiService);
        }
    }

    public @Nullable ServerData getCurrentServer() {
        return Optionull.map(this.getConnection(), ClientPacketListener::getServerData);
    }

    public boolean isLocalServer() {
        return this.isLocalServer;
    }

    public boolean hasSingleplayerServer() {
        return this.isLocalServer && this.singleplayerServer != null;
    }

    public @Nullable IntegratedServer getSingleplayerServer() {
        return this.singleplayerServer;
    }

    public boolean isSingleplayer() {
        IntegratedServer singleplayerServer = this.getSingleplayerServer();
        return singleplayerServer != null && !singleplayerServer.isPublished();
    }

    public boolean isLocalPlayer(UUID profileId) {
        return profileId.equals(this.getUser().getProfileId());
    }

    public User getUser() {
        return this.user;
    }

    public GameProfile getGameProfile() {
        ProfileResult profileResult = this.profileFuture.join();
        return profileResult != null ? profileResult.profile() : new GameProfile(this.user.getProfileId(), this.user.getName());
    }

    public Proxy getProxy() {
        return this.proxy;
    }

    public TextureManager getTextureManager() {
        return this.textureManager;
    }

    public ShaderManager getShaderManager() {
        return this.shaderManager;
    }

    public ResourceManager getResourceManager() {
        return this.resourceManager;
    }

    public PackRepository getResourcePackRepository() {
        return this.resourcePackRepository;
    }

    public VanillaPackResources getVanillaPackResources() {
        return this.vanillaPackResources;
    }

    public DownloadedPackSource getDownloadedPackSource() {
        return this.downloadedPackSource;
    }

    public Path getResourcePackDirectory() {
        return this.resourcePackDirectory;
    }

    public LanguageManager getLanguageManager() {
        return this.languageManager;
    }

    public boolean isPaused() {
        return this.pause;
    }

    public GpuWarnlistManager getGpuWarnlistManager() {
        return this.gpuWarnlistManager;
    }

    public SoundManager getSoundManager() {
        return this.soundManager;
    }

    public @Nullable Music getSituationalMusic() {
        Music screenMusic = Optionull.map(this.screen, Screen::getBackgroundMusic);
        if (screenMusic != null) {
            return screenMusic;
        } else {
            Camera camera = this.gameRenderer.getMainCamera();
            if (this.player != null && camera != null) {
                Level playerLevel = this.player.level();
                if (playerLevel.dimension() == Level.END && this.gui.getBossOverlay().shouldPlayMusic()) {
                    return Musics.END_BOSS;
                } else {
                    BackgroundMusic backgroundMusic = camera.attributeProbe().getValue(EnvironmentAttributes.BACKGROUND_MUSIC, 1.0F);
                    boolean isCreative = this.player.getAbilities().instabuild && this.player.getAbilities().mayfly;
                    boolean isUnderwater = this.player.isUnderWater();
                    return backgroundMusic.select(isCreative, isUnderwater).orElse(null);
                }
            } else {
                return Musics.MENU;
            }
        }
    }

    public float getMusicVolume() {
        if (this.screen != null && this.screen.getBackgroundMusic() != null) {
            return 1.0F;
        } else {
            Camera camera = this.gameRenderer.getMainCamera();
            return camera != null ? camera.attributeProbe().getValue(EnvironmentAttributes.MUSIC_VOLUME, 1.0F) : 1.0F;
        }
    }

    public Services services() {
        return this.services;
    }

    public SkinManager getSkinManager() {
        return this.skinManager;
    }

    public @Nullable Entity getCameraEntity() {
        return this.gameRenderer.getMainCamera().entity();
    }

    public void setCameraEntity(@Nullable Entity cameraEntity) {
        this.gameRenderer.getMainCamera().setEntity(cameraEntity);
        this.gameRenderer.checkEntityPostEffect(cameraEntity);
    }

    public boolean shouldEntityAppearGlowing(Entity entity) {
        return entity.isCurrentlyGlowing()
            || this.player != null && this.player.isSpectator() && this.options.keySpectatorOutlines.isDown() && entity.is(EntityType.PLAYER);
    }

    @Override
    protected Thread getRunningThread() {
        return this.gameThread;
    }

    @Override
    public Runnable wrapRunnable(Runnable runnable) {
        return runnable;
    }

    @Override
    protected boolean shouldRun(Runnable task) {
        return true;
    }

    public EntityRenderDispatcher getEntityRenderDispatcher() {
        return this.entityRenderDispatcher;
    }

    public BlockEntityRenderDispatcher getBlockEntityRenderDispatcher() {
        return this.blockEntityRenderDispatcher;
    }

    public MapRenderer getMapRenderer() {
        return this.mapRenderer;
    }

    public DataFixer getFixerUpper() {
        return this.fixerUpper;
    }

    public DeltaTracker getDeltaTracker() {
        return this.deltaTracker;
    }

    public BlockColors getBlockColors() {
        return this.blockColors;
    }

    public boolean showOnlyReducedInfo() {
        return this.player != null && this.player.isReducedDebugInfo() || this.options.reducedDebugInfo().get();
    }

    public ToastManager getToastManager() {
        return this.toastManager;
    }

    public Tutorial getTutorial() {
        return this.tutorial;
    }

    public boolean isWindowActive() {
        return this.window.isFocused();
    }

    public HotbarManager getHotbarManager() {
        return this.hotbarManager;
    }

    public ModelManager getModelManager() {
        return this.modelManager;
    }

    public AtlasManager getAtlasManager() {
        return this.atlasManager;
    }

    public MapTextureManager getMapTextureManager() {
        return this.mapTextureManager;
    }

    public WaypointStyleManager getWaypointStyles() {
        return this.waypointStyles;
    }

    public Component grabPanoramixScreenshot(File folder) {
        int downscaleFactor = 4;
        int width = 4096;
        int height = 4096;
        int ow = this.window.getWidth();
        int oh = this.window.getHeight();
        RenderTarget target = this.getMainRenderTarget();
        float xRot = this.player.getXRot();
        float yRot = this.player.getYRot();
        float xRotO = this.player.xRotO;
        float yRotO = this.player.yRotO;
        this.gameRenderer.setRenderBlockOutline(false);
        Camera camera = this.gameRenderer.getMainCamera();

        MutableComponent var14;
        try {
            camera.enablePanoramicMode();
            this.window.setWidth(4096);
            this.window.setHeight(4096);
            target.resize(4096, 4096);

            for (int i = 0; i < 6; i++) {
                switch (i) {
                    case 0:
                        this.player.setYRot(yRot);
                        this.player.setXRot(0.0F);
                        break;
                    case 1:
                        this.player.setYRot((yRot + 90.0F) % 360.0F);
                        this.player.setXRot(0.0F);
                        break;
                    case 2:
                        this.player.setYRot((yRot + 180.0F) % 360.0F);
                        this.player.setXRot(0.0F);
                        break;
                    case 3:
                        this.player.setYRot((yRot - 90.0F) % 360.0F);
                        this.player.setXRot(0.0F);
                        break;
                    case 4:
                        this.player.setYRot(yRot);
                        this.player.setXRot(-90.0F);
                        break;
                    case 5:
                    default:
                        this.player.setYRot(yRot);
                        this.player.setXRot(90.0F);
                }

                this.player.yRotO = this.player.getYRot();
                this.player.xRotO = this.player.getXRot();
                this.gameRenderer.update(DeltaTracker.ONE, true);
                this.gameRenderer.extract(DeltaTracker.ONE, true);
                this.gameRenderer.renderLevel(DeltaTracker.ONE);

                try {
                    Thread.sleep(10L);
                } catch (InterruptedException var19) {
                }

                Screenshot.grab(folder, "panorama_" + i + ".png", target, 4, result -> {});
            }

            Component name = Component.literal(folder.getName())
                .withStyle(ChatFormatting.UNDERLINE)
                .withStyle(s -> s.withClickEvent(new ClickEvent.OpenFile(folder.getAbsoluteFile())));
            return Component.translatable("screenshot.success", name);
        } catch (Exception var20) {
            LOGGER.error("Couldn't save image", (Throwable)var20);
            var14 = Component.translatable("screenshot.failure", var20.getMessage());
        } finally {
            this.player.setXRot(xRot);
            this.player.setYRot(yRot);
            this.player.xRotO = xRotO;
            this.player.yRotO = yRotO;
            this.gameRenderer.setRenderBlockOutline(true);
            this.window.setWidth(ow);
            this.window.setHeight(oh);
            target.resize(ow, oh);
            camera.disablePanoramicMode();
        }

        return var14;
    }

    public SplashManager getSplashManager() {
        return this.splashManager;
    }

    public @Nullable Overlay getOverlay() {
        return this.overlay;
    }

    public PlayerSocialManager getPlayerSocialManager() {
        return this.playerSocialManager;
    }

    public Window getWindow() {
        return this.window;
    }

    public TextInputManager textInputManager() {
        return this.textInputManager;
    }

    public void onTextInputFocusChange(GuiEventListener element, boolean isFocused) {
        this.textInputManager.onTextInputFocusChange(isFocused);
        if (this.screen != null) {
            if (isFocused) {
                this.keyboardHandler.resubmitLastPreeditEvent(element);
            } else {
                KeyboardHandler.submitPreeditEvent(element, null);
            }
        }
    }

    public FramerateLimitTracker getFramerateLimitTracker() {
        return this.framerateLimitTracker;
    }

    public DebugScreenOverlay getDebugOverlay() {
        return this.gui.getDebugOverlay();
    }

    public RenderBuffers renderBuffers() {
        return this.renderBuffers;
    }

    public void updateMaxMipLevel(int mipmapLevels) {
        this.atlasManager.updateMaxMipLevel(mipmapLevels);
    }

    public EntityModelSet getEntityModels() {
        return this.modelManager.entityModels().get();
    }

    public boolean isTextFilteringEnabled() {
        return this.userProperties().flag(UserFlag.PROFANITY_FILTER_ENABLED);
    }

    public void prepareForMultiplayer() {
        this.playerSocialManager.startOnlineMode();
        this.getProfileKeyPairManager().prepareKeyPair();
    }

    public InputType getLastInputType() {
        return this.lastInputType;
    }

    public void setLastInputType(InputType lastInputType) {
        this.lastInputType = lastInputType;
    }

    public GameNarrator getNarrator() {
        return this.narrator;
    }

    public ChatListener getChatListener() {
        return this.chatListener;
    }

    public ReportingContext getReportingContext() {
        return this.reportingContext;
    }

    public RealmsDataFetcher realmsDataFetcher() {
        return this.realmsDataFetcher;
    }

    public QuickPlayLog quickPlayLog() {
        return this.quickPlayLog;
    }

    public CommandHistory commandHistory() {
        return this.commandHistory;
    }

    public DirectoryValidator directoryValidator() {
        return this.directoryValidator;
    }

    public PlayerSkinRenderCache playerSkinRenderCache() {
        return this.playerSkinRenderCache;
    }

    private float getTickTargetMillis(float defaultTickTargetMillis) {
        if (this.level != null) {
            TickRateManager manager = this.level.tickRateManager();
            if (manager.runsNormally()) {
                return Math.max(defaultTickTargetMillis, manager.millisecondsPerTick());
            }
        }

        return defaultTickTargetMillis;
    }

    public BlockModelResolver getBlockModelResolver() {
        return this.blockModelResolver;
    }

    public ItemModelResolver getItemModelResolver() {
        return this.itemModelResolver;
    }

    public boolean canInterruptScreen() {
        return (this.screen == null || this.screen.canInterruptWithAnotherScreen()) && !this.clientLevelTeardownInProgress;
    }

    public static @Nullable String getLauncherBrand() {
        return System.getProperty("minecraft.launcher.brand");
    }

    public PacketProcessor packetProcessor() {
        return this.packetProcessor;
    }

    public Gizmos.TemporaryCollection collectPerTickGizmos() {
        return Gizmos.withCollector(this.perTickGizmos);
    }

    public Collection<SimpleGizmoCollector.GizmoInstance> getPerTickGizmos() {
        return this.drainedLatestTickGizmos;
    }

    private void pick(float partialTicks) {
        Entity cameraEntity = this.getCameraEntity();
        if (cameraEntity != null) {
            if (this.level != null && this.player != null) {
                Profiler.get().push("pick");
                this.hitResult = this.player.raycastHitResult(partialTicks, cameraEntity);
                this.crosshairPickEntity = this.hitResult instanceof EntityHitResult entityHitResult ? entityHitResult.getEntity() : null;
                Profiler.get().pop();
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private record GameLoadCookie(RealmsClient realmsClient, GameConfig.QuickPlayData quickPlayData) {
    }
}
