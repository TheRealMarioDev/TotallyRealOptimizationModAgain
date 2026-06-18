package name.modid.client.horror;

import name.modid.horror.HorrorOperator;
import name.modid.horror.spy.SpyActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Feature 2 — Silent inventory spy screen (client-side).
 *
 * <p>Renders a mirror of the target player's inventory without opening any container
 * on the server, without triggering sounds, and without the target player seeing any
 * animation or GUI flicker on their end.</p>
 *
 * <h3>Layout</h3>
 * <pre>
 *  ┌──────────────────────────────────────┐
 *  │  Spying on: PlayerName               │
 *  │  [Armor: H B L F]                    │
 *  │  [Main inventory grid 9×3]           │
 *  │  [Hotbar 9×1]                        │
 *  │  [Offhand]          [Close]          │
 *  └──────────────────────────────────────┘
 * </pre>
 *
 * <p>Admin interactions (left-click to pick up, right-click to delete) are sent back to
 * the server as {@link SpyActionPayload} packets. The server applies the change silently
 * to the live inventory — no sound or animation on the target's side.</p>
 *
 * <p>The screen auto-refreshes whenever a new {@link name.modid.horror.spy.SpyPayload} is
 * received from the server (every {@code SpyNetworking.SYNC_INTERVAL_TICKS} ticks).</p>
 */
public final class SpyInventoryScreen extends Screen {

    // -----------------------------------------------------------------------
    // Singleton instance — updated by the network handler
    // -----------------------------------------------------------------------

    /** The currently-open spy screen, or {@code null} if none is open. */
    private static SpyInventoryScreen current = null;

    // -----------------------------------------------------------------------
    // Instance fields
    // -----------------------------------------------------------------------

    private final String targetName;

    /** Live item stack snapshot: 36 main + 4 armor + 1 offhand = 41 slots. */
    private final List<ItemStack> slots;

    /** Currently "held" item (picked up by left-click, pending drop or placement). */
    private ItemStack heldStack = ItemStack.EMPTY;
    private int heldSlotIndex = -1;

    // Layout constants
    private static final int SLOT_SIZE  = 18;
    private static final int GRID_COLS  = 9;
    private static final int PADDING    = 8;

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    private SpyInventoryScreen(String targetName, List<ItemStack> slots) {
        super(Component.literal("§4[Admin Spy] §r" + targetName + "'s Inventory"));
        this.targetName = targetName;
        this.slots      = new ArrayList<>(slots);
    }

    // -----------------------------------------------------------------------
    // Public static API — called by OptimizationModClient network handler
    // -----------------------------------------------------------------------

    /**
     * Opens (or refreshes) the spy screen with a new inventory snapshot.
     * Must be called on the Minecraft render thread.
     *
     * @param targetName the display name of the target player
     * @param snapShot   41-element list of ItemStacks (36 main + 4 armor + 1 offhand)
     */
    public static void openOrRefresh(String targetName, List<ItemStack> snapShot) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (current == null || !(mc.screen instanceof SpyInventoryScreen)) {
            // New screen — open it
            current = new SpyInventoryScreen(targetName, snapShot);
            mc.setScreen(current);
        } else {
            // Already open — just refresh the slot data in-place
            current.slots.clear();
            current.slots.addAll(snapShot);
        }
    }

    /**
     * Forces the spy screen closed. Called when the server session ends or the admin
     * disconnects.
     */
    public static void close() {
        if (current != null) {
            current.onClose();
            current = null;
        }
    }

    // -----------------------------------------------------------------------
    // Screen lifecycle
    // -----------------------------------------------------------------------

    @Override
    public boolean isPauseScreen() {
        return false; // Don't pause single-player while spying
    }

    @Override
    public void onClose() {
        super.onClose();
        // Notify server the spy session is ending so it stops sending sync packets
        if (ClientPlayNetworking.canSend(SpyActionPayload.TYPE)) {
            ClientPlayNetworking.send(new SpyActionPayload(
                    SpyActionPayload.ACTION_CLOSE, -1, ItemStack.EMPTY, targetName));
        }
        current = null;
    }

    // -----------------------------------------------------------------------
    // Rendering
    // -----------------------------------------------------------------------

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Override to do nothing. We don't want the vanilla blur or heavy darken effect.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Light translucent background so we can still see the target clearly
        graphics.fill(0, 0, this.width, this.height, 0x55000000);

        int centerX = this.width / 2;
        int startX  = centerX - (GRID_COLS * SLOT_SIZE / 2);
        int startY  = 30;

        // ---- Title ----
        graphics.drawCenteredString(this.font,
                "§4§lSPYING §r§7on §f" + targetName, centerX, 10, 0xFFFFFF);

        // ---- Armor row (slots 36–39: head, chest, legs, feet) ----
        graphics.drawString(this.font, "§7Armor:", startX, startY, 0xAAAAAA);
        int armorY = startY + 12;
        for (int i = 0; i < 4; i++) {
            int slotIdx = 36 + i;
            renderSlot(graphics, slots.get(slotIdx), startX + i * SLOT_SIZE, armorY, slotIdx, mouseX, mouseY);
        }

        // ---- Main inventory (slots 9–35: rows 1–3) ----
        int invStartY = armorY + SLOT_SIZE + PADDING;
        graphics.drawString(this.font, "§7Inventory:", startX, invStartY - 10, 0xAAAAAA);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int slotIdx = 9 + row * GRID_COLS + col;
                renderSlot(graphics, slots.get(slotIdx), startX + col * SLOT_SIZE, invStartY + row * SLOT_SIZE, slotIdx, mouseX, mouseY);
            }
        }

        // ---- Hotbar (slots 0–8) ----
        int hotbarY = invStartY + 3 * SLOT_SIZE + PADDING;
        graphics.drawString(this.font, "§7Hotbar:", startX, hotbarY - 10, 0xAAAAAA);
        for (int col = 0; col < GRID_COLS; col++) {
            renderSlot(graphics, slots.get(col), startX + col * SLOT_SIZE, hotbarY, col, mouseX, mouseY);
        }

        // ---- Offhand (slot 40) ----
        int offhandY = hotbarY + SLOT_SIZE + PADDING;
        graphics.drawString(this.font, "§7Offhand:", startX, offhandY - 10, 0xAAAAAA);
        renderSlot(graphics, slots.get(40), startX, offhandY, 40, mouseX, mouseY);

        // ---- Close button hint ----
        graphics.drawString(this.font, "§8[ESC to close]", startX + GRID_COLS * SLOT_SIZE - 70, offhandY, 0x555555);

        // ---- Held stack follows cursor ----
        if (!heldStack.isEmpty()) {
            graphics.renderFakeItem(heldStack, mouseX - 8, mouseY - 8);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderSlot(GuiGraphics graphics, ItemStack stack, int x, int y, int slotIdx, int mouseX, int mouseY) {
        // Slot background
        boolean hovered = mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE;
        int bgColor = hovered ? 0x88444444 : 0x88222222;
        graphics.fill(x, y, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, bgColor);

        // Item rendering
        if (!stack.isEmpty()) {
            graphics.renderFakeItem(stack, x + 1, y + 1);
            graphics.renderItemDecorations(this.font, stack, x + 1, y + 1);
        }

        // Tooltip on hover
        if (hovered && !stack.isEmpty()) {
            graphics.renderTooltip(this.font, stack, mouseX, mouseY);
        }
    }

    // -----------------------------------------------------------------------
    // Mouse input
    // -----------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int slotIdx = getSlotAt((int) mouseX, (int) mouseY);
        if (slotIdx < 0) return super.mouseClicked(mouseX, mouseY, button);

        if (button == 0) { // Left click — pick up / place
            if (heldStack.isEmpty()) {
                // Pick up item from slot
                heldStack = slots.get(slotIdx).copy();
                heldSlotIndex = slotIdx;
                slots.set(slotIdx, ItemStack.EMPTY);
                sendAction(SpyActionPayload.ACTION_DELETE, slotIdx, ItemStack.EMPTY);
            } else {
                // Place held item into this slot
                sendAction(SpyActionPayload.ACTION_SET, slotIdx, heldStack);
                slots.set(slotIdx, heldStack.copy());
                heldStack = ItemStack.EMPTY;
                heldSlotIndex = -1;
            }
        } else if (button == 1) { // Right click — delete slot directly
            slots.set(slotIdx, ItemStack.EMPTY);
            sendAction(SpyActionPayload.ACTION_DELETE, slotIdx, ItemStack.EMPTY);
        }

        return true;
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private int getSlotAt(int mouseX, int mouseY) {
        int centerX = this.width / 2;
        int startX  = centerX - (GRID_COLS * SLOT_SIZE / 2);
        int startY  = 30;
        int armorY  = startY + 12;
        int invStartY = armorY + SLOT_SIZE + PADDING;
        int hotbarY   = invStartY + 3 * SLOT_SIZE + PADDING;
        int offhandY  = hotbarY + SLOT_SIZE + PADDING;

        // Armor
        for (int i = 0; i < 4; i++) {
            if (inSlot(mouseX, mouseY, startX + i * SLOT_SIZE, armorY)) return 36 + i;
        }
        // Main inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                if (inSlot(mouseX, mouseY, startX + col * SLOT_SIZE, invStartY + row * SLOT_SIZE))
                    return 9 + row * GRID_COLS + col;
            }
        }
        // Hotbar
        for (int col = 0; col < GRID_COLS; col++) {
            if (inSlot(mouseX, mouseY, startX + col * SLOT_SIZE, hotbarY)) return col;
        }
        // Offhand
        if (inSlot(mouseX, mouseY, startX, offhandY)) return 40;

        return -1;
    }

    private static boolean inSlot(int mouseX, int mouseY, int slotX, int slotY) {
        return mouseX >= slotX && mouseX < slotX + SLOT_SIZE
                && mouseY >= slotY && mouseY < slotY + SLOT_SIZE;
    }

    private void sendAction(int action, int slotIdx, ItemStack stack) {
        if (ClientPlayNetworking.canSend(SpyActionPayload.TYPE)) {
            ClientPlayNetworking.send(new SpyActionPayload(action, slotIdx, stack, targetName));
        }
    }
}
