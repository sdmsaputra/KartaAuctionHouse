# KartaAuctionHouse Commands Guide

## 🎮 Available Commands

### Main Commands

#### `/ah` (Primary Command)
**Aliases:** `auction`, `auctionhouse`
**Permission:** `kartaauctionhouse.use` (default: true)

**Usage:**
```bash
/ah                              # Open main auction house GUI
/ah sell <price> [duration]      # Sell item in hand
/ah search <keyword>             # Search auctions
/ah mailbox                      # Open mailbox (coming soon)
/ah listings / myauctions        # View your listings (coming soon)
/ah reload                       # Reload configuration (admin only)
/ah notify <on/off>             # Toggle notifications
/ah history [player]             # View auction history
```

**Examples:**
```bash
/ah                              # Opens auction house
/ah sell 1000                    # Sell item in hand for $1000
/ah sell 500 24h                  # Sell for $500 with 24h duration
/ah search diamond               # Search for diamond items
/ah reload                       # Reload plugin config (admin)
/ah notify on                    # Enable notifications
```

### Debug Commands

#### `/kahdebug` (Debug Mode Only)
**Permission:** `kartaauctionhouse.debug` (default: op)
**Requires:** `debug.enabled: true` in config.yml

**Usage:**
```bash
/kahdebug                         # Show help
/kahdebug test                     # Test basic GUI
/kahdebug testitem                # Test GUI with sample items
/kahdebug services                 # Test all services
/kahdebug config                   # Show configuration status
/kahdebug economy                  # Test economy integration
```

## 🔧 Permission System

### Basic Permissions
```yaml
kartaauctionhouse.use:           true    # Use basic AH features
kartaauctionhouse.sell:           true    # Sell items
kartaauctionhouse.cancel:         true    # Cancel auctions
kartaaauctionhouse.search:         true    # Search auctions
kartaaauctionhouse.notify:         true    # Toggle notifications
kartaaauctionhouse.history:        true    # View history
```

### Admin Permissions
```yaml
kartaauctionhouse.reload:         op      # Reload config
kartaaauctionhouse.debug:          op      # Debug commands
kartaaauctionhouse.admin:          op      # All admin features
```

## 🚀 Quick Start Guide

### For Players:
1. **Open Auction House:**
   ```bash
   /ah
   ```

2. **Sell an Item:**
   - Hold item in main hand
   - Type: `/ah sell <price>`
   - Example: `/ah sell 1000`

3. **Search Items:**
   ```bash
   /ah search diamond
   ```

### For Server Admins:

#### Step 1: Enable Debug Mode (Optional)
Edit `config.yml`:
```yaml
debug:
  enabled: true
```
Restart plugin.

#### Step 2: Test Commands
```bash
/kahdebug services    # Check all services
/kahdebug test        # Test GUI
/kahdebug economy     # Test economy
```

#### Step 3: Verify Functionality
```bash
/ah                    # Should open GUI
/ah sell 1000          # Should sell item
/kahdebug services     # Should show all services as OK
```

## 🔍 Troubleshooting Commands

### Check Plugin Status
```bash
/plugin list            # Check if KartaAuctionHouse is loaded
/plugins                # Same as above
```

### Debug Commands (Debug Mode Only)
```bash
/kahdebug services       # Check all services
/kahdebug config         # Check configuration
/kahdebug economy        # Test economy system
/kahdebug test           # Test GUI opening
```

### Permission Check
```bash
/perm user <player> show perms    # LuckPerms
/pex user <player>               # PermissionsEx
```

## 📋 Command Responses

### Success Messages
- `✓ GUI created successfully`
- `✓ Auction listed successfully`
- `✓ Configuration reloaded`

### Error Messages
- `✗ You don't have permission`
- `✗ No economy service found`
- `✗ Failed to load auctions`
- `✗ Invalid price`

## 🎯 Command Flow Chart

```
Player executes /ah
    ↓
Permission check (kartaauctionhouse.use)
    ↓
Open Main GUI
    ↓
Player clicks item
    ↓
Process purchase
    ↓
Update GUI & notify
```

## 🔧 Configuration Files

### config.yml
```yaml
# Enable debug mode
debug:
  enabled: false

# Database settings
database:
  type: "YAML"  # or "MYSQL"

# Auction settings
auction:
  max-auctions-per-player: 5
  min-price: 1.0
  max-price: 1000000.0
```

### plugin.yml
```yaml
commands:
  ah:
    description: Main auction house command
    aliases: [auction, auctionhouse]
  kahdebug:
    description: Debug commands
    permission: kartaauctionhouse.debug

permissions:
  kartaauctionhouse.use:
    default: true
  kartaauctionhouse.debug:
    default: op
```

## 🚨 Common Issues & Solutions

### "Unknown command" Error
- **Cause:** Plugin not loaded or command not registered
- **Solution:** Check `/plugins` and restart plugin
- **Debug:** `/kahdebug services`

### "You don't have permission" Error
- **Cause:** Missing permission
- **Solution:** Add permission to user/group
- **Debug:** Check with permissions plugin

### GUI Not Opening
- **Cause:** Services not loaded properly
- **Solution:** `/kahdebug test` and check console
- **Common:** Vault not installed, no economy plugin

### Economy Issues
- **Cause:** No economy service found
- **Solution:** Install Vault + economy plugin
- **Debug:** `/kahdebug economy`

## 📞 Support

If commands still don't work:

1. **Check console for errors**
2. **Enable debug mode** and run `/kahdebug services`
3. **Verify dependencies** (Vault, economy plugin)
4. **Check permissions** using your permissions plugin
5. **Report with:**
   - Server version
   - Plugin version
   - Console errors
   - Command used