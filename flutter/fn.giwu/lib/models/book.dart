class BookModel {
  final int b;
  final String n;
  // 't' is OT/NT — nullable because some DB rows store null here.
  final String? t;

  const BookModel({required this.b, required this.n, this.t});

  factory BookModel.fromJson(Map<String, dynamic> json) => BookModel(
        b: (json['b'] as num).toInt(),
        n: json['n'] as String,
        t: json['t'] as String?,
      );
}
