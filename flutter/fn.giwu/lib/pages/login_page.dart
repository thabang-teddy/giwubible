import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../providers/auth_provider.dart';
import '../theme.dart';

class LoginPage extends ConsumerStatefulWidget {
  const LoginPage({super.key, this.onSuccess});

  final VoidCallback? onSuccess;

  @override
  ConsumerState<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends ConsumerState<LoginPage> {
  bool _isLogin = true;
  bool _submitting = false;
  String? _error;

  final _nameCtrl = TextEditingController();
  final _emailCtrl = TextEditingController();
  final _passwordCtrl = TextEditingController();

  @override
  void dispose() {
    _nameCtrl.dispose();
    _emailCtrl.dispose();
    _passwordCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    setState(() { _submitting = true; _error = null; });
    try {
      if (_isLogin) {
        await ref
            .read(authProvider.notifier)
            .login(_emailCtrl.text.trim(), _passwordCtrl.text);
      } else {
        await ref.read(authProvider.notifier).register(
              _nameCtrl.text.trim(),
              _emailCtrl.text.trim(),
              _passwordCtrl.text,
            );
      }
      if (mounted) widget.onSuccess?.call();
    } on Exception catch (e) {
      setState(() { _error = _parseError(e); });
    } finally {
      if (mounted) setState(() { _submitting = false; });
    }
  }

  String _parseError(Object e) {
    final msg = e.toString();
    if (msg.contains('401') || msg.contains('Invalid')) return 'Invalid email or password.';
    if (msg.contains('422')) return 'Please check your details and try again.';
    if (msg.contains('SocketException') || msg.contains('Connection')) {
      return 'Cannot reach the server. Check your connection.';
    }
    return 'Something went wrong. Please try again.';
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Giwu Bible'),
        centerTitle: false,
        leading: Navigator.of(context).canPop()
            ? IconButton(
                icon: const Icon(Icons.arrow_back),
                onPressed: () => Navigator.of(context).pop(),
              )
            : null,
      ),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 400),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  // Tabs
                  Container(
                    decoration: BoxDecoration(
                      color: isDark ? kDarkSurface : const Color(0xFFF3F4F6),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    padding: const EdgeInsets.all(4),
                    child: Row(
                      children: [
                        _tab('Sign in', _isLogin, () => setState(() { _isLogin = true; _error = null; })),
                        _tab('Create account', !_isLogin, () => setState(() { _isLogin = false; _error = null; })),
                      ],
                    ),
                  ),
                  const SizedBox(height: 28),

                  // Name field (register only)
                  if (!_isLogin) ...[
                    _field('Name', _nameCtrl, TextInputType.name,
                        TextInputAction.next),
                    const SizedBox(height: 14),
                  ],

                  _field('Email', _emailCtrl, TextInputType.emailAddress,
                      TextInputAction.next),
                  const SizedBox(height: 14),

                  _field(
                    'Password',
                    _passwordCtrl,
                    TextInputType.visiblePassword,
                    TextInputAction.done,
                    obscure: true,
                    hint: _isLogin ? null : 'At least 8 characters',
                    onSubmit: _submit,
                  ),

                  if (_error != null) ...[
                    const SizedBox(height: 14),
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: cs.errorContainer,
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Text(
                        _error!,
                        style: TextStyle(color: cs.onErrorContainer, fontSize: 13),
                      ),
                    ),
                  ],

                  const SizedBox(height: 20),

                  FilledButton(
                    onPressed: _submitting ? null : _submit,
                    style: FilledButton.styleFrom(
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(10)),
                    ),
                    child: _submitting
                        ? const SizedBox(
                            height: 18,
                            width: 18,
                            child: CircularProgressIndicator(
                                strokeWidth: 2, color: Colors.white),
                          )
                        : Text(_isLogin ? 'Sign in' : 'Create account'),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _tab(String label, bool active, VoidCallback onTap) {
    final cs = Theme.of(context).colorScheme;
    return Expanded(
      child: GestureDetector(
        onTap: onTap,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 180),
          padding: const EdgeInsets.symmetric(vertical: 8),
          decoration: BoxDecoration(
            color: active ? cs.surface : Colors.transparent,
            borderRadius: BorderRadius.circular(7),
            boxShadow: active
                ? [BoxShadow(color: Colors.black.withOpacity(0.08), blurRadius: 4)]
                : null,
          ),
          child: Text(
            label,
            textAlign: TextAlign.center,
            style: TextStyle(
              fontSize: 13,
              fontWeight: active ? FontWeight.w600 : FontWeight.w400,
              color: active
                  ? cs.primary
                  : Theme.of(context).brightness == Brightness.dark
                      ? kMuted
                      : const Color(0xFF6B7280),
            ),
          ),
        ),
      ),
    );
  }

  Widget _field(
    String label,
    TextEditingController ctrl,
    TextInputType type,
    TextInputAction action, {
    bool obscure = false,
    String? hint,
    VoidCallback? onSubmit,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label,
            style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w500)),
        const SizedBox(height: 6),
        TextField(
          controller: ctrl,
          keyboardType: type,
          textInputAction: action,
          obscureText: obscure,
          autocorrect: false,
          onSubmitted: onSubmit != null ? (_) => onSubmit() : null,
          decoration: InputDecoration(
            hintText: hint,
            contentPadding:
                const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
            border: OutlineInputBorder(borderRadius: BorderRadius.circular(8)),
          ),
        ),
      ],
    );
  }
}
