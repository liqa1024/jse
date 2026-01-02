#!/usr/bin/env bash

set -o pipefail

# =============================
# Basic config
# =============================

JSE_VERSION="3.12.6"
JSE_DEFAULT_DIR="${HOME}/jse"
JSE_RELEASE_URL="https://github.com/CHanzyLazer/jse/releases/download/v${JSE_VERSION}/jse-${JSE_VERSION}.tar.gz"

JAVA_REQUIRED="21"
JDK_VENDOR="Oracle"
JDK_VERSION="21"
JDK_URL="https://download.oracle.com/java/21/latest/jdk-21_linux-x64_bin.tar.gz"
JDK_BASE="${HOME}/java"

BASHRC="${HOME}/.bashrc"

# =============================
# Helper functions
# =============================

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
    remote_get "$1" > "$2" || raise "download failed: $1"
}


# =========================================================
# logo
# =========================================================

echo '       __  ____  ____          '
echo '     _(  )/ ___)(  __)         '
echo '    / \) \\___ \ ) _)          '
echo '    \____/(____/(____) getter  '
echo '                               '

# =========================================================
# detect java
# =========================================================

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

# =========================================================
# install jdk
# =========================================================

if [ "$INSTALL_JDK" -eq 1 ]; then
    echo "Installing JDK into $JDK_BASE"
    mkdir -p "$JDK_BASE" || raise "cannot create $JDK_BASE"

    tmpdir=$(mktemp -d) || raise "mktemp failed"
    download_file "$JDK_URL" "$tmpdir/jdk.tar.gz"

    echo "Extracting..."
    tar -xzf "$tmpdir/jdk.tar.gz" -C "$JDK_BASE" || raise "extract jdk failed"
    rm -rf "$tmpdir"

    JAVA_HOME_DETECTED=$(find "$JDK_BASE" -maxdepth 1 -type d -name "jdk-*" | sort | tail -n 1)
    [ -n "$JAVA_HOME_DETECTED" ] || raise "cannot detect extracted JDK directory"

    # env for current session
    export JAVA_HOME="$JAVA_HOME_DETECTED"
    export PATH="$JAVA_HOME/bin:$PATH"
    # env for bashrc
    if ask_yes_no "Add JAVA_HOME and PATH to ~/.bashrc?" y; then
        {
            echo ""
            echo "# >>> jse java"
            echo "export JAVA_HOME=\"$JAVA_HOME\""
            echo "export PATH=\"\$JAVA_HOME/bin:\$PATH\""
            echo "# <<< jse java"
        } >> "$BASHRC"
    fi
fi

# =========================================================
# install jse
# =========================================================

JSE_DIR="${1:-$JSE_DEFAULT_DIR}"

echo "Installing jse into $JSE_DIR"
if [ -d "$JSE_DIR" ] && [ "$(ls -A "$JSE_DIR" 2>/dev/null)" ]; then
    raise "installation directory not empty: $JSE_DIR"
fi
mkdir -p "$JSE_DIR" || raise "cannot create $JSE_DIR"

tmpdir=$(mktemp -d) || raise "mktemp failed"
download_file "$JSE_RELEASE_URL" "$tmpdir/jse.tar.gz"

echo "Extracting..."
tar -xzf "$tmpdir/jse.tar.gz" -C "$tmpdir" || raise "extract jse failed"
inner=$(find "$tmpdir" -maxdepth 1 -type d -name "jse-*")
[ -n "$inner" ] || raise "invalid jse archive"
mv "$inner"/* "$JSE_DIR" || raise "install jse failed"
rm -rf "$tmpdir"

# env for current session
export PATH="$JSE_DIR/bin:$PATH"
# env for bashrc
if ask_yes_no "Add jse to PATH in ~/.bashrc?" y; then
    {
        echo ""
        echo "# >>> jse path"
        echo "export PATH=\"$JSE_DIR/bin:\$PATH\""
        echo "# <<< jse path"
    } >> "$BASHRC"
fi

# =========================================================
# verify
# =========================================================

jse -v || raise "jse failed to run... Why?"

if ask_yes_no "Run 'jse -jnibuild' to install JNI libraries?" n; then
    jse -jnibuild || raise "jni build failed"
fi

echo ""
echo "Installation completed!"
