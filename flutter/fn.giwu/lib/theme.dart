import 'package:flutter/material.dart';

const double kDesktopBreakpoint = 768.0;
const double kSidebarWidth = 200.0;
const double kPanelWidth = 320.0;

// Brand palette
const Color kPrimary    = Color(0xFFE30613); // brand red
const Color kBrandBlack = Color(0xFF0A0A0A); // brand black
const Color kBrandGray  = Color(0xFF6D6E71); // brand gray

// Light mode surfaces
const Color kDivider   = Color(0xFFE5E7EB);
const Color kSidebarBg = Color(0xFFF9FAFB);
const Color kMuted     = Color(0xFF6D6E71);

// Dark mode surfaces (neutral, not blue-gray)
const Color kDarkBg      = Color(0xFF0A0A0A);
const Color kDarkSurface = Color(0xFF1A1A1A);
const Color kDarkBorder  = Color(0xFF2A2A2A);
const Color kDarkBorder2 = Color(0xFF3D3D3D);

ThemeData buildTheme(Brightness brightness) {
  final isDark = brightness == Brightness.dark;
  return ThemeData(
    colorScheme: ColorScheme.fromSeed(
      seedColor: kPrimary,
      brightness: brightness,
    ),
    useMaterial3: true,
    scaffoldBackgroundColor: isDark ? kDarkBg : Colors.white,
    dividerColor: isDark ? kDarkBorder : kDivider,
    dividerTheme: DividerThemeData(
      space: 1,
      thickness: 1,
      color: isDark ? kDarkBorder : kDivider,
    ),
    appBarTheme: AppBarTheme(
      elevation: 0,
      scrolledUnderElevation: 0,
      backgroundColor: isDark ? kDarkSurface : Colors.white,
      foregroundColor: isDark ? Colors.white : kBrandBlack,
      surfaceTintColor: Colors.transparent,
      titleTextStyle: TextStyle(
        fontSize: 16,
        fontWeight: FontWeight.w700,
        color: isDark ? Colors.white : kBrandBlack,
      ),
      shape: Border(
        bottom: BorderSide(
          color: isDark ? kDarkBorder : kPrimary,
          width: 3,
        ),
      ),
    ),
  );
}
