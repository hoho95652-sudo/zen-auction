package com.example.donutauction;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AuctionScreen extends Screen {
    private static final int COLS = 13, ROWS = 7, CELL = 20;

    private final List<ItemStack> all = new ArrayList<>();
    private List<ItemStack> filtered = new ArrayList<>();
    private TextFieldWidget search, bidField;
    private TimerSlider slider;
    private ItemStack selected = ItemStack.EMPTY;
    private int scrollRow = 0;
    private int gridX, gridY;

    public AuctionScreen() {
        super(Text.literal("Donut Auction"));
        for (Item i : Registries.ITEM) {
            if (i != Items.AIR) all.add(new ItemStack(i));
        }
        filtered = new ArrayList<>(all);
    }

    @Override
    protected void init() {
        int w = COLS * CELL;
        gridX = (width - w) / 2;
        gridY = 50;

        search = new TextFieldWidget(textRenderer, gridX, 24, w, 18, Text.literal("Search"));
        search.setPlaceholder(Text.literal("Search items..."));
        search.setChangedListener(s -> { applyFilter(s); scrollRow = 0; });
        addDrawableChild(search);

        int by = gridY + ROWS * CELL + 30;
        slider = new TimerSlider(gridX, by, 160, 20, 29 / 59.0);
        addDrawableChild(slider);

        bidField = new TextFieldWidget(textRenderer, gridX + 170, by, 90, 20, Text.literal("Start bid"));
        bidField.setPlaceholder(Text.literal("Start bid (5k)"));
        addDrawableChild(bidField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Start Auction"), b -> startAuction())
                .dimensions(gridX + 270, by, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel Live"), b -> AuctionManager.cancel(MinecraftClient.getInstance()))
                .dimensions(gridX + 380, by, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(modeText(), b -> {
            AuctionManager.payMode = !AuctionManager.payMode;
            b.setMessage(modeText());
        }).dimensions(gridX + 340, by + 24, 120, 20).build());
        setInitialFocus(search);
    }

    private Text modeText() {
        return Text.literal("Bids: " + (AuctionManager.payMode ? "/pay me" : "chat 'bid'"));
    }

    private void applyFilter(String q) {
        String needle = q.toLowerCase(Locale.ROOT).trim();
        filtered = new ArrayList<>();
        for (ItemStack s : all) {
            if (needle.isEmpty()
                    || s.getName().getString().toLowerCase(Locale.ROOT).contains(needle)
                    || Registries.ITEM.getId(s.getItem()).getPath().contains(needle)) filtered.add(s);
        }
    }

    private void startAuction() {
        if (selected.isEmpty()) return;
        long bid = bidField.getText().isBlank() ? 1 : AuctionManager.parseAmount(bidField.getText());
        if (bid < 1) return;
        AuctionManager.start(MinecraftClient.getInstance(), selected, slider.seconds(), bid);
        close();
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 8, 0xFFFFFF);

        ItemStack hover = ItemStack.EMPTY;
        int start = scrollRow * COLS;
        for (int i = 0; i < COLS * ROWS; i++) {
            int idx = start + i;
            if (idx >= filtered.size()) break;
            int x = gridX + (i % COLS) * CELL, y = gridY + (i / COLS) * CELL;
            ItemStack s = filtered.get(idx);
            boolean isSel = ItemStack.areItemsEqual(s, selected);
            ctx.fill(x, y, x + CELL - 1, y + CELL - 1, isSel ? 0xAA55FF55 : 0x66000000);
            ctx.drawItem(s, x + 2, y + 2);
            if (mx >= x && mx < x + CELL && my >= y && my < y + CELL) hover = s;
        }

        int infoY = gridY + ROWS * CELL + 6;
        String sel = selected.isEmpty() ? "None" : selected.getName().getString();
        ctx.drawTextWithShadow(textRenderer, "Selected: " + sel + "   (" + filtered.size() + " items, scroll for more)", gridX, infoY, 0xFFFFFF);
        ctx.drawTextWithShadow(textRenderer, (AuctionManager.payMode ? "Timer: 1-60s | Bidders /pay you; their payments are the bids" : "Timer: 1-60s | Bidders type \"bid <amount>\" in chat"), gridX, infoY + 12, 0xAAAAAA);

        if (AuctionManager.active) {
            String top = AuctionManager.highestBidder == null ? "no bids" : AuctionManager.highestBidder + " $" + AuctionManager.fmt(AuctionManager.highestBid);
            ctx.drawTextWithShadow(textRenderer, "LIVE: " + AuctionManager.secondsLeft() + "s - " + top, gridX, gridY + ROWS * CELL + 58, 0xFFD700);
        }
        if (!hover.isEmpty()) ctx.drawItemTooltip(textRenderer, hover, mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        int cx = (int) ((mx - gridX) / CELL), cy = (int) ((my - gridY) / CELL);
        if (mx >= gridX && my >= gridY && cx < COLS && cy < ROWS) {
            int idx = scrollRow * COLS + cy * COLS + cx;
            if (idx < filtered.size()) { selected = filtered.get(idx).copy(); return true; }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        int maxRow = Math.max(0, (filtered.size() + COLS - 1) / COLS - ROWS);
        scrollRow = Math.max(0, Math.min(maxRow, scrollRow - (int) Math.signum(vAmt)));
        return true;
    }

    private static class TimerSlider extends SliderWidget {
        TimerSlider(int x, int y, int w, int h, double v) { super(x, y, w, h, Text.empty(), v); updateMessage(); }
        int seconds() { return 1 + (int) Math.round(value * 59); }
        @Override protected void updateMessage() { setMessage(Text.literal("Timer: " + seconds() + "s")); }
        @Override protected void applyValue() {}
    }
}
