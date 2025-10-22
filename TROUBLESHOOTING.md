# KartaAuctionHouse Troubleshooting Guide

## GUI Not Opening? Follow These Steps:

### 1. **Check Plugin Status**
```
/plugin list
```
Make sure KartaAuctionHouse is loaded and enabled.

### 2. **Enable Debug Mode** (Optional)
Edit `config.yml` and add:
```yaml
debug:
  enabled: true
```
Then restart the plugin. You'll get `/kahdebug` command for testing.

### 3. **Test with Debug Commands**
If debug is enabled:
```
/kahdebug test          # Test basic GUI
/kahdebug testitem     # Test with sample items
/kahdebug services      # Check all services
/kahdebug economy       # Test economy integration
```

### 4. **Check Requirements**
- Vault or another economy plugin must be installed
- Database (MySQL) should be configured correctly
- Player needs `kartaauctionhouse.use` permission

### 5. **Common Issues & Solutions**

#### **GUI opens but no items appear:**
- Check if there are any active auctions in the database
- Verify database connection
- Check console for errors

#### **Permission errors:**
```
# Basic usage
/kah open
/kah sell <price>
/kah search <keyword>

# Admin permissions
kartaauctionhouse.admin
kartaauctionhouse.debug
```

#### **Economy errors:**
- Install Vault plugin
- Install an economy plugin (EssentialsX, CMI, etc.)
- Restart server after installing economy plugins

#### **Database errors:**
```yaml
# config.yml
database:
  type: "MYSQL"  # or "YAML" for file-based
  host: "localhost"
  port: 3306
  database: "auctionhouse"
  username: "your_username"
  password: "your_password"
```

### 6. **Console Commands for Debugging**

Check if all services are loaded:
```
/kahdebug services
```

Test economy integration:
```
/kahdebug economy
```

### 7. **Common Error Messages**

**"Failed to load auctions"**
- Database connection issue
- No auctions in database
- Permission issue

**"You don't have permission"**
- Add `kartaauctionhouse.use` permission
- Check your permissions plugin

**"No economy service found"**
- Install Vault
- Install economy plugin
- Restart server

### 8. **Manual GUI Test**
If `/ah` doesn't work, try this command:
```
/kahdebug test
```

### 9. **File Structure Check**
Make sure these files exist:
```
plugins/KartaAuctionHouse/
├── config.yml
├── messages.yml
└── data/
    ├── auctions.yml (if using YAML)
    ├── mailbox.yml
    └── transactions.yml
```

### 10. **Performance Optimization**
If GUI is slow:
- Reduce `items-per-page` in config
- Use MySQL instead of YAML for better performance
- Check server TPS

### 11. **Support**
If issues persist:
1. Check console logs for errors
2. Enable debug mode and run `/kahdebug services`
3. Report issue with:
   - Server version
   - Plugin version
   - Console errors
   - Steps to reproduce

## Quick Test Commands
```bash
# Basic functionality
/ah                              # Should open GUI
/ah sell 100                      # Should sell item in hand for $100
/ah search diamond                # Should search for diamonds
/ah mailbox                       # Should open mailbox (coming soon)

# Debug commands (if enabled)
/kahdebug test                    # Test basic GUI
/kahdebug services               # Check all services
```