<?php

declare(strict_types=1);

namespace App\Support;

use App\Models\User;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\RateLimiter;
use Illuminate\Support\Str;
use Throwable;

/**
 * The one place a verification link is sent from.
 *
 * Every path that mails a link — registering, changing the address on the
 * profile, and the two resend endpoints — goes through here, because the two
 * guarantees below have to hold for all of them:
 *
 * 1. A capped number of links per address per hour. Route throttles are keyed
 *    on the caller, so on their own they still let one account mail hundreds
 *    of links to a stranger by alternating the address on its profile. This
 *    cap is keyed on the recipient instead, which is what protects the person
 *    being mailed — and the sending mailbox's reputation with it.
 *
 * 2. A mail transport that is down does not take the request with it. The user
 *    row is written before the link is sent; a 500 at that point would leave
 *    an account that exists, cannot be verified, and whose address is now
 *    taken. Logging and carrying on leaves the reader with a resend button
 *    that works once the transport recovers.
 */
final class VerificationMail
{
    /** Links one address may receive per [WINDOW_SECONDS]. */
    private const PER_WINDOW = 5;

    private const WINDOW_SECONDS = 3600;

    /**
     * Sends the verification link to [$user]'s current address.
     *
     * Returns false when nothing was sent: the address is already verified,
     * or it has had its share of links for now.
     */
    public static function sendTo(User $user): bool
    {
        if ($user->hasVerifiedEmail()) {
            return false;
        }

        $outcome = RateLimiter::attempt(
            key: self::keyFor($user->getEmailForVerification()),
            maxAttempts: self::PER_WINDOW,
            callback: static function () use ($user): void {
                try {
                    $user->sendEmailVerificationNotification();
                } catch (Throwable $e) {
                    Log::error('Could not send a verification link.', [
                        'user_id' => $user->getKey(),
                        'exception' => $e->getMessage(),
                    ]);
                }
            },
            decaySeconds: self::WINDOW_SECONDS,
        );

        return $outcome !== false;
    }

    /**
     * How many more links this user's address may receive in the window.
     *
     * The notice page asks before claiming a link was just sent. A capped
     * send is otherwise invisible: registration fires the notification
     * through an event, so the controller never sees the `false` that
     * [sendTo] returned, and the reader is left waiting for mail that was
     * never going to arrive. That is the same shape of failure as a broken
     * mailer, and it should not be left for anyone to debug twice.
     */
    public static function remainingFor(User $user): int
    {
        return RateLimiter::remaining(
            self::keyFor($user->getEmailForVerification()),
            self::PER_WINDOW,
        );
    }

    /** Seconds until the cap frees up, for telling the reader how long. */
    public static function secondsUntilAvailable(User $user): int
    {
        return RateLimiter::availableIn(self::keyFor($user->getEmailForVerification()));
    }

    /**
     * Hashed and case-folded: the cap must not be sidestepped by changing the
     * case of an address, and the key should not carry the address itself
     * into the cache store.
     */
    private static function keyFor(string $email): string
    {
        return 'verification-mail:'.sha1(Str::lower($email));
    }
}
