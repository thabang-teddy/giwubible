<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\BibleVersionKey;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;

class ChapterController extends Controller
{
    public function index(Request $request): JsonResponse
    {
        $request->validate([
            'bible'   => 'required|string',
            'book'    => 'required|integer|min:1',
            'chapter' => 'required|integer|min:1',
        ]);

        $table = $this->resolveTable($request->input('bible'));
        if (!$table) {
            return response()->json(['error' => 'Unknown bible version', 'code' => 'INVALID_BIBLE'], 422);
        }

        $verses = DB::connection('bible_sqlite')
            ->table($table)
            ->select('b', 'c', 'v', 't')
            ->where('b', $request->input('book'))
            ->where('c', $request->input('chapter'))
            ->orderBy('v')
            ->get();

        return response()->json(['data' => $verses]);
    }

    private function resolveTable(string $requested): ?string
    {
        return BibleVersionKey::where('table', $requested)->value('table');
    }
}
