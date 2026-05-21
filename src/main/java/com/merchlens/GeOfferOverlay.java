package com.merchlens;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.util.AsyncBufferedImage;

class GeOfferOverlay extends Overlay
{
	private static final NumberFormat QUANTITY = NumberFormat.getIntegerInstance(Locale.US);
	private static final Font HEADER_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 11);
	private static final Font BODY_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
	private static final int CARD_WIDTH = 98;
	private static final int CARD_HEIGHT = 68;
	private static final int CARD_GAP = 4;
	private static final int ICON_SIZE = 24;
	private static final Color BUY_COLOR = new Color(94, 166, 255);
	private static final Color SELL_COLOR = new Color(255, 157, 67);
	private static final Color COMPLETE_COLOR = new Color(83, 227, 139);
	private static final Color CANCELLED_COLOR = new Color(214, 111, 111);

	private final OfferTracker offerTracker;
	private final ItemManager itemManager;

	GeOfferOverlay(OfferTracker offerTracker, ItemManager itemManager)
	{
		this.offerTracker = offerTracker;
		this.itemManager = itemManager;
		setPosition(OverlayPosition.TOP_CENTER);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(OverlayPriority.HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		List<TrackedOffer> offers = offerTracker.activeOffers();
		if (offers.isEmpty())
		{
			return null;
		}

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		int x = 0;
		for (TrackedOffer offer : offers)
		{
			drawOffer(graphics, offer, x, 0);
			x += CARD_WIDTH + CARD_GAP;
		}
		return new Dimension(offers.size() * CARD_WIDTH + Math.max(0, offers.size() - 1) * CARD_GAP, CARD_HEIGHT);
	}

	private void drawOffer(Graphics2D graphics, TrackedOffer offer, int x, int y)
	{
		Color accent = accentColor(offer.getState());
		graphics.setColor(new Color(18, 18, 18, 214));
		graphics.fillRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 6, 6);
		graphics.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 178));
		graphics.setStroke(new BasicStroke(1.2f));
		graphics.drawRoundRect(x, y, CARD_WIDTH - 1, CARD_HEIGHT - 1, 6, 6);

		graphics.setFont(HEADER_FONT);
		graphics.setColor(Color.LIGHT_GRAY);
		graphics.drawString("GE " + (offer.getSlot() + 1), x + 6, y + 13);
		drawRightText(graphics, stateText(offer.getState()), x + CARD_WIDTH - 6, y + 13, accent);

		AsyncBufferedImage image = itemManager.getImage(offer.getItemId());
		int iconX = x + (CARD_WIDTH - ICON_SIZE) / 2;
		int iconY = y + 18;
		graphics.setColor(new Color(6, 6, 6, 150));
		graphics.fillRoundRect(iconX - 2, iconY - 2, ICON_SIZE + 4, ICON_SIZE + 4, 4, 4);
		graphics.drawImage(image, iconX, iconY, ICON_SIZE, ICON_SIZE, null);

		graphics.setFont(BODY_FONT);
		drawCenteredText(graphics, quantityText(offer), x + CARD_WIDTH / 2, y + 53, Color.WHITE);
		drawCenteredText(graphics, trackedText(offer), x + CARD_WIDTH / 2, y + 64, offer.isTrackedStartKnown() ? Color.LIGHT_GRAY : CANCELLED_COLOR);
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
}
