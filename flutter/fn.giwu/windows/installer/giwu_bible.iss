; Inno Setup script for the Giwu Bible Windows installer.
;
; Build the app first, then compile from flutter/fn.giwu:
;
;   flutter build windows --release
;   iscc /DAppVersion=1.0.0 /DOutputName=giwu-bible-windows-setup windows\installer\giwu_bible.iss
;
; CI passes the release tag in both defines (see the windows job in ci.yml).
; Installs per user (no admin prompt) into %LOCALAPPDATA%\Programs\Giwu Bible.

#ifndef AppVersion
  #define AppVersion "0.0.0"
#endif
#ifndef OutputName
  #define OutputName "giwu-bible-windows-setup"
#endif

#define AppName "Giwu Bible"
#define AppExe "giwu_bible.exe"
#define BuildDir "..\..\build\windows\x64\runner\Release"

[Setup]
; Never change AppId: Windows uses it to recognise upgrades of this app.
AppId={{6476BDEB-F291-4B17-9A47-E689F2286472}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher=Giwu
AppPublisherURL=https://github.com/thabang-teddy/giwubible
DefaultDirName={autopf}\{#AppName}
DefaultGroupName={#AppName}
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
OutputDir=..\..\build\installer
OutputBaseFilename={#OutputName}
SetupIconFile=..\runner\resources\app_icon.ico
UninstallDisplayIcon={app}\{#AppExe}
Compression=lzma2
SolidCompression=yes
WizardStyle=modern

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"

[Files]
Source: "{#BuildDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\{#AppName}"; Filename: "{app}\{#AppExe}"
Name: "{autodesktop}\{#AppName}"; Filename: "{app}\{#AppExe}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#AppExe}"; Description: "{cm:LaunchProgram,{#AppName}}"; Flags: nowait postinstall skipifsilent
