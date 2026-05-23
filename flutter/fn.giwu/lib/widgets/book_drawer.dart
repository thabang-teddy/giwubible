import 'package:flutter/material.dart';
import '../models/book.dart';
import 'app_sidebar.dart';

class BookDrawer extends StatelessWidget {
  const BookDrawer({
    super.key,
    required this.books,
    this.isLoading = false,
    this.isError = false,
    this.errorMessage,
    this.onRetry,
  });

  final List<BookModel> books;
  final bool isLoading;
  final bool isError;
  final String? errorMessage;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) {
    return Drawer(
      child: SafeArea(
        child: AppSidebar(
          books: books,
          isLoading: isLoading,
          isError: isError,
          errorMessage: errorMessage,
          onRetry: onRetry,
          onClose: () => Navigator.of(context).pop(),
          afterSelect: () => Navigator.of(context).pop(),
        ),
      ),
    );
  }
}
