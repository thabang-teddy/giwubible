<?php

declare(strict_types=1);

namespace App\Http\Controllers\Web;

use App\Http\Controllers\Controller;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Inertia\Inertia;
use Inertia\Response;

class BookmarkController extends Controller
{
    /** The bookmarks list. Data itself is a shared Inertia prop. */
    public function index(): Response
    {
        return Inertia::render('Bookmarks');
    }

    public function store(Request $request): RedirectResponse
    {
        $data = $request->validate([
            'bible' => ['required', 'string', 'max:64'],
            'book' => ['required', 'integer', 'min:1'],
            'chapter' => ['required', 'integer', 'min:1'],
            'verse' => ['required', 'integer', 'min:1'],
            'text' => ['required', 'string'],
        ]);

        $request->user()->bookmarks()->firstOrCreate(
            ['bible' => $data['bible'], 'book' => $data['book'], 'chapter' => $data['chapter'], 'verse' => $data['verse']],
            $data,
        );

        return back();
    }

    public function destroy(Request $request, int $id): RedirectResponse
    {
        $request->user()->bookmarks()->findOrFail($id)->delete();

        return back();
    }
}
