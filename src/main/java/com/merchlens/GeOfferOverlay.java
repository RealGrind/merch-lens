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
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
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
	private static final Font COMPACT_HEADER_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 10);
	private static final Font COMPACT_BODY_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
	private static final int MAX_CARD_WIDTH = 98;
	private static final int MIN_CARD_WIDTH = 78;
	private static final int CARD_HEIGHT = 90;
	private static final int MAX_COMPACT_CARD_WIDTH = 68;
	private static final int MIN_COMPACT_CARD_WIDTH = 54;
	private static final int COMPACT_CARD_HEIGHT = 74;
	private static final int CARD_GAP = 4;
	private static final int MAX_ICON_SIZE = 30;
	private static final int MIN_ICON_SIZE = 24;
	private static final int COMPACT_ICON_SIZE = 22;
	private static final int CANVAS_PADDING = 8;
	private static final int MINIMAP_GAP = 8;
	private static final int FIXED_HUD_FALLBACK_WIDTH = 215;
	private static final int[] TOP_RIGHT_HUD_COMPONENTS = {
		ComponentID.FIXED_VIEWPORT_MINIMAP,
		ComponentID.RESIZABLE_VIEWPORT_MINIMAP,
		ComponentID.RESIZABLE_VIEWPORT_MINIMAP_ORB_HOLDER,
		ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_MINIMAP,
		ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_MINIMAP_ORB_HOLDER
	};
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
			int y = row * (layout.cardHeight + CARD_GAP);
			TrackedOffer offer = offers.get(i);
			drawOffer(graphics, offer, x, y, layout);
			addTooltipIfHovered(offer, x, y, layout);
		}
		return new Dimension(layout.width, layout.height);
	}

	private Layout layout(int offerCount)
	{
		int canvasWidth = Math.max(MIN_CARD_WIDTH, client.getCanvasWidth() - CANVAS_PADDING);
		Layout normal = layoutWithin(offerCount, canvasWidth, MIN_CARD_WIDTH, MAX_CARD_WIDTH, CARD_HEIGHT, false);
		Rectangle reservedHud = topRightHudBounds();
		int normalRight = (client.getCanvasWidth() + normal.width) / 2;
		if (reservedHud == null || normalRight + MINIMAP_GAP <= reservedHud.x)
		{
			setPosition(OverlayPosition.TOP_CENTER);
			return normal;
		}

		int safeWidth = Math.max(MIN_COMPACT_CARD_WIDTH, reservedHud.x - CANVAS_PADDING - MINIMAP_GAP);
		setPosition(OverlayPosition.TOP_LEFT);
		return layoutWithin(offerCount, safeWidth, MIN_COMPACT_CARD_WIDTH, MAX_COMPACT_CARD_WIDTH, COMPACT_CARD_HEIGHT, true);
	}

	private Layout layoutWithin(int offerCount, int availableWidth, int minCardWidth, int maxCardWidth, int cardHeight, boolean compact)
	{
		int columns = Math.min(offerCount, Math.max(1, (availableWidth + CARD_GAP) / (minCardWidth + CARD_GAP)));
		int cardWidth = Math.min(maxCardWidth, Math.max(minCardWidth, (availableWidth - (columns - 1) * CARD_GAP) / columns));
		int rows = (offerCount + columns - 1) / columns;
		int width = columns * cardWidth + Math.max(0, columns - 1) * CARD_GAP;
		int height = rows * cardHeight + Math.max(0, rows - 1) * CARD_GAP;
		return new Layout(columns, cardWidth, cardHeight, width, height, compact);
	}

	private Rectangle topRightHudBounds()
	{
		Rectangle reserved = null;
		for (int componentId : TOP_RIGHT_HUD_COMPONENTS)
		{
			Widget widget = client.getWidget(componentId);
			if (widget == null || widget.isHidden())
			{
				continue;
			}
			Rectangle bounds = widget.getBounds();
			if (bounds.width <= 0 || bounds.height <= 0 || bounds.y > CARD_HEIGHT)
			{
				continue;
			}
			reserved = reserved == null ? new Rectangle(bounds) : reserved.union(bounds);
		}
		if (reserved == null && !client.isResized())
		{
			int x = Math.max(MIN_COMPACT_CARD_WIDTH, client.getCanvasWidth() - FIXED_HUD_FALLBACK_WIDTH);
			return new Rectangle(x, 0, client.getCanvasWidth() - x, CARD_HEIGHT);
		}
		return reserved;
	}

	private void drawOffer(Graphics2D graphics, TrackedOffer offer, int x, int y, Layout layout)
	{
		int cardWidth = layout.cardWidth;
		int cardHeight = layout.cardHeight;
		Color accent = accentColor(offer.getState());
		graphics.setColor(new Color(18, 18, 18, 214));
		graphics.fillRoundRect(x, y, cardWidth, cardHeight, 6, 6);
		graphics.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 178));
		graphics.setStroke(new BasicStroke(1.2f));
		graphics.drawRoundRect(x, y, cardWidth - 1, cardHeight - 1, 6, 6);

		graphics.setFont(layout.compact ? COMPACT_HEADER_FONT : HEADER_FONT);
		graphics.setColor(Color.LIGHT_GRAY);
		int headerPadding = layout.compact ? 4 : 6;
		graphics.drawString((layout.compact ? "GE" : "GE ") + (offer.getSlot() + 1), x + headerPadding, y + 13);
		drawRightText(graphics, stateText(offer.getState()), x + cardWidth - headerPadding, y + 13, accent);

		AsyncBufferedImage image = itemManager.getImage(offer.getItemId());
		int iconSize = layout.compact ? COMPACT_ICON_SIZE : Math.min(MAX_ICON_SIZE, Math.max(MIN_ICON_SIZE, cardWidth / 3));
		int iconX = x + (cardWidth - iconSize) / 2;
		int iconY = y + (layout.compact ? 17 : 20);
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

		graphics.setColor(new Color(76, 76, 76, 120));
		graphics.setStroke(new BasicStroke(1f));
		int dividerY = layout.compact ? y + 47 : y + 61;
		graphics.drawLine(x + headerPadding, dividerY, x + cardWidth - headerPadding, dividerY);

		graphics.setFont(layout.compact ? COMPACT_BODY_FONT : BODY_FONT);
		drawCenteredText(graphics, layout.compact ? smallQuantityText(offer) : compactQuantityText(offer),
			x + cardWidth / 2, y + (layout.compact ? 60 : 74), Color.WHITE);
		drawCenteredText(graphics, trackedText(offer), x + cardWidth / 2, y + (layout.compact ? 71 : 86),
			offer.isTrackedStartKnown() ? Color.LIGHT_GRAY : CANCELLED_COLOR);
	}

	private void addTooltipIfHovered(TrackedOffer offer, int x, int y, Layout layout)
	{
		Point mouse = client.getMouseCanvasPosition();
		Rectangle bounds = getBounds();
		if (mouse == null || bounds == null)
		{
			return;
		}

		int localX = mouse.getX() - bounds.x;
		int localY = mouse.getY() - bounds.y;
		if (localX >= x && localX <= x + layout.cardWidth && localY >= y && localY <= y + layout.cardHeight)
		{
			tooltipManager.add(new Tooltip(itemName(offer.getItemId())
				+ "<br>GE " + (offer.getSlot() + 1) + " " + stateText(offer.getState())
				+ "<br>" + exactQuantityText(offer) + " - " + trackedText(offer)));
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

	private String compactQuantityText(TrackedOffer offer)
	{
		return compactQuantity(Math.max(0, offer.getFilledQuantity()))
			+ " / "
			+ compactQuantity(Math.max(0, offer.getTotalQuantity()));
	}

	private String smallQuantityText(TrackedOffer offer)
	{
		return smallQuantity(Math.max(0, offer.getFilledQuantity()))
			+ "/"
			+ smallQuantity(Math.max(0, offer.getTotalQuantity()));
	}

	private String compactQuantity(int quantity)
	{
		if (quantity >= 1_000_000_000)
		{
			return String.format(Locale.US, "%.1fb", quantity / 1_000_000_000.0);
		}
		if (quantity >= 1_000_000)
		{
			return String.format(Locale.US, "%.1fm", quantity / 1_000_000.0);
		}
		if (quantity >= 1_000)
		{
			return String.format(Locale.US, "%.1fk", quantity / 1_000.0);
		}
		return QUANTITY.format(quantity);
	}

	private String smallQuantity(int quantity)
	{
		if (quantity >= 1_000_000_000)
		{
			return abbreviatedQuantity(quantity, 1_000_000_000.0, "b");
		}
		if (quantity >= 1_000_000)
		{
			return abbreviatedQuantity(quantity, 1_000_000.0, "m");
		}
		if (quantity >= 1_000)
		{
			return abbreviatedQuantity(quantity, 1_000.0, "k");
		}
		return QUANTITY.format(quantity);
	}

	private String abbreviatedQuantity(int quantity, double unit, String suffix)
	{
		double value = quantity / unit;
		return value == Math.rint(value)
			? String.format(Locale.US, "%.0f%s", value, suffix)
			: String.format(Locale.US, "%.1f%s", value, suffix);
	}

	private String exactQuantityText(TrackedOffer offer)
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
		private final int cardHeight;
		private final int width;
		private final int height;
		private final boolean compact;

		private Layout(int columns, int cardWidth, int cardHeight, int width, int height, boolean compact)
		{
			this.columns = columns;
			this.cardWidth = cardWidth;
			this.cardHeight = cardHeight;
			this.width = width;
			this.height = height;
			this.compact = compact;
		}
	}
}
