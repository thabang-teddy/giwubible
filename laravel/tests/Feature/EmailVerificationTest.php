<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Auth\Notifications\VerifyEmail;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Notification;
use Illuminate\Support\Facades\URL;
use Inertia\Testing\AssertableInertia as Assert;
use Tests\TestCase;

/**
 * Email verification on the web side.
 *
 * The shape of the feature: reading never needs an account, so verification
 * guards bookmarks and nothing else. The profile stays open while unverified
 * on purpose — a reader who mistyped their address has to be able to fix it.
 */
class EmailVerificationTest extends TestCase
{
    use RefreshDatabase;

    public function test_registering_sends_a_verification_link_and_lands_on_the_notice(): void
    {
        Notification::fake();

        $this->post('/register', [
            'name' => 'Test User',
            'email' => 'test@example.com',
            'password' => 'password',
            'password_confirmation' => 'password',
        ])->assertRedirect('/email/verify');

        $this->assertAuthenticated();

        $user = User::whereEmail('test@example.com')->firstOrFail();
        $this->assertFalse($user->hasVerifiedEmail());
        Notification::assertSentTo($user, VerifyEmail::class);
    }

    public function test_the_notice_names_the_address_the_link_went_to(): void
    {
        $user = User::factory()->unverified()->create(['email' => 'reader@example.com']);

        $this->actingAs($user)
            ->get('/email/verify')
            ->assertOk()
            ->assertInertia(fn (Assert $page) => $page
                ->component('VerifyEmail')
                ->where('email', 'reader@example.com'));
    }

    public function test_the_notice_redirects_a_verified_reader_to_the_reader(): void
    {
        $this->actingAs(User::factory()->create())
            ->get('/email/verify')
            ->assertRedirect('/read');
    }

    public function test_an_unverified_reader_is_sent_to_the_notice_instead_of_the_bookmarks(): void
    {
        $this->actingAs(User::factory()->unverified()->create())
            ->get('/bookmarks')
            ->assertRedirect('/email/verify');
    }

    public function test_an_unverified_reader_cannot_save_a_bookmark(): void
    {
        $this->actingAs(User::factory()->unverified()->create())
            ->post('/bookmarks', [
                'bible' => 't_kjv',
                'book' => 1,
                'chapter' => 1,
                'verse' => 1,
                'text' => 'In the beginning God created the heaven and the earth.',
            ])
            ->assertRedirect('/email/verify');

        $this->assertDatabaseCount('bookmarks', 0);
    }

    public function test_a_verified_reader_still_reaches_the_bookmarks(): void
    {
        $this->actingAs(User::factory()->create())
            ->get('/bookmarks')
            ->assertOk();
    }

    public function test_reading_needs_no_verified_address(): void
    {
        $this->actingAs(User::factory()->unverified()->create())
            ->get('/read')
            ->assertOk();
    }

    public function test_the_signed_link_verifies_the_address(): void
    {
        $user = User::factory()->unverified()->create();

        $this->actingAs($user)
            ->get($this->verificationUrl($user))
            ->assertRedirect('/read');

        $this->assertTrue($user->fresh()->hasVerifiedEmail());
    }

    public function test_an_unsigned_link_is_refused(): void
    {
        $user = User::factory()->unverified()->create();

        $this->actingAs($user)
            ->get('/email/verify/'.$user->id.'/'.sha1($user->email))
            ->assertForbidden();

        $this->assertFalse($user->fresh()->hasVerifiedEmail());
    }

    public function test_someone_elses_link_does_not_verify_your_address(): void
    {
        $target = User::factory()->unverified()->create();
        $attacker = User::factory()->unverified()->create();

        $this->actingAs($attacker)
            ->get($this->verificationUrl($target))
            ->assertForbidden();

        $this->assertFalse($target->fresh()->hasVerifiedEmail());
        $this->assertFalse($attacker->fresh()->hasVerifiedEmail());
    }

    public function test_a_reader_can_ask_for_another_link(): void
    {
        Notification::fake();
        $user = User::factory()->unverified()->create();

        $this->actingAs($user)
            ->from('/email/verify')
            ->post('/email/verification-notification')
            ->assertRedirect('/email/verify')
            ->assertSessionHas('success');

        Notification::assertSentTo($user, VerifyEmail::class);
    }

    public function test_an_already_verified_reader_is_sent_no_further_links(): void
    {
        Notification::fake();
        $user = User::factory()->create();

        $this->actingAs($user)
            ->post('/email/verification-notification')
            ->assertRedirect('/read');

        Notification::assertNothingSent();
    }

    public function test_the_profile_stays_reachable_while_unverified(): void
    {
        $this->actingAs(User::factory()->unverified()->create())
            ->get('/profile')
            ->assertOk();
    }

    public function test_changing_the_address_restarts_verification(): void
    {
        Notification::fake();
        $user = User::factory()->create();

        $this->actingAs($user)
            ->put('/profile', ['email' => 'moved@example.com'])
            ->assertSessionHas('success');

        $user->refresh();
        $this->assertSame('moved@example.com', $user->email);
        $this->assertFalse($user->hasVerifiedEmail());
        Notification::assertSentTo($user, VerifyEmail::class);
    }

    public function test_changing_only_the_name_keeps_the_address_verified(): void
    {
        Notification::fake();
        $user = User::factory()->create();

        $this->actingAs($user)
            ->put('/profile', ['name' => 'New Name'])
            ->assertSessionHas('success');

        $this->assertTrue($user->fresh()->hasVerifiedEmail());
        Notification::assertNothingSent();
    }

    public function test_pages_know_whether_the_address_is_verified(): void
    {
        $this->actingAs(User::factory()->unverified()->create())
            ->get('/read')
            ->assertInertia(fn (Assert $page) => $page->where('auth.user.email_verified', false));

        $this->actingAs(User::factory()->create())
            ->get('/read')
            ->assertInertia(fn (Assert $page) => $page->where('auth.user.email_verified', true));
    }

    /**
     * How an Android or desktop reader actually verifies.
     *
     * They have no web session: the link opens a browser, `auth` sends them to
     * the login page, and the intended URL has to survive the sign-in or the
     * link is spent for nothing.
     */
    public function test_a_link_opened_without_a_session_verifies_after_signing_in(): void
    {
        $user = User::factory()->unverified()->create();
        $url = $this->verificationUrl($user);

        $this->get($url)->assertRedirect('/login');
        $this->assertFalse($user->fresh()->hasVerifiedEmail());

        $this->post('/login', ['email' => $user->email, 'password' => 'password'])
            ->assertRedirect($url);

        $this->get($url)->assertRedirect('/read');

        $this->assertTrue($user->fresh()->hasVerifiedEmail());
    }

    /**
     * The resend endpoint and the link have separate budgets. Sharing one —
     * which an unnamed `throttle:6,1` would do, since its key is just the
     * user — would let a few impatient presses lock the reader out of the
     * link they were waiting for.
     */
    public function test_asking_for_links_does_not_lock_the_reader_out_of_the_link(): void
    {
        Notification::fake();
        $user = User::factory()->unverified()->create();
        $url = $this->verificationUrl($user);

        for ($i = 0; $i < 6; $i++) {
            $this->actingAs($user)->post('/email/verification-notification');
        }

        $this->actingAs($user)
            ->post('/email/verification-notification')
            ->assertStatus(429);

        $this->actingAs($user)->get($url)->assertRedirect('/read');

        $this->assertTrue($user->fresh()->hasVerifiedEmail());
    }

    public function test_bookmarks_are_not_shared_with_an_unverified_reader(): void
    {
        $user = User::factory()->unverified()->create();
        $user->bookmarks()->create([
            'bible' => 't_kjv',
            'book' => 1,
            'chapter' => 1,
            'verse' => 1,
            'text' => 'In the beginning God created the heaven and the earth.',
        ]);

        // The row exists, but the reader cannot reach /bookmarks, so the
        // navbar badge must not count it either.
        $this->actingAs($user)
            ->get('/read')
            ->assertInertia(fn (Assert $page) => $page->where('bookmarks', []));

        $user->forceFill(['email_verified_at' => now()])->save();

        $this->actingAs($user)
            ->get('/read')
            ->assertInertia(fn (Assert $page) => $page->count('bookmarks', 1));
    }

    /** The link Laravel's own notification builds. */
    private function verificationUrl(User $user): string
    {
        return URL::temporarySignedRoute('verification.verify', now()->addMinutes(60), [
            'id' => $user->getKey(),
            'hash' => sha1($user->getEmailForVerification()),
        ]);
    }
}
