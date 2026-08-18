<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Repositories\BibleRepository;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class ChapterController extends Controller
{
    public function index(Request $request, BibleRepository $bibles): JsonResponse
    {
        $request->validate([
            'bible' => 'required|string',
            'book' => 'required|integer|min:1',
            'chapter' => 'required|integer|min:1',
        ]);

        $table = $bibles->resolveTable($request->input('bible'));
        if (! $table) {
            return response()->json(['error' => 'Unknown bible version', 'code' => 'INVALID_BIBLE'], 422);
        }

        $verses = $bibles->chapter($table, (int) $request->input('book'), (int) $request->input('chapter'));

        return response()->json(['data' => $verses]);
    }
}
