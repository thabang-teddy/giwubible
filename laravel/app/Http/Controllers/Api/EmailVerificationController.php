<?php

declare(strict_types=1);

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\User;
use App\Support\VerificationMail;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/**
 * Lets the Android and desktop apps ask for another verification link.
 *
 * The link itself always opens the web app — a signed URL is cheap to verify
 * in a browser and keeps the apps out of the signature business. Once the
 * reader has followed it, the app learns about it from `GET /auth/me`, whose
 * `email_verified_at` field answers the question.
 */
class EmailVerificationController extends Controller
{
    public function send(Request $request): JsonResponse
    {
        /** @var User $user */
        $user = $request->user();

        if ($user->hasVerifiedEmail()) {
            return response()->json([
                'message' => 'Your email address is already verified.',
                'verified' => true,
            ]);
        }

        if (! VerificationMail::sendTo($user)) {
            return response()->json([
                'message' => 'We have already sent several links to that address. Please check your inbox, including spam, and try again later.',
                'verified' => false,
            ], 429);
        }

        return response()->json([
            'message' => 'Verification email sent.',
            'verified' => false,
        ], 202);
    }
}
