package com.merchlens;

import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import net.runelite.api.Client;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.OverlayPosition;
import org.junit.Assert;
import org.junit.Test;

public class GeOfferOverlayTest
{
	@Test
	public void movesBesideBankAndReturnsToCenterWhenBankCloses() throws Exception
	{
		Rectangle bankBounds = new Rectangle(450, 20, 490, 800);
		GeOfferOverlay besideBank = overlay(client(1645, 1005, bankBounds));
		Object bankLayout = layout(besideBank, 2);

		Assert.assertNotNull(bankLayout);
		Assert.assertEquals(OverlayPosition.TOP_LEFT, besideBank.getPosition());
		Assert.assertTrue(layoutWidth(bankLayout) + 16 <= bankBounds.x);

		GeOfferOverlay withoutBank = overlay(client(1645, 1005, null));
		Assert.assertNotNull(layout(withoutBank, 2));
		Assert.assertEquals(OverlayPosition.TOP_CENTER, withoutBank.getPosition());
	}

	@Test
	public void hidesWhenBankLeavesNoRoomForCards() throws Exception
	{
		GeOfferOverlay overlay = overlay(client(765, 503, new Rectangle(45, 20, 490, 470)));

		Assert.assertNull(layout(overlay, 2));
	}

	@Test
	public void compactCardsFitBesideBankInSmallerClient() throws Exception
	{
		Rectangle bankBounds = new Rectangle(138, 20, 490, 470);
		GeOfferOverlay overlay = overlay(client(765, 503, bankBounds));
		Object bankLayout = layout(overlay, 8);

		Assert.assertNotNull(bankLayout);
		Assert.assertEquals(OverlayPosition.TOP_LEFT, overlay.getPosition());
		Assert.assertTrue(layoutWidth(bankLayout) + 16 <= bankBounds.x);
		Assert.assertTrue(layoutHeight(bankLayout) <= 503 - 8);
		Assert.assertTrue(isCompact(bankLayout));
	}

	private GeOfferOverlay overlay(Client client)
	{
		return new GeOfferOverlay(null, null, null, client, null);
	}

	private Object layout(GeOfferOverlay overlay, int offerCount) throws Exception
	{
		Method method = GeOfferOverlay.class.getDeclaredMethod("layout", int.class);
		method.setAccessible(true);
		return method.invoke(overlay, offerCount);
	}

	private int layoutWidth(Object layout) throws Exception
	{
		Field field = layout.getClass().getDeclaredField("width");
		field.setAccessible(true);
		return field.getInt(layout);
	}

	private int layoutHeight(Object layout) throws Exception
	{
		Field field = layout.getClass().getDeclaredField("height");
		field.setAccessible(true);
		return field.getInt(layout);
	}

	private boolean isCompact(Object layout) throws Exception
	{
		Field field = layout.getClass().getDeclaredField("compact");
		field.setAccessible(true);
		return field.getBoolean(layout);
	}

	private Client client(int canvasWidth, int canvasHeight, Rectangle bankBounds)
	{
		Widget bank = bankBounds == null ? null : (Widget) Proxy.newProxyInstance(
			Widget.class.getClassLoader(), new Class<?>[] {Widget.class}, (proxy, method, args) -> {
				if ("isHidden".equals(method.getName()))
				{
					return false;
				}
				if ("getBounds".equals(method.getName()))
				{
					return bankBounds;
				}
				return null;
			});
		return (Client) Proxy.newProxyInstance(Client.class.getClassLoader(), new Class<?>[] {Client.class}, (proxy, method, args) -> {
			if ("getCanvasWidth".equals(method.getName()))
			{
				return canvasWidth;
			}
			if ("getCanvasHeight".equals(method.getName()))
			{
				return canvasHeight;
			}
			if ("isResized".equals(method.getName()))
			{
				return true;
			}
			if ("getWidget".equals(method.getName()) && args != null && args.length == 1
				&& args[0] instanceof Integer && (Integer) args[0] == ComponentID.BANK_CONTAINER)
			{
				return bank;
			}
			return null;
		});
	}
}
