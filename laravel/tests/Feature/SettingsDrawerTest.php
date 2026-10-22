<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Inertia\Testing\AssertableInertia as Assert;
use Tests\TestCase;

/**
 * The settings drawer is the only account affordance now, and it offers a
 * guest nothing but Sign in and Sign up. These assert the server side of that:
 * the shared prop the drawer branches on, the gate behind every link it offers
 * a signed-in reader, and the deep link its Sign up button uses.
 */
class SettingsDrawerTest extends TestCase
{
    use RefreshDatabase;

    public function test_a_guest_reading_a_chapter_is_shared_no_user(): void
    {
        $this->get('/read')
            ->assertOk()
            ->assertInertia(fn (Assert $page) => $page
                ->component('Read')
                ->where('auth.user', null));
    }

    public function test_the_links_the_drawer_withholds_from_a_guest_are_gated(): void
    {
        $this->get('/profile')->assertRedirect('/login');
        $this->get('/bookmarks')->assertRedirect('/login');
    }

    public function test_a_signed_in_reader_can_reach_every_link_the_drawer_offers(): void
    {
        $user = User::factory()->create();

        $this->actingAs($user)->get('/profile')
            ->assertOk()
            ->assertInertia(fn (Assert $page) => $page->component('Profile'));

        $this->actingAs($user)->get('/bookmarks')
            ->assertOk()
            ->assertInertia(fn (Assert $page) => $page->component('Bookmarks'));

        $this->actingAs($user)->post('/logout')->assertRedirect();
        $this->assertGuest();
    }

    /**
     * The drawer's Sign up button links here. Which tab opens is decided in
     * the browser from `?mode=register`, so all the server owes it is the
     * Login page; this asserts the query string does not change that.
     */
    public function test_the_sign_up_links_query_string_still_serves_the_login_page(): void
    {
        $this->get('/login?mode=register')
            ->assertOk()
            ->assertInertia(fn (Assert $page) => $page->component('Login'));
    }

    public function test_the_register_endpoint_behind_sign_up_creates_an_account(): void
    {
        $this->post('/register', [
            'name' => 'New Reader',
            'email' => 'new-reader@example.test',
            'password' => 'password',
            'password_confirmation' => 'password',
        ])->assertRedirect();

        $this->assertAuthenticated();
        $this->assertDatabaseHas('users', ['email' => 'new-reader@example.test']);
    }
}
