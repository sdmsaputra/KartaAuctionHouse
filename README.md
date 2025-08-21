# KartaAuctionHouse

KartaAuctionHouse is a modern and feature-rich auction house plugin for PaperMC servers. It provides a robust, intuitive, and scalable platform for players to buy and sell items, fully integrated with Vault-based economies and PlaceholderAPI.

## ✨ Features

- **Advanced Auction Mechanics**:
  - **Standard Auctions**: Players can list items for a set starting price.
  - **Instant Buy (Buy It Now)**: Sellers can set a "buy now" price for immediate purchase. This option is disabled once a bid is placed.
  - **Reserve Price**: Sellers can set a minimum price they're willing to accept. If the auction ends below this price, the item is returned to the seller.
  - **Anti-Sniping**: If a bid is placed in the final seconds of an auction, the auction time is automatically extended, giving others a fair chance to bid again.
  - **Minimum Bid Increment**: Configure a minimum bid increase, either as a flat amount or a percentage, to prevent minuscule bid spam.

- **Enhanced GUI**:
  - **Intuitive Interface**: A clean and modern GUI for browsing, bidding, and managing auctions.
  - **Filtering & Sorting**: Easily sort auctions by price, time remaining, or newest listings.
  - **Category Tabs**: Filter items by category (Weapons, Armor, Blocks, Misc) for easier browsing.
  - **Search Functionality**: Search for items by name using `/ah search <keyword>` or the in-game search button.
  - **Pagination**: Smoothly navigate through multiple pages of auctions.
  - **Color-Coded Affordability**: Item prices are colored green if you can afford them and red if you can't, providing instant feedback.

- **Real-time Notifications**:
  - Stay informed with instant notifications for key events:
    - When you are outbid.
    - When an auction you bid on ends.
    - When an item you listed is sold or expires.
  - **Configurable Channels**: Choose how you receive notifications: chat message, action bar, title/subtitle, or sound effects.
  - **Personalized Settings**: Players can toggle their notifications on or off using `/ah notify <on/off>`.

- **Robust Economy & Logging**:
  - **Vault & KartaEmeraldCurrency Support**: Seamlessly integrates with any Vault-supported economy plugin.
  - **Transaction Tax**: A configurable tax/commission can be taken from each sale.
  - **Full Transaction Logging**: Every sale, expiration, and cancellation is logged to a MySQL database for administrative oversight.

- **PlaceholderAPI Support**:
  - Display auction data on scoreboards, holograms, and more.
  - Available placeholders: `%kartauctionhouse_highest_bid%`, `%kartauctionhouse_time_left%`, `%kartauctionhouse_seller%`.

## 🚀 Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/ah` | Opens the main auction house GUI. | `kartaauctionhouse.use` |
| `/ah sell <price> [buyNow] [duration]` | Lists the item in your hand for auction. | `kartaauctionhouse.sell` |
| `/ah search <keyword>` | Searches for items matching the keyword. | `kartaauctionhouse.search` |
| `/ah categories [category]` | Opens the auction house, optionally filtered by a category. | `kartaauctionhouse.categories` |
| `/ah notify <on/off>` | Toggles your personal auction notifications. | `kartaauctionhouse.notify` |
| `/ah history [player]` | Shows your or another player's auction history. | `kartaauctionhouse.history`, `kartaauctionhouse.history.others` |
| `/ah listings` | Opens a GUI of your active listings. | `kartaauctionhouse.use` |
| `/ah mailbox` | Opens your mailbox to claim items/money. | `kartaauctionhouse.use` |
| `/ah reload` | Reloads the plugin's configuration. | `kartaauctionhouse.reload` |

## ⚙️ Permissions

| Permission | Description | Default |
| --- | --- | --- |
| `kartaauctionhouse.use` | Allows opening and using the main GUI. | `true` |
| `kartaauctionhouse.sell` | Allows listing items for sale with `/ah sell`. | `true` |
| `kartaauctionhouse.cancel` | Allows cancelling your own auctions (if they have no bids). | `true` |
| `kartaauctionhouse.search` | Allows using the `/ah search` command. | `true` |
| `kartaauctionhouse.categories` | Allows using the `/ah categories` command. | `true` |
| `kartaauctionhouse.notify` | Allows using `/ah notify` to toggle notifications. | `true` |
| `kartaauctionhouse.history` | Allows viewing your own auction history. | `true` |
| `kartaauctionhouse.history.others` | Allows viewing another player's auction history. | `op` |
| `kartaauctionhouse.reload` | Allows reloading the plugin's configuration. | `op` |
| `kartaauctionhouse.admin` | Grants access to all administrative features. | `op` |

## 🔧 Configuration (`config.yml`)

A brief overview of key configuration options:

```yaml
auction:
  # The maximum number of auctions a player can have active at once.
  max-auctions-per-player: 5
  # The default duration for an auction (e.g., 48h, 1d, 30m).
  auction-duration: 48h
  # The minimum bid increment. Can be a percentage (e.g., "5%") or a flat value (e.g., "100.0").
  bid-increment: 5%
  # If a bid is placed in the last X seconds, extend the auction by this much.
  anti-sniping-extension: 30s
  # The percentage cut taken from each successful sale.
  tax-percentage: 5
  # How players receive notifications. Options: [chat, actionbar, title, sound]
  notification-methods: [chat, actionbar]
```
