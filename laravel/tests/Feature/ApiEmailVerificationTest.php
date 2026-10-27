<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Auth\Notifications\VerifyEmail;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Notification;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

/**
 * Email verification as the Android and desktop apps meet it.
 *
 * A token is issued whether or not the address is verified — the apps need it
 * to read, and `/auth/me` is how they learn which kind they hold. Only the
 * bookmark endpoints are closed, and they answer a JSON request with a 403.
 */
class ApiEmailVerificationTest extends TestCase
{
    use RefreshDatabase;

    public function test_registering_issues_a_token_and_sends_a_link(): void
    {
        Notification::fake();

        $response = $this->postJson('/api/auth/register', [
            'name' => 'App Reader',
            'email' => 'app@example.com',
            'password' => 'password',
            'password_confirmation' => 'password',
        ])->assertCreated();

        $this->assertNotEmpty($response->json('token'));
        $this->assertNull($response->json('user.email_verified_at'));

        Notification::assertSentTo(
            User::whereEmail('app@example.com')->firstOrFail(),
            VerifyEmail::class,
        );
    }

    public function test_signing_in_unverified_still_returns_a_working_token(): void
    {
        $user = User::factory()->unverified()->create();

        $response = $this->postJson('/api/auth/login', [
            'email' => $user->email,
            'password' => 'password',
        ])->assertOk();

        $this->assertNotEmpty($response->json('token'));
        $this->assertNull($response->json('user.email_verified_at'));
    }

    public function test_the_account_endpoint_reports_the_unverified_state(): void
    {
        Sanctum::actingAs(User::factory()->unverified()->create());

        $this->getJson('/api/auth/me')
            ->assertOk()
            ->assertJson(['email_verified_at' => null]);
    }

    public function test_an_unverified_token_is_refused_by_every_bookmark_endpoint(): void
    {
        Sanctum::actingAs(User::factory()->unverified()->create());

        $expected = ['message' => 'Your email address is not verified.'];

        $this->getJson('/api/bookmarks')->assertForbidden()->assertJson($expected);

        $this->postJson('/api/bookmarks', [
            'bible' => 't_kjv',
            'book' => 1,
            'chapter' => 1,
            'verse' => 1,
            'text' => 'In the beginning God created the heaven and the earth.',
        ])->assertForbidden()->assertJson($expected);

        $this->deleteJson('/api/bookmarks/1')->assertForbidden()->assertJson($expected);

        $this->assertDatabaseCount('bookmarks', 0);
    }

    public function test_a_verified_token_reaches_the_bookmarks(): void
    {
        Sanctum::actingAs(User::factory()->create());

        $this->getJson('/api/bookmarks')->assertOk();

        $this->postJson('/api/bookmarks', [
            'bible' => 't_kjv',
            'book' => 1,
            'chapter' => 1,
            'verse' => 1,
            'text' => 'In the beginning God created the heaven and the earth.',
        ])->assertSuccessful();

        $this->assertDatabaseCount('bookmarks', 1);
    }

    public function test_reading_endpoints_stay_public(): void
    {
        $this->getJson('/api/books')->assertOk();
        $this->getJson('/api/bibles')->assertOk();
    }

    public function test_an_app_can_ask_for_another_link(): void
    {
        Notification::fake();
        $user = User::factory()->unverified()->create();
        Sanctum::actingAs($user);

        $this->postJson('/api/auth/email/verification-notification')
            ->assertStatus(202)
            ->assertJson(['verified' => false]);

        Notification::assertSentTo($user, VerifyEmail::class);
    }

    public function test_asking_again_when_already_verified_sends_nothing(): void
    {
        Notification::fake();
        Sanctum::actingAs(User::factory()->create());

        $this->postJson('/api/auth/email/verification-notification')
            ->assertOk()
            ->assertJson(['verified' => true]);

        Notification::assertNothingSent();
    }

    public function test_changing_the_address_through_the_api_restarts_verification(): void
    {
        Notification::fake();
        $user = User::factory()->create();
        Sanctum::actingAs($user);

        $this->putJson('/api/auth/profile', ['email' => 'moved@example.com'])
            ->assertOk()
            ->assertJson(['email' => 'moved@example.com', 'email_verified_at' => null]);

        $this->assertFalse($user->fresh()->hasVerifiedEmail());
        Notification::assertSentTo($user, VerifyEmail::class);
    }
}
