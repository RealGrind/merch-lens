# Merch Lens

Merch Lens is a RuneLite external plugin for Grand Exchange market watching and flip research.

It shows OSRS Wiki market data, post-tax margins, buy limits, volume, freshness, 24-hour trend estimates, favorites, item lookup, local flip history, active GE offer tracking, and a profitable-only market screener inside RuneLite.

Merch Lens is a beta research tool. Verify trades manually in the Grand Exchange before buying or selling.

## Features

- Favorites, High Vol, and Screener tabs.
- Search any OSRS item by name or item ID.
- Post-tax margin and full 4-hour buy-limit profit estimates.
- Buy volume/hr, sell volume/hr, and buy/sell ratio.
- Price freshness and 24-hour trend labels.
- Daily chart popouts from OSRS Wiki 5-minute timeseries data.
- Display-only GE offer overlay with active slot progress and local timers.
- Local-only flip history and favorites stored in RuneLite profile config.

## Data Source

Merch Lens fetches read-only market data from the OSRS Wiki Prices API. It does not place, cancel, collect, or automate Grand Exchange offers.

## Development

Run tests:

```powershell
.\gradlew.bat test
```

Launch a local RuneLite development client:

```powershell
.\gradlew.bat runPlugin
```
