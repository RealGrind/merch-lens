package com.merchlens;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.Point;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.AsyncBufferedImage;

class GeOfferOverlay extends Overlay
{
	private static final NumberFormat QUANTITY = NumberFormat.getIntegerInstance(Locale.US);
	private static final Font HEADER_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 11);
	private static final Font BODY_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
	private static final int MAX_CARD_WIDTH = 98;
	private static final int MIN_CARD_WIDTH = 78;
	private static final int CARD_HEIGHT = 72;
	private static final int CARD_GAP = 4;
	private static final int MAX_ICON_SIZE = 30;
	private static final int MIN_ICON_SIZE = 24;
	private static final int CANVAS_PADDING = 8;
	private static final Color BUY_COLOR = new Color(94, 166, 255);
	private static final Color SELL_COLOR = new Color(255, 157, 67);
	private static final Color COMPLETE_COLOR = new Color(83, 227, 139);
	private static final Color CANCELLED_COLOR = new Color(214, 111, 111);

	private final OfferTracker offerTracker;
	private final ItemManager itemManager;
	private final MerchLensConfig config;
	private final Client client;
	private final TooltipManager tooltipManager;

	GeOfferOverlay(OfferTracker offerTracker, ItemManager itemManager, MerchLensConfig config, Client client, TooltipManager tooltipManager)
	{
		this.offerTracker = offerTracker;
		this.itemManager = itemManager;
		this.config = config;
		this.client = client;
		this.tooltipManager = tooltipManager;
		setPosition(OverlayPosition.TOP_CENTER);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(OverlayPriority.HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showGeOfferOverlay())
		{
			return null;
		}

		List<TrackedOffer> offers = offerTracker.activeOffers();
		if (offers.isEmpty())
		{
			return null;
		}

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		Layout layout = layout(offers.size());
		for (int i = 0; i < offers.size(); i++)
		{
			int row = i / layout.columns;
			int column = i % layout.columns;
			int x = column * (layout.cardWidth + CARD_GAP);
			int y = row * (CARD_HEIGHT + CARD_GAP);
			TrackedOffer offer = offers.get(i);
			drawOffer(graphics, offer, x, y, layout.cardWidth);
			addTooltipIfHovered(offer, x, y, layout.cardWidth);
		}
		return new Dimension(layout.width, layout.height);
	}

	private Layout layout(int offerCount)
	{
		int canvasWidth = Math.max(MIN_CARD_WIDTH, client.getCanvasWidth() - CANVAS_PADDING);
		int columns = Math.min(offerCount, Math.max(1, (canvasWidth + CARD_GAP) / (MIN_CARD_WIDTH + CARD_GAP)));
		int cardWidth = Math.min(MAX_CARD_WIDTH, Math.max(MIN_CARD_WIDTH, (canvasWidth - (columns - 1) * CARD_GAP) / columns));
		int rows = (offerCount + columns - 1) / columns;
		int width = columns * cardWidth + Math.max(0, columns - 1) * CARD_GAP;
		int height = rows * CARD_HEIGHT + Math.max(0, rows - 1) * CARD_GAP;
		return new Layout(columns, cardWidth, width, height);
	}

	private void drawOffer(Graphics2D graphics, TrackedOffer offer, int x, int y, int cardWidth)
	{
		Color accent = accentColor(offer.getState());
		graphics.setColor(new Color(18, 18, 18, 214));
		graphics.fillRoundRect(x, y, cardWidth, CARD_HEIGHT, 6, 6);
		graphics.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 178));
		graphics.setStroke(new BasicStroke(1.2f));
		graphics.drawRoundRect(x, y, cardWidth - 1, CARD_HEIGHT - 1, 6, 6);

		graphics.setFont(HEADER_FONT);
		graphics.setColor(Color.LIGHT_GRAY);
		graphics.drawString("GE " + (offer.getSlot() + 1), x + 6, y + 13);
		drawRightText(graphics, stateText(offer.getState()), x + cardWidth - 6, y + 13, accent);

		AsyncBufferedImage image = itemManager.getImage(offer.getItemId());
		int iconSize = Math.min(MAX_ICON_SIZE, Math.max(MIN_ICON_SIZE, cardWidth / 3));
		int iconX = x + (cardWidth - iconSize) / 2;
		int iconY = y + 19;
		int plate = iconSize + 8;
		int plateX = x + (cardWidth - plate) / 2;
		int plateY = iconY - 4;
		graphics.setColor(new Color(0, 0, 0, 190));
		graphics.fillRoundRect(plateX, plateY, plate, plate, 5, 5);
		graphics.setColor(new Color(255, 255, 255, 48));
		graphics.fillRoundRect(plateX + 1, plateY + 1, plate - 2, plate - 2, 5, 5);
		graphics.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 120));
		graphics.drawRoundRect(plateX, plateY, plate - 1, plate - 1, 5, 5);
		graphics.drawImage(image, iconX, iconY, iconSize, iconSize, null);

		graphics.setFont(BODY_FONT);
		drawCenteredText(graphics, quantityText(offer), x + cardWidth / 2, y + 57, Color.WHITE);
		drawCenteredText(graphics, trackedText(offer), x + cardWidth / 2, y + 68, offer.isTrackedStartKnown() ? Color.LIGHT_GRAY : CANCELLED_COLOR);
	}

	private void addTooltipIfHovered(TrackedOffer offer, int x, int y, int cardWidth)
	{
		Point mouse = client.getMouseCanvasPosition();
		Rectangle bounds = getBounds();
		if (mouse == null || bounds == null)
		{
			return;
		}

		int localX = mouse.getX() - bounds.x;
		int localY = mouse.getY() - bounds.y;
		if (localX >= x && localX <= x + cardWidth && localY >= y && localY <= y + CARD_HEIGHT)
		{
			tooltipManager.add(new Tooltip(itemName(offer.getItemId())
				+ "<br>GE " + (offer.getSlot() + 1) + " " + stateText(offer.getState())
				+ "<br>" + quantityText(offer) + " - " + trackedText(offer)));
		}
	}

	private String itemName(int itemId)
	{
		ItemComposition item = itemManager.getItemComposition(itemId);
		if (item != null && item.getName() != null && !item.getName().trim().isEmpty())
		{
			return item.getName();
		}
		return "Item " + itemId;
	}

	private String quantityText(TrackedOffer offer)
	{
		return QUANTITY.format(Math.max(0, offer.getFilledQuantity()))
			+ " / "
			+ QUANTITY.format(Math.max(0, offer.getTotalQuantity()));
	}

	private String trackedText(TrackedOffer offer)
	{
		if (!offer.isTrackedStartKnown())
		{
			return "Time unknown";
		}
		long seconds = Math.max(0, Instant.now().getEpochSecond() - offer.getFirstSeenAt());
		long hours = seconds / 3600;
		long minutes = (seconds % 3600) / 60;
		if (hours > 0)
		{
			return hours + "h " + minutes + "m";
		}
		return minutes + "m";
	}

	private String stateText(String state)
	{
		if ("BUYING".equals(state))
		{
			return "Buy";
		}
		if ("SELLING".equals(state))
		{
			return "Sell";
		}
		if ("BOUGHT".equals(state))
		{
			return "Bought";
		}
		if ("SOLD".equals(state))
		{
			return "Sold";
		}
		if (state != null && state.startsWith("CANCELLED"))
		{
			return "Cancel";
		}
		return "";
	}

	private Color accentColor(String state)
	{
		if ("BUYING".equals(state))
		{
			return BUY_COLOR;
		}
		if ("SELLING".equals(state))
		{
			return SELL_COLOR;
		}
		if ("BOUGHT".equals(state) || "SOLD".equals(state))
		{
			return COMPLETE_COLOR;
		}
		if (state != null && state.startsWith("CANCELLED"))
		{
			return CANCELLED_COLOR;
		}
		return Color.GRAY;
	}

	private void drawCenteredText(Graphics2D graphics, String text, int centerX, int baselineY, Color color)
	{
		FontMetrics metrics = graphics.getFontMetrics();
		int width = metrics.stringWidth(text);
		graphics.setColor(color);
		graphics.drawString(text, centerX - width / 2, baselineY);
	}

	private void drawRightText(Graphics2D graphics, String text, int rightX, int baselineY, Color color)
	{
		FontMetrics metrics = graphics.getFontMetrics();
		graphics.setColor(color);
		graphics.drawString(text, rightX - metrics.stringWidth(text), baselineY);
	}

	private static class Layout
	{
		private final int columns;
		private final int cardWidth;
		private final int width;
		private final int height;

		private Layout(int columns, int cardWidth, int width, int height)
		{
			this.columns = columns;
			this.cardWidth = cardWidth;
			this.width = width;
			this.height = height;
		}
	}
}
