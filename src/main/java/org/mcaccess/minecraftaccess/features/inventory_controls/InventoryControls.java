package org.mcaccess.minecraftaccess.features.inventory_controls;

import java.util.List;
import java.util.Objects;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.extern.slf4j.Slf4j;
import net.blay09.mods.balm.client.platform.event.callback.ClientTickCallback;
import net.blay09.mods.balm.client.platform.module.BalmClientModule;
import net.blay09.mods.kuma.api.InputBinding;
import net.blay09.mods.kuma.api.KeyModifier;
import net.blay09.mods.kuma.api.KeyModifiers;
import net.blay09.mods.kuma.api.Kuma;
import net.blay09.mods.kuma.api.ScreenInputEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.features.NarrateHeldItem;
import org.mcaccess.minecraftaccess.mixin.AbstractContainerScreenAccessor;
import org.mcaccess.minecraftaccess.mixin.AbstractRecipeBookScreenAccessor;
import org.mcaccess.minecraftaccess.mixin.AnvilScreenAccessor;
import org.mcaccess.minecraftaccess.mixin.CreativeModeInventoryScreenAccessor;
import org.mcaccess.minecraftaccess.mixin.EditBoxAccessor;
import org.mcaccess.minecraftaccess.mixin.RecipeBookComponentAccessor;
import org.mcaccess.minecraftaccess.mixin.RecipeBookPageAccessor;
import org.mcaccess.minecraftaccess.utils.KeyMappingCategories;
import org.mcaccess.minecraftaccess.utils.condition.Interval;
import org.mcaccess.minecraftaccess.utils.system.MouseUtils;

/**
 * This features lets us use keyboard in inventory screens. Works with all default minecraft screens.
 *
 * <p>Key binds and combinations:
 * (all key binds are re-mappable(except two keys) from the game's controls menu and these key binds do not interrupt with any other key with same key.)<br>
 * 1) Up Key (default: I) = Focus to slot above.<br>
 * 2) Right Key (default: L) = Focus to slot right.<br>
 * 3) Down Key (default: K) = Focus to slot down.<br>
 * 4) Left Key (default: J) = Focus to slot left.<br>
 * 5) Group Key (default: C) = Select next group.<br>
 * 6) Left Shift + Group Key = Select previous group.<br>
 * 7) Switch Tab Key (default: V) = Select next tab (only for creative inventory screen and inventory/crafting screen).<br>
 * 8) Left Shift + Switch Tab Key = Select previous tab (only for creative inventory screen and inventory/crafting screen).<br>
 * 9) Toggle Craftable Key (default: R) = Toggle between show all and show only craftable recipes in inventory/crafting screen.<br>
 * 10) T Key (not re-mappable) = Select the search box.<br>
 * 11) Enter Key (not re-mappable) = Deselect the search box.
 */
@Slf4j
public class InventoryControls implements BalmClientModule {
    private static @Nullable InventoryControls active;

    private Config.InventoryControls config;
    private final Interval interval = Interval.defaultDelay();

    private AbstractContainerScreenAccessor previousScreen = null;
    private AbstractContainerScreenAccessor currentScreen = null;

    private List<SlotsGroup> currentSlotsGroupList = null;
    private SlotsGroup currentGroup = null;
    private int currentGroupIndex = 0;
    private SlotItem currentSlotItem = null;
    private RecipeBookComponent<?> currentRecipeBookWidget = null;
    private String previousSlotText = "";
    private final FocusedVariantTracker<RecipeButton, RecipeVariant> recipeVariantTracker = new FocusedVariantTracker<>();

    public InventoryControls() {
        loadConfig();
    }

    @Override
    public @NotNull Identifier getId() {
        return Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "inventory_controls");
    }

    @Override
    public void initialize() {
        active = this;
        ClientTickCallback.AFTER.register(this::tick);

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "inventory_controls.change_group/previous"))
                .withDefault(InputBinding.key(InputConstants.KEY_C, KeyModifiers.of(KeyModifier.SHIFT)))
                .overrideCategory(KeyMappingCategories.INVENTORY_CONTROLS)
                .ignoreScreenFocus()
                .handleScreenInput(handleInventoryInput(_ -> {
                    log.debug("Group key pressed");
                    changeGroup(false);
                    return true;
                }))
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "inventory_controls.change_group/next"))
                .withDefault(InputBinding.key(InputConstants.KEY_C))
                .overrideCategory(KeyMappingCategories.INVENTORY_CONTROLS)
                .ignoreScreenFocus()
                .handleScreenInput(handleInventoryInput(_ -> {
                    log.debug("Group key pressed");
                    changeGroup(true);
                    return true;
                }))
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "inventory_controls.switch_tab/previous"))
                .withDefault(InputBinding.key(InputConstants.KEY_V, KeyModifiers.of(KeyModifier.SHIFT)))
                .overrideCategory(KeyMappingCategories.INVENTORY_CONTROLS)
                .ignoreScreenFocus()
                .handleScreenInput(handleInventoryInput(_ -> {
                    log.debug("Switch Tab key pressed");
                    if (currentScreen instanceof InventoryScreen || currentScreen instanceof CraftingScreen) {
                        changeRecipeTab(false);
                        return true;
                    } else if (currentScreen instanceof CreativeModeInventoryScreen) {
                        changeCreativeInventoryTab(false);
                        return true;
                    }
                    return false;
                }))
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "inventory_controls.switch_tab/next"))
                .withDefault(InputBinding.key(InputConstants.KEY_V))
                .overrideCategory(KeyMappingCategories.INVENTORY_CONTROLS)
                .ignoreScreenFocus()
                .handleScreenInput(handleInventoryInput(_ -> {
                    log.debug("Switch Tab key pressed");
                    if (currentScreen instanceof InventoryScreen || currentScreen instanceof CraftingScreen) {
                        changeRecipeTab(true);
                        return true;
                    } else if (currentScreen instanceof CreativeModeInventoryScreen) {
                        changeCreativeInventoryTab(true);
                        return true;
                    }
                    return false;
                }))
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "inventory_controls.move/left"))
                .withDefault(InputBinding.key(InputConstants.KEY_J))
                .overrideCategory(KeyMappingCategories.INVENTORY_CONTROLS)
                .ignoreScreenFocus()
                .handleScreenInput(handleInventoryInput(_ -> {
                    log.debug("Left key pressed");
                    focusSlotItemAt(FocusDirection.LEFT);
                    return true;
                }))
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "inventory_controls.move/right"))
                .withDefault(InputBinding.key(InputConstants.KEY_L))
                .overrideCategory(KeyMappingCategories.INVENTORY_CONTROLS)
                .ignoreScreenFocus()
                .handleScreenInput(handleInventoryInput(_ -> {
                    log.debug("Right key pressed");
                    focusSlotItemAt(FocusDirection.RIGHT);
                    return true;
                }))
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "inventory_controls.move/up"))
                .withDefault(InputBinding.key(InputConstants.KEY_I))
                .overrideCategory(KeyMappingCategories.INVENTORY_CONTROLS)
                .ignoreScreenFocus()
                .handleScreenInput(handleInventoryInput(_ -> {
                    log.debug("Up key pressed");
                    focusSlotItemAt(FocusDirection.UP);
                    return true;
                }))
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "inventory_controls.switch_recipe_book_page/previous"))
                .withDefault(InputBinding.key(InputConstants.KEY_I, KeyModifiers.of(KeyModifier.SHIFT)))
                .overrideCategory(KeyMappingCategories.INVENTORY_CONTROLS)
                .ignoreScreenFocus()
                .handleScreenInput(handleInventoryInput(_ -> {
                    if (currentGroup.isScrollable) {
                        log.debug("Previous Recipe Book page key pressed");
                        if (isRecipeBookOpen() && currentGroup.isRecipeGroup()) {
                            changeRecipeBookPage(false);
                        } else {
                            MouseUtils.Wheel.UP.scroll();
                        }
                        return true;
                    }
                    return false;
                }))
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "inventory_controls.move/down"))
                .withDefault(InputBinding.key(InputConstants.KEY_K))
                .overrideCategory(KeyMappingCategories.INVENTORY_CONTROLS)
                .ignoreScreenFocus()
                .handleScreenInput(handleInventoryInput(_ -> {
                    log.debug("Down key pressed");
                    focusSlotItemAt(FocusDirection.DOWN);
                    return true;
                }))
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "inventory_controls.switch_recipe_book_page/next"))
                .withDefault(InputBinding.key(InputConstants.KEY_K, KeyModifiers.of(KeyModifier.SHIFT)))
                .overrideCategory(KeyMappingCategories.INVENTORY_CONTROLS)
                .ignoreScreenFocus()
                .handleScreenInput(handleInventoryInput(_ -> {
                    if (currentGroup.isScrollable) {
                        log.debug("Next Recipe Book page key pressed");
                        if (isRecipeBookOpen() && currentGroup.isRecipeGroup()) {
                            changeRecipeBookPage(true);
                        } else {
                            MouseUtils.Wheel.DOWN.scroll();
                        }
                        return true;
                    }
                    return false;
                }))
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "inventory_controls.jump_to_textbox"))
                .withDefault(InputBinding.key(InputConstants.KEY_T))
                .overrideCategory(KeyMappingCategories.INVENTORY_CONTROLS)
                .ignoreScreenFocus()
                .handleScreenInput(handleInventoryInput(_ -> {
                    if (CreativeModeInventoryScreenAccessor.getSelectedTab().getType() == CreativeModeTab.Type.SEARCH
                            && currentScreen instanceof CreativeModeInventoryScreen creativeInventoryScreen) {
                        setSearchBoxFocus(((CreativeModeInventoryScreenAccessor) creativeInventoryScreen).getSearchBox(), true);
                        return true;
                    } else if (currentScreen instanceof AnvilScreen anvilScreen) {
                        setSearchBoxFocus(((AnvilScreenAccessor) anvilScreen).getName(), true);
                        return true;
                    } else if (isRecipeBookOpen()) {
                        // resolve can-not-enter-characters-issue https://github.com/minecraft-access/minecraft-access/issues/67
                        Minecraft.getInstance().gui.screen().setFocused(currentRecipeBookWidget);
                        setSearchBoxFocus(((RecipeBookComponentAccessor) currentRecipeBookWidget).getSearchBox(), true);
                        return true;
                    }
                    return false;
                }))
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "inventory_controls.toggle_craftable"))
                .withDefault(InputBinding.key(InputConstants.KEY_R))
                .overrideCategory(KeyMappingCategories.INVENTORY_CONTROLS)
                .ignoreScreenFocus()
                .handleScreenInput(handleInventoryInput(_ -> {
                    if (currentRecipeBookWidget == null) return false;
                    if (!currentRecipeBookWidget.isVisible()) return false;

                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
                    ((RecipeBookComponentAccessor) currentRecipeBookWidget).getFilterButton().onPress(new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));

                    String narration = ((RecipeBookComponentAccessor) currentRecipeBookWidget).getFilterButton().getValue()
                            ? ((RecipeBookComponentAccessor) currentRecipeBookWidget).callGetRecipeFilterName().getString()
                            : I18n.get("gui.recipebook.toggleRecipes.all");
                    MainClass.narrate(narration, true);

                    return true;
                }))
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "inventory_controls.fuel_status"))
                .withDefault(InputBinding.key(InputConstants.KEY_U))
                .overrideCategory(KeyMappingCategories.INVENTORY_CONTROLS)
                .ignoreScreenFocus()
                .handleScreenInput(handleInventoryInput(_ -> {
                    if (currentScreen.getMenu() instanceof AbstractFurnaceMenu furnace) {
                        MainClass.narrate(I18n.get("minecraft_access.inventory_controls.fuel_status",
                                Math.round(furnace.getLitProgress() * 100),
                                Math.round(furnace.getBurnProgress() * 100)), true);
                        return true;
                    } else if (currentScreen.getMenu() instanceof BrewingStandMenu brewingStand) {
                        int fuelPercent = brewingStand.getTotalFuel() > 0 ? (brewingStand.getFuel() * 100) / brewingStand.getTotalFuel() : 0;
                        int brewingTicks = brewingStand.getBrewingTicks();
                        int totalBrewingTicks = brewingStand.getTotalBrewingTicks();
                        int brewPercent = (brewingTicks > 0 && totalBrewingTicks > 0)
                                ? ((totalBrewingTicks - brewingTicks) * 100) / totalBrewingTicks
                                : 0;
                        MainClass.narrate(I18n.get("minecraft_access.inventory_controls.fuel_status", fuelPercent, brewPercent), true);
                        return true;
                    }
                    return false;
                }))
                .build();
    }

    private ScreenInputEventHandler handleInventoryInput(ScreenInputEventHandler handler) {
        return event -> InventoryScreenInput.shouldHandle(event.getFocusedElement(), getCapturingSearchBox() != null)
                && handler.handle(event);
    }

    private @Nullable EditBox getCapturingSearchBox() {
        if (currentScreen instanceof CreativeModeInventoryScreen creativeInventoryScreen) {
            EditBox searchBox = ((CreativeModeInventoryScreenAccessor) creativeInventoryScreen).getSearchBox();
            if (searchBox.canConsumeInput()) return searchBox;
        }
        if (currentScreen instanceof AnvilScreen anvilScreen) {
            EditBox searchBox = ((AnvilScreenAccessor) anvilScreen).getName();
            if (searchBox.canConsumeInput()) return searchBox;
        }
        if (isRecipeBookOpen()) {
            EditBox searchBox = ((RecipeBookComponentAccessor) currentRecipeBookWidget).getSearchBox();
            if (searchBox.canConsumeInput()) return searchBox;
        }
        return null;
    }

    private void tick(Minecraft client) {
        tickRecipeVariant(client);
        if (!interval.isReady()) return;

        if (client.player == null) return;
        if (client.gui.screen() == null) {
            previousScreen = null;
            currentScreen = null;
            currentGroupIndex = 0;
            currentGroup = null;
            currentRecipeBookWidget = null;
            recipeVariantTracker.reset();
            return;
        }
        if (!(client.gui.screen() instanceof AbstractContainerScreen)) return;
        if (!config.enabled) {
            return;
        }

        loadConfig();
        currentScreen = (AbstractContainerScreenAccessor) client.gui.screen();
        currentRecipeBookWidget = getRecipeBookWidget(client.gui.screen());
        currentSlotsGroupList = GroupGenerator.generateGroupsFromSlots(currentScreen);

        interval.adjustNextReadyTimeBy(keyListener());

        // On screen open
        if (previousScreen != currentScreen) {
            previousScreen = currentScreen;
            if (currentScreen instanceof AnvilScreen anvilScreen) {
                setSearchBoxFocus(((AnvilScreenAccessor) anvilScreen).getName(), false);
            }
            if (currentScreen instanceof CreativeModeInventoryScreen creativeInventoryScreen) {
                EditBox searchBox = ((CreativeModeInventoryScreenAccessor) creativeInventoryScreen).getSearchBox();
                if (searchBox.canConsumeInput()) {
                    setSearchBoxFocus(searchBox, false);
                }
            }

            //<editor-fold desc="Always open recipe book on screen open">
            if (config.autoOpenRecipeBook && currentRecipeBookWidget != null) {
                if (!currentRecipeBookWidget.isVisible()) currentRecipeBookWidget.toggleVisibility();
                setSearchBoxFocus(((RecipeBookComponentAccessor) currentRecipeBookWidget).getSearchBox(), false);
            }
            //</editor-fold>

            refreshGroupListAndSelectFirstGroup(false); // Interrupt is false to let it narrate the screen's name
        }

        if (currentSlotsGroupList.isEmpty()) return;

        if (config.narrateFocusedSlotChanges) {
            String slotNarrationText = getCurrentSlotNarrationText();
            if (!previousSlotText.equals(slotNarrationText)) {
                previousSlotText = slotNarrationText;
                MainClass.narrate(previousSlotText, true);
            }
        }
    }

    private @Nullable RecipeBookComponent<?> getRecipeBookWidget(Screen screen) {
        if (screen instanceof AbstractRecipeBookScreen<?> recipeBookScreen) {
            return ((AbstractRecipeBookScreenAccessor) recipeBookScreen).getRecipeBookComponent();
        }
        return null;
    }

    /**
     * Load configs from config.json.
     */
    private void loadConfig() {
        config = Config.getInstance().inventoryControls;
        interval.setDelay(config.delayMilliseconds, Interval.Unit.MILLISECOND);
    }

    /**
     * Handles the key inputs.
     */
    private boolean keyListener() {
        boolean isEnterPressed = InputConstants.isKeyDown(InputConstants.KEY_RETURN)
                || InputConstants.isKeyDown(InputConstants.KEY_NUMPADENTER);
        EditBox searchBox = getCapturingSearchBox();
        if (searchBox == null || !isEnterPressed) return false;

        setSearchBoxFocus(searchBox, false);
        if (currentScreen instanceof CreativeModeInventoryScreen) {
            refreshGroupListAndSelectFirstGroup(true);
        } else {
            previousSlotText = "";
        }
        return true;
    }

    private boolean isRecipeBookOpen() {
        return currentRecipeBookWidget != null && currentRecipeBookWidget.isVisible();
    }

    private boolean clickRecipeBookPage(boolean next) {
        RecipeBookPageAccessor page = (RecipeBookPageAccessor) ((RecipeBookComponentAccessor) currentRecipeBookWidget).getRecipeBookPage();
        ImageButton button = next ? page.getForwardButton() : page.getBackButton();
        if (button == null || !button.visible) return false;
        return clickRecipeBookWidget(button);
    }

    private boolean clickRecipeBookWidget(AbstractWidget widget) {
        MouseButtonEvent event = new MouseButtonEvent(
                widget.getX() + widget.getWidth() / 2.0,
                widget.getY() + widget.getHeight() / 2.0,
                new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)
        );
        return currentRecipeBookWidget.mouseClicked(event, false);
    }

    private void changeRecipeBookPage(boolean next) {
        if (currentSlotItem == null || !isRecipeBookOpen()) return;

        int referenceX = currentSlotItem.x;
        RecipeBookPageAccessor page = (RecipeBookPageAccessor) ((RecipeBookComponentAccessor) currentRecipeBookWidget).getRecipeBookPage();
        int previousPage = page.getCurrentPage();
        if (!clickRecipeBookPage(next)) {
            MainClass.narrate(I18n.get(next ? "minecraft_access.recipe_book.last_page" : "minecraft_access.recipe_book.first_page"), true);
            return;
        }
        if (page.getCurrentPage() == previousPage) {
            MainClass.narrate(I18n.get(next ? "minecraft_access.recipe_book.last_page" : "minecraft_access.recipe_book.first_page"), true);
            return;
        }

        refreshGroupsAfterRecipeStructureChange();
        SlotItem target = currentGroup != null && currentGroup.isRecipeGroup()
                ? RecipeBookPageNavigator.nearestInEdgeRow(currentGroup.slotItems, !next, referenceX)
                : null;
        String pageText = I18n.get("minecraft_access.recipe_book.page", page.getCurrentPage() + 1, page.getTotalPages());
        if (target != null) {
            focusSlotItem(target, true, pageText);
        } else {
            currentSlotItem = null;
            recipeVariantTracker.reset();
            MainClass.narrate(pageText, true);
        }
    }

    /**
     * Focuses a slot item in the specified direction if available.
     *
     * @param focusDirection The direction of the slot item to focus.
     */
    private void focusSlotItemAt(FocusDirection focusDirection) {
        if (currentGroup == null) {
            changeGroup(true);
            return;
        }
        if (currentSlotItem == null) {
            focusSlotItem(currentGroup.getFirstGroupItem(), true);
            return;
        }

        SlotItem slotItem = getGroupItemInDirection(focusDirection);
        if (slotItem == null) {
            if ((focusDirection == FocusDirection.UP || focusDirection == FocusDirection.DOWN)
                    && currentGroup.isRecipeGroup()
                    && isRecipeBookOpen()) {
                changeRecipeBookPage(focusDirection == FocusDirection.DOWN);
                return;
            }

            MainClass.narrate(I18n.get("minecraft_access.inventory_controls.no_slot_in_direction", I18n.get(focusDirection.getString())), true);
            return;
        }

        focusSlotItem(slotItem, true);
    }

    /**
     * Returns the slot item in the specified direction if available.
     *
     * @param focusDirection The direction of the slot item.
     * @return The object of the slot item if found else null.
     */
    private SlotItem getGroupItemInDirection(FocusDirection focusDirection) {
        switch (focusDirection) {
            case UP -> {
                if (!currentGroup.hasSlotItemAbove(currentSlotItem)) return null;

                if (currentSlotItem.upSlotItem != null) return currentSlotItem.upSlotItem;

                for (SlotItem item : currentGroup.slotItems) {
                    if (item.x == currentSlotItem.x && item.y == currentSlotItem.y - 18) {
                        return item;
                    }
                }
            }
            case RIGHT -> {
                if (!currentGroup.hasSlotItemRight(currentSlotItem)) return null;

                if (currentSlotItem.rightSlotItem != null) return currentSlotItem.rightSlotItem;

                for (SlotItem item : currentGroup.slotItems) {
                    if (item.x == currentSlotItem.x + 18 && item.y == currentSlotItem.y) {
                        return item;
                    }
                }
            }
            case DOWN -> {
                if (!currentGroup.hasSlotItemBelow(currentSlotItem)) return null;

                if (currentSlotItem.downSlotItem != null) return currentSlotItem.downSlotItem;

                for (SlotItem item : currentGroup.slotItems) {
                    if (item.x == currentSlotItem.x && item.y == currentSlotItem.y + 18) {
                        return item;
                    }
                }
            }
            case LEFT -> {
                if (!currentGroup.hasSlotItemLeft(currentSlotItem)) return null;

                if (currentSlotItem.leftSlotItem != null) return currentSlotItem.leftSlotItem;

                for (SlotItem item : currentGroup.slotItems) {
                    if (item.x == currentSlotItem.x - 18 && item.y == currentSlotItem.y) {
                        return item;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Focuses at the specified slot item in the current group and narrate its details.
     *
     * @param slotItem  The object of the slot item to focus.
     * @param interrupt Whether to stop the narrator from narrating the previous message or not.
     */
    private void focusSlotItem(@NotNull SlotItem slotItem, boolean interrupt) {
        focusSlotItem(slotItem, interrupt, null);
    }

    private void focusSlotItem(@NotNull SlotItem slotItem, boolean interrupt, @Nullable String prefix) {
        currentSlotItem = slotItem;

        String narration = getCurrentSlotNarrationText();
        if (!narration.isEmpty() && prefix != null) {
            narration = prefix + " " + narration;
        }
        if (!narration.isEmpty()) {
            previousSlotText = narration;
            MainClass.narrate(narration, interrupt);
        }

        if (slotItem.recipeButton != null) {
            recipeVariantTracker.update(slotItem.recipeButton, RecipeVariant.from(slotItem.recipeButton));
        } else {
            recipeVariantTracker.reset();
        }
    }

    private void refreshGroupsAfterRecipeStructureChange() {
        if (currentScreen == null) return;

        currentSlotsGroupList = GroupGenerator.generateGroupsFromSlots(currentScreen);
        if (currentSlotsGroupList.isEmpty()) {
            currentGroup = null;
            currentSlotItem = null;
            recipeVariantTracker.reset();
            return;
        }

        int recipeGroupIndex = -1;
        for (int i = 0; i < currentSlotsGroupList.size(); i++) {
            if (currentSlotsGroupList.get(i).isRecipeGroup()) {
                recipeGroupIndex = i;
                break;
            }
        }

        if (recipeGroupIndex >= 0 && (currentGroup == null || currentGroup.isRecipeGroup())) {
            currentGroupIndex = recipeGroupIndex;
            currentGroup = currentSlotsGroupList.get(recipeGroupIndex);
        }
    }

    private void tickRecipeVariant(Minecraft client) {
        if (!config.enabled) {
            recipeVariantTracker.reset();
            return;
        }
        if (currentSlotItem == null || currentSlotItem.recipeButton == null) {
            recipeVariantTracker.reset();
            return;
        }

        RecipeButton recipeButton = currentSlotItem.recipeButton;
        if (!recipeButton.visible
                || !isRecipeBookOpen()
                || client.gui.screen() == null
                || getRecipeBookWidget(client.gui.screen()) != currentRecipeBookWidget) {
            recipeVariantTracker.reset();
            return;
        }

        RecipeVariant currentRecipe = RecipeVariant.from(recipeButton);
        if (recipeVariantTracker.update(recipeButton, currentRecipe)) {
            String narration = RecipeBookNarration.describe(recipeButton);
            if (!narration.equals(previousSlotText)) {
                previousSlotText = narration;
                MainClass.narrate(narration, true);
            }
        }
    }

    public static boolean ownsRecipeNarration(RecipeButton button) {
        if (active == null || !active.config.enabled) return false;
        return active.isRecipeBookOpen()
                && active.currentSlotItem != null
                && active.currentSlotItem.recipeButton == button;
    }

    public static boolean syncFocusedCursorForMouseAction() {
        if (active == null || !active.config.enabled) return true;

        Minecraft client = Minecraft.getInstance();
        if (client.gui.screen() == null) return true;
        if (!(client.gui.screen() instanceof AbstractContainerScreen)) return true;
        if (active.currentScreen == null || client.gui.screen() != (Screen) (Object) active.currentScreen) return false;
        if (active.currentSlotItem == null) return false;
        if (active.currentSlotItem.recipeButton != null
                && !active.currentSlotItem.recipeButton.visible) return false;

        active.moveToSlotItem(active.currentSlotItem);
        return true;
    }

    /**
     * Moves the mouse cursor over to the slot item specified.
     *
     * @param slotItem The object of the slot item to move the mouse cursor over to.
     */
    private void moveToSlotItem(SlotItem slotItem) {
        if (slotItem == null) return;

        int x = slotItem.x;
        int y = slotItem.y;

        MouseUtils.Coordinates p = MouseUtils.calcRealPositionOfWidget(currentScreen.getLeftPos() + x, currentScreen.getTopPos() + y);
        MouseUtils.move(p.x(), p.y());
    }

    /**
     * Get the details of the current slot item to narrate.
     *
     * @return The details of the current slot item.
     */
    private String getCurrentSlotNarrationText() {
        if (currentSlotItem == null) return "";

        Slot slot = currentSlotItem.slot;
        if (slot == null) {
            return Objects.requireNonNullElse(currentSlotItem.getNarratableText(), I18n.get("minecraft_access.inventory_controls.Unknown"));
        }
        if (!slot.hasItem()) {
            return I18n.get("minecraft_access.inventory_controls.empty_slot", currentGroup.getSlotPrefix(slot));
        }

        ItemStack itemStack = slot.getItem();
        StringBuilder narration = new StringBuilder();
        String prefix = currentGroup.getSlotPrefix(slot);
        if (!prefix.isEmpty()) {
            narration.append(prefix).append(' ');
        }

        int count = itemStack.getCount();
        if (count != 1 && !itemStack.isEmpty()) {
            narration.append(count).append(' ');
        }

        String baseItemName = NarrateHeldItem.getItemName(itemStack, false);

        List<Component> toolTipList = itemStack.getTooltipLines(TooltipContext.EMPTY, Minecraft.getInstance().player, TooltipFlag.NORMAL);
        StringBuilder toolTipBuilder = new StringBuilder();
        for (Component line : toolTipList) {
            String text = line.getString();
            if (!text.isEmpty()) {
                toolTipBuilder.append(text).append(' ');
            }
        }
        String toolTipText = toolTipBuilder.toString().trim();

        if (toolTipText.contains(baseItemName)) {
            narration.append(toolTipText);
        } else if (toolTipText.isEmpty()) {
            narration.append(baseItemName);
        } else {
            narration.append(baseItemName);
            for (Component line : toolTipList) {
                String text = line.getString();
                if (!baseItemName.contains(text)) {
                    narration.append(' ').append(text);
                }
            }
        }

        return narration.toString().trim();
    }

    /**
     * Change the selected group.
     *
     * @param goForward Whether to switch to next group or previous group.
     */
    private void changeGroup(boolean goForward) {
        int nextGroupIndex = currentGroupIndex + (goForward ? 1 : -1);
        nextGroupIndex = Mth.clamp(nextGroupIndex, 0, currentSlotsGroupList.size() - 1);

        if (nextGroupIndex == currentGroupIndex) return; // Skip if already at the first or last group
        currentGroupIndex = nextGroupIndex;
        selectGroup(true);
    }

    /**
     * Refreshes the current group list and selects the first group.
     *
     * @param interrupt Whether to stop the narrator from narrating the previous message or not.
     */
    private void refreshGroupListAndSelectFirstGroup(boolean interrupt) {
        currentSlotsGroupList = GroupGenerator.generateGroupsFromSlots(currentScreen);
        if (currentSlotsGroupList.isEmpty()) return;
        currentGroupIndex = 0;
        selectGroup(interrupt);
    }

    private void selectGroup(boolean interrupt) {
        currentGroup = currentSlotsGroupList.get(currentGroupIndex);
        log.debug("Group(name:{}) {}/{} selected", currentGroup.getGroupName(), currentGroupIndex + 1, currentSlotsGroupList.size());
        MainClass.narrate(I18n.get("minecraft_access.inventory_controls.group_selected",
                currentGroup.isScrollable ? I18n.get("minecraft_access.inventory_controls.scrollable") : "",
                currentGroup.getGroupName()), interrupt);
        focusSlotItem(currentGroup.getFirstGroupItem(), false);
    }

    /**
     * Changes the selected tab for creative inventory screen.
     *
     * @param goForward Whether to switch to next tab or previous tab.
     */
    private void changeCreativeInventoryTab(boolean goForward) {
        if (!(currentScreen instanceof CreativeModeInventoryScreen creativeInventoryScreen)) return;

        int tab = CreativeModeTabs.tabs().indexOf(CreativeModeInventoryScreenAccessor.getSelectedTab());

        if (goForward && tab + 1 < CreativeModeTabs.tabs().size()) {
            ((CreativeModeInventoryScreenAccessor) creativeInventoryScreen).invokeSelectTab(CreativeModeTabs.tabs().get(tab + 1));
            refreshGroupListAndSelectFirstGroup(false);
        } else if (!goForward && tab - 1 >= 0) {
            ((CreativeModeInventoryScreenAccessor) creativeInventoryScreen).invokeSelectTab(CreativeModeTabs.tabs().get(tab - 1));
            refreshGroupListAndSelectFirstGroup(false);
        }
    }

    /**
     * Changes the selected tab for inventory/crafting screen.
     *
     * @param goForward Whether to switch to next tab or previous tab.
     */
    private void changeRecipeTab(boolean goForward) {
        if (currentRecipeBookWidget == null) return;
        if (!currentRecipeBookWidget.isVisible()) return;

        RecipeBookComponentAccessor recipeBookComponentAccessor = (RecipeBookComponentAccessor) currentRecipeBookWidget;
        List<RecipeBookTabButton> visibleTabs = recipeBookComponentAccessor.getTabButtons().stream()
                .filter(tab -> tab.visible)
                .toList();
        if (visibleTabs.isEmpty()) return;

        int currentTabIndex = visibleTabs.indexOf(recipeBookComponentAccessor.getSelectedTab());
        int nextTabIndex = currentTabIndex + (goForward ? 1 : -1);
        if (nextTabIndex < 0 || nextTabIndex >= visibleTabs.size()) {
            MainClass.narrate(I18n.get("minecraft_access.other.reached_the_border"), true);
            return;
        }

        RecipeBookTabButton targetTab = visibleTabs.get(nextTabIndex);
        if (!clickRecipeBookWidget(targetTab)) return;

        RecipeBookTabButton selectedTab = recipeBookComponentAccessor.getSelectedTab();
        if (selectedTab != targetTab) return;

        List<RecipeBookTabButton> selectedVisibleTabs = recipeBookComponentAccessor.getTabButtons().stream()
                .filter(tab -> tab.visible)
                .toList();
        String categoryName = RecipeBookCategoryLabels.describe(selectedTab.getCategory());
        int selectedIndex = selectedVisibleTabs.indexOf(selectedTab);
        if (selectedIndex < 0) selectedIndex = nextTabIndex;
        String position = "%d/%d".formatted(selectedIndex + 1, selectedVisibleTabs.size());
        MainClass.narrate(I18n.get("minecraft_access.inventory_controls.tab_selected", categoryName + " (" + position + ")"), true);
        log.debug("Change tab to {}", categoryName);

        refreshGroupsAfterRecipeStructureChange();
        if (currentGroup != null && currentGroup.isRecipeGroup() && !currentGroup.slotItems.isEmpty()) {
            currentSlotItem = currentGroup.getFirstGroupItem();
            previousSlotText = getCurrentSlotNarrationText();
            recipeVariantTracker.update(currentSlotItem.recipeButton, RecipeVariant.from(currentSlotItem.recipeButton));
        }
    }

    /**
     * Encapsulate the changes against the vanilla code here.
     * Correspond to the vanilla code after 1.20.x
     */
    private void setSearchBoxFocus(EditBox w, boolean focus) {
        if (focus) {
            log.debug("T key pressed, selecting the search box.");
            w.setFocused(true);
        } else {
            log.debug("Enter key pressed, deselecting the search box.");
            boolean origin = ((EditBoxAccessor) w).getCanLoseFocus();
            w.setCanLoseFocus(true);
            w.setFocused(false);
            w.setCanLoseFocus(origin);
        }
    }

    private enum FocusDirection {
        UP("gui.up"),
        DOWN("gui.down"),
        LEFT("minecraft_access.inventory_controls.direction_left"),
        RIGHT("minecraft_access.inventory_controls.direction_right");

        private final String value;

        FocusDirection(String value) {
            this.value = value;
        }

        String getString() {
            return value;
        }
    }

    private record RecipeVariant(RecipeDisplayId recipe, Item item, int count, DataComponentMap components) {
        private static RecipeVariant from(RecipeButton button) {
            ItemStack itemStack = button.getDisplayStack();
            return new RecipeVariant(button.getCurrentRecipe(), itemStack.getItem(), itemStack.getCount(), itemStack.getComponents());
        }
    }
}
