<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\BibleVersionKey;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;

class VerseController extends Controller
{
    public function show(Request $request): JsonResponse
    {
        $request->validate([
            'bible'   => 'required|string',
            'book'    => 'required|integer|min:1',
            'chapter' => 'required|integer|min:1',
            'verse'   => 'required|integer|min:1',
        ]);

        $version = $this->resolveVersion($request->input('bible'));
        if (!$version) {
            return response()->json(['error' => 'Unknown bible version', 'code' => 'INVALID_BIBLE'], 422);
        }

        $row = DB::connection('bible_sqlite')
            ->table($version->table)
            ->where('b', $request->input('book'))
            ->where('c', $request->input('chapter'))
            ->where('v', $request->input('verse'))
            ->first();

        if (!$row) {
            return response()->json(['error' => 'Verse not found', 'code' => 'NOT_FOUND'], 404);
        }

        return response()->json([
            'data' => [
                'bible'        => $version->table,
                'abbreviation' => $version->abbreviation,
                'version'      => $version->version,
                'text'         => $row->t,
            ],
        ]);
    }

    private function resolveVersion(string $requested): ?object
    {
        return BibleVersionKey::where('table', $requested)
            ->select('table', 'abbreviation', 'version')
            ->first();
    }
}
