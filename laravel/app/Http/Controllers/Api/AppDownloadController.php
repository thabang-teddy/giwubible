<?php

declare(strict_types=1);

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use Symfony\Component\HttpFoundation\BinaryFileResponse;

class AppDownloadController extends Controller
{
    /** Explicit allowlist — prevents directory traversal and serves only known binaries. */
    private const ALLOWED = [
        'giwu-bible-android.apk',
        'giwu-bible-windows-setup.exe',
    ];

    public function serve(string $filename): BinaryFileResponse
    {
        abort_unless(in_array($filename, self::ALLOWED, true), 404);

        $path = public_path("downloads/{$filename}");

        abort_unless(file_exists($path), 404);

        return response()->download($path, $filename, [
            'Content-Type'        => 'application/octet-stream',
            'Content-Disposition' => "attachment; filename=\"{$filename}\"",
        ]);
    }
}
