<?php

declare(strict_types=1);

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Http\Response;

class BookmarkController extends Controller
{
    public function index(Request $request): JsonResponse
    {
        $bookmarks = $request->user()->bookmarks()->latest()->get();

        return response()->json($bookmarks);
    }

    public function store(Request $request): JsonResponse
    {
        $data = $request->validate([
            'bible' => ['required', 'string', 'max:64'],
            'book' => ['required', 'integer', 'min:1'],
            'chapter' => ['required', 'integer', 'min:1'],
            'verse' => ['required', 'integer', 'min:1'],
            'text' => ['required', 'string'],
        ]);

        $bookmark = $request->user()->bookmarks()->firstOrCreate(
            ['bible' => $data['bible'], 'book' => $data['book'], 'chapter' => $data['chapter'], 'verse' => $data['verse']],
            $data,
        );

        return response()->json($bookmark, 201);
    }

    public function destroy(Request $request, int $id): Response
    {
        $request->user()->bookmarks()->findOrFail($id)->delete();

        return response()->noContent();
    }
}
