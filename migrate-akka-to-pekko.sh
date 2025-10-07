#!/bin/bash

###############################################################################
# Akka to Pekko Migration Script
# 
# This script automates the import statement and configuration migration
# from Akka to Apache Pekko.
#
# Usage:
#   ./migrate-akka-to-pekko.sh [--dry-run]
#
# Options:
#   --dry-run    Show what would be changed without making changes
#
# WARNING: This script modifies files in place. Make sure you have:
#   1. Committed all changes to git
#   2. Created a backup
#   3. Reviewed the changes it will make
#
###############################################################################

set -e  # Exit on error

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

DRY_RUN=false

# Parse arguments
for arg in "$@"; do
    case $arg in
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        *)
            echo -e "${RED}Unknown option: $arg${NC}"
            echo "Usage: $0 [--dry-run]"
            exit 1
            ;;
    esac
done

# Function to print colored output
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if we're in a git repository
if [ ! -d ".git" ]; then
    print_error "This script must be run from the root of a git repository"
    exit 1
fi

# Check for uncommitted changes
if [ "$DRY_RUN" = false ]; then
    if ! git diff-index --quiet HEAD --; then
        print_warning "You have uncommitted changes!"
        read -p "Do you want to continue? (yes/no): " confirm
        if [ "$confirm" != "yes" ]; then
            print_info "Migration aborted by user"
            exit 0
        fi
    fi
fi

print_info "=========================================="
print_info "  Akka to Pekko Migration Script"
print_info "=========================================="
echo ""

if [ "$DRY_RUN" = true ]; then
    print_warning "Running in DRY-RUN mode - no changes will be made"
    echo ""
fi

# Step 1: Create backup
if [ "$DRY_RUN" = false ]; then
    BACKUP_NAME="akka-backup-$(date +%Y%m%d-%H%M%S).tar.gz"
    print_info "Creating backup: $BACKUP_NAME"
    tar -czf "$BACKUP_NAME" \
        --exclude='.git' \
        --exclude='target' \
        --exclude='*.tar.gz' \
        --exclude='node_modules' \
        . 2>/dev/null
    print_success "Backup created: $BACKUP_NAME"
    echo ""
fi

# Step 2: Count files that will be affected
print_info "Analyzing repository..."
JAVA_FILES=$(find . -name "*.java" -type f | wc -l)
SCALA_FILES=$(find . -name "*.scala" -type f | wc -l)
CONF_FILES=$(find . -name "*.conf" -type f | wc -l)
JAVA_WITH_AKKA=$(find . -name "*.java" -type f -exec grep -l "import akka\." {} \; 2>/dev/null | wc -l)
CONF_WITH_AKKA=$(find . -name "*.conf" -type f -exec grep -l "akka\." {} \; 2>/dev/null | wc -l)

echo ""
print_info "Files found:"
echo "  - Total Java files: $JAVA_FILES"
echo "  - Java files with Akka imports: $JAVA_WITH_AKKA"
echo "  - Total Scala files: $SCALA_FILES"
echo "  - Total config files: $CONF_FILES"
echo "  - Config files with Akka: $CONF_WITH_AKKA"
echo ""

if [ "$DRY_RUN" = true ]; then
    print_info "Files that would be modified:"
    find . -name "*.java" -type f -exec grep -l "import akka\." {} \; 2>/dev/null | sed 's/^/  - /'
    echo ""
fi

# Function to perform replacement
replace_in_files() {
    local pattern=$1
    local replacement=$2
    local file_pattern=$3
    local description=$4
    
    print_info "Processing: $description"
    
    if [ "$DRY_RUN" = true ]; then
        COUNT=$(find . -name "$file_pattern" -type f -exec grep -l "$pattern" {} \; 2>/dev/null | wc -l)
        print_info "Would modify $COUNT files"
    else
        find . -name "$file_pattern" -type f -exec sed -i "s|$pattern|$replacement|g" {} + 2>/dev/null
        COUNT=$(find . -name "$file_pattern" -type f -exec grep -l "$replacement" {} \; 2>/dev/null | wc -l)
        print_success "Modified $COUNT files"
    fi
}

# Step 3: Replace Java imports
echo ""
print_info "=========================================="
print_info "Step 1: Replacing Java imports"
print_info "=========================================="
echo ""

replace_in_files "import akka\." "import org.apache.pekko." "*.java" "Java imports"

# Step 4: Replace Scala imports (if any)
if [ $SCALA_FILES -gt 0 ]; then
    echo ""
    print_info "=========================================="
    print_info "Step 2: Replacing Scala imports"
    print_info "=========================================="
    echo ""
    
    replace_in_files "import akka\." "import org.apache.pekko." "*.scala" "Scala imports"
fi

# Step 5: Replace configuration files
echo ""
print_info "=========================================="
print_info "Step 3: Updating configuration files"
print_info "=========================================="
echo ""

# Replace configuration namespace
print_info "Replacing akka namespace in .conf files"
if [ "$DRY_RUN" = true ]; then
    COUNT=$(find . -name "*.conf" -type f -exec grep -l "^akka\." {} \; 2>/dev/null | wc -l)
    print_info "Would modify $COUNT files"
else
    find . -name "*.conf" -type f -exec sed -i 's/^akka\./pekko./g' {} + 2>/dev/null
    print_success "Configuration namespace updated"
fi

print_info "Replacing akka class references in .conf files"
if [ "$DRY_RUN" = true ]; then
    COUNT=$(find . -name "*.conf" -type f -exec grep -l '"akka\.' {} \; 2>/dev/null | wc -l)
    print_info "Would modify $COUNT files"
else
    find . -name "*.conf" -type f -exec sed -i 's/"akka\./"org.apache.pekko./g' {} + 2>/dev/null
    print_success "Class references updated"
fi

print_info "Replacing akka in array/list references"
if [ "$DRY_RUN" = true ]; then
    print_info "Would update array references"
else
    find . -name "*.conf" -type f -exec sed -i 's/\[akka\./[org.apache.pekko./g' {} + 2>/dev/null
    find . -name "*.conf" -type f -exec sed -i "s/'akka\./'org.apache.pekko./g" {} + 2>/dev/null
    print_success "Array references updated"
fi

# Step 6: Verification
echo ""
print_info "=========================================="
print_info "Step 4: Verification"
print_info "=========================================="
echo ""

if [ "$DRY_RUN" = false ]; then
    REMAINING_IMPORTS=$(find . -name "*.java" -type f -exec grep -l "import akka\." {} \; 2>/dev/null | wc -l)
    REMAINING_CONF=$(find . -name "*.conf" -type f -exec grep "^akka\." {} \; 2>/dev/null | wc -l)
    
    if [ $REMAINING_IMPORTS -eq 0 ]; then
        print_success "All Java imports updated successfully"
    else
        print_warning "Found $REMAINING_IMPORTS files with remaining 'import akka.' statements"
        print_info "Files to review:"
        find . -name "*.java" -type f -exec grep -l "import akka\." {} \; 2>/dev/null | sed 's/^/  - /'
    fi
    
    if [ $REMAINING_CONF -eq 0 ]; then
        print_success "All configuration files updated successfully"
    else
        print_warning "Found $REMAINING_CONF lines with 'akka.' in configuration files"
    fi
    
    # Show summary of changes
    echo ""
    print_info "Summary of Pekko references:"
    PEKKO_IMPORTS=$(find . -name "*.java" -type f -exec grep -l "import org.apache.pekko\." {} \; 2>/dev/null | wc -l)
    PEKKO_CONF=$(find . -name "*.conf" -type f -exec grep -l "^pekko\." {} \; 2>/dev/null | wc -l)
    echo "  - Java files with Pekko imports: $PEKKO_IMPORTS"
    echo "  - Config files with Pekko: $PEKKO_CONF"
fi

# Step 7: Next steps
echo ""
print_info "=========================================="
print_info "Next Steps"
print_info "=========================================="
echo ""

if [ "$DRY_RUN" = true ]; then
    print_info "This was a dry run. No files were modified."
    print_info "Run without --dry-run to perform the migration."
else
    print_success "Automated migration complete!"
    echo ""
    print_warning "IMPORTANT: Manual steps still required:"
    echo "  1. Update all pom.xml files:"
    echo "     - Change akka.x.version → pekko.version"
    echo "     - Change scala.major.version from 2.11 → 2.13"
    echo "     - Update com.typesafe.akka → org.apache.pekko"
    echo "     - Update play2.version to 2.9.5 or 3.0.x"
    echo ""
    echo "  2. Update ActorStartModule.java:"
    echo "     - Change extends AkkaGuiceSupport to manual DI (Play 2.9)"
    echo "     - OR use PekkoGuiceSupport (Play 3.0)"
    echo ""
    echo "  3. Review and test:"
    echo "     - Run: mvn clean install"
    echo "     - Run: mvn test"
    echo "     - Review git diff"
    echo "     - Test the application thoroughly"
    echo ""
    echo "  4. Commit changes:"
    echo "     - git add ."
    echo "     - git commit -m 'Migrate from Akka to Pekko'"
    echo ""
    
    print_info "Backup location: $BACKUP_NAME"
    print_info "To rollback: tar -xzf $BACKUP_NAME"
fi

echo ""
print_info "Migration script finished"
print_info "For detailed guidance, see PLAY_PEKKO_MIGRATION_REPORT.md"
echo ""
