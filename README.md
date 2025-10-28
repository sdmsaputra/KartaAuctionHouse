# PlayerAuctions

PlayerAuctions is a modern and feature-rich auction house plugin for PaperMC servers. It provides a robust, intuitive, and scalable platform for players to buy and sell items, fully integrated with Vault-based economies and PlaceholderAPI.

## 🆕 Latest Version (v2.0.0)

- **Modern Build**: Compatible with Minecraft 1.19-1.21
- **Legacy Build**: Compatible with Minecraft 1.16-1.18 (v1.9.9)
- **Size Optimized**: Version-specific builds for better performance

## ✨ Features

- **Modern GUI Interface**:
  - **Clean Design**: Simple and intuitive auction house interface with color-coded items
  - **Configurable Borders**: Toggle decorative borders with customizable materials (default: black stained glass)
  - **Smart Layout**: Organized control panel with clear navigation buttons
  - **Real-time Updates**: Instant GUI refresh when auctions are bought, sold, or expired
  - **Player Info Display**: Shows player balance and current page in the center of the GUI

- **Auction Management**:
  - **Instant Buy**: Players can purchase items immediately at set prices
  - **Listing Management**: View and manage your active auction listings
  - **Search System**: Search for specific items using keywords
  - **Sort Options**: Sort auctions by newest, oldest, or price
  - **Purchase History**: Track your auction transaction history

- **Command System**:
  - **Tab Completion**: Full command suggestions with descriptions
  - **Multiple Aliases**: Use `/ah`, `/auction`, or `/auctionhouse`
  - **Help System**: Comprehensive help command with color-coded instructions
  - **Permission-based**: Granular permission system for different features

- **Visual Enhancements**:
  - **Color Coding**: Green/red indicators for affordable items
  - **Time Indicators**: Color-coded time remaining (green for long, red for urgent)
  - **Clean Borders**: Optional decorative borders for a polished look
  - **Responsive Design**: Items display properly with proper formatting

- **Integration Support**:
  - **Vault Economy**: Full integration with Vault-based economies
  - **PlaceholderAPI**: Support for displaying auction data externally
  - **SQLite Database**: Lightweight and reliable data storage
  - **Multiple Economy Providers**: Support for various economy plugins

## 🚀 Commands

| Command | Description | Permission |
|---|---|---|
| `/ah` | Opens the main auction house GUI | `playerauctions.use` |
| `/ah help` | Shows all available commands with descriptions | `playerauctions.use` |
| `/ah sell <price> [buy_now] [duration]` | Lists the item in your hand for auction | `playerauctions.sell` |
| `/ah search <keyword>` | Searches for items matching the keyword | `playerauctions.search` |
| `/ah notify <on/off>` | Toggles your personal auction notifications | `playerauctions.notify` |
| `/ah history [player]` | Shows your or another player's auction history | `playerauctions.history`, `playerauctions.history.others` |
| `/ah listings` | Opens a GUI of your active listings | `playerauctions.use` |
| `/ah myauctions` | Alternative command for your listings | `playerauctions.use` |
| `/ah reload` | Reloads the plugin's configuration | `playerauctions.reload` |

**Command Aliases**: All commands work with `/ah`, `/auction`, and `/auctionhouse`

## ⚙️ Permissions

| Permission | Description | Default |
|---|---|---|
| `playerauctions.use` | Allows opening and using the main GUI | `true` |
| `playerauctions.sell` | Allows listing items for sale with `/ah sell` | `true` |
| `playerauctions.cancel` | Allows cancelling your own auctions (if they have no bids) | `true` |
| `playerauctions.search` | Allows using the `/ah search` command | `true` |
| `playerauctions.categories` | Allows using the `/ah categories` command | `true` |
| `playerauctions.notify` | Allows using `/ah notify` to toggle notifications | `true` |
| `playerauctions.history` | Allows viewing your own auction history | `true` |
| `playerauctions.history.others` | Allows viewing another player's auction history | `op` |
| `playerauctions.reload` | Allows reloading the plugin's configuration | `op` |
| `playerauctions.admin` | Grants access to all administrative features | `op` |

## 🔧 Configuration (`config.yml`)

Key configuration options:

```yaml
# PlayerAuctions Configuration

auction:
  max-auctions-per-player: 5
  auction-duration: 48h
  defaults:
    duration: 48h
  min-price: 1.0
  tax-percentage: 5
  notification-methods: [chat, actionbar, title, sound]

economy:
  preferred: VAULT
  fallback: VAULT
  sink:
    mode: NONE
    target: "server_treasury"

gui:
  title-main: "&6PlayerAuctions"
  title-my-listings: "&6My Listings"
  system: "INVENTORY_FRAMEWORK"
  size: 54
  items-per-page: 45
  border:
    enabled: true
    material: BLACK_STAINED_GLASS_PANE
    name: " "
    lore: []

mailbox:
  enabled: true
  retention-days: 30
```

### GUI Border Configuration

The border system is fully customizable:

- `enabled`: Enable or disable the decorative border (default: true)
- `material`: Any valid Minecraft material (default: BLACK_STAINED_GLASS_PANE)
- `name`: Custom display name for border items
- `lore`: Custom lore lines for border items

## 🎨 GUI Features

### Main Auction House Interface
- **54-slot inventory** with configurable borders
- **Smart item display** with color-coded affordability
- **Control panel** with intuitive navigation buttons
- **Player info center** showing balance and page number

### Control Buttons
- **Previous Page**: Navigate to previous auction page
- **Sort Button**: Cycle through sort options (Newest, Oldest, Price)
- **Player Info**: Displays player balance and current page (center)
- **Next Page**: Navigate to next auction page

### Clean Layout Design
- **Minimal Interface**: Only essential controls for a clean look
- **Smart Positioning**: Controls are strategically placed for easy access
- **Visual Clarity**: Fewer buttons mean less visual clutter
- **Focus on Items**: More space available for auction items

### Item Display
- **Color-coded prices**: Green (affordable) or Red (expensive)
- **Time indicators**: Color-coded by urgency
- **Seller information**: Clear seller identification
- **Quantity display**: For stacked items
- **Purchase feedback**: Clear action buttons

## 📊 Size Optimization

PlayerAuctions uses version-specific builds to optimize plugin size:

### Size Breakdown
- **Modern Build (v2.0.0)**: ~16MB
- **Legacy Build (v1.9.9)**: ~15MB
- **Size Reduction**: Up to 1MB smaller than universal build

### Why Version-Specific Builds?
1. **Reduced Dependencies**: Only includes Minecraft version support you need
2. **Smaller Download Size**: Faster plugin downloads
3. **Faster Loading**: Less unused code to process
4. **Better Performance**: Optimized for specific Minecraft versions

### Components Included
- **PlayerAuctions Core**: Plugin logic and features (~800KB)
- **InventoryFramework**: GUI system (~10-12MB, version-optimized)
- **SQLite Database**: Data storage (~2MB)
- **Support Libraries**: Logging, YAML, etc. (~1MB)

## 🔌 Integration

### PlaceholderAPI Support
Available placeholders for displaying auction data:
- `%playerauctions_total_auctions%`
- `%playerauctions_player_listings%`
- `%playerauctions_player_balance%`

### Economy Support
- **Vault**: Standard economy integration
- **KartaEmeraldCurrency**: Custom currency support
- **Multiple providers**: Automatic fallback system

## 📁 Installation

### Version-Specific Downloads
PlayerAuctions provides optimized builds for different Minecraft versions:

- **Modern Versions (1.19-1.21)**: `PlayerAuctions-2.0.0-Modern.jar` (~16MB)
- **Legacy Versions (1.16-1.18)**: `PlayerAuctions-1.9.9-Legacy.jar` (~15MB)
- **Version-Specific Builds**: `PlayerAuctions-2.0.0-1.20.jar`, `PlayerAuctions-2.0.0-1.19.jar`

### Installation Steps
1. Choose the correct version for your Minecraft server
2. Download the appropriate `PlayerAuctions-*.jar` file
3. Place in your server's `plugins` folder
4. Restart or reload your server
5. Configure settings in `plugins/PlayerAuctions/config.yml`
6. Set up permissions as needed

### Build Commands
For developers who want to build from source:

#### Option 1: Build All Versions (Recommended)
```bash
# Linux/MacOS - Use provided script
./build-all.sh

# Windows - Use provided script
build-all.bat

# Or manually build all versions
mvn clean package                    # Modern (1.19-1.21)
mvn clean package -Plegacy           # Legacy (1.16-1.18)
mvn clean package -P1.20             # 1.20.x
mvn clean package -P1.19             # 1.19.x
```

#### Option 2: Build Specific Version
```bash
# Modern versions (1.19-1.21) - Default
mvn clean package

# Legacy versions (1.16-1.18) - 1MB smaller!
mvn clean package -Plegacy

# Specific versions
mvn clean package -P1.20
mvn clean package -P1.19
```

#### Option 3: Development Build
```bash
# Quick build without cleaning
mvn package

# Build with debug info
mvn package -DskipTests

# Clean build all profiles
mvn clean package -Pmodern,legacy,1.20,1.19
```

### Generated Files
After building, you'll find these files in `target/`:
- `PlayerAuctions-2.0.0-Modern.jar` (~16MB) - Minecraft 1.19-1.21
- `PlayerAuctions-1.9.9-Legacy.jar` (~15MB) - Minecraft 1.16-1.18
- `PlayerAuctions-2.0.0-1.20.jar` (~16MB) - Minecraft 1.20.x
- `PlayerAuctions-2.0.0-1.19.jar` (~16MB) - Minecraft 1.19.x

### Version Naming Scheme
- **Modern Builds (v2.0.0)**: Latest features for modern Minecraft versions
- **Legacy Builds (v1.9.9)**: Compatible build for older Minecraft versions
- **Semantic Versioning**: Uses consistent version numbers instead of Minecraft versions
- **No Breaking Changes**: All versions have same features, just optimized for different Minecraft versions

## 🐛 Troubleshooting

**Common Issues:**
- **Economy not found**: Ensure Vault is installed and an economy plugin is active
- **Permissions not working**: Check your permission plugin configuration
- **GUI not opening**: Verify the player has `playerauctions.use` permission
- **Border not showing**: Check `gui.border.enabled` in config.yml

## 📄 License

This plugin is developed by MinekartaStudio. Please ensure you have the proper license to use this software on your server.

## 🤝 Support

For support, bug reports, or feature requests:
- Join our Discord server
- Create an issue on our GitHub repository
- Contact the development team