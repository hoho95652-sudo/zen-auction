# Donut Auction (Fabric, MC 1.21.4, client-side)

Build:  gradle build   (or ./gradlew build after `gradle wrapper`)  -> build/libs/donut-auction-1.0.0.jar
Install: put the jar + Fabric API in your mods folder (Fabric Loader 0.16+).

Use: press G -> search/click an item -> set timer (1-60s) and starting bid -> Start Auction.
Default: bidders /pay you the amount (payments add up); the mod reads the server payment notice. Toggle to chat "bid 500k" mode in the menu.
The winner is told to /pay you; the mod never moves money or items itself.
