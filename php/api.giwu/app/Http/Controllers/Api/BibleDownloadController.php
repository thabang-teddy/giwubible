<?php

declare(strict_types=1);

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\BibleVersionKey;
use Illuminate\Http\JsonResponse;
use Illuminate\Support\Facades\DB;

class BibleDownloadController extends Controller
{
    public function download(string $table): JsonResponse
    {
        $version = BibleVersionKey::where('table', $table)->first();

        if (! $version) {
            return response()->json([
                'error' => 'Unknown bible version',
                'code'  => 'INVALID_BIBLE',
            ], 422);
        }

        // PDO/SQLite may return INTEGER columns as strings depending on the
        // driver version and fetch mode.  Cast b, c, v to int explicitly so
        // the JSON response always contains numbers, never quoted strings.
        // Clients rely on these keys to store and query verses by position.
        $verses = DB::connection('bible_sqlite')
            ->table($table)
            ->select('b', 'c', 'v', 't')
            ->orderBy('b')
            ->orderBy('c')
            ->orderBy('v')
            ->get()
            ->map(fn ($row) => [
                'b' => (int) $row->b,
                'c' => (int) $row->c,
                'v' => (int) $row->v,
                't' => (string) $row->t,
            ]);

        return response()->json([
            'data' => [
                'table'        => (string) $version->getAttribute('table'),
                'abbreviation' => (string) $version->abbreviation,
                'version'      => (string) $version->version,
                'verses'       => $verses,
            ],
        ]);
    }
}
