package com.merchlens;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class MerchLensPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(MerchLensPlugin.class);
		RuneLite.main(args);
	}
}
