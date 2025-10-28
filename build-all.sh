#!/bin/bash

# PlayerAuctions Multi-Version Build Script
# Builds all version variants of PlayerAuctions plugin

echo "🔨 Building PlayerAuctions - All Versions..."
echo "============================================="

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Clean previous builds
echo -e "${YELLOW}🧹 Cleaning previous builds...${NC}"
mvn clean

# Function to build and check
build_version() {
    local profile=$1
    local name=$2
    local version=$3

    echo -e "\n${GREEN}📦 Building $name (v$version)...${NC}"
    mvn package -P$profile

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ $name build SUCCESS!${NC}"

        # Check if jar exists
        jar_file=$(find target -name "PlayerAuctions-$version-*.jar" | head -1)
        if [ -n "$jar_file" ]; then
            size=$(ls -lh "$jar_file" | awk '{print $5}')
            echo -e "${GREEN}📁 Created: $jar_file ($size)${NC}"
        fi
    else
        echo -e "${RED}❌ $name build FAILED!${NC}"
        return 1
    fi
}

# Build all versions
build_version "modern" "Modern" "2.0.0"
build_version "legacy" "Legacy" "1.9.9"
build_version "1.20" "1.20" "2.0.0"
build_version "1.19" "1.19" "2.0.0"

echo -e "\n${GREEN}🎉 All builds completed!${NC}"
echo -e "${YELLOW}📂 Check target/ directory for all jar files:${NC}"
ls -lh target/PlayerAuctions-*.jar

echo -e "\n${GREEN}📋 Build Summary:${NC}"
echo "================================"
ls -lh target/PlayerAuctions-*.jar | while read line; do
    size=$(echo $line | awk '{print $5}')
    file=$(echo $line | awk '{print $9}')
    echo "• $file - $size"
done
echo ""
echo -e "${YELLOW}📝 Version Scheme:${NC}"
echo "• Modern versions (1.19-1.21): v2.0.0"
echo "• Legacy versions (1.16-1.18): v1.9.9"