<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Repositories\BibleRepository;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class VerseController extends Controller
{
    public function show(Request $request, BibleRepository $bibles): JsonResponse
    {
        $request->validate([
            'bible' => 'required|string',
            'book' => 'required|integer|min:1',
            'chapter' => 'required|integer|min:1',
            'verse' => 'required|integer|min:1',
        ]);

        $version = $bibles->resolveVersion($request->input('bible'));
        if (! $version) {
            return response()->json(['error' => 'Unknown bible version', 'code' => 'INVALID_BIBLE'], 422);
        }

        $row = $bibles->verse(
            $version->table,
            (int) $request->input('book'),
            (int) $request->input('chapter'),
            (int) $request->input('verse'),
        );

        if (! $row) {
            return response()->json(['error' => 'Verse not found', 'code' => 'NOT_FOUND'], 404);
        }

        return response()->json([
            'data' => [
                'bible' => $version->table,
                'abbreviation' => $version->abbreviation,
                'version' => $version->version,
                'text' => $row->t,
            ],
        ]);
    }

    /**
     * The same verse in every translation, in one response — powers the web
     * comparison panel with a single AJAX call instead of one per version.
     */
    public function comparisons(Request $request, BibleRepository $bibles): JsonResponse
    {
        $request->validate([
            'book' => 'required|integer|min:1',
            'chapter' => 'required|integer|min:1',
            'verse' => 'required|integer|min:1',
        ]);

        $data = $bibles->comparisons(
            (int) $request->input('book'),
            (int) $request->input('chapter'),
            (int) $request->input('verse'),
        );

        return response()->json(['data' => $data]);
    }
}
