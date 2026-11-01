<?php

declare(strict_types=1);

namespace App\Http\Controllers\Web;

use App\Http\Controllers\Controller;
use App\Support\VerificationMail;
use Illuminate\Foundation\Auth\EmailVerificationRequest;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Inertia\Inertia;
use Inertia\Response;

/**
 * The web half of email verification.
 *
 * Reading needs no account, so nothing here blocks the reader: only the
 * bookmark routes sit behind the `verified` middleware, and a reader who
 * never verifies simply keeps reading signed out of their bookmarks.
 */
class EmailVerificationController extends Controller
{
    /** "Check your inbox" — where the `verified` middleware sends people. */
    public function notice(Request $request): Response|RedirectResponse
    {
        if ($request->user()->hasVerifiedEmail()) {
            return redirect()->intended(route('read'));
        }

        $user = $request->user();

        return Inertia::render('VerifyEmail', [
            'email' => $user->email,
            // False when the address has had its share of links for now. The
            // page must not claim one was just sent when it was not: that is
            // what makes a capped registration indistinguishable from a
            // broken mailer.
            'canResend' => VerificationMail::remainingFor($user) > 0,
            'retryMinutes' => (int) ceil(VerificationMail::secondsUntilAvailable($user) / 60),
        ]);
    }

    /**
     * The signed link from the email.
     *
     * `EmailVerificationRequest` checks the id and hash against the signed-in
     * user, so a link forwarded to someone else's browser is refused.
     */
    public function verify(EmailVerificationRequest $request): RedirectResponse
    {
        if ($request->user()->hasVerifiedEmail()) {
            return redirect()->route('read')->with('success', 'Your email address is already verified.');
        }

        $request->fulfill();

        return redirect()->route('read')->with('success', 'Thanks — your email address is verified.');
    }

    /** Sends another link, for the one that expired or never arrived. */
    public function resend(Request $request): RedirectResponse
    {
        if ($request->user()->hasVerifiedEmail()) {
            return redirect()->route('read');
        }

        if (! VerificationMail::sendTo($request->user())) {
            return back()->with(
                'error',
                'We have already sent several links to that address. Please check your inbox, including spam, and try again later.',
            );
        }

        return back()->with('success', 'We have sent you another verification link.');
    }
}
