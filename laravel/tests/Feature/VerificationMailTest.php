<?php

namespace Tests\Feature;

use App\Models\User;
use App\Support\VerificationMail;
use Illuminate\Auth\Notifications\VerifyEmail;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Notification;
use Tests\TestCase;

/**
 * The one place verification links are sent from.
 *
 * Its job is to protect two parties the route throttles cannot: the person
 * being mailed, whose address is chosen by whoever is signed in, and the
 * reader whose account would otherwise be left half-created by a mail
 * transport that is down.
 */
class VerificationMailTest extends TestCase
{
    use RefreshDatabase;

    public function test_a_link_goes_to_an_unverified_address(): void
    {
        Notification::fake();
        $user = User::factory()->unverified()->create();

        $this->assertTrue(VerificationMail::sendTo($user));

        Notification::assertSentTo($user, VerifyEmail::class);
    }

    public function test_an_already_verified_address_is_sent_nothing(): void
    {
        Notification::fake();

        $this->assertFalse(VerificationMail::sendTo(User::factory()->create()));

        Notification::assertNothingSent();
    }

    public function test_one_address_only_gets_so_many_links(): void
    {
        Notification::fake();
        $user = User::factory()->unverified()->create();

        for ($i = 0; $i < 5; $i++) {
            $this->assertTrue(VerificationMail::sendTo($user), "send number $i was refused");
        }

        $this->assertFalse(VerificationMail::sendTo($user));

        Notification::assertSentToTimes($user, VerifyEmail::class, 5);
    }

    /**
     * The attack the cap exists for: one signed-in account moving a stranger's
     * address on and off its own profile to mail them over and over. The route
     * throttle is keyed on the caller, so it does not help the recipient.
     */
    public function test_moving_one_address_on_and_off_a_profile_stops_mailing_it(): void
    {
        Notification::fake();
        $user = User::factory()->create();

        for ($i = 0; $i < 8; $i++) {
            $user->updateProfile(['email' => 'victim@example.com']);
            $user->updateProfile(['email' => "mine$i@example.com"]);
        }

        // Sixteen moves, but the victim's address is only mailed five times.
        // Their own addresses are each new, so those are not capped.
        Notification::assertSentToTimes($user, VerifyEmail::class, 13);
    }

    public function test_the_cap_ignores_the_case_of_the_address(): void
    {
        Notification::fake();
        $lower = User::factory()->unverified()->create(['email' => 'reader@example.com']);

        for ($i = 0; $i < 5; $i++) {
            VerificationMail::sendTo($lower);
        }

        $upper = User::factory()->unverified()->create(['email' => 'READER@example.net']);
        $upper->forceFill(['email' => 'Reader@Example.com'])->save();

        $this->assertFalse(VerificationMail::sendTo($upper));
    }

    public function test_two_different_addresses_do_not_share_a_budget(): void
    {
        Notification::fake();
        $spent = User::factory()->unverified()->create(['email' => 'one@example.com']);
        $fresh = User::factory()->unverified()->create(['email' => 'two@example.com']);

        for ($i = 0; $i < 5; $i++) {
            VerificationMail::sendTo($spent);
        }

        $this->assertFalse(VerificationMail::sendTo($spent));
        $this->assertTrue(VerificationMail::sendTo($fresh));
    }

    /**
     * A transport that is down must not take the request with it: the user row
     * is written first, so a 500 here would leave an account that exists,
     * cannot be verified, and whose address is now taken.
     */
    public function test_a_mail_transport_failure_does_not_break_registration(): void
    {
        config(['mail.default' => 'a-mailer-that-does-not-exist']);

        $this->post('/register', [
            'name' => 'Test User',
            'email' => 'test@example.com',
            'password' => 'password',
            'password_confirmation' => 'password',
        ])->assertRedirect('/email/verify');

        $this->assertAuthenticated();
        $this->assertDatabaseHas('users', ['email' => 'test@example.com']);
    }

    public function test_a_mail_transport_failure_does_not_break_an_address_change(): void
    {
        $user = User::factory()->create();
        config(['mail.default' => 'a-mailer-that-does-not-exist']);

        $this->actingAs($user)
            ->put('/profile', ['email' => 'moved@example.com'])
            ->assertSessionHasNoErrors();

        $user->refresh();
        $this->assertSame('moved@example.com', $user->email);
        $this->assertFalse($user->hasVerifiedEmail());
    }
}
