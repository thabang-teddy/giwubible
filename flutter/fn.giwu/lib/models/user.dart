class UserModel {
  const UserModel({
    required this.id,
    required this.name,
    required this.email,
    this.emailVerified = false,
  });

  final int id;
  final String name;
  final String email;

  /// Mirrors `email_verified_at` on the server. Bookmarks are refused until
  /// it is true, so the UI uses it to explain why rather than letting a save
  /// fail with a bare error.
  final bool emailVerified;

  factory UserModel.fromJson(Map<String, dynamic> json) => UserModel(
        id: json['id'] as int,
        name: json['name'] as String,
        email: json['email'] as String,
        emailVerified: json['email_verified_at'] != null,
      );

  /// Value equality, because `AuthNotifier` re-reads the account on every
  /// window focus. Without this, each read would be a new object, the
  /// provider would notify, and `BookmarksNotifier` — which reloads whenever
  /// the signed-in user changes — would refetch the bookmark list and blank
  /// it to a spinner every time the window came forward.
  @override
  bool operator ==(Object other) =>
      other is UserModel &&
      other.id == id &&
      other.name == name &&
      other.email == email &&
      other.emailVerified == emailVerified;

  @override
  int get hashCode => Object.hash(id, name, email, emailVerified);
}
