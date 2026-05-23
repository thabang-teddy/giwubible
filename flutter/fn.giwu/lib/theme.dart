import 'package:flutter/material.dart';

const double kDesktopBreakpoint = 768.0;
const double kSidebarWidth = 200.0;
const double kPanelWidth = 320.0;

const Color kPrimary = Color(0xFF6366F1);
const Color kDivider = Color(0xFFE5E7EB);
const Color kSidebarBg = Color(0xFFF9FAFB);
const Color kMuted = Color(0xFF6B7280);

ThemeData buildTheme(Brightness brightness) {
  final isDark = brightness == Brightness.dark;
  return ThemeData(
    colorScheme: ColorScheme.fromSeed(
      seedColor: kPrimary,
      brightness: brightness,
    ),
    useMaterial3: true,
    scaffoldBackgroundColor: isDark ? const Color(0xFF0F172A) : Colors.white,
    dividerColor: kDivider,
    dividerTheme: const DividerThemeData(space: 1, thickness: 1, color: kDivider),
    appBarTheme: AppBarTheme(
      elevation: 0,
      scrolledUnderElevation: 0,
      backgroundColor: isDark ? const Color(0xFF1E293B) : Colors.white,
      foregroundColor: isDark ? Colors.white : const Color(0xFF111827),
      surfaceTintColor: Colors.transparent,
      titleTextStyle: TextStyle(
        fontSize: 16,
        fontWeight: FontWeight.w700,
        color: isDark ? Colors.white : const Color(0xFF111827),
      ),
      shape: Border(
        bottom: BorderSide(
          color: isDark ? const Color(0xFF334155) : kDivider,
        ),
      ),
    ),
  );
}
