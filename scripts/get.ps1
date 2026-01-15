#!/usr/bin/env pwsh
#Requires -Version 5

# jse getter
# usage: (in powershell)
#    Invoke-Expression (Invoke-Webrequest <my location> -UseBasicParsing).Content

param (
    [string]$installdir = ""
)

& {
    $LastRelease = "3.13.7"
    $ErrorActionPreference = 'Stop'
    
    $WorkingDir = Join-Path ([System.IO.Path]::GetTempPath()) ("jse-getter-" + [guid]::NewGuid().ToString())
    New-Item -ItemType Directory -Path $WorkingDir | Out-Null
    
    function WriteErrorTip($msg) {
        Write-Host $msg -BackgroundColor Red -ForegroundColor White
    }
    function AskYesNo($msg, $defaultYes = $true) {
        $suffix = if ($defaultYes) { "(Y/n)" } else { "(y/N)" }
        $ans = Read-Host "$msg $suffix"
        if ([string]::IsNullOrWhiteSpace($ans)) {
            return $defaultYes
        }
        return $ans.ToLower().StartsWith("y")
    }
    
    # start script
    if (-not $env:CI) {
        $logo = @(
            '       __  ____  ____          ',
            '     _(  )/ ___)(  __)         ',
            '    / \) \\___ \ ) _)          ',
            '    \____/(____/(____) getter  ',
            '                               ')
        Write-Host ($logo -join "`n") -ForegroundColor Green
    }
    if ($IsLinux -or $IsMacOS) {
        writeErrorTip 'Install on *nix is not supported, try '
        writeErrorTip '(Use curl) "bash <(curl -fsSL https://raw.githubusercontent.com/liqa1024/jse/main/scripts/get.sh)"'
        writeErrorTip 'or'
        writeErrorTip '(Use wget) "bash <(wget https://raw.githubusercontent.com/liqa1024/jse/main/scripts/get.sh -O -)"'
        throw 'Unsupported platform'
    }
    
    function GetJavaInfo {
        try {
            # compat for old powershell
            $output = & java --version
        } catch {
            return $null
        }
        $info = @{
            Vendor  = ""
            IsMSJdk = $false
        }
        foreach ($line in $output) {
            # openjdk 21.0.9
            if (-not $info.Version -and $line -match '\b(\d+)(?:\.(\d+))?(?:\.(\d+))?\b') {
                $info.Version = $Matches[0]
            }
            if ($line -match '\bMicrosoft\b') {
                $info.IsMSJdk = $true
            }
        }
        if (-not $info.Version) {
            return $null
        }
        return $info
    }
    function InstallOpenJdk {
        $jdkInstaller = Join-Path $WorkingDir "microsoft-jdk-21.0.9-windows-x64.exe"
        $url = "https://aka.ms/download-jdk/microsoft-jdk-21.0.9-windows-x64.exe"
        
        Write-Host "Downloading Microsoft OpenJDK 21..."
        Invoke-WebRequest $url -OutFile $jdkInstaller -UseBasicParsing
        
        Write-Host "Launching JDK installer..."
        Start-Process -FilePath $jdkInstaller -Wait
        
        # Try detect JAVA_HOME automatically, and add to current session
        $jdkRoot = Get-ChildItem "C:\Program Files\Microsoft" -Directory |
                   Where-Object { $_.Name -match "jdk-21" } |
                   Select-Object -First 1
        if ($jdkRoot) {
            $env:JAVA_HOME = $jdkRoot.FullName
            $env:Path = "$env:JAVA_HOME\bin;$env:Path"
        } else {
            WriteErrorTip "Unable to auto-detect JDK path, please check manually."
        }
    }
    
    # jdk part
    $javaInfo = GetJavaInfo
    if (-not $javaInfo) {
        Write-Host "No suitable Java environment detected."
        if (-not (AskYesNo "Install Microsoft OpenJDK 21 now?" $true)) {
            WriteErrorTip "Java is required. Abort."
            return
        }
        InstallOpenJdk
    } else {
        $major = [int]($javaInfo.Version -split '\.')[0]
        if ($major -lt 21) {
            Write-Host "Java detected: $($javaInfo.Version)"
            if (AskYesNo "Recommend installing Microsoft OpenJDK 21. Install now?" $true) {
                InstallOpenJdk
            }
        } elseif (-not $javaInfo.IsMSJdk) {
            Write-Host "Non-Microsoft Java detected: $($javaInfo.Version)"
            if (AskYesNo "Recommend installing Microsoft OpenJDK 21. Install now?" $true) {
                InstallOpenJdk
            }
        } else {
            Write-Host "Microsoft OpenJDK $($javaInfo.Version) detected."
            if (AskYesNo "Reinstall Microsoft OpenJDK 21?" $false) {
                InstallOpenJdk
            }
        }
    }
    
    # jse install dir
    if (-not $installdir) {
        $installdir = Join-Path $HOME "jse"
    }
    $installdir = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($installdir)
    
    Write-Host "Install to $installdir"
    if (Test-Path $installdir) {
        $items = Get-ChildItem -Path $installdir -Force -ErrorAction SilentlyContinue
        if ($items -and $items.Count -gt 0) {
            Write-Host "Install directory is not empty, following will be removed: " -ForegroundColor Yellow
            $items | ForEach-Object {
                Write-Host "  - $($_.FullName)"
            }
            if (-not (AskYesNo "Confirm to update?" $false)) {
                WriteErrorTip "Update aborted."
                return
            }
            Remove-Item -Path $installdir -Recurse -Force
        }
    }
    New-Item -ItemType Directory -Path $installdir -Force | Out-Null
    
    # download jse
    $zipFile = Join-Path $WorkingDir "jse-$LastRelease.zip"
    $downloadUrl = "https://github.com/liqa1024/jse/releases/download/v$LastRelease/jse-$LastRelease.zip"
    
    Write-Host "Downloading jse..."
    Invoke-WebRequest $downloadUrl -OutFile $zipFile -UseBasicParsing
    Write-Host "Extracting..."
    Expand-Archive $zipFile -DestinationPath $WorkingDir -Force
    
    $innerDir = Get-ChildItem -Path $WorkingDir -Directory |
        Where-Object { $_.PSIsContainer -and $_.Name -like "jse-*" } |
        Select-Object -First 1
    if (-not $innerDir) {
        WriteErrorTip "Unexpected zip structure."
        return
    }
    Move-Item -Path (Join-Path $innerDir.FullName '*') -Destination $installdir -Force
    
    # add to path (auto for current session)
    $env:Path = "$installdir;$env:Path"
    if (AskYesNo "Add jse to PATH for current user (permanent)?" $true) {
        try {
            $userPath = [Environment]::GetEnvironmentVariable(
                "Path",
                [EnvironmentVariableTarget]::User
            )
            if (-not $userPath) {
                $userPath = ""
            }
            $paths = $userPath -split ';' | Where-Object { $_ -and $_.Trim() }
            
            if ($paths -notcontains $installdir) {
                $newPath = "$installdir;$userPath"
                [Environment]::SetEnvironmentVariable(
                    "Path",
                    $newPath,
                    [EnvironmentVariableTarget]::User
                )
                Write-Host "jse has been added to user PATH."
            } else {
                Write-Host "jse already exists in user PATH."
            }
        } catch {
            WriteErrorTip "Failed to update user PATH."
            WriteErrorTip "You may need to add it manually:"
            WriteErrorTip $installdir
        }
    } else {
        Write-Host "Skipped adding jse to permanent PATH."
        Write-Host "You can add it manually:"
        Write-Host $installdir
    }
    try {
        & jse -v
    } catch {
        WriteErrorTip "jse failed to run... Why?"
        return
    }
    
    # jni part
    if (AskYesNo "Run 'jse --jnibuild' to install JNI libraries?" $false) {
        & jse --jnibuild
    }
    Write-Host ""
    Write-Host "Installation completed!" -ForegroundColor Green
    
    # cleanup isolated temp directory
    if (Test-Path $WorkingDir) {
        Remove-Item -Path $WorkingDir -Recurse -Force -ErrorAction SilentlyContinue
    }
}
