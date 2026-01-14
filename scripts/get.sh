#!/usr/bin/env bash

# jse getter
# usage:
#    bash <(curl -s <my location>)

set -o pipefail

# ----------------------------------------
# basic config
#

JSE_DIR="${1:-"${HOME}/jse"}"
JSE_VERSION="3.13.6"
JSE_PKG_NAME="jse-${JSE_VERSION}.tar.gz"
JSE_URL="https://github.com/liqa1024/jse/releases/download/v${JSE_VERSION}/${JSE_PKG_NAME}"

JAVA_REQUIRED="21"
JDK_VENDOR="Oracle"
JDK_VERSION="21"
JDK_PKG_NAME="jdk-${JDK_VERSION}_linux-x64_bin.tar.gz"
JDK_URL="https://download.oracle.com/java/${JDK_VERSION}/latest/${JDK_PKG_NAME}"

BASHRC="${HOME}/.bashrc"
CACHE_DIR="${HOME}/.cache/jse-getter"
mkdir -p "$CACHE_DIR" || raise "cannot create cache dir: $CACHE_DIR"

NL=$'\n'

# ----------------------------------------
# helper functions
#

raise() {
    echo "[ERROR] $*" 1>&2
    exit 1
}
ask_yes_no() {
    local prompt="$1"
    local default="$2"
    local answer

    if [ "$default" = "y" ]; then
        prompt="$prompt (Y/n): "
    else
        prompt="$prompt (y/N): "
    fi
    read -r -p "$prompt" answer || return 0
    [ -z "$answer" ] && [ "$default" = "y" ] && return 0
    case "$answer" in y|Y|yes|YES) return 0 ;; *) return 1 ;; esac
}

remote_get() {
    if curl --version >/dev/null 2>&1; then
        curl -fSL "$1"
    elif wget --version >/dev/null 2>&1 || wget --help >/dev/null 2>&1; then
        wget "$1" -O -
    else
        return 1
    fi
}
download_file() {
    local url="$1"
    local outfile="$2"
    if [ -f "$outfile" ]; then
        echo "Using cached file: $outfile"
        return 0
    fi
    echo "Downloading $url"
    echo "or you can download it manually and put into $CACHE_DIR"
    tmpfile="$(mktemp "${outfile}.tmp.XXXXXX")" || raise "mktemp failed"
    if remote_get "$url" > "$tmpfile"; then
        mv "$tmpfile" "$outfile"
        return 0
    else
        raise "download failed"
    fi
}
write_bashrc_block() {
    local block_name="$1"
    local block_content="$2"
    if grep -q "# >>> $block_name" "$BASHRC" 2>/dev/null; then
        echo "Existing $block_name configuration found in ~/.bashrc, it will be updated."
        sed -i "/# >>> $block_name/,/# <<< $block_name/d" "$BASHRC"
    else
        echo "" >> "$BASHRC"
    fi
    {
        echo "# >>> $block_name"
        echo "$block_content"
        echo "# <<< $block_name"
    } >> "$BASHRC"
}

# ----------------------------------------
# start script
#

echo '       __  ____  ____          '
echo '     _(  )/ ___)(  __)         '
echo '    / \) \\___ \ ) _)          '
echo '    \____/(____/(____) getter  '
echo '                               '

echo "Install to $JSE_DIR"
mkdir -p "$JSE_DIR" || raise "cannot create $JSE_DIR"

# ----------------------------------------
# detect java
#

JAVA_FOUND=0
JAVA_VERSION=0
if java --version >/dev/null 2>&1; then
    JAVA_FOUND=1
    JAVA_VERSION=$(java --version | head -n1 | awk '{print $2}' | cut -d. -f1)
fi

INSTALL_JDK=0
if [ "$JAVA_FOUND" -eq 0 ]; then
    echo "No suitable Java environment detected."
    ask_yes_no "Install $JDK_VENDOR JDK $JDK_VERSION now?" y || raise "Java is required. Abort."
    INSTALL_JDK=1
elif [ "$JAVA_VERSION" -lt "$JAVA_REQUIRED" ]; then
    echo "Java detected: $JAVA_VERSION"
    ask_yes_no "Recommend installing $JDK_VENDOR JDK $JDK_VERSION. Install now?" y && INSTALL_JDK=1
else
    echo "Java detected: $JAVA_VERSION"
    ask_yes_no "Install $JDK_VENDOR JDK $JDK_VERSION anyway?" n && INSTALL_JDK=1
fi

# ----------------------------------------
# install jdk
#

if [ "$INSTALL_JDK" -eq 1 ]; then
    echo "JDK will be installed into $JSE_DIR"
    download_file "$JDK_URL" "$CACHE_DIR/$JDK_PKG_NAME"
    echo "Extracting..."
    tar -xzf "$CACHE_DIR/$JDK_PKG_NAME" -C "$JSE_DIR" || raise "extract jdk failed"
    
    JAVA_HOME_DETECTED=$(find "$JSE_DIR" -maxdepth 1 -type d -name "jdk-*" | sort | tail -n 1)
    [ -n "$JAVA_HOME_DETECTED" ] || raise "cannot detect extracted JDK directory"
    
    # env for current session
    export JAVA_HOME="$JAVA_HOME_DETECTED"
    export PATH="$JAVA_HOME/bin:$PATH"
    # env for bashrc
    if ask_yes_no "Add JAVA_HOME and PATH to ~/.bashrc?" y; then
        write_bashrc_block "jse java" "export JAVA_HOME=\"$JAVA_HOME\"${NL}export PATH=\"\$JAVA_HOME/bin:\$PATH\""
        JAVA_ENV_WRITTEN=1
    else
        JAVA_ENV_WRITTEN=0
        echo "You can add it manually if needed:"
        echo "----------------------------------------"
        echo "export JAVA_HOME=\"$JAVA_HOME\""
        echo "export PATH=\"\$JAVA_HOME/bin:\$PATH\""
        echo "----------------------------------------"
    fi
fi

# ----------------------------------------
# install jse
#

if [ -d "${JSE_DIR}/jse-${JSE_VERSION}" ]; then
    raise "The same jse already exists: ${JSE_DIR}/jse-${JSE_VERSION}"
fi

download_file "$JSE_URL" "$CACHE_DIR/$JSE_PKG_NAME"
echo "Extracting..."
tar -xzf "$CACHE_DIR/$JSE_PKG_NAME" -C "$JSE_DIR" || raise "extract jse failed"

JSE_HOME_DETECTED=$(find "$JSE_DIR" -maxdepth 1 -type d -name "jse-*" | sort | tail -n 1)
[ -n "$JSE_HOME_DETECTED" ] || raise "cannot detect extracted jse directory"

# env for current session
export PATH="$JSE_HOME_DETECTED:$PATH"
# env for bashrc
if ask_yes_no "Add jse PATH in ~/.bashrc?" y; then
    write_bashrc_block "jse path" "export PATH=\"$JSE_HOME_DETECTED:\$PATH\""
    JSE_ENV_WRITTEN=1
else
    JSE_ENV_WRITTEN=0
    echo "You can add it manually if needed:"
    echo "----------------------------------------"
    echo "export PATH=\"$JSE_HOME_DETECTED:\$PATH\""
    echo "----------------------------------------"
fi

# ----------------------------------------
# verify
#

jse -v || raise "jse failed to run... Why?"

if ask_yes_no "Run 'jse --jnibuild' to install JNI libraries?" n; then
    jse --jnibuild || raise "jni build failed"
fi

echo ""
if [ "${JAVA_ENV_WRITTEN:-0}" -eq 1 ] || [ "${JSE_ENV_WRITTEN:-0}" -eq 1 ]; then
    echo "------------Environment Note------------"
    echo "The environment variables have been added to ~/.bashrc,"
    echo "to make them effective in current shell, run:"
    echo "  source ~/.bashrc"
    echo "or open a new terminal."
    echo "----------------------------------------"
fi
echo "Installation completed!"
