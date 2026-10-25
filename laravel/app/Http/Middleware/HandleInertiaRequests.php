<?php

declare(strict_types=1);

namespace App\Http\Middleware;

use Illuminate\Http\Request;
use Inertia\Middleware;
use Tighten\Ziggy\Ziggy;

class HandleInertiaRequests extends Middleware
{
    /**
     * The root template loaded on the first page visit.
     */
    protected $rootView = 'app';

    /**
     * Props shared with every Inertia response.
     *
     * @return array<string, mixed>
     */
    public function share(Request $request): array
    {
        $user = $request->user();

        return array_merge(parent::share($request), [
            'auth' => [
                'user' => $user ? [
                    'id' => $user->id,
                    'name' => $user->name,
                    'email' => $user->email,
                    // Drives the "verify your email" banner. Shared rather
                    // than fetched so every page can show it without a round
                    // trip, including the reader itself.
                    'email_verified' => $user->hasVerifiedEmail(),
                ] : null,
            ],
            // The signed-in user's bookmarks, shared so the reader's verse
            // state and the navbar badge stay in sync across every page.
            //
            // Gated on verification like the bookmark routes themselves, so
            // the badge cannot show rows the reader is not allowed to reach.
            'bookmarks' => fn () => $user && $user->hasVerifiedEmail()
                ? $user->bookmarks()->latest()->get(['id', 'bible', 'book', 'chapter', 'verse', 'text'])
                : [],
            'flash' => [
                'success' => fn () => $request->session()->get('success'),
                'error' => fn () => $request->session()->get('error'),
            ],
            'ziggy' => fn () => [
                ...(new Ziggy)->toArray(),
                'location' => $request->url(),
            ],
        ]);
    }
}
