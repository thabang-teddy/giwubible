[Setup]
AppName=Giwu Bible
AppVersion=1.0.0
AppPublisher=Giwu Bible
AppPublisherURL=https://giwu.app
AppSupportURL=https://giwu.app
AppUpdatesURL=https://giwu.app
DefaultDirName={autopf}\Giwu Bible
DefaultGroupName=Giwu Bible
AllowNoIcons=yes
OutputDir=output
OutputBaseFilename=giwu-bible-windows-setup
SetupIconFile=..\runner\resources\app_icon.ico
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
UninstallDisplayIcon={app}\fn_giwu.exe
ArchitecturesInstallIn64BitMode=x64compatible
MinVersion=10.0

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "..\..\build\windows\x64\runner\Release\fn_giwu.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\..\build\windows\x64\runner\Release\flutter_windows.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\..\build\windows\x64\runner\Release\sqlite3.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\..\build\windows\x64\runner\Release\data\*"; DestDir: "{app}\data"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\Giwu Bible"; Filename: "{app}\fn_giwu.exe"; IconFilename: "{app}\fn_giwu.exe"
Name: "{group}\{cm:UninstallProgram,Giwu Bible}"; Filename: "{uninstallexe}"
Name: "{autodesktop}\Giwu Bible"; Filename: "{app}\fn_giwu.exe"; IconFilename: "{app}\fn_giwu.exe"; Tasks: desktopicon

[Run]
Filename: "{app}\fn_giwu.exe"; Description: "{cm:LaunchProgram,Giwu Bible}"; Flags: nowait postinstall skipifsilent
